package service

import entity.*
import entity.SaveState
import java.io.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.module.SimpleModule
import kotlin.math.max

/**
 * The game service class of the Cascadia Game. It includes all functions which work mostly on the system-logic side
 */

class GameService(private val rootService: RootService) : AbstractRefreshingService() {

    /**
     * A Jackson object mapper configured with a custom `SimpleModule` to handle
     * specific key deserialization needs for JSON Maps.
     *
     * This mapper enables seamless conversion of JSON map keys formatted as strings
     * (e.g., `(1, -1, 0)`) into actual Kotlin `Triple<Int, Int, Int>` objects
     * through the `TripleKeyDeserializer`.
     *
     * The customization is essential for deserializing game-related data structures
     * that involve triples as keys.
     */
    private val mapper = jacksonObjectMapper().apply {
        val module = SimpleModule()
        module.addKeyDeserializer(Triple::class.java, TripleKeyDeserializer())
        registerModule(module)
    }

    /**
     * A function to start a new game from scratch
     *
     * @param playerList A list of the players to be entered into the game. The list must not be longer then four
     * elements or shorter than two elements. The [List] object contains a [Pair] object for every player with the
     * players name as a [String] and the players Type as a [PlayerType]
     * @param scoringCards The scoring cards to be used in the game, as a [List] of [Boolean] objects,
     * see [CascadiaGame.scoringCards]
     *
     * @throws IllegalArgumentException If the list-size is not 2 - 4, if there are duplicate/ blankspace names
     * or if there are not exactly five scoringCards
     * @throws IllegalStateException If there is currently a game running
     */
    fun startNewGame(
        playerList: List<Pair<String, PlayerType>>, scoringCards: List<Boolean>,
        startingTiles: List<Int>? = null, tileIDs: List<Int>? = null, wildlifeBag: List<WildlifeToken>? = null
    ) {

        //Gültigkeiten der Spieleranzahl und Spielernamen überprüfen
        require(playerList.size in 2..4) { "PlayerList size must be between 2 and 4" }
        val playerNames = playerList.map { it.first.trim() }
        require(!playerNames.contains("")) { "ungültige Strings für die Namen" }
        require(playerNames.distinct().size == playerNames.size) { "Duplikate erhalten" }

        //Gültigkeiten ScoringCard anzahl testen
        require(scoringCards.size == 5) { "falsche Anzahl von Scoringcards" }
        val playerTypes = playerList.map { it.second }
        var local = true

        //prüfen ob lokales Spiel
        for (playerType in playerTypes) {
            if (playerType == PlayerType.NETWORK) {
                local = false
                break
            }
        }
        //Spiel initialisieren
        val game = CascadiaGame(scoringCards, local)

        //Spieler zur playerqueue hinzufügen
        for (player in playerList) {
            game.playerQueue.add(Player(player.first.trim(), player.second))
        }

        //aktuelles Spiel auf das initialisierte Spiel setzen
        rootService.currentGame = game

        //Wildlife und Habitatbeutel, Startlandschaften, Shop und natureTokens erstellen
        if (wildlifeBag == null) createWildlifes() else game.wildlifeTokens.pushAll(wildlifeBag)


        createStartingTiles(startingTiles)
        createHabitatStack(tileIDs)
        createChoices()
        game.natureTokens = 25
        //deep copy des Spiels

        rootService.history.prevMoves.clear()
        rootService.history.undoneMoves.clear()

        rootService.history.prevMoves.push(CascadiaGame(game))

        if (game.isLocal) onAllRefreshables { refreshAfterStartGame() }

    }

    /**
     * Funktion erstellt die Starterlandschaften
     */
    private fun createStartingTiles(startingTilesList: List<Int>? = null) {

        val game = rootService.currentGame
        checkNotNull(game) { "Spiel nicht initialisiert" }

        //start_tiles csv als input stream
        val input = javaClass.getResourceAsStream("/start_tiles.csv")
        checkNotNull(input) { "Datei nicht gefunden" }

        //Liste für die Zeilen aus der csv(konkreter nur die tile zeilen)
        val lines = mutableListOf<String>()

        //für jede Zeile in der csv:
        for (line in input.bufferedReader().readLines()) {
            //ignoriere Leerzeilen, die startzeile, die kommentarzeilen
            if (line.isBlank()) {
                continue
            }
            if (line.startsWith("id")) {
                continue
            }
            if (line.startsWith("-")) {
                continue
            }
            // füge rest in lines hinzu
            lines.add(line)
        }
        //Liste für die 3er startingtiles
        val startingTiles = mutableListOf<MutableList<Tile>>()

        var i = 0

        while (i + 2 < lines.size) {
            //eine liste für einen 3er starter
            val starter = mutableListOf<Tile>()
            //die nächsten 3 zeilen werden zu tiles gemacht und in den starter hinzugefügt
            starter.add(createHabitatTile(lines[i], true))
            starter.add(createHabitatTile(lines[i + 1], true))
            starter.add(createHabitatTile(lines[i + 2], true))
            //in die Liste aller startlandschaften hinzufügen
            startingTiles.add(starter)
            i += 3
        }

        if (startingTilesList != null) {
            game.playerQueue.forEachIndexed { i, player ->
                val board = startingTiles[startingTilesList[i] / 10 - 1]

                player.board[Triple(0, 0, 0)] = board[0]
                player.board[Triple(0, 1, -1)] = board[1]
                player.board[Triple(-1, 1, 0)] = board[2]
            }
        } else {
            startingTiles.shuffle()

            for (player in game.playerQueue) {
                val board = startingTiles.removeAt(0)
                /**
                 * Hier nach:       tile1
                 *              tile2  tile3
                 */
                player.board[Triple(0, 0, 0)] = board[0]
                player.board[Triple(0, 1, -1)] = board[1]
                player.board[Triple(-1, 1, 0)] = board[2]
            }
        }
    }

