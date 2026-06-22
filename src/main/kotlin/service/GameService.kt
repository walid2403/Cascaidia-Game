package service

import entity.*
import kotlin.math.max

/**
 * The game service class of the Cascadia Game. It includes all functions which work mostly on the system-logic side
 */

class GameService(private val rootService: RootService): AbstractRefreshingService() {

    /**
     * A function to start a new game from scratch
     *
     * @param playerList A list of the players to be entered into the game. The list must not be longer then four
     * elements or shorter than two elements. The [List] object contains a [Pair] object for every player with the
     * players name as a [String] and the players Type as a [PlayerType]
     * @param scoringCards The scoring cards to be used in the game, as a [List] of [Boolean] objects,
     * see [CascadiaGame.scoringCards]
     *
     * @throws IllegalArgumentException If the list-size is not 2 - 4, if there are duplicate names or if there are
     * not exactly five scoringCards
     * @throws IllegalStateException If there is currently a game running
     */
    fun startNewGame(playerList: List<Pair<String, PlayerType>>, scoringCards: List<Boolean>) {

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
     * @throws IllegalStateException If the gameState is not [GameState.START_OF_TURN] or [GameState.HAS_EXTERMINATED]
     * or if there are not at least 3 tokens of the same type or
     * if there are 3 and the current gameState is not [GameState.START_OF_TURN]
     */
    fun exterminate() {

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
    fun calculateScores() {
        val scores = mutableListOf<Pair<String,List<Int>>>()
        val currentGame = rootService.currentGame
        checkNotNull(currentGame) { "Es existiert kein Spiel" }

        for (player in currentGame.playerQueue) {
            val playerScore = mutableListOf<Int>()
            val nodes = createGraph(player.board)
            playerScore.addAll(createCorridorScores(nodes))

            if (currentGame.scoringCards[0]) playerScore.add(bearScoringA(nodes))
            else playerScore.add(bearScoringB(nodes))

            if (currentGame.scoringCards[1]) playerScore.add(elkScoringA(nodes))
            else playerScore.add(elkScoringB(nodes))

            if (currentGame.scoringCards[2]) playerScore.add(salmonScoringA(nodes))
            else playerScore.add(salmonScoringB(nodes))

            scores.add(Pair(player.name, playerScore))
        }
    }

    private fun createGraph(board: Map<Triple<Int,Int,Int>,Tile>) : List<Node>{
        val nodes = mutableListOf<Node>()
        val seen = mutableListOf<Tile>()
        for (entry in board){
            seen.add(entry.value)
            val node = Node(entry.value)
            val first = entry.key.first
            val second = entry.key.second
            val third = entry.key.third
            for (i in listOf(-1,1)) {
                val xChange = board[Triple(first+i,second,third)]
                val yChange = board[Triple(first,second+i,third)]
                val zChange = board[Triple(first,second,third+i)]
                if (xChange in seen) {
                    node.neighbours[(1.5 - (i*1.5)).toInt()] = nodes.first { it.tile == xChange }
                }
                if (yChange in seen) {
                    node.neighbours[(2.5 - (i*1.5)).toInt()] = nodes.first { it.tile == yChange }
                }
                if (zChange in seen) {
                    node.neighbours[(3.5 - (i*1.5)).toInt()] = nodes.first { it.tile == zChange }
                }
            }
        }
        return nodes
    }

    /*
     * Alle Methoden stellen sicher, dass am Ende alle Marked flags false sind und ändern daher nichts an den Knoten
     */

    private fun createCorridorScores(nodes : List<Node>) : List<Int> {
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
                        val neighbour = cur.neighbours[index]?: continue
                        if (neighbour.tile.habs[(index+3)%6] == habitat) {
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

    private fun bearScoringA(nodes : List<Node>) : Int {
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
                    { it.tile.occupant == WildlifeToken.BEAR }.size != 1)
                    continue
                count++
            }
        }
        nodes.forEach { node -> node.marked = false }
        return when(count) {
            0 -> 0
            1 -> 4
            2 -> 11
            3 -> 19
            else -> 27
        }
    }

    private fun bearScoringB(nodes : List<Node>) : Int {
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
                        {it.tile.occupant == WildlifeToken.BEAR }.size != 2) continue
                } else {
                    val firstNeighbour = neighbours.first()
                    val secondNeighbour = neighbours.last()
                    val firstNeighbourNeighbours = firstNeighbour.neighbours.filterNotNull()
                    val secondNeighbourNeighbours = secondNeighbour.neighbours.filterNotNull()
                    firstNeighbourNeighbours.forEach { it.marked = true }
                    secondNeighbourNeighbours.forEach { it.marked = true }
                    if ((firstNeighbourNeighbours.filter { it.tile.occupant == WildlifeToken.BEAR }.size != 1 ) or
                        (firstNeighbourNeighbours.filter { it.tile.occupant == WildlifeToken.BEAR }.size == 2 &&
                                !firstNeighbourNeighbours.contains(secondNeighbour))) continue
                    if ((secondNeighbourNeighbours.filter { it.tile.occupant == WildlifeToken.BEAR }.size != 1 ) or
                        (secondNeighbourNeighbours.filter { it.tile.occupant == WildlifeToken.BEAR }.size == 2 &&
                                !secondNeighbourNeighbours.contains(firstNeighbour))) continue
                }
                count++
            }
        }
        nodes.forEach { node -> node.marked = false }
        return 10 * count
    }

    private fun elkScoringA(nodes : List<Node>) : Int {
        var sum = 0
        for (node in nodes) {
            if (node.marked) continue
            node.marked = true
            if (node.tile.occupant != WildlifeToken.ELK) continue
            var cur = node
            var count = 0
            while ((cur.neighbours[1] != null) && (cur.neighbours[1]!!.tile.occupant == WildlifeToken.ELK)) {
                cur = cur.neighbours[1]!!
                cur.marked = true
                count++
            }
            cur = node
            while ((cur.neighbours[4] != null) && (cur.neighbours[4]!!.tile.occupant == WildlifeToken.ELK)) {
                cur = cur.neighbours[4]!!
                cur.marked = true
                count++
            }
            sum += when (count) {
                0 -> 0
                1 -> 2
                2 -> 5
                3 -> 9
                else -> 13
            }
        }
        nodes.forEach { node -> node.marked = false }
        return sum
    }

    private fun elkScoringB(nodes : List<Node>) : Int {
        var sum = 0
        loop@ for (node in nodes) {
            if (node.marked) continue
            node.marked = true
            if (node.tile.occupant != WildlifeToken.ELK) continue
            node.neighbours.filterNotNull().forEach { it.marked = true }
            val neighbours = node.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.ELK }
            if (neighbours.isEmpty()) {
                sum += 2
            }
            if (neighbours.size == 1) {
                if (neighbours.single().neighbours.filterNotNull().
                    filter {it.tile.occupant == WildlifeToken.ELK }.size != 1) continue
                sum += 5
            }
            if (neighbours.size == 2) {
                val neighbourNeighbourCount = neighbours.map { directNeighbour ->
                    directNeighbour.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.ELK  }.size }
                neighbourNeighbourCount.forEach { if (it !in 2..3) continue@loop }
                if (neighbourNeighbourCount.contains(3)) {
                    node.neighbours.filterNotNull().forEach { it.marked = false }
                } else {
                    val firstNeighbour = neighbours.first()
                    val secondNeighbour = neighbours.last()
                    if (firstNeighbour.neighbours.contains(secondNeighbour)) {
                        sum += 9
                    }
                }
            }
            if (neighbours.size == 3) {
                neighbours.forEach { directNeighbour ->
                    directNeighbour.neighbours.filterNotNull().forEach { it.marked = true } }
                val count = neighbours.fold(0) { acc,directNeighbourNeighbour ->
                    acc + directNeighbourNeighbour.neighbours.filterNotNull().
                    filter { it.tile.occupant == WildlifeToken.ELK }.size }
                if (count != 7) continue
                val bigNeighbour = neighbours.single { directNeighbourNeighbour ->
                    directNeighbourNeighbour.neighbours.filterNotNull()
                        .filter { it.tile.occupant == WildlifeToken.ELK }.size == 3
                }
                val bigNeighbourNeighbours =
                    bigNeighbour.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.ELK }
                if ((!bigNeighbourNeighbours.contains(node)) or
                    (neighbours.fold(0) {acc, it -> acc + if(bigNeighbourNeighbours.contains(it)) 1 else 0} != 2)
                    ) continue
                sum += 19
            }
        }
        nodes.forEach { node -> node.marked = false }
        return sum
    }

    //TODO Problem behandeln, wenn Kreis in Lachskette ist
    private fun salmonScoringA(nodes : List<Node>) : Int {
        var sum = 0
        for (node in nodes) {
            if (node.marked) continue
            node.marked = true
            if (node.tile.occupant != WildlifeToken.SALMON) continue
            val neighbours = node.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
            if (neighbours.isEmpty()) {
                sum += 2
            }
            if (neighbours.size == 1) {
                var curNeighbour = neighbours.single()
                var last = node
                var run : Boolean
                var count = 0
                while (true) {
                    curNeighbour.marked = true
                    count++
                    val curNeighbours =
                        curNeighbour.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }.
                        filter { it != last}
                    if (curNeighbours.size != 1) {
                        run = curNeighbours.isEmpty()
                        break
                    }
                    last = curNeighbour
                    curNeighbour = curNeighbours.single()
                }
                if (run) count++
                sum += when(count) {
                    1 -> 2
                    2 -> 5
                    3 -> 8
                    4 -> 12
                    5 -> 16
                    6 -> 20
                    else -> 25
                }
            }
            if (neighbours.size == 2) {
                val firstNeighbourNeighbours =
                    neighbours.first().neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
                val secondNeighbourNeighbours =
                    neighbours.last().neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
                if ((firstNeighbourNeighbours.size != 2) or (!firstNeighbourNeighbours.contains(neighbours.last())))
                    continue
                if ((secondNeighbourNeighbours.size != 2) or (!secondNeighbourNeighbours.contains(neighbours.first())))
                    continue
                neighbours.forEach { it.marked = true }
                sum += 8
            }

        }
        return sum
    }
    //nur diplicate code, mit angepasstem score
    //TODO beide zusammenlegen und die score Stellen mit Boolean ändern. In calcScore Aufruf Boolean mitgeben für ist a true
    private fun salmonScoringB(nodes : List<Node>) : Int {
        var sum = 0
        for (node in nodes) {
            if (node.marked) continue
            node.marked = true
            if (node.tile.occupant != WildlifeToken.SALMON) continue
            val neighbours = node.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
            if (neighbours.isEmpty()) {
                sum += 2
            }
            if (neighbours.size == 1) {
                var curNeighbour = neighbours.single()
                var last = node
                var run : Boolean
                var count = 0
                while (true) {
                    curNeighbour.marked = true
                    count++
                    val curNeighbours =
                        curNeighbour.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }.
                        filter { it != last}
                    if (curNeighbours.size != 1) {
                        run = curNeighbours.isEmpty()
                        break
                    }
                    last = curNeighbour
                    curNeighbour = curNeighbours.single()
                }
                if (run) count++
                sum += when(count) {
                    1 -> 2
                    2 -> 4
                    3 -> 9
                    4 -> 11
                    else -> 17
                }
            }
            if (neighbours.size == 2) {
                val firstNeighbourNeighbours =
                    neighbours.first().neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
                val secondNeighbourNeighbours =
                    neighbours.last().neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }
                if ((firstNeighbourNeighbours.size != 2) or (!firstNeighbourNeighbours.contains(neighbours.last())))
                    continue
                if ((secondNeighbourNeighbours.size != 2) or (!secondNeighbourNeighbours.contains(neighbours.first())))
                    continue
                neighbours.forEach { it.marked = true }
                sum += 9
            }

        }
        return sum
    }

    private class Node(val tile: Tile) {
        val neighbours = Array<Node?>(6) { null }
        var marked = false
    }

    /**
     * the function changes the current Player by rotating the [CascadiaGame.playerQueue]
     * and setting the [CascadiaGame.gameState] to [GameState.START_OF_TURN]
     *
     * it also checks if the [CascadiaGame.tileStack] is empty
     * if the condition is true, [calculateScores] is executed
     *
     *@throws IllegalStateException if Game is not in [GameState.END_OF_TURN]
     *@throws IllegalArgumentException if the [CascadiaGame.playerQueue] is empty
     */
    fun changeTurn() {

    }
}