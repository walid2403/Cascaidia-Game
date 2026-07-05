package service.bot

import entity.GameState
import entity.Player
import entity.Tile
import entity.TurnOptions
import entity.WildlifeToken
import service.RootService
import kotlin.collections.iterator
import kotlin.collections.plusAssign

class HeuristicBot(private val rootService: RootService) {

    fun makeTurn() {
        val currentGame = rootService.currentGame
        checkNotNull(currentGame) { "Es existiert kein Spiel" }
        val player = currentGame.playerQueue.peek()
        checkNotNull(player) { "Es existiert kein Spiel" }

        val legalTurns = mutableListOf(TurnOptions.MAKE_SELECTION)
        if (player.natureTokens > 0) {
            legalTurns += TurnOptions.NATURE_TOKEN_FREE_SELECTION
            legalTurns += TurnOptions.NATURE_TOKEN_CHANGE_WILDLIFE
        }
        if (currentGame.choices.map { it.second }.groupBy { it }.entries.maxOfOrNull { it.value.size } == 3) {
            legalTurns += TurnOptions.CLEAR_SEMIPOPULATION
        }

        while (legalTurns.isNotEmpty()) {
            val turn = legalTurns.first() // Später wählen wir hier schlauer aus

            when (turn) {
                // Aktuell lassen wir die unfertigen Züge leer oder brechen ab, damit nichts abstürzt
                TurnOptions.PLACE_HABITAT_TILE -> heuristicBotPlaceHabitatTile(player)
                TurnOptions.PLACE_WILDLIFE_TOKEN -> heuristicBotPlaceWildlifeToken(player)
                TurnOptions.MAKE_SELECTION -> heuristicBotChooseMarketPair(player)


                else -> { legalTurns.clear(); currentGame.gameState = GameState.END_OF_TURN }
            }
        }
    }

    private fun getPossibleTilePositions(player: Player): List<Triple<Int, Int, Int>> {
        val possiblePositions = mutableListOf<Triple<Int, Int, Int>>()

        for (entry in player.board) {
            for (i in listOf(-1, 1)) {
                var option = Triple(entry.key.first, entry.key.second + i, entry.key.third - i)
                if (option !in possiblePositions && player.board[option] == null) possiblePositions += option

                option = Triple(entry.key.first + i, entry.key.second, entry.key.third - i)
                if (option !in possiblePositions && player.board[option] == null) possiblePositions += option

                option = Triple(entry.key.first + i, entry.key.second - i, entry.key.third)
                if (option !in possiblePositions && player.board[option] == null) possiblePositions += option
            }
        }

        return possiblePositions
    }

    private fun heuristicBotPlaceHabitatTile(player: Player) {
        val currentGame = rootService.currentGame
        checkNotNull(currentGame)

        val selectedTileIndex = currentGame.selectedChoice.first
        val selectedTile = currentGame.choices[selectedTileIndex].first

        //gibt alle leere plätze im Board zurück
        val possiblePositions = getPossibleTilePositions(player)


        val bestPosition = possiblePositions.maxByOrNull { position ->
            evaluateHabitatPosition(position, selectedTile, player)
        }

        if (bestPosition != null) {
            rootService.playerActionService.placeTile(bestPosition)
        } else { //falls keine freien Plätze mehr gibt
            currentGame.gameState = GameState.END_OF_TURN
        }
    }

