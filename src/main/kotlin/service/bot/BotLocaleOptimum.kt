package service.bot

import entity.*
import service.*
import kotlin.collections.set

/**
 * this Bot for making the decision with the highest immediate
 * score, it picks the option that gives the most points without
 * planning ahead
 */
class BotLocaleOptimum(private val rootService: RootService, private val bot: Bot) {
    //store the 6 possible moves from one hexagon to its 6 neighboring hexagons.
    private val directionX = intArrayOf(0, -1, -1, 0, 1, 1)
    private val directionY = intArrayOf(-1, 0, 1, 1, 0, -1)
    private val directionZ = intArrayOf(1, 1, 0, -1, -1, 0)

    var bestFreeSelectionPoints: Int = 0
    var bestTileIdx = 0
    var bestWildlifeIdx = 0
    private var bestCombinedScore: Int = 0

    var isFinished = false
    /** this plays one bot turn: handles overpopulation, then picks the best scoring
     * option
     */
    fun makeTurn(){
        val currentGame= rootService.currentGame
        checkNotNull(currentGame){"no current game"}
        val player= currentGame.playerQueue.peek()
        checkNotNull(player){"no player"}
        //overpopulation control
        var maxNum=0
        for( wildLife in WildlifeToken.entries){
            var count=0
            for( paar in currentGame.choices){
                if ( paar.second==wildLife){
                    count++
                }
            }
            if (count > maxNum){
                maxNum = count
            }
        }
        if(maxNum==4){
            rootService.gameService.exterminate(false)
            return
        }
        if (currentGame.gameState== GameState.START_OF_TURN ||
            currentGame.gameState== GameState.HAS_EXTERMINATED){
            chooseBestPair(currentGame,player)
        }else if (currentGame.gameState== GameState.MADE_CHOICE){
            placeBestPlace(currentGame,player)
        }else if (currentGame.gameState== GameState.PLAYED_TILE){
            placeBestAnimal(currentGame,player)
        }else {
            isFinished = true
        }
    }

    private fun findNeighbor(position: Triple<Int,Int,Int>): List<Triple<Int,Int,Int>>{
        val neighbours= mutableListOf<Triple<Int,Int,Int>>()
        for(i in 0..5){
            val newX = position.first + directionX[i]
            val newY = position.second + directionY[i]
            val newZ = position.third + directionZ[i]
            neighbours.add(Triple(newX, newY, newZ))
        }
        return neighbours
    }

    private fun findFreePlace(board: Map<Triple<Int, Int, Int>, Tile>):
            List<Triple<Int,Int,Int>>{
        val freePlaces = mutableListOf<Triple<Int,Int,Int>>()
        for (p in board.keys){
            val neighbour = findNeighbor(p)
            for(n in neighbour){
                val isTaken= board[n] != null
                val isInList= freePlaces.contains(n)
                if(!isTaken && !isInList){
                    freePlaces.add(n)
                }
            }
        }
        return freePlaces
    }

    private fun rotatedTile(tile: Tile, steps: Int): Tile{
        val copy= Tile(tile)
        var numOfSteps = steps%6
        if(numOfSteps<0){
            numOfSteps += 6
        }
        var count= 0
        while(count<numOfSteps){
            val newT= copy.habs.removeAt(0)
            copy.habs.add(newT)
            count += 1
        }
        copy.rotation=(copy.rotation+numOfSteps)%6
        return copy
    }

    //calculating total points corridor+animals
    private fun allPoints() = rootService.gameService.calculateScores(true).single().second.sum()

