package service.bot

import entity.CascadiaGame
import entity.GameState
import entity.Tile
import entity.WildlifeToken
import service.RootService

/**
 * Eine Klasse für einen Bot, der eine Greedy Entscheidung trifft und diese durch eine Heuristic unterstützt
 */
class GreedyHeuristicBot(private val rootService: RootService, private val bot : Bot, private val marginForNT: Int = 4,
                         private val thresholdForExterminate: Int = 6, private val thresholdForChangeWildlife: Int = 6,
                         private val dummy : Boolean = false) {

    private var chosenTileCoordinateNormal: Triple<Int, Int, Int>? = null
    private var chosenWildlifeCoordinateNormal: Triple<Int, Int, Int>? = null
    private var chosenRotationNormal: Int? = null
    private var chosenTileCoordinateNT: Triple<Int, Int, Int>? = null
    private var chosenWildlifeCoordinateNT: Triple<Int, Int, Int>? = null
    private var chosenRotationNT: Int? = null
    private var freeSelection = false

    /**
     * Führt den Zug dieser Instanz aus
     */
    fun makeTurn() {
        val currentGame = rootService.currentGame
        checkNotNull(currentGame)

        chosenTileCoordinateNormal = null
        chosenWildlifeCoordinateNormal = null
        chosenRotationNormal = null
        chosenTileCoordinateNT = null
        chosenWildlifeCoordinateNT = null
        chosenRotationNT = null
        freeSelection = false

        while (currentGame.gameState != GameState.MADE_CHOICE) {
            select(currentGame)
        }

        val chosenTileCoordinates = if (freeSelection) chosenTileCoordinateNT else chosenTileCoordinateNormal
        val chosenWildlifeCoordinates=if (freeSelection) chosenWildlifeCoordinateNT else chosenWildlifeCoordinateNormal
        val chosenRotation = if (freeSelection) chosenRotationNT else chosenRotationNormal

        requireNotNull(chosenTileCoordinates) { "Tile Koordinate nicht gespeichert" }
        requireNotNull(chosenRotation) { "Rotation nicht gespeichert" }

        bot.coordinatesTile = chosenTileCoordinates
        bot.coordinatesWildlifeToken = chosenWildlifeCoordinates ?: Triple(null,null,null)

        repeat(chosenRotation) {
            rootService.playerActionService.rotateTile(true)
        }

        /*println("freeselection: $freeSelection")
        println("Wildlife Coords: $chosenWildlifeCoordinates\nTile Coords: $chosenTileCoordinates")
        println("Chosen Indices: ${currentGame.selectedChoice}")*/

        rootService.playerActionService.placeTile(chosenTileCoordinates)

        if (chosenWildlifeCoordinates != null) {
            rootService.playerActionService.placeWildlife(chosenWildlifeCoordinates)
        }
    }

    private fun select(currentGame: CascadiaGame) {
        val currScore = rootService.gameService.calculateScores(true).single().second.sum()
        val options = calcBestScore(currentGame)
        val normalGain = options.first.first - currScore
        var ntGain = options.second.first - currScore
        if (currentGame.playerQueue.peek().natureTokens == 0) {
            ntGain = -100
        }
        //println("Normal: $normalGain\n NT: $ntGain")

        if (currentGame.choices.groupBy { it.second }.entries.maxOf {it.value.size} == 3 &&
            maxOf(ntGain, normalGain) < thresholdForExterminate &&
            currentGame.gameState == GameState.START_OF_TURN) {
            rootService.gameService.exterminate(true)
            //println("Has exterminated\n")
            return
        }

        if (currentGame.playerQueue.peek().natureTokens > 0 &&
            maxOf(normalGain, ntGain) < thresholdForChangeWildlife) {
            removeTokens()
            return
        }

        if (ntGain > normalGain + marginForNT) {
            rootService.playerActionService.freeSelection(
                options.third.first,
                options.third.second
            )
            freeSelection = true
        } else {
            rootService.playerActionService.selectColumn(options.first.second)
        }
    }

    private fun removeTokens() {
        rootService.playerActionService.changeWildlife(listOf(0,1,2,3))
    }

    /**
     * Gibt ein Triple zurück mit der Auswertung ohne und mit NT.
     * Im inneren Triple sind Score, Index und hScore von der Funktion ohne NT
     * und im ersten Pair sind Score und hScore von der Funktion mit NT und mit zweiten Pair sind tIndex und wIndex
     */
    private fun calcBestScore(currentGame: CascadiaGame)
    : Triple<Triple<Int, Int, Int>, Pair<Int, Int>, Pair<Int, Int>> {
        var result : Triple<Triple<Int, Int, Int>, Pair<Int, Int>, Pair<Int, Int>> =
            Triple(Triple(-1,-1, -1), Pair(-1,-1), Pair(-1, -1))
        for (tileChoiceIndex in currentGame.choices.indices) {
            for (wildlifeChoiceIndex in currentGame.choices.indices) {
                result = evaluateCombinationOptimal(currentGame, tileChoiceIndex, wildlifeChoiceIndex, result)
            }
        }
        return result
    }

    private fun setAttributes(normalSelection: Boolean, tPosition: Triple<Int, Int, Int>,
                              wPosition: Triple<Int, Int, Int>?, rotation: Int) {
        chosenTileCoordinateNT = if (normalSelection) chosenTileCoordinateNT else tPosition
        chosenWildlifeCoordinateNT = if (normalSelection) chosenWildlifeCoordinateNT else wPosition
        chosenRotationNT = if (normalSelection) chosenRotationNT else rotation
        chosenTileCoordinateNormal = if (normalSelection) tPosition else chosenTileCoordinateNormal
        chosenWildlifeCoordinateNormal = if (normalSelection) wPosition else chosenWildlifeCoordinateNormal
        chosenRotationNormal = if (normalSelection) rotation else chosenRotationNormal
    }

    private fun evaluateCombinationOptimal(game: CascadiaGame, tileChoiceIndex: Int, wildlifeChoiceIndex: Int,
                                           result: Triple<Triple<Int, Int, Int>, Pair<Int, Int>, Pair<Int, Int>>)
    : Triple<Triple<Int, Int, Int>, Pair<Int, Int>, Pair<Int, Int>> {
        val board = game.playerQueue.peek().board
        val wildlife = game.choices[wildlifeChoiceIndex].second
        val tileOptions = possibleTilePositions(game)
        val normalSelection = tileChoiceIndex == wildlifeChoiceIndex

        var bestScore = if (normalSelection) result.first.first else result.second.first
        var bestScoreHeuristic = if (normalSelection) result.first.third else result.second.second

        for (tPosition in tileOptions) {
            val tile = game.choices[tileChoiceIndex].first
            repeat(6) {
                require(board[tPosition] == null) { "Besetzte Stelle für Tile übergeben" }
                board[tPosition] = tile
                val wildlifeOptions = possibleWildlifePositions(board, wildlife)
                for (wPosition in wildlifeOptions) {
                    val score = test(wildlife, wPosition, board)
                    val hScore = heuristicEvaluate(board, tPosition, wPosition, wildlife, dummy)
                    /*println("Score für tOption $tileChoiceIndex und wildlife $wildlife\n" +
                            "$score Position wildlife: $wPosition und Position tile: $tPosition und Rotation: $it\n")*/
                    if (score == bestScore && hScore > bestScoreHeuristic) {
                        bestScoreHeuristic = hScore
                        setAttributes(normalSelection, tPosition, wPosition, it)
                    }
                    else if (score > bestScore) {
                        bestScore = score
                        setAttributes(normalSelection, tPosition, wPosition, it)
                    }
                }
                val score = test(wildlife, Triple(-100,0,0), board)
                val hScore = heuristicEvaluate(board, tPosition, null, null, dummy)

                if (score == bestScore && hScore > bestScoreHeuristic) {
                    bestScoreHeuristic = hScore
                    setAttributes(normalSelection, tPosition, null, it)
                }
                else if (score > bestScore) {
                    bestScore = score
                    setAttributes(normalSelection, tPosition, null, it)
                }

                board.remove(tPosition)
                rotateTile(tile)
            }
        }

        /*println("Best result for tIndex: $tileChoiceIndex und wIndex: $wildlifeChoiceIndex:\n" +
                "Score: $bestScore " +
                tPosition: ${if(normalSelection) chosenTileCoordinateNormal else chosenTileCoordinateNT} " +
                "wPosition: ${if(normalSelection) chosenWildlifeCoordinateNormal else chosenWildlifeCoordinateNT}\n")*/

        return updateResult(result, bestScore, bestScoreHeuristic, normalSelection,
            tileChoiceIndex, wildlifeChoiceIndex)
    }

    private fun updateResult(result: Triple<Triple<Int, Int, Int>, Pair<Int, Int>, Pair<Int, Int>>,
                             bestScore: Int, bestScoreHeuristic: Int, normalSelection: Boolean,
                             tileChoiceIndex: Int, wildlifeChoiceIndex: Int,)
    : Triple<Triple<Int, Int, Int>, Pair<Int, Int>, Pair<Int, Int>>{
        var normalAnswer = result.first
        var ntScore = result.second
        var ntAnswer = result.third

        if (normalSelection) {
            val normalBest = normalAnswer
            var currBest = normalBest.first
            var currIndex = normalBest.second
            var currHScore = normalBest.third
            if (currBest == bestScore && currHScore < bestScoreHeuristic) {
                currIndex = tileChoiceIndex
                currHScore = bestScoreHeuristic
            } else if (currBest < bestScore) {
                currBest = bestScore
                currIndex = tileChoiceIndex
                currHScore = bestScoreHeuristic
            }
            normalAnswer = Triple(currBest, currIndex, currHScore)
        } else {
            var currBest = ntScore.first
            var currHScore = ntScore.second
            var currTIndex = ntAnswer.first
            var currWIndex = ntAnswer.second
            if (currBest == bestScore && currHScore < bestScoreHeuristic) {
                currHScore = bestScoreHeuristic
                currTIndex = tileChoiceIndex
                currWIndex = wildlifeChoiceIndex
            }
            if (currBest < bestScore) {
                currBest = bestScore
                currTIndex = tileChoiceIndex
                currWIndex = wildlifeChoiceIndex
                currHScore = bestScoreHeuristic
            }
            ntScore = Pair(currBest, currHScore)
            ntAnswer = Pair(currTIndex, currWIndex)
        }
        return Triple(normalAnswer, ntScore, ntAnswer)
    }

    private fun neighbourHeuristic(tile: Tile, board: Map<Triple<Int, Int, Int>, Tile>,
                                   neighbours: List<Triple<Int, Int, Int>>): Int {
        var score = 0

        for (i in tile.habs.indices) {
            val neighbour = board[neighbours[i]] ?: continue
            score += if (tile.habs[i] == neighbour.habs[(i+3)%6]) 1 else 0
        }
        return score
    }

    private fun foxHeuristic(neighbours: List<Triple<Int, Int, Int>>, board: Map<Triple<Int, Int, Int>, Tile>,
                             wildlife: WildlifeToken): Int {
        var score = 0
        for (pos in neighbours) {
            val tile = board[pos] ?: continue
            if (tile.occupant == WildlifeToken.FOX) {
                val foxNeighbours = getNeighbours(pos)
                score -= if (foxNeighbours.mapNotNull { board[it] }.any { it.occupant == wildlife}) 1 else 0
            }
        }
        return score
    }

    private fun restHeuristic(wildlife: WildlifeToken, neighbours: List<Triple<Int, Int, Int>>,
                              board: Map<Triple<Int, Int, Int>, Tile>): Int {
        var score = 0
        when(wildlife) {
            WildlifeToken.HAWK -> {
                for (pos in neighbours) {
                    val neighbour = board[pos] ?: continue
                    score -= if (neighbour.occupant == wildlife) 2 else 0
                }
            }
            WildlifeToken.BEAR -> {
                for (pos in neighbours) {
                    val neighbour = board[pos] ?: continue
                    score -= if (neighbour.occupant == WildlifeToken.BEAR) 2 else 0
                }
            }
            else -> {}
        }
        return score
    }

    private fun heuristicEvaluate(board: Map<Triple<Int, Int, Int>, Tile>,
                                  tPosition: Triple<Int, Int, Int>, wPosition: Triple<Int, Int, Int>? = null,
                                  wildlife: WildlifeToken? = null, dummy: Boolean = false) : Int {
        if (dummy) return 0
        val tile = board[tPosition]
        requireNotNull(tile)
        var neighbours = getNeighbours(tPosition)

        var score = 0

        score += neighbourHeuristic(tile, board, neighbours)
        if ((wPosition == null) || (wildlife == null)) return score
        neighbours = getNeighbours(wPosition)

        score -= foxHeuristic(neighbours, board, wildlife)

        score -= restHeuristic(wildlife, neighbours, board)

        return score
    }

    private fun test(wildlife: WildlifeToken, wPosition: Triple<Int, Int, Int>,
                     board: MutableMap<Triple<Int, Int, Int>, Tile>) : Int {
        var score: Int
        if (wPosition.first != -100) {
            val wildlifeTile = board[wPosition]
            requireNotNull(wildlifeTile) { "Leere Stelle für Wildlife übergeben" }
            wildlifeTile.occupant = wildlife
            score = rootService.gameService.calculateScores(true).single().second.sum()
            wildlifeTile.occupant = null
        } else {
            score = rootService.gameService.calculateScores(true).single().second.sum()
        }
        return score
    }

    private fun rotateTile(tile: Tile) {
        tile.habs.add(0, tile.habs.removeLast())
        tile.rotation = (tile.rotation + 1) % 6
    }

    private fun possibleTilePositions(currentGame: CascadiaGame): List<Triple<Int,Int,Int>> {
        val options = mutableListOf<Triple<Int,Int,Int>>()
        val player = currentGame.playerQueue.peek()
        val board = player.board
        for (k in board.keys) {
            options.addAll(getNeighbours(k).filter { board[it] == null })
        }
        return options.distinct()
    }

    private fun getNeighbours(position: Triple<Int, Int, Int>)
    : List<Triple<Int,Int,Int>> {
        return listOf(
            Triple(position.first, position.second - 1, position.third + 1),
            Triple(position.first, position.second + 1, position.third - 1),
            Triple(position.first - 1, position.second, position.third + 1),
            Triple(position.first + 1, position.second, position.third - 1),
            Triple(position.first - 1, position.second + 1, position.third),
            Triple(position.first + 1, position.second - 1, position.third)
        )
    }

    private fun possibleWildlifePositions(board: Map<Triple<Int, Int, Int>, Tile>,
                                          wildlife: WildlifeToken): List<Triple<Int,Int,Int>> =
        board.entries.filter { it.value.occupant == null && wildlife in it.value.possibles }.map { it.key }
}