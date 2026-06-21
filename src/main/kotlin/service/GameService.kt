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

            scores.add(Pair(player.name, playerScore))
        }
    }

    private fun createGraph(board: Map<Triple<Int,Int,Int>,Tile>) : List<Node>{
        val nodes = mutableListOf<Node>()
        val seen = mutableListOf<Tile>()
        for (entry in board){
            seen.add(entry.value)
            val node = Node(entry.value)
            for (habitat in Habitates.entries) {
                node.sizes[habitat] = 0
            }
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

    /**
     * Stellt sicher, dass am Ende alle Marked flags false sind und ändert daher nichts an den Knoten
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

    //evtl beim erstellen schon zusammenhänge checken

    //immer 3 Abstand bei Verbindung
    //jeder Knoten setzt alle 5 auf Max von Nachbarn, nur die mit passender Verbindung betrachten für einzelne Gebiete
    //erhöhe alle Werte, die in Habitates vorkommen um 1
    //füge alle Nachbarn mit nur 0 in sizes zur Queue hinzu
    private class Node(val tile: Tile) {
        val sizes : MutableMap<Habitates,Int> = mutableMapOf()
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