    private fun evaluateHabitatPosition(position: Triple<Int, Int, Int>, newTile: Tile, player: Player): Int {
        var score = 0

        //1. wir berechnen die 6 direkten Nachbar-Koordinaten dieses Bauplatzes
        val neighbours = mutableListOf<Triple<Int,Int,Int>>()
        for (i in listOf(-1,1)){
            neighbours.add(Triple(position.first, position.second + i, position.third - i))
            neighbours.add(Triple(position.first + i, position.second, position.third - i))
            neighbours.add(Triple(position.first + i, position.second - i, position.third))
        }

        //2. Wir schauen uns jeden Nachbarn an
        for (neighborPos in neighbours){
            val neighborTile = player.board[neighborPos]

            //Liegt auf diesem Nachbarfeld überhaupt schon ein Plättchen?
            if(neighborTile != null){

                //3. wir vergleichen die Landschaften!
                for(myHabitat in newTile.habs){
                    if(neighborTile.habs.contains(myHabitat)){
                        // Treffer! Eine passende Landschaft grenzt an.
                        // Wir geben 10 Punkte dafür.
                        score += 10
                    }
                }
            }
        }

        // Optionaler Bonus: Wenn das Plättchen Platz für viele Tiere hat, ist es auch gut!
        // (Wir geben 1 Punkt pro möglichem Tier, als kleinen Tie-Breaker, wenn Landschaften gleich gut sind)
        score += newTile.possibles.size
        return score
    }


    private fun heuristicBotPlaceWildlifeToken(player: Player) {
        val currentGame = rootService.currentGame
        checkNotNull(currentGame)

        //welches Tier besitzt der Bot gerade?
        val selectedWildlife = currentGame.choices[currentGame.selectedChoice.second].second

        //1. Alle gültigen Plätze finden
        val possiblePositions = player.board.entries
            .filter { it.value.occupant == null && selectedWildlife in it.value.possibles }
            .map { it.key }

        //Falls die Liste leer ist beenden wir den Zug (das Tier wird verworfen)
        if(possiblePositions.isEmpty()){ //muss updated !!!!!!!!!!!!!!!!!!!!!!!!
            currentGame.gameState = GameState.END_OF_TURN
            return
        }

        //2. Den besten Platz berechnen
        val bestPosition = possiblePositions.maxByOrNull { position ->
            evaluateWildlifePosition(position, selectedWildlife, player)
        }

        //3.Tier platzieren
        if(bestPosition != null){
            rootService.playerActionService.placeWildlife(bestPosition)
        } else {
            currentGame.gameState = GameState.END_OF_TURN
        }
    }