    /**
     * Hilfsmethode um den Habitatstack zu erstellen
     */
    private fun createHabitatStack(tileIDs: List<Int>? = null) {
        val game = rootService.currentGame
        checkNotNull(game) { "Spiel nicht initialisiert" }

        //csv als inputstream
        val input = javaClass.getResourceAsStream("/tiles.csv")
        checkNotNull(input) { "Datei nicht gefunden" }

        //Liste der tiles aus der csv
        val lines = mutableListOf<String>()

        //jede zeile der csv durchgehen
        for (line in input.bufferedReader().readLines()) {
            //ignoriere Leerzeilen, startzeile und Kommentarzeile
            if (line.isBlank()) {
                continue
            }
            if (line.startsWith("id")) {
                continue
            }
            if (line.startsWith("-")) {
                continue
            }

            //Rest(Tiles) in die liste
            lines.add(line)
        }

        if (tileIDs != null) {
            tileIDs.forEach { tileID ->
                val tile = createHabitatTile(lines[tileID])
                game.tileStack.push(tile)
            }
        } else {
            //Tiles mischen
            lines.shuffle()

            // Stackgröße hängt von Spielergröße ab
            val stackSize = game.playerQueue.size * 20 + 3

            // Die zeilen in tiles umwandeln und in den stack hinzufügen
            for (i in 0 until stackSize) {
                val tile = createHabitatTile(lines[i])
                game.tileStack.push(tile)
            }
        }


    }

    /**
     * Hilfsfunktion um den Tierbeutel zuerstellen
     */

    private fun createWildlifes() {
        val game = rootService.currentGame
        checkNotNull(game) { "Spiel nicht initialisiert" }

        game.wildlifeTokens.popAll()

        //genau 20 pro Tier in den Beutel
        repeat(20) { game.wildlifeTokens.push(WildlifeToken.ELK) }
        repeat(20) { game.wildlifeTokens.push(WildlifeToken.FOX) }
        repeat(20) { game.wildlifeTokens.push(WildlifeToken.BEAR) }
        repeat(20) { game.wildlifeTokens.push(WildlifeToken.HAWK) }
        repeat(20) { game.wildlifeTokens.push(WildlifeToken.SALMON) }

        //Beutel mischen
        game.wildlifeTokens.shuffle()
    }

    /**
     * Hilfsmethode um eingelesene Zeile in ein Habitat umzuwandeln
     * @return Habitatstile
     * @param line eine Zeile aus der csv datei die bereits ein String ist
     */
    private fun createHabitatTile(line: String, startingTile: Boolean = false): Tile {

        // Aus line eine Liste machen, welche die 4 Attribute der Bezeichner besitzt
        val parts = line.split(";")


        val id = parts[0].toInt() * if (startingTile) 10 else 1
        val habitats = parts[1]
        val wildlife = parts[2]

        //HabitatString in Liste von Habitaten umwandeln
        val habitatList: MutableList<Habitates> = mutableListOf()

        for (i in 0..5) {
            val habitat = when (habitats[i]) {
                'M' -> Habitates.MOUNTAINS
                'W' -> Habitates.WETLANDS
                'F' -> Habitates.FORESTS
                'R' -> Habitates.RIVERS
                'P' -> Habitates.PRAIRIES
                else -> throw IllegalArgumentException("Unexpected habitat")
            }
            habitatList.add(habitat)
        }

        //mögliche Tiere (String) in Liste von wildlifes umwandeln
        val possibles: MutableList<WildlifeToken> = mutableListOf()

        for (animal in wildlife) {
            val possible = when (animal) {
                'E' -> WildlifeToken.ELK
                'F' -> WildlifeToken.FOX
                'S' -> WildlifeToken.SALMON
                'B' -> WildlifeToken.BEAR
                'H' -> WildlifeToken.HAWK
                else -> throw IllegalArgumentException("Unexpected wildlife")
            }

            possibles.add(possible)
        }

        //Tile erstellen und zurückgeben
        return Tile(id, habitatList, possibles)
    }


    /**
     * Hilfsfunktion für den shop
     */
    private fun createChoices() {
        val game = rootService.currentGame
        checkNotNull(game) { "Spiel nicht initialisiert" }
        // Überpopulation prüfen
        while (game.wildlifeTokens.peekAll(4).distinct().size == 1) {
            createWildlifes()
        }

        // 4 Animal paare in den shop hinzufügen
        repeat(4) {
            val tile = game.tileStack.pop()
            val animal = game.wildlifeTokens.pop()
            game.choices.add(Pair(tile, animal))
        }
    }

    /**
     * Restores a `CascadiaGame` instance from a provided `GameSnapshot`.
     *
     * This method initializes a new `CascadiaGame` object based on the state encapsulated
     * in the given `GameSnapshot`. It restores the game elements such as nature tokens, state,
     * selected choices, players, and token stacks.
     *
     * @param snapshot The `GameSnapshot` object containing the serialized game state to restore.
     * @return A new `CascadiaGame` instance with the restored game state.
     */
    private fun restoreSnapshot(snapshot: GameSnapshot): CascadiaGame {
        val game = CascadiaGame(snapshot.scoringCards, snapshot.isLocal)
        game.natureTokens = snapshot.natureTokens
        game.gameState = snapshot.gameState
        game.selectedChoice = snapshot.selectedChoice

        game.choices.clear()
        game.choices.addAll(snapshot.choicesList)
        game.removedTokens.clear()
        game.removedTokens.addAll(snapshot.removedTokensList)

        snapshot.tileStackList.reversed().forEach { game.tileStack.push(it) }
        snapshot.wildlifeTokensList.reversed().forEach { game.wildlifeTokens.push(it) }

        game.playerQueue.clear()
        game.playerQueue.addAll(snapshot.playerQueue)

        return game
    }

