package service.bot

import entity.GameState
import entity.Player
import entity.Tile
import entity.WildlifeToken
import service.RootService

/**
 * Steuert die Logik für einen KI-Spieler (Bot) in Cascadia basierend auf Heuristiken.
 * * Der Bot bewertet Spielsituationen anhand von festen Punktewerten (Scores) und
 * entscheidet so, welches Markt-Paar gewählt, wo das Landschaftsplättchen angelegt
 * und wo das Tiertoken platziert wird. Er nutzt zudem Natur-Zapfen, falls ein
 * normaler Zug zu schlecht bewertet wird.
 * * @property rootService Referenz auf den Haupt-Service für den Zugriff auf den Spielstatus.
 */
class HeuristicBot(private val rootService: RootService) {

    //die 6 Richtungen eines Hexagons im Uhrzeigersinn
    private val directions = listOf(
        Triple(0,-1,1), //Richtung 0: Oben-Rechts
        Triple(1,-1,0), //Richtung 1: Rechts
        Triple(1,0,-1), //Richtung 2: Unten-Rechts
        Triple(0,1,-1), //Richtung 3: Unten-Links
        Triple(-1,1,0), //Richtung 4: Links
        Triple(-1,0,1)  //Richtung 5: Oben-Links
    )

    /**
     * Hauptmethode für den Zug des Bots.
     * Prüft zunächst auf Überpopulation (3 oder 4 gleiche Tiere im Markt).
     * Leitet danach die Ausführung an die spezifische Methode weiter,
     * je nachdem, in welchem [GameState] sich das Spiel gerade befindet.
     */
    fun makeTurn() {
        val currentGame = rootService.currentGame
        checkNotNull(currentGame) { "Es existiert kein Spiel" }
        val player = currentGame.playerQueue.peek()
        checkNotNull(player) { "Es existiert kein Spiel" }

        //Die richtige Action ausführen
        //wir schauen einfach, in welchem Status das Spiel gerade ist
        when(currentGame.gameState){

            GameState.START_OF_TURN-> {
                // Überpopulation prüfen (nur beim echten Start des Zuges erlaubt!)
                if (handleOverPopulation(player, isStartOfTurn = true)) {
                    return
                }
                heuristicBotChooseMarketPair(player)
            }
            GameState.HAS_EXTERMINATED-> {
                // Wenn bereits automatisch gewischt wurde, darf der Spieler laut Framework
                // kein freiwilliges Wischen (3 gleiche) mehr triggern. Wir prüfen nur noch auf 4 gleiche.
                if (handleOverPopulation(player, isStartOfTurn = false)) {
                    return
                }
                heuristicBotChooseMarketPair(player)
            }

            GameState.MADE_CHOICE -> {
                // Wenn das Spiel auf das Plättchen wartet
                heuristicBotPlaceHabitatTile(player)
            }

            GameState.PLAYED_TILE -> {
                // Wenn das Spiel auf das Tier wartet
                heuristicBotPlaceWildlifeToken(player)
            }

            else -> {
                // Falls das Spiel auf etwas anderes wartet (z.B. Zug-Ende), leiten wir das Ende ein
                currentGame.gameState = GameState.END_OF_TURN
            }
        }
    }