    private fun evaluateWildlifePosition(position: Triple<Int, Int, Int>, wildlife: WildlifeToken, player: Player): Int {
        var score = 0

        // Da fast alle Tiere auf ihre Nachbarn achten, berechnen wir die hier einmal zentral
        val neighborPositions = listOf(
            Triple(position.first, position.second - 1, position.third + 1),
            Triple(position.first + 1, position.second - 1, position.third),
            Triple(position.first + 1, position.second, position.third - 1),
            Triple(position.first, position.second + 1, position.third - 1),
            Triple(position.first - 1, position.second + 1, position.third),
            Triple(position.first - 1, position.second, position.third + 1)
        )

        // Wir sammeln alle Tiere, die direkt angrenzen (das brauchen wir für die Bewertung)
        val neighborAnimals = mutableListOf<WildlifeToken>()
        for (neighborPos in neighborPositions) {
            val occupant = player.board[neighborPos]?.occupant
            if (occupant != null) {
                neighborAnimals.add(occupant)
            }
        }

        val currentGame = rootService.currentGame
        checkNotNull(currentGame)

        // Wir holen uns den Wertungs-Typ für das aktuelle Tier.
        // wildlife.ordinal gibt uns die Position im Enum (z.B. BEAR = 0, ELK = 1)
        val isTypeA = currentGame.scoringCards[wildlife.ordinal]

        //Jetzt verteilen wir Punkte je nach Tierart
        when(wildlife) {
            WildlifeToken.BEAR -> {
                // Wie viele Bären liegen direkt auf den 6 Nachbarfeldern?
                val bearNeighbors = neighborAnimals.count{it == WildlifeToken.BEAR}

                if(isTypeA){
                    // === BÄR TYP A (Paare) ===
                    when (bearNeighbors) {
                        1->score += 50 //PERFEKT! Wir bilden genau ein Paar. Hohe Priorität!
                        0->score += 10 //Okay, wir fangen ein neues Paar an.
                        else->score -= 100 //SCHLECHT! 2 oder mehr Bären.
                    }

                } else {
                    // === BÄR TYP B (Dreiergruppen) ===
                    when(bearNeighbors) {
                        2->score += 50 // PERFEKT! Wir legen den 3. Bär an. Gruppe komplett!
                        1->score += 20 // Gut, wir erweitern einen Bären zu einem Zweier-Grüppchen.
                        0->score += 10 // Okay, wir fangen eine neue Gruppe an.
                        else->score -= 100 // SCHLECHT! Zu viele Bären auf einem Haufen.
                    }
                }
            }
            WildlifeToken.ELK -> {
                // Wie viele Hirsche berühren diesen Platz?
                val elkNeighbors = neighborAnimals.count{it == WildlifeToken.ELK}

                if(isTypeA){
                    // === HIRSCH TYP A (Linien) ===
                    when (elkNeighbors) {
                        0->score += 10 // Okay, wir fangen eine neue Linie an.
                        1->score += 30 // PERFEKT! Wir verlängern eine Linie an einem Ende.
                        2->score -= 10// GEFÄHRLICH! Könnte die Mitte einer Linie sein, lieber vermeiden
                        else->score -= 100 //  SCHLECHT! 3 Nachbarn machen definitiv die Linie kaputt.

                    }
                } else {
                    // === HIRSCH TYP B (Gruppen) ===
                    // Je mehr Hirsche er berührt, desto besser!
                    score += elkNeighbors * 20
                }
            }
            WildlifeToken.SALMON -> {
                // Wie viele Lachse schwimmen auf den angrenzenden Feldern?
                val salmonNeighbors = neighborAnimals.count { it == WildlifeToken.SALMON }

                if(isTypeA){
                    // === LACHS TYP A (Möglichst lange Flüsse) ===
                    when (salmonNeighbors) {
                        1 -> score += 50 // Wir setzen den Lachs an das Ende eines Flusses und verlängern ihn.
                        2 -> score += 20 // Wir verbinden zwei Lachse, verhindert aber keine Kreise!
                        0 -> score += 10 // Wir starten einen ganz neuen Lachs-Fluss.
                        else -> score -= 100 // 3 oder mehr Lachse bilden einen Knotenpunkt (Y-Kreuzung).
                    }
                } else {
                    // === LACHS TYP B (Flüsse bestimmter Länge) ===
                    // Die Grundregel bleibt gleich: keine Kreuzungen!
                    // Wir können die Punkteverteilung ähnlich lassen, da der Bot einfach versuchen soll,
                    // gültige Linien zu bilden, ohne sich selbst zu blockieren.
                    when (salmonNeighbors) {
                        1 -> score += 40
                        2 -> score += 10
                        0 -> score += 10
                        else -> score -= 100
                    }
                }
            }
            WildlifeToken.HAWK -> { //type B noch nicht fertig
                if(isTypeA){
                    // === HAWK TYP A (Absoluter Einzelgänger) ===
                    if(WildlifeToken.HAWK in neighborAnimals){
                        score -= 100 // KATASTROPHE! Bussarde nebeneinander machen die Wertung kaputt.
                    } else {
                        score += 50 // PERFEKT! Der Bussard hat sein eigenes Revier.
                    }

                } else {
                    // === HAWK TYP B ===
                    if(WildlifeToken.HAWK in neighborAnimals){
                        score -= 100 // KATASTROPHE! Bussarde nebeneinander machen die Wertung kaputt.
                    } else {
                        // Wir prüfen die "Sichtlinie" mit Distanz 2 in alle 6 Richtungen
                        val sightLinePositions = listOf(
                            Triple(position.first, position.second - 2, position.third + 2),
                            Triple(position.first + 2, position.second - 2, position.third),
                            Triple(position.first + 2, position.second, position.third - 2),
                            Triple(position.first, position.second + 2, position.third - 2),
                            Triple(position.first - 2, position.second + 2, position.third),
                            Triple(position.first - 2, position.second, position.third + 2)
                        )
                        var seesHawk = false
                        for (sightPos in sightLinePositions){
                            if(player.board[sightPos]?.occupant == WildlifeToken.HAWK){
                                seesHawk = true
                            }
                        }
                        if(seesHawk){
                            score += 50 // SUPER! Er sieht einen anderen Bussard in gerader Linie
                        } else {
                            score += 20 //Okay, aber nicht optimal
                        }
                    }
                }
            }

            WildlifeToken.FOX -> {
                if(isTypeA){
                    // === FUCHS TYP A (Verschiedene Tiere) ===
                    // .distinct() filtert alle doppelten Tiere heraus.
                    // Wenn 3 verschiedene Tiere angrenzen, bringt das 3 × 10 = 30 Punkte.
                    val uniqueNeighborsCount = neighborAnimals.distinct().size
                    score += uniqueNeighborsCount * 10
                } else {
                    // === FUCHS TYP B (Tier-Paare) ===
                    // Wir zählen, wie oft jedes Tier vorkommt und schauen, ob es mindestens 2 sind.
                    //wir erzeugen ein map Key: Tier -> [tier, tier]
                    val animalsCounts = neighborAnimals.groupBy { it }
                    var pairs = 0
                    // Wir gehen jeden Eintrag (Tier -> Liste) durch
                    for (entry in animalsCounts) {
                        // entry.value ist direkt die Liste, wir müssen nicht nochmal in der Map suchen!
                        if (entry.value.size >= 2) {
                            pairs++
                        }
                    }
                    // Für jedes angrenzende Paar gibt es 20 Punkte
                    score += pairs * 20
                }

            }
        }

        return score
    }