    /**
     * A function to load a previously saved game. The saved game is identified by the name Parameter.
     *
     * @param name The name of the previously saved game
     *
     * @throws IllegalArgumentException If the name is empty or if there isn't a saved game with the entered name
     * @throws IllegalStateException If there is currently a game running
     */
    fun loadGame(name: String) {

        if (rootService.currentGame != null) throw IllegalStateException("Es läuft bereits ein Spiel.")
        if (name.isBlank()) throw IllegalArgumentException("Der Name darf nicht leer sein.")

        val file = File(RootService.SAVE_DIRECTORY, "$name${RootService.SAVE_EXTENSION}")
        if (!file.exists()) throw IllegalArgumentException("Spielstand '$name' existiert nicht.")

        val loadedState: SaveState = mapper.readValue(file)

        rootService.currentGame = restoreSnapshot(loadedState.currentGame)

        rootService.history.prevMoves.clear()
        loadedState.prevMovesList.forEach { snapshot ->
            rootService.history.prevMoves.push(restoreSnapshot(snapshot))
        }

        rootService.history.undoneMoves.clear()
        loadedState.undoneMovesList.forEach { snapshot ->
            rootService.history.undoneMoves.push(restoreSnapshot(snapshot))
        }

        onAllRefreshables { refreshAfterLoadGame() }
    }

    /**
     * this functions resolves the overpopulation of the wildlife tokens
     *
     * If all four wildlife tokens are identical, they are automatically
     * removed and replaced. If only three identical wildlife tokens are present,
     * the current player may choose whether to remove and replace them if the gameState is [GameState.START_OF_TURN].
     *
     * Removed wildlife tokens are temporarily set aside
     *
     * The game ends if there are not enough wildlifeTokens available
     *
     * recursively calls itself if there are 4 tokens of the same type at the end of the function
     *
     * @param playerTrigger indicates whether the extermination is initiated by the player (true)
     * or automatically by the game (false)
     *
     * @throws IllegalStateException If the gameState is not [GameState.START_OF_TURN] or [GameState.HAS_EXTERMINATED]
     * or if there are not at least 3 tokens of the same type or
     * if there are 3 and the current gameState is not [GameState.START_OF_TURN]
     */
    fun exterminate(playerTrigger: Boolean) {
        val game = rootService.currentGame ?: error("No current game")
//        check(
//            game.gameState == GameState.START_OF_TURN ||
//                    game.gameState == GameState.HAS_EXTERMINATED
//        ) { "Extermination is not allowed in the current game state" }
        if (playerTrigger && game.gameState != GameState.START_OF_TURN) {
            throw IllegalStateException("Player can only exterminate at the START_OF_TURN.")
        }
        //counting the wildlife tokens
        val wildlifeTokens = game.choices.map { it.second }
        var duplicatedToken: WildlifeToken? = null
        var highestCount = 0
        for (token in WildlifeToken.entries) {
            val count = wildlifeTokens.count { it == token }
            if (count > highestCount) {
                highestCount = count
                duplicatedToken = token
            }
        }

        if (highestCount < 4) {
            if (!playerTrigger) {
                return
            }
        }
//        check(highestCount >= 3) {
//            "There are not at least three identical wildlife tokens"
//        }
//        if (playerTrigger) {
//            if (highestCount != 3) {
//                throw IllegalStateException("Player extermination requires exactly three identical wildlife tokens")
//            }
//        } else {
//            if (highestCount < 4) throw IllegalStateException("Automatic extermination requires four identical wildlife tokens")
//        }
        val affectedIndices = mutableListOf<Int>()
        for (i in game.choices.indices) {
            if (game.choices[i].second == duplicatedToken) {
                affectedIndices.add(i)
            }
        }
        if (game.wildlifeTokens.size < affectedIndices.size) {
            calculateScores()
            return
        }
        //executing extermination
        for (j in affectedIndices) {
            game.removedTokens.add(game.choices[j].second)
            val tile = game.choices[j].first
            val newToken = game.wildlifeTokens.pop()
            game.choices[j] = Pair(tile, newToken)
        }
        if (playerTrigger) {
            game.gameState = GameState.HAS_EXTERMINATED
        }

        onAllRefreshables { refreshAfterExterminate() }

        val remainingTokens = game.choices.map { it.second }
        if (remainingTokens.distinct().size == 1) {
            exterminate(false)
            return
        } else {
            for (token in game.removedTokens) {
                game.wildlifeTokens.push(token)
            }
            game.removedTokens.clear()

            if (game.playerQueue.peek().type != PlayerType.NETWORK) {
                game.wildlifeTokens.shuffle()

                if (!game.isLocal) {
                    rootService.networkService.sendExterminate(affectedIndices, false)
                }
            }

//            refreshing only at the final resolved state
//            onAllRefreshables {
//                refreshAfterExterminate()
//            }

        }
    }