    /**
     * Analysiert die Tiertoken im Markt auf Überpopulation (3 oder 4 gleiche Tiere).
     * Bei 4 gleichen Tieren wird der Markt zwingend ersetzt. Bei 3 gleichen Tieren entscheidet
     * eine Heuristik basierend auf dem potenziellen Punktgewinn, ob der Tausch sinnvoll ist.
     *
     * @param player Der KI-Spieler, dessen Board für die Punkte-Evaluierung herangezogen wird.
     * @param isStartOfTurn Gibt an, ob wir uns im START_OF_TURN befinden (erlaubt freiwilliges Wischen bei 3 gleichen).
     * @return true, wenn der Markt wegen Überpopulation modifiziert wurde und der Zug pausiert,
     * false, wenn keine Überpopulation vorliegt oder kein Tausch durchgeführt wurde.
     */
    private fun handleOverPopulation(player : Player, isStartOfTurn : Boolean): Boolean {
        val currentGame = rootService.currentGame
        checkNotNull(currentGame)
        // Überpopulation prüfen (vor jedem regulären Zug)
        val animalsCounts = currentGame.choices.map { it.second }.groupBy { it }
        // groupBy erzeugt eine Map,
        // in der gleiche WildlifeTokens als Schlüssel gruppiert und in Listen gespeichert werden
        // Elvis Operator, da mir nicht sicher bin, ob die Liste leer sein Kann
        val maxCount = animalsCounts.values.maxOfOrNull { it.size } ?:0

        if(maxCount == 4){
            //exterminate ist automatisch vom Spiel
            /** Achtung! exterminate darf nur 2 Mal aufgerufen werden */
            rootService.gameService.exterminate(false)
            return true // Wir beenden makeTurn() hier. Das Spiel ruft den Bot danach automatisch neu auf.
        } else if (maxCount == 3 && isStartOfTurn) {
            //Bei 3 KANN getauscht werden.
            // 1. Welches Tier ist 3-mal da?
            val semiPopulatedAnimal = animalsCounts.entries.firstOrNull { it.value.size == 3 }?.key

            // Wir suchen das 4. Tier auch (das nur 1-mal im Markt liegt).
            val otherAnimal = animalsCounts.entries.firstOrNull { it.value.size == 1 }?.key

            if(semiPopulatedAnimal != null){ //es kann nicht null sein!
                // A. Check: Wie gut ist das Tier, das 3-mal da ist?
                // 2. Wo könnte das Tier liegen und was bringt es?
                val possibleAnimalPositions = player.board.entries
                    .filter { it.value.occupant == null && semiPopulatedAnimal in it.value.possibles }
                    .map{it.key}

                val maxAnimalScore = possibleAnimalPositions.maxOfOrNull { pos ->
                    evaluateWildlifePosition(pos,semiPopulatedAnimal,player) } ?:0

                // B. Check: Wie gut ist das 4. Einzel-Tier?
                var maxOtherAnimalScore = 0
                if(otherAnimal != null){
                    val possibleOtherAnimalPositions = player.board.entries
                        .filter {it.value.occupant == null && otherAnimal in it.value.possibles }
                        .map{it.key}

                    maxOtherAnimalScore = possibleOtherAnimalPositions.maxOfOrNull { pos ->
                        evaluateWildlifePosition(pos, otherAnimal,player) } ?:0
                }

                // 3. Wenn das Tier uns weniger als 15 Punkte bringt, fegen wir es weg!
                // (15 ist ein guter Wert, da ein sehr guter Zug oft 20-50 Punkte bringt)
                if(maxAnimalScore < 15 && maxOtherAnimalScore < 15){
                    rootService.gameService.exterminate(true)
                    return true//Zug beendet, Bot wird mit neuem Markt wieder aufgerufen.
                }
            }

        }
        return false
    }

    /**
     * Berechnet den Heuristik-Score für ein Landschaftsplättchen an einer spezifischen Position
     * mit einer bestimmten Rotation.
     * Bonuspunkte gibt es für perfekt passende Kanten (Landschaften) und Flexibilität (mögliche Tiere).
     * @param position Die Ziel-Koordinate auf dem Tableau.
     * @param newTile Das anzulegende Landschaftsplättchen.
     * @param testRotation Die zu testende Drehung (0 bis 5).
     * @param player Der Spieler (Bot).
     * @return Ein Integer-Wert, der angibt, wie gut dieser Zug wäre.
     */
    private fun evaluateHabitatPositionWithRotation(
        position : Triple<Int, Int, Int>,
        newTile : Tile,
        testRotation: Int,
        player: Player
    ): Int{
        var score = 0

        //wir prüfen alle 6 Kanten
        for(dirIndex in 0..5){
            val offset = directions[dirIndex]
            //alle NachbarTile vom newTile suchen
            val neighborPos = Triple(position.first+offset.first, position.second+offset.second
                , position.third+offset.third)
            val neighborTile = player.board[neighborPos]

            if(neighborTile != null){
                //1. welche Kante von unserem Plättchen zeigt in diese Richtung?
                val myEdgeIndex = (dirIndex - testRotation + 6) % 6 //art vom Tile im Index (z.B. Wald)
                val myHabitat = newTile.habs[myEdgeIndex] // z.B. im Index x gibt ein Berg

                //2.welche Kante des Nachbarn Zeigt zu uns zurück
                val neighborDirIndex = (dirIndex + 3) % 6 //wenn dirIndex 0 ist, ist sein nachbar 3
                val neighborHabitat = neighborTile.habs[neighborDirIndex] // z.B. im Index x gibt es auch ein Berg

                //3.wenn die Landschaften exakt zusammenpassen -> Volle Punkte
                if(myHabitat == neighborHabitat){
                    score += 15
                }
            }
        }
        score += newTile.possibles.size //je mehr WildlifeToken die Liste enthält, desto besser
        return score
    }

