package service

import entity.*
import java.util.Vector
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
     * @param playerTrigger indicates whether the extermination is initiated by the player (true)
     * or automatically by the game (false)
     *
     * @throws IllegalStateException If the gameState is not [GameState.START_OF_TURN] or [GameState.HAS_EXTERMINATED]
     * or if there are not at least 3 tokens of the same type or
     * if there are 3 and the current gameState is not [GameState.START_OF_TURN]
     */
    fun exterminate( playerTrigger: Boolean) {
        val game = rootService.currentGame ?: error("No current game")
        check(
            game.gameState == GameState.START_OF_TURN ||
                    game.gameState == GameState.HAS_EXTERMINATED
        ) { "Extermination is not allowed in the current game state" }
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
        check(highestCount >= 3) {
            "There are not at least three identical wildlife tokens"
        }
        if (playerTrigger) {
            if (highestCount != 3) {
                throw IllegalStateException("Player extermination requires exactly three identical wildlife tokens")
            }
        } else {
            if (highestCount < 4) return
        }
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
        val remainingTokens = game.choices.map { it.second }
        if (remainingTokens.distinct().size == 1) {
            exterminate(false)
            return
        }else {
            for (token in game.removedTokens) {
                game.wildlifeTokens.push(token)
            }
            game.wildlifeTokens.shuffle()
            game.removedTokens.clear()
            //refreshing only at the final resolved state

            onAllRefreshables {
                refreshAfterExterminate()
            }


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
    fun calculateScores() {
        val scores = mutableListOf<Pair<String,MutableList<Int>>>()
        val currentGame = rootService.currentGame
        checkNotNull(currentGame) { "Es existiert kein Spiel" }

        for (player in currentGame.playerQueue) {
            val playerScore = mutableListOf<Int>()
            val nodes = createGraph(player.board)
            playerScore.addAll(createCorridorScores(nodes))

            if (currentGame.scoringCards[0]) playerScore.add(bearScoringA(nodes))
            else playerScore.add(bearScoringB(nodes))

            /*if (currentGame.scoringCards[1]) playerScore.add(elkScoringA(nodes))
            else playerScore.add(elkScoringB(nodes))*/
            val elkGroupList = sortElks(nodes)
            playerScore.add(elkScore(elkGroupList,currentGame.scoringCards[1]))
            nodes.forEach { it.marked = false }

            playerScore.add(salmonScoring(nodes, currentGame.scoringCards[2]))

            if (currentGame.scoringCards[3]) playerScore.add(hawkScoringA(nodes))
            else playerScore.add(hawkScoringB(nodes))

            if (currentGame.scoringCards[4]) playerScore.add(foxScoringA(nodes))
            else playerScore.add(foxScoringB(nodes))

            scores.add(Pair(player.name, playerScore))
        }

        calculateHabitatCorridorMajority(scores,currentGame)

        scores.forEachIndexed { index, score -> score.second.add(currentGame.playerQueue.elementAt(index).natureTokens) }

        onAllRefreshables {
            refreshAfterEndGame(scores)
        }
    }
    private fun createGraph(board: Map<Triple<Int,Int,Int>,Tile>) : List<Node>{
        val nodes = mutableListOf<Node>()
        val seen = mutableListOf<Tile>()
        for (entry in board){
            seen.add(entry.value)
            val node = Node(entry.value, entry.key)
            val first = entry.key.first
            val second = entry.key.second
            val third = entry.key.third
            for (i in listOf(-1,1)) {
                val xAxis = board[Triple(first,second+i,third-i)]
                val yAxis = board[Triple(first+i,second,third-i)]
                val zAxis = board[Triple(first+i,second-i,third)]
                if (xAxis in seen) {
                    node.neighbours[(1.5 + (i*1.5)).toInt()] = nodes.single { it.tile == xAxis }
                    nodes.single { it.tile == xAxis }.neighbours[((1.5 + (i*1.5)).toInt()+3)%6] = node
                }
                if (yAxis in seen) {
                    node.neighbours[(2.5 + (i*1.5)).toInt()] = nodes.single { it.tile == yAxis }
                    nodes.single { it.tile == yAxis }.neighbours[((2.5 + (i*1.5)).toInt()+3)%6] = node
                }
                if (zAxis in seen) {
                    node.neighbours[(3.5 + (i*1.5)).toInt()] = nodes.single { it.tile == zAxis }
                    nodes.single { it.tile == zAxis }.neighbours[((3.5 + (i*1.5)).toInt()+3)%6] = node
                }
            }
            nodes.add(node)//this is needed
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

    private fun calculateHabitatCorridorMajority(scores : MutableList<Pair<String,MutableList<Int>>>, currentGame : CascadiaGame) {
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
        } else {    //evtl. auf foreach {} ändern, wenn Detekt sonst meckert
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
/*
    private fun elkAScore(size: Int) : Int {
        return when (size) {
            0 -> 0
            1 -> 2
            2 -> 5
            3 -> 9
            else -> 13
        }
    }

    private fun recursiveTest(unused: MutableList<Node>, sizes: MutableList<Int>) : Int {
        if (unused.isEmpty()) {
            return sizes.fold(0) { acc,size -> acc + elkAScore(size) }
        }
        val currentStart = unused.first()
        val removed = mutableListOf<Node>()
        val scoreOptions = mutableListOf<Int>()
        if (!currentStart.marked) {
            currentStart.marked = true
            scoreOptions.add(recursiveTest(unused, sizes))
        }
        unused.remove(currentStart)
        sizes.add(1)
        scoreOptions.add(recursiveTest(unused, sizes))
        sizes.removeLast()
        for (i in 0..2) {
            var jNeighbour = currentStart
            for (j in 1..3) {
                jNeighbour = jNeighbour.neighbours[i] ?: break
                if (jNeighbour !in unused) break
                unused.remove(jNeighbour)
                sizes.add(j+1)
                removed.add(jNeighbour)
                scoreOptions.add(recursiveTest(unused, sizes))
                sizes.removeLast()
            }
            unused.addAll(removed)
            removed.clear()
        }
        unused.add(currentStart)
        return scoreOptions.max()
    }

    private fun elkScoringATest(nodes : List<Node>) : Int {
        val elkNodes = nodes.filter { it.tile.occupant == WildlifeToken.ELK }
        return 0
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
    }*/

    private fun sortElks(nodes : List<Node>) : List<List<Node>> {
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

    private fun markElks(node : Node, elkList : MutableList<Node>) : MutableList<Node> {
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

    private fun elkScore(elkGroupList : List<List<Node>>, scoringCardA : Boolean) : Int {
        val elkScores = mutableListOf<Int>()

        for (elkGroup in elkGroupList) {
            if (elkGroup.size < 3) {
                elkScores.add(scoreElk(elkGroup.size))
                continue
            }
            else {
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
                }
                else {
                    elkScores.add(scoreElkGroup(elkGroup, scoringCardA, 0))
                }
            }
        }

        return elkScores.sum()
    }

    private fun scoreElkGroup(elkGroup: List<Node>, scoringCardA: Boolean, depth: Int = 0) : Int {
        elkGroup.forEach {elk ->
            if (elk.marked) {
                elk.marked2 = depth
                elk.marked = false
            }
        }

        val scores = mutableListOf<Int>()
        for (node in elkGroup) {
            if (node.marked2 != 0) {
                if (node.marked2 <= depth) continue
            }
            var neighborElks = mutableListOf<Int>()
            node.neighbours.forEachIndexed { index, node ->
                if (node == null) return@forEachIndexed
                if (node.tile.occupant == WildlifeToken.ELK) neighborElks.add(index)
            }
            var neighborElks2: Any
            if (scoringCardA) {
                neighborElks2 = neighborElks.filter({ (it - 3) !in neighborElks }).toMutableList()
                neighborElks2.replaceAll {
                    if (it < 3) it + 3 else it
                }
            } else {
                val neighborElksT = neighborElks.toMutableList()
                neighborElksT.sort()

                neighborElksT.filter { node.neighbours[it]?.marked2 == 0 }

                neighborElks2 = mutableListOf<MutableList<Int>>()

                while (neighborElksT.isNotEmpty()) {
                    val tempList = mutableListOf<Int>()

                    var currIdx = neighborElksT[0]
                    while (getNeighbors(currIdx).first in neighborElksT) {
                        currIdx = getNeighbors(currIdx).first
                    }
                    tempList.add(currIdx)
                    neighborElksT.remove(currIdx)
                    while (getNeighbors(currIdx).second in neighborElksT) {
                        currIdx = getNeighbors(currIdx).second
                        tempList.add(currIdx)
                        neighborElksT.remove(currIdx)
                    }

                    if (tempList.size < 3) {
                        neighborElks2.add(tempList)
                    } else {
                        for (i in 0..(tempList.size - 3)) {
                            neighborElks2.add(tempList.subList(i, i + 3))
                        }
                    }
                }
            }

            //Sonst kannst du auch 2 Variablen einfach nehmen jeweils mit dem Typ
            //Oder eine normale for Schleife, damit man den duplicate code nicht hat
            //TODO ändern um Warning zu entfernen
            if (scoringCardA) {
                (neighborElks2 as MutableList<Int>).forEach {
                    markStraightLine(node, it)
                    markStraightLine(node, it - 3)
                    val tmpScore = elkGroup.count {elk -> elk.marked }
                    if (elkGroup.any{ elk -> (elk.marked2 == 0) && !elk.marked }) scores.add(scoreElk(tmpScore) + scoreElkGroup(elkGroup, scoringCardA, depth + 1))
                    else scores.add(scoreElk(tmpScore))
                    elkGroup.forEach { elk -> elk.marked = false }
                    elkGroup.forEach { elk ->
                        if (elk.marked2 > depth) elk.marked2 = 0
                    }
                }
            } else {
                (neighborElks2 as MutableList<List<Int>>).forEach {
                    markElkGroup(node, it)
                    val tmpScore = elkGroup.count {elk -> elk.marked }
//                    println("tmpScore: $tmpScore, depth: $depth, size: ${elkGroup.size}, scores: $scores, it: $it")
                    if (elkGroup.any{ elk -> (elk.marked2 == 0) && !elk.marked }) scores.add(scoreElk(tmpScore) + scoreElkGroup(elkGroup, scoringCardA, depth + 1))
                    else scores.add(scoreElk(tmpScore))
                    elkGroup.forEach { elk -> elk.marked = false }
                    elkGroup.forEach { elk ->
                        if (elk.marked2 > depth) elk.marked2 = 0
                    }
                }
            }
            /*neighborElks2.forEach {
                if (scoringCardA) {
                    markStraightLine(node, it)
                    markStraightLine(node, it - 3)
                } else {
                    markElkGroup(node, it)
                }
                val tmpScore = elkGroup.count {elk -> elk.marked }
                if (elkGroup.any{ elk -> (elk.marked2 == 0) || !elk.marked }) scores.add(scoreElk(tmpScore) + scoreElkGroup(elkGroup, scoringCardA, depth + 1))
                else scores.add(scoreElk(tmpScore))
                elkGroup.forEach { elk -> elk.marked = false }
                elkGroup.forEach { elk ->
                    if (elk.marked2 > depth) elk.marked2 = 0
                }
            }*/
        }
        return scores.maxOrNull() ?: 0
    }

    private fun getNeighbors(index : Int) : Pair<Int, Int> {
        return when(index) {
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

    private fun scoreElk(length: Int) : Int {
        return  when(length) {
            0 -> 0
            1 -> 2
            2 -> 5
            3 -> 9
            4 -> 13
            else -> scoreElk(4) * (length / 4) + scoreElk(length % 4)
        }
    }

    private fun salmonScoring(nodes : List<Node>, isA : Boolean) : Int {
        var sum = 0
        val breakPointList = mutableListOf<Node>()  //Diese Knoten werden ignoriert für Wege
        for (node in nodes) {
            val neighbours = node.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON  }
            if (neighbours.size > 2) breakPointList.add(node)
        }
        breakPointList.forEach { it.marked = true }

        for (node in nodes) {
            if ((node.marked) or (node.tile.occupant != WildlifeToken.SALMON)) continue
            node.marked = true
            val neighbours = node.neighbours.filterNotNull().
            filter { it.tile.occupant == WildlifeToken.SALMON }.filter { it !in breakPointList  }
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
                        curNeighbour.neighbours.filterNotNull().filter { it.tile.occupant == WildlifeToken.SALMON }.
                        filter { it != last}.filter { it !in breakPointList }
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

    private fun testCircle(start : Node, breakPointList : List<Node>) : Int {
        val neighbours = start.neighbours.filterNotNull().
        filter { it.tile.occupant == WildlifeToken.SALMON }.filter { it !in breakPointList  }
        var last = start
        var cur = neighbours.first()
        val testedNodes = mutableListOf<Node>()
        var count = 1
        while (true) {
            count++
            testedNodes.add(cur)
            cur.marked = true
            val newNeighbour = cur.neighbours.filterNotNull().
            filter { it.tile.occupant == WildlifeToken.SALMON }.
            filter { it !in breakPointList  }.filter { it != last }
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

    private fun scoreSalmon(isA : Boolean, length: Int) : Int {
        if (isA) {
            return  when(length) {
                1 -> 2
                2 -> 5
                3 -> 8
                4 -> 12
                5 -> 16
                6 -> 20
                else -> 25
            }
        } else {
            return when(length) {
                1 -> 2
                2 -> 4
                3 -> 9
                4 -> 11
                else -> 17
            }
        }
    }

    private fun hawkScoringA(nodes : List<Node>) : Int {
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
        return when(count) {
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

    private fun hawkScoringB(nodes : List<Node>) : Int {
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
                Triple(0,-1,1),
                Triple(-1,0,1),
                Triple(-1,1,0),
                Triple(0,1,-1),
                Triple(1,0,-1),
                Triple(1,-1,0)
            )
            val start = node.coords
            val coordList = nodes.filter { it.tile.occupant == WildlifeToken.HAWK }.map { it.coords }
            for (distance in 1..15) {
                for (direction in directions) {
                    val first = start.first + direction.first * distance
                    val second = start.second + direction.second * distance
                    val third = start.third + direction.third * distance
                    if (coordList.contains(Triple(first,second,third))) {
                        found = true
                        break
                    }
                }
                if (found) break
            }
            if (found) count++
        }
        nodes.forEach { node -> node.marked = false }
        return when(count) {
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

    private fun foxScoringA(nodes : List<Node>) : Int {
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

    private fun foxScoringB(nodes : List<Node>) : Int {
        var sum = 0
        for (node in nodes) {
            if ((node.marked) or (node.tile.occupant != WildlifeToken.FOX)) continue
            node.marked = true
            val types = node.neighbours.filterNotNull().map { it.tile.occupant }.filter { it != WildlifeToken.FOX }
            val doubles = types.filter {type -> types.filter{ it == type }.size == 2}
            sum += when (doubles.size/2) {
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