    /**
     * the function calculates the score for every player. The score consist of points in the following categories:
     * - For each wildlife scoring card
     * - For each habitat corridor
     * - For each habitat corridor majority
     * - Nature tokens
     *
     * the resulting score is parsed directly to the GUI
     *
     * @throws IllegalStateException if there is no current game or
     *                               if not every player has 20 habitat tiles
     */
    fun calculateScores(botCall: Boolean = false) : List<Pair<String,List<Int>>> {
        val scores = mutableListOf<Pair<String, MutableList<Int>>>()
        val currentGame = rootService.currentGame
        checkNotNull(currentGame) { "Es existiert kein Spiel" }

        for (player in currentGame.playerQueue) {
            if (botCall && player != currentGame.playerQueue.peek()) continue
            val playerScore = mutableListOf<Int>()
            val nodes = createGraph(player.board)
            playerScore.addAll(createCorridorScores(nodes))

            if (currentGame.scoringCards[0]) playerScore.add(bearScoringA(nodes))
            else playerScore.add(bearScoringB(nodes))

            /*if (currentGame.scoringCards[1]) playerScore.add(elkScoringA(nodes))
            else playerScore.add(elkScoringB(nodes))*/
            val elkGroupList = sortElks(nodes)
            playerScore.add(elkScore(elkGroupList, currentGame.scoringCards[1]))
            nodes.forEach { it.marked = false }

            playerScore.add(salmonScoring(nodes, currentGame.scoringCards[2]))

            if (currentGame.scoringCards[3]) playerScore.add(hawkScoringA(nodes))
            else playerScore.add(hawkScoringB(nodes))

            if (currentGame.scoringCards[4]) playerScore.add(foxScoringA(nodes))
            else playerScore.add(foxScoringB(nodes))

            scores.add(Pair(player.name, playerScore))
        }

        if (!botCall) {
            calculateHabitatCorridorMajority(scores, currentGame)
        }

        scores.forEachIndexed { index, score ->
            score.second.add(currentGame.playerQueue.elementAt(index).natureTokens)
        }

        if (!botCall) {
            onAllRefreshables {
                refreshAfterEndGame(scores)
            }
        }
        return scores
    }

    private fun createGraph(board: Map<Triple<Int, Int, Int>, Tile>): List<Node> {
        val nodes = mutableListOf<Node>()
        val seen = mutableListOf<Tile>()
        for (entry in board) {
            seen.add(entry.value)
            val node = Node(entry.value, entry.key)
            val first = entry.key.first
            val second = entry.key.second
            val third = entry.key.third
            for (i in listOf(-1, 1)) {
                val xAxis = board[Triple(first, second + i, third - i)]
                val yAxis = board[Triple(first + i, second, third - i)]
                val zAxis = board[Triple(first + i, second - i, third)]
                if (xAxis in seen) {
                    node.neighbours[(1.5 + (i * 1.5)).toInt()] = nodes.single { it.tile == xAxis }
                    nodes.single { it.tile == xAxis }.neighbours[((1.5 + (i * 1.5)).toInt() + 3) % 6] = node
                }
                if (yAxis in seen) {
                    node.neighbours[(2.5 + (i * 1.5)).toInt()] = nodes.single { it.tile == yAxis }
                    nodes.single { it.tile == yAxis }.neighbours[((2.5 + (i * 1.5)).toInt() + 3) % 6] = node
                }
                if (zAxis in seen) {
                    node.neighbours[(3.5 + (i * 1.5)).toInt()] = nodes.single { it.tile == zAxis }
                    nodes.single { it.tile == zAxis }.neighbours[((3.5 + (i * 1.5)).toInt() + 3) % 6] = node
                }
            }
            nodes.add(node)//this is needed
        }
        return nodes
    }

    /*
     * Alle Methoden stellen sicher, dass am Ende alle Marked flags false sind und ändern daher nichts an den Knoten
     */

    private fun createCorridorScores(nodes: List<Node>): List<Int> {
        val scores = mutableListOf<Int>()
        for (habitat in Habitates.entries) {
            var maxSize = 0

            for (node in nodes) {
                if (node.marked || !node.tile.habs.contains(habitat)) continue
                val open = ArrayDeque<Node>()
                open.add(node)
                node.marked = true
                var size = 0
                while (open.isNotEmpty()) {
                    val cur = open.removeFirst()
                    size++
                    for (index in cur.tile.habs.indices) {
                        if (cur.tile.habs[index] != habitat) continue
                        val neighbour = cur.neighbours[index] ?: continue
                        if (neighbour.tile.habs[(index + 3) % 6] == habitat) {
                            if (!neighbour.marked) {
                                open.add(neighbour)
                                neighbour.marked = true
                            }
                        }
                    }
                }
                maxSize = max(size, maxSize)
            }
            scores.add(maxSize)
            nodes.forEach { node -> node.marked = false }
        }
        return scores
    }