    private fun heuristicBotChooseMarketPair(player: Player){
        val currentGame = rootService.currentGame
        checkNotNull(currentGame)

        var bestScore = -1000 /// Ein sehr niedriger Startwert
        var bestIndex = 0 //Hier merken wir uns, welches der 4 Paare gewinnt

        // Wir gehen die 4 ausliegenden Paare im Markt durch (Index 0 bis 3)
        for (i in currentGame.choices.indices){
            val marketTile = currentGame.choices[i].first
            val marketAnimal = currentGame.choices[i].second

            // === 1. Bester Score für das Landschaftsplättchen ===
            // (Wir suchen alle leeren Nachbarfelder auf dem Board)
            val possibleHabitatPositions = getPossibleTilePositions(player)

            // maxOfOrNull gibt uns direkt den HÖCHSTEN Score zurück, den dieses Plättchen erzielen kann
            val maxHabitatScore = possibleHabitatPositions.maxOfOrNull{ pos ->
                evaluateHabitatPosition(pos,marketTile,player)} ?: 0

            // === 2. Bester Score für das Tier ===
            // (Wir suchen alle Plätze, wo dieses Tier legal liegen darf)
            val possibleAnimalPositions = player.board.entries
                .filter { it.value.occupant == null && marketAnimal in it.value.possibles }
                .map { it.key }

            val maxAnimalScore = possibleAnimalPositions.maxOfOrNull { pos ->
                evaluateWildlifePosition(pos, marketAnimal, player)
            } ?: 0

            // === 3. Gesamtpunkte vergleichen ===
            val totalScore = maxHabitatScore + maxAnimalScore

            // Wenn dieses Paar besser ist als unser bisheriges bestes, merken wir es uns!
            if (totalScore > bestScore) {
                bestScore = totalScore
                bestIndex = i
            }
        }
        // === 4. Dem Spiel unsere Entscheidung mitteilen ===
        rootService.playerActionService.selectColumn(bestIndex)

    }
}