    private fun chooseBestPairInnerFirst(player: Player, tile: Tile, p: Triple<Int, Int, Int>,
                                         animal: WildlifeToken) {
        for(s in 0..5){
            val rotated = rotatedTile(tile, s)
            player.board[p] = rotated
            var bestForThisTilePlacement = allPoints()
            val animalPositions = player.board.entries
                .filter { it.value.occupant == null && animal in it.value.possibles }
                .map { it.key }

            for (pos in animalPositions) {
                val tile = player.board[pos]
                checkNotNull(tile)
                tile.occupant = animal
                val combinedScore = allPoints()
                tile.occupant = null
                if (combinedScore > bestForThisTilePlacement) {
                    bestForThisTilePlacement = combinedScore
                }
            }
            player.board.remove(p)
            if (bestForThisTilePlacement > bestCombinedScore) {
                bestCombinedScore = bestForThisTilePlacement
            }
        }

    //chooses one of the given pairs
    private fun chooseBestPair(currentGame: CascadiaGame, player: Player){
        val freePlaces= findFreePlace(player.board)
        val startPoints= allPoints()
        var bestIndex = 0
        var bestWinner= Int.MIN_VALUE
        for(i in currentGame.choices.indices){
            val tile= currentGame.choices[i].first
            val animal=currentGame.choices[i].second
            bestCombinedScore = startPoints
            for(p in freePlaces){
                chooseBestPairInnerFirst(player, tile, p, animal)
            }
            val winner=bestCombinedScore-startPoints
            if(winner>bestWinner){
                bestWinner = winner
                bestIndex=i
            }
        }
        if(player.natureTokens>0) {
            bestFreeSelectionPoints = 0
            bestTileIdx = 0
            bestWildlifeIdx = 0

            for (i in currentGame.choices.indices) {
                chooseBestPairInnerSecond(currentGame, startPoints, freePlaces, i, player)
            }
            if (bestFreeSelectionPoints > bestWinner + 5) {
                rootService.playerActionService.freeSelection(bestTileIdx, bestWildlifeIdx)
                return
            }
        }
        rootService.playerActionService.selectColumn(bestIndex)
    }

    private fun chooseBestPairInnerSecond(currentGame: CascadiaGame, startPoints: Int,
                                          freePlaces: List<Triple<Int, Int, Int>>, i: Int, player: Player) {
        val tile = currentGame.choices[i].first
        var bestScoreForThisTile = startPoints
        for (p in freePlaces) {
            for (s in 0..5) {
                val possibleRotatedTile = rotatedTile(tile, s)
                val simulateBoard = player.board.toMutableMap()
                simulateBoard[p] = possibleRotatedTile
                player.board[p] = possibleRotatedTile
                val score = allPoints()
                player.board.remove(p)
                if (score > bestScoreForThisTile) {
                    bestScoreForThisTile = score
                }
            }
        }
        for (wildlifeIdx in currentGame.choices.indices) {
            if (i == wildlifeIdx) continue
            val wildlife = currentGame.choices[wildlifeIdx].second
            val wildlifePosition = mutableListOf<Triple<Int, Int, Int>>()
            for (e in player.board.entries) {
                val unoccupied = e.value.occupant == null
                val allowedHere = e.value.possibles.contains(wildlife)
                if (unoccupied && allowedHere) {
                    wildlifePosition.add(e.key)
                }
            }
            var bestScoreWL = startPoints
            for (p in wildlifePosition) {
                val tile = player.board[p]
                requireNotNull(tile)
                tile.occupant = wildlife
                val score = allPoints()
                tile.occupant = null
                if (score > bestScoreWL) {
                    bestScoreWL = score
                }
            }
            val totalGain = (bestScoreForThisTile - startPoints) + (bestScoreWL - startPoints)
            if (totalGain > bestFreeSelectionPoints) {
                bestFreeSelectionPoints = totalGain
                bestTileIdx = i
                bestWildlifeIdx = wildlifeIdx
            }
        }
    }

    private fun placeBestPlace(currentGame: CascadiaGame, player: Player){
        val chosenTile= currentGame.choices[currentGame.selectedChoice.first].first
        val freePlaces=findFreePlace(player.board)
        if(freePlaces.isEmpty()){
            isFinished = true
            return
        }
        var bestPlace=freePlaces[0]
        var bestStep=0
        var bestScore= Int.MIN_VALUE
        for(p in freePlaces){
            for(s in 0..5){
                val rotatedTile= rotatedTile(chosenTile,s)
                val simulateBoard= player.board.toMutableMap()
                simulateBoard[p]=rotatedTile
                player.board[p] = rotatedTile
                val points= allPoints()
                player.board.remove(p)
                if(points>bestScore){
                    bestScore=points
                    bestPlace=p
                    bestStep=s
                }
            }
        }
        var i=0
        while(i< bestStep){
            rootService.playerActionService.rotateTile(true)
            i +=1
        }

        bot.coordinatesTile = bestPlace
        rootService.playerActionService.placeTile(bestPlace)
    }

    private fun placeBestAnimal(currentGame: CascadiaGame, player: Player){
        val chosenToken= currentGame.choices[currentGame.selectedChoice.second].second
        val possiblePlaces=mutableListOf<Triple<Int, Int, Int>>()
        for(i in player.board.entries){
            val unoccupied=i.value.occupant==null
            val isAllowedHere= i.value.possibles.contains(chosenToken)
            if(unoccupied && isAllowedHere){
                possiblePlaces.add(i.key)
            }
        }
        if (possiblePlaces.isEmpty()){
            isFinished = true
            return
        }
        var bestPlace=possiblePlaces[0]
        var bestScore= Int.MIN_VALUE
        for(p in possiblePlaces){
            val tile = player.board[p]
            checkNotNull(tile)

            tile.occupant = chosenToken
            val points=allPoints()
            tile.occupant = null
            if(points>bestScore){
                bestScore=points
                bestPlace=p
            }
        }
        if (bestScore < 0) {
            isFinished = true
            return
        }

        bot.coordinatesWildlifeToken = bestPlace
        rootService.playerActionService.placeWildlife(bestPlace)
    }

}