    /**
     * Evaluiert alle möglichen Positionen und Rotationen für das aktuell gewählte Plättchen.
     * Führt anschließend die Rotationen im Spiel aus und platziert das Plättchen.
     * @param player Der Spieler (Bot).
     */
    private fun heuristicBotPlaceHabitatTile(player: Player) {
        /** wie kann ich wissen, dass auf einem Tile ein nature Token gibt */
        val currentGame = rootService.currentGame
        checkNotNull(currentGame)

        val selectedTileIndex = currentGame.selectedChoice.first
        val selectedTile = currentGame.choices[selectedTileIndex].first

        //gibt alle leere plätze im Board zurück
        val possiblePositions = getPossibleTilePositions(player)

        var bestPosition = possiblePositions.firstOrNull() //rand fäller
            ?: error("Kein gültiger Platz zum Platzieren eines Plättchens")
        var bestRotation = 0
        var maxScore = -1000

        //für jeden freien Platz testen wir alle 6 Rotationen
        for(position in possiblePositions){
            for(rot in 0..5){
                val score = evaluateHabitatPositionWithRotation(position,selectedTile,rot,player)
                if (score > maxScore){
                    maxScore = score
                    bestPosition = position
                    bestRotation = rot
                }
            }

        }

        //Berechnen, wie oft wir im Uhrzeigersinn drehen müssen
        val currentRot = selectedTile.rotation //falls currentRot == 4
        val turnsNeeded = (bestRotation - currentRot + 6) % 6 //muss 4 Mal umgedreht werden

        for(i in 0 until turnsNeeded){
            rootService.playerActionService.rotateTile(true)
        }

        rootService.playerActionService.placeTile(bestPosition)

    }

    /**
     * Sucht alle aktuell unbelegten, aber angrenzenden Hexagon-Felder auf dem Tableau.
     * @param player Der Spieler (Bot), dessen Tableau durchsucht wird.
     * @return Eine Liste von Koordinaten (Triples), an denen ein neues Plättchen angelegt werden kann.
     */
    private fun getPossibleTilePositions(player: Player): List<Triple<Int, Int, Int>> {
        val possiblePositions = mutableListOf<Triple<Int, Int, Int>>()

        for (entry in player.board) {
            for (i in listOf(-1, 1)) {
                //Blick nach oben Rechts und unten Links
                var option = Triple(entry.key.first, entry.key.second + i, entry.key.third - i)
                if (option !in possiblePositions && player.board[option] == null) possiblePositions += option

                //Blick nach oben Links und unten Rechts
                option = Triple(entry.key.first + i, entry.key.second, entry.key.third - i)
                if (option !in possiblePositions && player.board[option] == null) possiblePositions += option

                //Blick nach Rects und Links
                option = Triple(entry.key.first + i, entry.key.second - i, entry.key.third)
                if (option !in possiblePositions && player.board[option] == null) possiblePositions += option
            }
        }

        return possiblePositions
    }