    private fun calculateHabitatCorridorMajority(
        scores: MutableList<Pair<String, MutableList<Int>>>,
        currentGame: CascadiaGame
    ) {
        if (currentGame.playerQueue.isEmpty()) return
        if (currentGame.playerQueue.size == 2) {
            for (habitat in 0..4) {
                if (scores[0].second[habitat] == scores[1].second[habitat]) {
                    scores[0].second.add(1)
                    scores[1].second.add(1)
                } else if (scores[0].second[habitat] > scores[1].second[habitat]) {
                    scores[0].second.add(2)
                    scores[1].second.add(0)
                } else {
                    scores[1].second.add(2)
                    scores[0].second.add(0)
                }
            }
            return
        }
        for (habitat in 0..4) {
            val localeScores = mutableListOf<Int>()
            for (playerScore in scores) {
                localeScores.add(playerScore.second[habitat])
            }
            val largest = localeScores.max()
            val largestCount = localeScores.count { it == largest }
            when (largestCount) {
                1 -> {
                    for (index in scores.indices) {
                        val secondLargest = localeScores.toList().filter { it != largest }.max()
                        val secondLargestCount = localeScores.count { it == secondLargest }
                        if (localeScores[index] == largest) scores[index].second.add(3)
                        else if (secondLargestCount == 1 && localeScores[index] == secondLargest)
                            scores[index].second.add(1)
                        else scores[index].second.add(0)
                    }
                }

                2 -> {
                    for (index in scores.indices) {
                        if (localeScores[index] == largest) scores[index].second.add(2)
                        else scores[index].second.add(0)
                    }
                }

                else -> {
                    for (index in scores.indices) {
                        if (localeScores[index] == largest) scores[index].second.add(1)
                        else scores[index].second.add(0)
                    }
                }
            }
        }
    }

