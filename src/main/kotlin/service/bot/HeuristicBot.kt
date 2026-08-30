package service.bot

import entity.GameState
import entity.Player
import entity.Tile
import entity.WildlifeToken
import service.RootService

/**
 * This Bot works with predetermined weights to decide which move is best at the current time
 * @param rootService Link to the [RootService] class
 * @param bot Link to the [Bot] class
 * @property rootService Link to the [RootService] class
 * @property bot Link to the [Bot] class
 */

class HeuristicBot(private val rootService: RootService, private val bot: Bot) {

    var isFinished = false

    //die 6 Richtungen eines Hexagons im Uhrzeigersinn
    private val directions = listOf(
        Triple(1,-1,0), //Richtung 0: Oben-Rechts
        Triple(1,0,-1), //Richtung 1: Rechts
        Triple(0,1,-1), //Richtung 2: Unten-Rechts
        Triple(-1,1,0), //Richtung 3: Unten-Links
        Triple(-1,0,1), //Richtung 4: Links
        Triple(0,-1,1)  //Richtung 5: Oben-Links
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
                isFinished = true
            }
        }
    }

    /**
     * Analysiert die Tiertoken im Markt auf Überpopulation (3 oder 4 gleiche Tiere).
     * Bei 4 gleichen Tieren wird der Markt zwingend ersetzt. Bei 3 gleichen Tieren entscheidet
     * eine Heuristik basierend auf dem potenziellen Punktgewinn, ob der Tausch sinnvoll ist.
     *
     * @param player Der KI-Spieler, dessen Board für die Punkte-Evaluierung herangezogen wird.
     * @param isStartOfTurn Gibt an, ob wir uns im START_OF_TURN befinden
     * (erlaubt freiwilliges Wischen bei 3 gleichen).
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

        // Wie viele Schritte nach RECHTS müssen wir von der AKTUELLEN
        // Rotation aus machen, um die Test-Rotation zu erreichen?
        val additionalTurns = (testRotation - newTile.rotation + 6) % 6

        // 2. Wir prüfen alle 6 Kanten
        for(dirIndex in 0..5){
            val offset = directions[dirIndex]
            val neighborPos = Triple(position.first+offset.first, position.second+offset.second,
                position.third+offset.third)
            val neighborTile = player.board[neighborPos]

            if(neighborTile != null){
                // 3. Welches Habitat landet nach unseren Drehungen an dieser Kante?
                // Da rotateTile(true) nach rechts verschiebt, ziehen wir turnsNeeded ab.
                val myEdgeIndex = (dirIndex - additionalTurns + 6) % 6
                val myHabitat = newTile.habs[myEdgeIndex]

                // 4. Welche Kante des Nachbarn zeigt zu uns zurück?
                val neighborDirIndex = (dirIndex + 3) % 6

                // 5. KEINE Rotation vom Nachbarn abziehen! Das Framework hat
                // das Array des Nachbarn beim Legen bereits physisch rotiert.
                val neighborHabitat = neighborTile.habs[neighborDirIndex]

                // 6. Wenn die Landschaften exakt zusammenpassen -> Volle Punkte
                if(myHabitat == neighborHabitat){
                    score += 15
                }
            }
        }

        // Bonuspunkte für Flexibilität (viele mögliche Tiere)
        score += newTile.possibles.size
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
                //currentGame.gameState = GameState.END_OF_TURN
                //isFinished = true
            }

        }

        // SICHERER RE-FIX: Wir berechnen die exakte Anzahl an Rechts-Drehungen,
        // die nötig sind, um von der jetzigen Rotation zur target-Rotation zu kommen.
        val currentRotation = selectedTile.rotation
        val turnsNeeded = (bestRotation - currentRotation + 6) % 6

        for(i in 0 until turnsNeeded){
            rootService.playerActionService.rotateTile(true)
        }
        bot.coordinatesTile = bestPosition
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
        if(possiblePositions.isEmpty()){ //muss updated !!!!!!!!!!!!!!!!!!!!!!!!
            //currentGame.gameState = GameState.END_OF_TURN
            isFinished = true
            return
        }

        //2. Den besten Platz berechnen
        val bestPosition = possiblePositions.maxByOrNull { position ->
            evaluateWildlifePosition(position, selectedWildlife, player)
        }


        //3.Tier platzieren
        if(bestPosition != null){
            bot.coordinatesWildlifeToken = bestPosition
            rootService.playerActionService.placeWildlife(bestPosition)
            isFinished = true
        } else {
            //currentGame.gameState = GameState.END_OF_TURN
            isFinished = true
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
            WildlifeToken.BEAR -> scoreBear(neighborAnimals, isTypeA, position, player)
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
        val neighborPositions = getNeighborPositions(position)

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

    /** gibt eine liste zurück, die die Nachbarn platz enthält */
    private fun getNeighborPositions(position: Triple<Int, Int, Int>): List<Triple<Int, Int, Int>> {
        return listOf(
            Triple(position.first, position.second - 1, position.third + 1),
            Triple(position.first + 1, position.second - 1, position.third),
            Triple(position.first + 1, position.second, position.third - 1),
            Triple(position.first, position.second + 1, position.third - 1),
            Triple(position.first - 1, position.second + 1, position.third),
            Triple(position.first - 1, position.second, position.third + 1)
        )
    }

    /**
     * Bär-Bewertung. Typ A belohnt Paare (genau 1 Nachbar), Typ B Dreiergruppen (genau 2 Nachbarn).
     * Mehr als die angestrebte Nachbarzahl wird stark bestraft, um zu große Cluster zu vermeiden.
     */
    private fun scoreBear(neighborAnimals: List<WildlifeToken>, isTypeA: Boolean,
                          position: Triple<Int, Int, Int>,
                          player : Player): Int {
        val bearNeighbors = neighborAnimals.count { it == WildlifeToken.BEAR }

        if (isTypeA) {
            // === BÄR TYP A (Genau Paare) ===
            if (bearNeighbors == 0) return 10
            if (bearNeighbors > 1) return -100 // 2+ Nachbarn sind sofort schlecht (3+ Gruppe)

            // Wir haben genau 1 Bären als direkten Nachbarn.
            // ABER: Hat dieser Bär vielleicht schon einen ANDEREN Bären?
            val neighborBearPos = getNeighborPositions(position).first { pos ->
                player.board[pos]?.occupant == WildlifeToken.BEAR
            }

            // Wir zählen die Bären, die an unseren zukünftigen Partner grenzen
            val neighborsOfNeighborCount = getNeighborAnimals(neighborBearPos, player)
                .count { it == WildlifeToken.BEAR }

            if (neighborsOfNeighborCount > 0) {
                return -100 // KATASTROPHE: Der Nachbar ist schon in einer Beziehung! Wir würden eine 3er-Gruppe bauen.
            }

            return 50 // PERFEKT: Beide Bären sind noch Single, wir bilden ein sauberes Paar.

        }/* else {
            // === BÄR TYP B (Genau 3er Gruppen) ===
            if (bearNeighbors == 0) return 10
            if (bearNeighbors > 2) return -100 // 4er Gruppe vermeiden

            if (bearNeighbors == 1) {
                // Wir berühren 1 Bären. Hat der schon Nachbarn?
                val neighborBearPos = getNeighborPositions(position).first { pos ->
                    player.board[pos]?.occupant == WildlifeToken.BEAR
                }
                val neighborsOfNeighborCount = getNeighborAnimals(neighborBearPos, player)
                    .count { it == WildlifeToken.BEAR }

                return when (neighborsOfNeighborCount) {
                    0 -> 20 // Wir machen aus einem Single-Bär ein 2er Grüppchen.
                    1 -> 50 // PERFEKT: Er hat schon einen, wir sind der 3. Bär, der die Gruppe abschließt!
                    else -> -100 // Er hat schon 2 oder mehr. Wenn wir uns anlegen, werden es 4+ Bären.
                }
            }

            if (bearNeighbors == 2) {
                // Wir füllen eine Lücke zwischen 2 Bären. Wenn diese beiden noch ANDERE Bären
                // außerhalb unserer direkten Reichweite berühren, wird die Gruppe zu groß.
                val neighborBearPositions = getNeighborPositions(position).filter { pos ->
                    player.board[pos]?.occupant == WildlifeToken.BEAR
                }

                for (nbPos in neighborBearPositions) {
                    val bearsConnectedToNeighbor = getNeighborPositions(nbPos).filter { p ->
                        player.board[p]?.occupant == WildlifeToken.BEAR
                    }
                    // Zähle Bären, die NICHT zu den direkten Nachbarn unseres Feldes gehören
                    val outsideBears = bearsConnectedToNeighbor.count { it !in neighborBearPositions }

                    if (outsideBears > 0) return -100 // Gruppe würde auf 4+ wachsen!
                }
                return 50 // PERFEKT: Wir schließen die Lücke und bilden genau eine 3er-Gruppe.
            }
        }*/
        return 0
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
        //Fox dürfen nicht neben einander sein
        if (WildlifeToken.FOX in neighborAnimals) {
            return -100
        }
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

/**
 * sopra-gitlab.package-registry.token=glpat-QR-8dwWiy3pN9p_FBMz7eW86MQp1OjE3Mwk.01.0z0oiawrb
 *
 * token access
 */