    /**
     * Sucht den besten Platz für das aktuell gewählte Tiertoken.
     * Wenn kein Platz verfügbar ist, wird das Tier abgeworfen und der Zug beendet.
     * @param player Der Spieler (Bot).
     */
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
        if(possiblePositions.isEmpty()){
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
            currentGame.gameState = GameState.END_OF_TURN
        } else {
            currentGame.gameState = GameState.END_OF_TURN
        }
    }

    /**
     * Berechnet den Heuristik-Score für ein Tier an einer spezifischen Position.
     * Berücksichtigt die aktiven Wertungskarten (Typ A oder B) und bewertet,
     * wie gut das Tier mit seinen direkten Nachbarn harmoniert.
     * @param position Die Position, an der das Tier platziert werden soll.
     * @param wildlife Die Tierart, die bewertet wird.
     * @param player Der Spieler (Bot).
     * @return Ein Integer-Wert für die Güte dieses Spielzugs.
     */
    private fun evaluateWildlifePosition(position: Triple<Int, Int, Int>, wildlife: WildlifeToken,
                                         player: Player): Int{

        val neighborAnimals = getNeighborAnimals(position, player)

        val currentGame = rootService.currentGame
        checkNotNull(currentGame)

        // Wir holen uns den Wertungs-Typ für das aktuelle Tier.
        // wildlife.ordinal gibt uns die Position im Enum (z.B. BEAR = 0, ELK = 1)
        val isTypeA = currentGame.scoringCards[wildlife.ordinal]

        //Jetzt verteilen wir Punkte je nach Tierart
        return when(wildlife) {
            WildlifeToken.BEAR -> scoreBear(neighborAnimals, isTypeA)
            WildlifeToken.ELK -> scoreElk(neighborAnimals, isTypeA)
            WildlifeToken.SALMON -> scoreSalmon(neighborAnimals, isTypeA)
            WildlifeToken.HAWK -> scoreHawk(neighborAnimals, isTypeA, position, player)
            WildlifeToken.FOX -> scoreFox(neighborAnimals, isTypeA)
        }
    }

    /**
     * Sammelt alle Tiertoken, die auf den 6 direkt angrenzenden Feldern liegen.
     * @param position Die Ausgangsposition.
     * @param player Der Spieler, dessen Board durchsucht wird.
     * @return Liste der angrenzenden [WildlifeToken] (leere Felder werden ignoriert).
     */
    private fun getNeighborAnimals(
        position: Triple<Int, Int, Int>,
        player: Player
    ): List<WildlifeToken> {
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
        return neighborAnimals
    }

    /**
     * Bär-Bewertung. Typ A belohnt Paare (genau 1 Nachbar), Typ B Dreiergruppen (genau 2 Nachbarn).
     * Mehr als die angestrebte Nachbarzahl wird stark bestraft, um zu große Cluster zu vermeiden.
     */
    private fun scoreBear(neighborAnimals: List<WildlifeToken>, isTypeA: Boolean): Int {
        val bearNeighbors = neighborAnimals.count { it == WildlifeToken.BEAR }
        return if (isTypeA) {
            // === BÄR TYP A (Paare) ===
            when (bearNeighbors) {
                1 -> 50    // PERFEKT! Wir bilden genau ein Paar.
                0 -> 10    // Okay, wir fangen ein neues Paar an.
                else -> -100 // SCHLECHT! 2 oder mehr Bären.
            }
        } else {
            // === BÄR TYP B (Dreiergruppen) ===
            when (bearNeighbors) {
                2 -> 50    // PERFEKT! Der 3. Bär macht die Gruppe komplett.
                1 -> 20    // Gut, wir erweitern zu einem Zweier-Grüppchen.
                0 -> 10    // Okay, wir fangen eine neue Gruppe an.
                else -> -100 // SCHLECHT! Zu viele Bären auf einem Haufen.
            }
        }
    }

    /**
     * Hirsch-Bewertung. Typ A belohnt gerade Linien (genau 1 Nachbar = Linienende),
     * Typ B belohnt einfach jede weitere angrenzende Hirsch-Verbindung.
     */
    private fun scoreElk(neighborAnimals: List<WildlifeToken>, isTypeA: Boolean): Int {
        val elkNeighbors = neighborAnimals.count { it == WildlifeToken.ELK }
        return if (isTypeA) {
            // === HIRSCH TYP A (Linien) ===
            when (elkNeighbors) {
                0 -> 10    // Okay, wir fangen eine neue Linie an.
                1 -> 30    // PERFEKT! Wir verlängern eine Linie an einem Ende.
                2 -> -10   // GEFÄHRLICH! Könnte die Mitte einer Linie sein.
                else -> -100 // SCHLECHT! 3 Nachbarn machen die Linie kaputt.
            }
        } else {
            // === HIRSCH TYP B (Gruppen) ===
            // Je mehr Hirsche er berührt, desto besser.
            elkNeighbors * 20
        }
    }

    /**
     * Lachs-Bewertung. In beiden Typen möglichst 1 Nachbar (Fluss verlängern);
     * 3+ Nachbarn erzeugen eine Y-Kreuzung und werden bestraft.
     */
    private fun scoreSalmon(neighborAnimals: List<WildlifeToken>, isTypeA: Boolean): Int {
        val salmonNeighbors = neighborAnimals.count { it == WildlifeToken.SALMON }
        return if (isTypeA) {
            // === LACHS TYP A (Möglichst lange Flüsse) ===
            when (salmonNeighbors) {
                1 -> 50    // Lachs an das Ende eines Flusses – Fluss wird länger.
                2 -> 20    // Verbindet zwei Lachse, aber Kreis-Gefahr.
                0 -> 10    // Neuer Lachs-Fluss.
                else -> -100 // 3+ Lachse bilden einen Y-Knoten.
            }
        } else {
            // === LACHS TYP B (Flüsse bestimmter Länge) ===
            when (salmonNeighbors) {
                1 -> 40
                2 -> 10
                0 -> 10
                else -> -100
            }
        }
    }

    /**
     * Bussard-Bewertung. Nachbar-Bussarde sind in beiden Typen katastrophal.
     * Typ A: Einzelgänger. Typ B: Bonus, wenn ein anderer Bussard in Sichtlinie liegt.
     */
    private fun scoreHawk(
        neighborAnimals: List<WildlifeToken>,
        isTypeA: Boolean,
        position: Triple<Int, Int, Int>,
        player: Player
    ): Int {
        // KATASTROPHE! Bussarde direkt nebeneinander machen die Wertung in beiden Typen kaputt.
        if (WildlifeToken.HAWK in neighborAnimals) return -100

        // === HAWK TYP A (Absoluter Einzelgänger) ===
        if (isTypeA) return 50

        // === HAWK TYP B (Sichtlinie) ===
        return if (hasHawkInSightLine(position, player)) 50 else 20
    }

    /**
     * Prüft, ob innerhalb von 2 Feldern in einer der 6 geraden Richtungen ein weiterer Bussard liegt.
     * (Vereinfachte Sichtlinien-Heuristik – die volle Regel erlaubt beliebige Distanz.)
     */
    private fun hasHawkInSightLine(position: Triple<Int, Int, Int>, player: Player): Boolean {
        val sightLinePositions = listOf(
            Triple(position.first, position.second - 2, position.third + 2),
            Triple(position.first + 2, position.second - 2, position.third),
            Triple(position.first + 2, position.second, position.third - 2),
            Triple(position.first, position.second + 2, position.third - 2),
            Triple(position.first - 2, position.second + 2, position.third),
            Triple(position.first - 2, position.second, position.third + 2)
        )
        return sightLinePositions.any { player.board[it]?.occupant == WildlifeToken.HAWK }
    }

    /**
     * Fuchs-Bewertung. Typ A: 10 Punkte pro einzigartiger Nachbar-Tierart.
     * Typ B: 20 Punkte pro angrenzendem Paar gleicher Tiere.
     */
    private fun scoreFox(neighborAnimals: List<WildlifeToken>, isTypeA: Boolean): Int {
        if (isTypeA) {
            // === FUCHS TYP A (Verschiedene Tiere) ===
            val uniqueNeighborsCount = neighborAnimals.distinct().size
            return uniqueNeighborsCount * 10
        }
        // === FUCHS TYP B (Tier-Paare) ===
        val animalsCounts = neighborAnimals.groupBy { it }
        var pairs = 0
        for (entry in animalsCounts) {
            if (entry.value.size >= 2) {
                pairs++
            }
        }
        return pairs * 20
    }

    /**
     * Bewertet alle ausliegenden Markt-Paare (Plättchen + Tier) und wählt die beste aus.
     * Besitzt der Bot einen Natur-Zapfen und ist der normale Markt schlecht bewertet,
     * wird versucht, Plättchen und Tier separat zu wählen (Free Selection) oder den Markt zu tauschen.
     * @param player Der Spieler (Bot).
     */
    private fun heuristicBotChooseMarketPair(player: Player){
        val currentGame = rootService.currentGame
        checkNotNull(currentGame)

        var bestScore = -1000 /// Ein sehr niedriger Startwert
        var bestIndex = 0 //Hier merken wir uns, welches der 4 Paare gewinnt

        // === Bester Score für das Landschaftsplättchen ===
        // (Wir suchen alle leeren Nachbarfelder auf dem Board)
        val possibleHabitatPositions = getPossibleTilePositions(player)

        // Wir gehen die 4 ausliegenden Paare im Markt durch (Index 0 bis 3)
        for (i in currentGame.choices.indices){
            val marketTile = currentGame.choices[i].first
            val marketAnimal = currentGame.choices[i].second

            val totalScore = getMaxTileScore(marketTile, possibleHabitatPositions, player) +
                    getMaxWildlifeScore(marketAnimal, player)

            // Wenn dieses Paar besser ist als unser bisheriges bestes, merken wir es uns!
            if (totalScore > bestScore) {
                bestScore = totalScore
                bestIndex = i
            }
        }

        //Nature Token Logik.
        //wenn der beste normale Zug schlecht ist (< 30 Punkte) und wir Zapfen haben (FreeSelection)
        if(bestScore < 30 && player.natureTokens > 0){
            if (handleNatureTokenSelection(player, possibleHabitatPositions, bestScore)) {
                return // Zug beendet, Bot wartet auf den nächsten State oder frischen Markt
            }
        }

        // === STANDARD ZUG (Wenn kein Zapfen genutzt wird) ===
        rootService.playerActionService.selectColumn(bestIndex)

    }

    /**
     * Kapselt die Logik für den Einsatz eines Natur-Zapfens, wenn der reguläre Markt unbrauchbar ist.
     * Berechnet alle 16 Kombinationen für Free Selection oder tauscht alternativ den Markt durch.
     */
    private fun handleNatureTokenSelection(
        player: Player,
        possibleHabitatPositions: List<Triple<Int, Int, Int>>,
        bestScore: Int
    ): Boolean{
        val currentGame = rootService.currentGame
        checkNotNull(currentGame)
        var bestFreeScore = -1000
        var bestTileIndex = 0
        var bestAnimalIndex = 0

        //wir testen alle 16 Kombinationen (4 Plättchen * 4 Tiere)
        for(x in currentGame.choices.indices){
            for (y in currentGame.choices.indices){
                val tile = currentGame.choices[x].first
                val animal = currentGame.choices[y].second

                val totalFreeScore = getMaxTileScore(tile, possibleHabitatPositions, player) +
                        getMaxWildlifeScore(animal, player)

                if(totalFreeScore > bestFreeScore){
                    bestFreeScore = totalFreeScore
                    bestTileIndex = x
                    bestAnimalIndex = y
                }
            }
        }

        // Lohnt sich der Zapfen? Ein Zapfen bringt am Ende 1 Punkt, aber taktisch ist er wertvoll.
        // Wir nutzen Free Selection nur, wenn es uns mindestens 5 Punkte MEHR bringt als der normale Zug.
        if(bestFreeScore > bestScore + 5){
            rootService.playerActionService.freeSelection(bestTileIndex,bestAnimalIndex)

        }
        else{ //im schlimmsten fall werden alle Tiere getauscht! (geht nun mit Glück).
            // Wenn auch mischen (Free Selection) nichts bringt, sind wohl die Tiere im Markt unbrauchbar.
            // Wir opfern den Zapfen und tauschen ALLE 4 Tiere im Markt aus!
            val indicesToChange = mutableListOf(0, 1, 2, 3)
            rootService.playerActionService.changeWildlife(indicesToChange)

        }
        return true
    }

    /**
     * Berechnet den maximalen Score, den ein bestimmtes Landschaftsplättchen auf allen
     * verfügbaren Positionen in allen Rotationen erzielen kann.
     */
    private fun getMaxTileScore(tile: Tile, positions: List<Triple<Int, Int, Int>>, player: Player): Int {
        return positions.maxOfOrNull { pos ->
            (0..5).maxOf { rot ->
                evaluateHabitatPositionWithRotation(pos, tile, rot, player)
            }
        } ?: 0
    }

    /**
     * Berechnet den maximalen Score, den ein bestimmtes Tiertoken auf allen dafür
     * legalen, freien Plätzen des Boards erzielen kann.
     */
    private fun getMaxWildlifeScore(animal: WildlifeToken, player: Player): Int {
        val possiblePositions = player.board.entries
            .filter { it.value.occupant == null && animal in it.value.possibles }
            .map { it.key }

        return possiblePositions.maxOfOrNull { pos ->
            evaluateWildlifePosition(pos, animal, player)
        } ?: 0
    }
}