    private fun bearScoringA(nodes: List<Node>): Int {
        var count = 0
        for (node in nodes) {
            if (node.marked) continue
            node.marked = true
            if (node.tile.occupant == WildlifeToken.BEAR) {
                node.neighbours.filterNotNull().forEach { it.marked = true }
                val neighbours = node.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.BEAR }
                if (neighbours.size != 1) continue
                neighbours.single().neighbours.filterNotNull().forEach { it.marked = true }
                if (neighbours.single().neighbours.filterNotNull().filter
                    { it.tile.occupant == WildlifeToken.BEAR }.size != 1
                )
                    continue
                count++
            }
        }
        nodes.forEach { node -> node.marked = false }
        return when (count) {
            0 -> 0
            1 -> 4
            2 -> 11
            3 -> 19
            else -> 27
        }
    }

    private fun bearScoringB(nodes: List<Node>): Int {
        var count = 0
        for (node in nodes) {
            if (node.marked) continue
            node.marked = true
            if (node.tile.occupant == WildlifeToken.BEAR) {
                node.neighbours.filterNotNull().forEach { it.marked = true }
                val neighbours = node.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.BEAR }
                if (neighbours.isEmpty() or (neighbours.size > 2)) continue
                if (neighbours.size == 1) {
                    neighbours.single().neighbours.filterNotNull().forEach { it.marked = true }
                    if (neighbours.single().neighbours.filterNotNull().filter
                        { it.tile.occupant == WildlifeToken.BEAR }.size != 2
                    ) continue
                } else {
                    val firstNeighbour = neighbours.first()
                    val secondNeighbour = neighbours.last()
                    val firstNeighbourNeighbours = firstNeighbour.neighbours.filterNotNull()
                    val secondNeighbourNeighbours = secondNeighbour.neighbours.filterNotNull()
                    firstNeighbourNeighbours.forEach { it.marked = true }
                    secondNeighbourNeighbours.forEach { it.marked = true }
                    if ((firstNeighbourNeighbours.filter { it.tile.occupant == WildlifeToken.BEAR }.size != 1) or
                        (firstNeighbourNeighbours.filter { it.tile.occupant == WildlifeToken.BEAR }.size == 2 &&
                                !firstNeighbourNeighbours.contains(secondNeighbour))
                    ) continue
                    if ((secondNeighbourNeighbours.filter { it.tile.occupant == WildlifeToken.BEAR }.size != 1) or
                        (secondNeighbourNeighbours.filter { it.tile.occupant == WildlifeToken.BEAR }.size == 2 &&
                                !secondNeighbourNeighbours.contains(firstNeighbour))
                    ) continue
                }
                count++
            }
        }
        nodes.forEach { node -> node.marked = false }
        return 10 * count
    }

    private fun sortElks(nodes: List<Node>): List<List<Node>> {
        val elkGroupList = mutableListOf<MutableList<Node>>()
        for (node in nodes) {
            if (node.tile.occupant != WildlifeToken.ELK) continue
            if (node.marked) continue

            node.marked = true
            elkGroupList.add(markElks(node, mutableListOf(node)))
        }
        nodes.forEach { node -> node.marked = false }
        return elkGroupList
    }

    private fun markElks(node: Node, elkList: MutableList<Node>): MutableList<Node> {
        var elkList = elkList
        node.neighbours.filterNotNull().forEach {
            if (!it.marked && it.tile.occupant == WildlifeToken.ELK) {
                elkList.add(it)
                it.marked = true
                elkList = markElks(it, elkList)
            }
        }
        return elkList
    }

    private fun elkScore(elkGroupList: List<List<Node>>, scoringCardA: Boolean): Int {
        val elkScores = mutableListOf<Int>()

        for (elkGroup in elkGroupList) {
            if (elkGroup.size < 3) {
                elkScores.add(scoreElk(elkGroup.size))
                continue
            } else {
                if (scoringCardA) {
                    val neighborElks = mutableListOf<Int>()
                    var straight = true
                    elkGroup.elementAt(0).neighbours.forEachIndexed { index, node ->
                        if (node == null) return@forEachIndexed
                        if (node.tile.occupant == WildlifeToken.ELK) neighborElks.add(index)
                    }
                    if (neighborElks.size == 1) {
                        if (neighborElks.elementAt(0) < 3) neighborElks.elementAt(0) + 3
                        neighborElks.add(neighborElks.elementAt(0) - 3)
                    } else if (neighborElks.size == 2) {
                        neighborElks.sort()
                        if (neighborElks.elementAt(1) - 3 != neighborElks.elementAt(0)) straight = false
                    }

                    if (straight) {
                        for (node in elkGroup) {
                            val neighborElks2 = mutableListOf<Int>()
                            node.neighbours.forEachIndexed { index, node ->
                                if (node == null) return@forEachIndexed
                                if (node.tile.occupant == WildlifeToken.ELK) neighborElks2.add(index)
                            }

                            straight = neighborElks2.all { it in neighborElks }
                        }
                    }

                    if (straight) {
                        val longStraigths = elkGroup.size / 4
                        val shortStraightLength = elkGroup.size % 4
                        elkScores.add(scoreElk(4) * longStraigths + scoreElk(shortStraightLength))
                    } else {
                        elkScores.add(scoreElkGroup(elkGroup, scoringCardA, 0))
                    }
                } else {
                    elkScores.add(scoreElkGroup(elkGroup, scoringCardA, 0))
                }
            }
        }

        return elkScores.sum()
    }

    private fun scoreElkGroup(elkGroup: List<Node>, scoringCardA: Boolean, depth: Int = 0): Int {
        elkGroup.forEach { elk ->
            if (elk.marked) {
                elk.marked2 = depth
                elk.marked = false
            }
        }

        val maxScore = scoreElk(elkGroup.count { !it.marked })

        val scores = mutableListOf<Int>()
        val combinations = mutableListOf<MutableList<Int>>()
        for (node in elkGroup) {
            if (node.marked2 != 0) {
                if (node.marked2 <= depth) continue
            }
            val neighborElks = mutableListOf<Int>()
            node.neighbours.forEachIndexed { index, node ->
                if (node == null) return@forEachIndexed
                if (node.tile.occupant == WildlifeToken.ELK) neighborElks.add(index)
            }
            val neighborElksA = mutableListOf<Int>()
            val neighborElksB = mutableListOf<MutableList<Int>>()
            if (scoringCardA) {
                neighborElksA.addAll(neighborElks.filter { (it - 3) !in neighborElks }.toMutableList())
                neighborElksA.replaceAll {
                    if (it < 3) it + 3 else it
                }
            } else {
                val neighborElksT2 = neighborElks.toMutableList()
                neighborElksT2.sort()

                val neighborElksT = neighborElksT2.filter { node.neighbours[it]?.marked2 == 0 }.toMutableList()

                while (neighborElksT.isNotEmpty()) {
                    val tempList = mutableListOf<Int>()

                    var currIdx = neighborElksT[0]
                    val currIdx2 = neighborElksT[0]
                    while (getNeighbors(currIdx).first in neighborElksT) {
                        currIdx = getNeighbors(currIdx).first
                        if (currIdx == currIdx2) break
                    }
                    tempList.add(currIdx)
                    neighborElksT.remove(currIdx)
                    while (getNeighbors(currIdx).second in neighborElksT) {
                        currIdx = getNeighbors(currIdx).second
                        tempList.add(currIdx)
                        neighborElksT.remove(currIdx)
                    }

                    if (tempList.size < 3) {
                        neighborElksB.add(tempList)
                    } else {
                        for (i in 0..(tempList.size - 3)) {
                            neighborElksB.add(tempList.subList(i, i + 3))
                        }
                    }
                }

                if (neighborElksB.isEmpty()) {
                    node.marked = true

                    val score =
                        if (elkGroup.any { elk -> (elk.marked2 == 0) && !elk.marked }) {
                            scoreElk(1) + scoreElkGroup(elkGroup, scoringCardA, depth + 1)
                        } else scoreElk(1)

                    if (score == maxScore) return score
                    else scores.add(score)

                    elkGroup.forEach { elk -> elk.marked = false }
                    elkGroup.forEach { elk ->
                        if (elk.marked2 > depth) elk.marked2 = 0
                    }
                }
            }

            //Sonst kannst du auch 2 Variablen einfach nehmen jeweils mit dem Typ
            //Oder eine normale for Schleife, damit man den duplicate code nicht hat
            if (scoringCardA) {
                neighborElksA.forEach {
                    markStraightLine(node, it)
                    markStraightLine(node, it - 3)

                    val combination = elkGroup.filter { elk -> elk.marked }.map { elk -> elk.tile.id }.toMutableList()
                    combination.sort()

                    if (combination in combinations) {
                        elkGroup.forEach { elk -> elk.marked = false }
                        elkGroup.forEach { elk ->
                            if (elk.marked2 > depth) elk.marked2 = 0
                        }

                        return@forEach
                    } else combinations.add(combination)

                    val tmpScore = elkGroup.count { elk -> elk.marked }
//                    println("tmpScore: $tmpScore, depth: $depth, size: ${elkGroup.size}, scores: $scores, it: $it, combination: $combination")

                    val score =
                        if (elkGroup.any { elk -> (elk.marked2 == 0) && !elk.marked }) {
                            scoreElk(tmpScore) + scoreElkGroup(elkGroup, scoringCardA, depth + 1)
                        } else scoreElk(tmpScore)

                    if (score == maxScore) return score
                    else scores.add(score)

                    elkGroup.forEach { elk -> elk.marked = false }
                    elkGroup.forEach { elk ->
                        if (elk.marked2 > depth) elk.marked2 = 0
                    }
                }
            } else {
                neighborElksB.forEach {
                    markElkGroup(node, it)

                    val combination = elkGroup.filter { elk -> elk.marked }.map { elk -> elk.tile.id }.toMutableList()
                    combination.sort()

                    if (combination in combinations) {
                        elkGroup.forEach { elk -> elk.marked = false }
                        elkGroup.forEach { elk ->
                            if (elk.marked2 > depth) elk.marked2 = 0
                        }

                        return@forEach
                    } else combinations.add(combination)

                    val tmpScore = elkGroup.count { elk -> elk.marked }
//                    println("tmpScore: $tmpScore, depth: $depth, size: ${elkGroup.size}, scores: $scores, it: $it")
//                    elkGroup.forEach {elk ->
//                        println("ID: ${elk.tile.id}, Marked: ${elk.marked}, Marked2: ${elk.marked2}")
//                    }

                    val score =
                        if (elkGroup.any { elk -> (elk.marked2 == 0) && !elk.marked }) {
                            scoreElk(tmpScore) + scoreElkGroup(elkGroup, scoringCardA, depth + 1)
                        } else scoreElk(tmpScore)

                    if (score == maxScore) return score
                    else scores.add(score)

                    elkGroup.forEach { elk -> elk.marked = false }
                    elkGroup.forEach { elk ->
                        if (elk.marked2 > depth) elk.marked2 = 0
                    }
                }
            }
        }
        return scores.maxOrNull() ?: 0
    }

    private fun getNeighbors(index: Int): Pair<Int, Int> {
        return when (index) {
            0 -> Pair(5, 1)
            5 -> Pair(4, 0)
            else -> Pair(index - 1, index + 1)
        }
    }

    private fun markElkGroup(node: Node, directions: List<Int>) {
        if (node.marked2 != 0) return
        node.marked = true
        for (direction in directions) {
            if (node.neighbours[direction]?.marked2 == 0) node.neighbours[direction]?.marked = true
        }
    }

    private fun markStraightLine(node: Node, direction: Int) {
        if (node.marked2 != 0) return
        node.marked = true
        val neighbour = node.neighbours.elementAt(direction) ?: return
        if (neighbour.tile.occupant != WildlifeToken.ELK) return
        markStraightLine(neighbour, direction)
    }

    private fun scoreElk(length: Int): Int {
        return when (length) {
            0 -> 0
            1 -> 2
            2 -> 5
            3 -> 9
            4 -> 13
            else -> scoreElk(4) * (length / 4) + scoreElk(length % 4)
        }
    }

    private fun salmonScoring(nodes: List<Node>, isA: Boolean): Int {
        var sum = 0
        val breakPointList = mutableListOf<Node>()  //Diese Knoten werden ignoriert für Wege
        for (node in nodes) {
            val neighbours = node.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
            if (neighbours.size > 2) breakPointList.add(node)
        }
        breakPointList.forEach { it.marked = true }

        for (node in nodes) {
            if ((node.marked) or (node.tile.occupant != WildlifeToken.SALMON)) continue
            node.marked = true
            val neighbours = node.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
                .filter { it !in breakPointList }
            if (neighbours.isEmpty()) {
                sum += 2
            }
            if (neighbours.size == 1) {
                var curNeighbour = neighbours.single()
                var last = node
                var count = 1
                while (true) {
                    curNeighbour.marked = true
                    count++
                    val curNeighbours =
                        curNeighbour.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
                            .filter { it != last }.filter { it !in breakPointList }
                    if (curNeighbours.size != 1) break
                    last = curNeighbour
                    curNeighbour = curNeighbours.single()
                }
                sum += scoreSalmon(isA, count)
            }
            if (neighbours.size == 2) {
                val testCircleScore = testCircle(node, breakPointList)
                if (testCircleScore != -1) {
                    sum += scoreSalmon(isA, testCircleScore)
                }
                val firstNeighbourNeighbours =
                    neighbours.first().neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
                val secondNeighbourNeighbours =
                    neighbours.last().neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
                if ((firstNeighbourNeighbours.size != 2) or (!firstNeighbourNeighbours.contains(neighbours.last())))
                    continue
                if ((secondNeighbourNeighbours.size != 2) or (!secondNeighbourNeighbours.contains(neighbours.first())))
                    continue
                neighbours.forEach { it.marked = true }
                sum += if (isA) 8 else 9
            }

        }
        nodes.forEach { node -> node.marked = false }
        return sum
    }

    private fun testCircle(start: Node, breakPointList: List<Node>): Int {
        val neighbours = start.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
            .filter { it !in breakPointList }
        var last = start
        var cur = neighbours.first()
        val testedNodes = mutableListOf<Node>()
        var count = 1
        while (true) {
            count++
            testedNodes.add(cur)
            cur.marked = true
            val newNeighbour = cur.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
                .filter { it !in breakPointList }.filter { it != last }
            if (newNeighbour.size != 1) {
                break
            }
            if (newNeighbour.single().marked) {
                break
            }
            last = cur
            cur = newNeighbour.single()
        }
        if (cur == start) {
            return count
        } else {
            testedNodes.forEach { it.marked = false }
            return -1
        }
    }

    private fun scoreSalmon(isA: Boolean, length: Int): Int {
        if (isA) {
            return when (length) {
                1 -> 2
                2 -> 5
                3 -> 8
                4 -> 12
                5 -> 16
                6 -> 20
                else -> 25
            }
        } else {
            return when (length) {
                1 -> 2
                2 -> 4
                3 -> 9
                4 -> 11
                else -> 17
            }
        }
    }

    private fun hawkScoringA(nodes: List<Node>): Int {
        var count = 0
        for (node in nodes) {
            if (node.marked) continue
            node.marked = true
            if (node.tile.occupant == WildlifeToken.HAWK) {
                node.neighbours.filterNotNull().forEach { it.marked = true }
                val neighbours = node.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.HAWK }
                if (neighbours.isNotEmpty()) continue
                count++
            }
        }
        nodes.forEach { node -> node.marked = false }
        return when (count) {
            0 -> 0
            1 -> 2
            2 -> 5
            3 -> 8
            4 -> 11
            5 -> 14
            6 -> 18
            7 -> 22
            else -> 26
        }
    }

    private fun hawkScoringB(nodes: List<Node>): Int {
        var count = 0
        for (node in nodes) {
            if (node.marked) continue
            node.marked = true
            if (node.tile.occupant != WildlifeToken.HAWK) continue
            node.neighbours.filterNotNull().forEach { it.marked = true }
            val neighbours = node.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.HAWK }
            if (neighbours.isNotEmpty()) continue
            var found = false
            val directions = listOf(
                Triple(0, -1, 1),
                Triple(-1, 0, 1),
                Triple(-1, 1, 0),
                Triple(0, 1, -1),
                Triple(1, 0, -1),
                Triple(1, -1, 0)
            )
            val start = node.coords
            val coordList = nodes.filter { it.tile.occupant == WildlifeToken.HAWK }.map { it.coords }
            for (distance in 1..15) {
                for (direction in directions) {
                    val first = start.first + direction.first * distance
                    val second = start.second + direction.second * distance
                    val third = start.third + direction.third * distance
                    if (coordList.contains(Triple(first, second, third))) {
                        found = true
                        break
                    }
                }
                if (found) break
            }
            if (found) count++
        }
        nodes.forEach { node -> node.marked = false }
        return when (count) {
            0 -> 0
            1 -> 0
            2 -> 5
            3 -> 9
            4 -> 12
            5 -> 16
            6 -> 20
            7 -> 24
            else -> 28
        }
    }

    private fun foxScoringA(nodes: List<Node>): Int {
        var sum = 0
        for (node in nodes) {
            if ((node.marked) or (node.tile.occupant != WildlifeToken.FOX)) continue
            node.marked = true
            val types = node.neighbours.filterNotNull().map { it.tile.occupant }.distinct()
            sum += when (types.size) {
                0 -> 0
                1 -> 1
                2 -> 2
                3 -> 3
                4 -> 4
                else -> 5
            }
        }
        nodes.forEach { node -> node.marked = false }
        return sum
    }

    private fun foxScoringB(nodes: List<Node>): Int {
        var sum = 0
        for (node in nodes) {
            if ((node.marked) or (node.tile.occupant != WildlifeToken.FOX)) continue
            node.marked = true
            val types = node.neighbours.filterNotNull().map { it.tile.occupant }.filter { it != WildlifeToken.FOX }
            val doubles = types.filter { type -> types.filter { it == type }.size == 2 }
            sum += when (doubles.size / 2) {
                0 -> 0
                1 -> 3
                2 -> 5
                else -> 7
            }
        }
        nodes.forEach { node -> node.marked = false }
        return sum
    }

    private class Node(val tile: Tile, val coords: Triple<Int, Int, Int>) {
        val neighbours = Array<Node?>(6) { null }
        var marked = false
        var marked2 = 0
    }

    /**
     * This function changes the currentPlayer by rotating the [CascadiaGame.playerQueue]. It also changes all other
     * relevant variables, like the [CascadiaGame.gameState]. If all players have played their 20 rounds this function
     * ends the game by calling [calculateScores] and sending the game-end Refresh with the scores to the GUI
     *
     *@throws IllegalStateException if Game is not in [GameState.END_OF_TURN]
     *@throws IllegalArgumentException if the [CascadiaGame.playerQueue] is empty
     */
    fun changeTurn() {
        val game = rootService.currentGame
        checkNotNull(game) { "No current game" }

        val checkCondition = game.gameState == GameState.PLAYED_TILE || game.gameState == GameState.END_OF_TURN
        check(checkCondition) { "Current Turn can not be ended" }

        val currentPlayer = game.playerQueue.peek()

        if (currentPlayer.type != PlayerType.NETWORK && !game.isLocal) {
            rootService.networkService.sendPlace(game.tileCoordinates, game.tokenCoordinates, game.tileRotation)
        }

        if (game.gameState == GameState.PLAYED_TILE) {
            if (currentPlayer.type != PlayerType.NETWORK) {
                game.wildlifeTokens.push(game.choices[game.selectedChoice.second].second)
                game.wildlifeTokens.shuffle()

                if (!game.isLocal) rootService.networkService.sendExterminate(listOf(), false)
            }
        }

        val nextPlayer = game.playerQueue.elementAt(1)

        if (nextPlayer.board.size == 23) {
            calculateScores()
            return
        }

        val newTile = game.tileStack.pop()  //hier kann davon ausgegangen werden, dass immer ein Tile da ist
        val newWildlifeToken = game.wildlifeTokens.pop()

        val tileChoice = game.choices[game.selectedChoice.first]
        game.choices[game.selectedChoice.first] = Pair(newTile, tileChoice.second)
        val tokenChoice = game.choices[game.selectedChoice.second]
        game.choices[game.selectedChoice.second] = Pair(tokenChoice.first, newWildlifeToken)

        game.playerQueue.add(game.playerQueue.poll())

        game.tokenCoordinates = null
        game.tileRotation = 0

        game.gameState = GameState.START_OF_TURN

        game.selectedChoice = Pair(-1, -1)

        exterminate(false)

        for (i in 0..3) println(game.choices[i].second.name)

        if (nextPlayer.type == PlayerType.HUMAN && game.isLocal) {
            rootService.history.prevMoves.push(CascadiaGame(game))
        }

        onAllRefreshables { refreshAfterChangeTurn(nextPlayer.board.size == 22) }
    }
}