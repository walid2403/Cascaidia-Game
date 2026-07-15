package service.bot

import entity.*
import service.*

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
    private fun calculateCorridorPoints(board: Map<Triple<Int, Int, Int>, Tile>):
            Int{
        var totalPoints= 0
        for (habitat in Habitates.entries){
            var biggestCorridor=0
            val visited= mutableListOf<Triple<Int, Int, Int>>()
            for (p in board.keys){
                if (visited.contains(p)) continue
                val tile=board[p]!!
                if(!tile.habs.contains(habitat))continue

                val queue= ArrayDeque<Triple<Int,Int,Int>>()
                queue.add(p)
                visited.add(p)
                var corridorsSize=0
                while (queue.isNotEmpty()){
                    val currentPosition= queue.removeFirst()
                    corridorsSize++
                    val currentTile=board[currentPosition]!!
                    val neighbour= findNeighbor(currentPosition)

                    for(i in 0..5){
                        if (currentTile.habs[i]!= habitat)continue
                        val neighbourPosition= neighbour[i]
                        if ( visited.contains(neighbourPosition))continue
                        val neighbourTile= board[neighbourPosition]?:continue

                        val neighboursSide=(i+3)%6
                        if(neighbourTile.habs[neighboursSide]==habitat){
                            visited.add(neighbourPosition)
                            queue.add(neighbourPosition)
                        }
                    }
                }
                if(corridorsSize> biggestCorridor){
                    biggestCorridor=corridorsSize
                }
            }
            totalPoints += biggestCorridor
        }
        return totalPoints

    }
    private fun calculateAnimalPoints(board: Map<Triple<Int, Int, Int>, Tile>, animal: WildlifeToken,
                                      isTypeA: Boolean): Int {
        var points=0
        for(pos in board.keys){
            val tile = board[pos]!!
            if(tile.occupant != animal) continue
            val neighbour= findNeighbor(pos)
            var sameNeighbours = 0
            var haveSameNeighbours = false
            for(neighbourPosition in neighbour){
                val neighbourTile = board[neighbourPosition]
                if(neighbourTile != null && neighbourTile.occupant == animal){
                    sameNeighbours++
                    haveSameNeighbours = true
                }
            }
            when(animal){
                WildlifeToken.BEAR->{
                    if(isTypeA){
                        if(sameNeighbours==1) points +=4
                        else points +=0
                    }else{
                        if(sameNeighbours==2) points +=10
                        else points +=0
                    }
                }

                WildlifeToken.ELK->{
                    if(isTypeA){
                        if (sameNeighbours==0) points +=0
                        else if (sameNeighbours==1) points +=2
                        else points +=5
                    }else{
                        if (sameNeighbours==0) points +=2
                        else if (sameNeighbours==1) points +=5
                        else if (sameNeighbours==2) points +=9
                        else points+= 19
                    }
                }

                WildlifeToken.SALMON->{
                    if (isTypeA){
                        if(sameNeighbours==1) points+=5
                        else if (sameNeighbours==2) points +=8
                        else if (sameNeighbours==0) points +=2
                        else points+=0
                    }else{
                        if(sameNeighbours==1)points +=4
                        else if(sameNeighbours==2)points+=9
                        else if(sameNeighbours==0) points +=2
                        else points+=0
                    }
                }

                WildlifeToken.HAWK->{
                    if(!haveSameNeighbours)points+=2
                    else points+=0
                }

                WildlifeToken.FOX->{
                    val neighbourAnimals=mutableListOf<WildlifeToken>()
                    for(neighbourPosition in neighbour){
                        val neighbouringTile= board[neighbourPosition]
                        if(neighbouringTile!=null && neighbouringTile.occupant!=null){
                            neighbourAnimals.add(neighbouringTile.occupant!!)
                        }
                    }
                    if(isTypeA){
                        val differentAnimals=neighbourAnimals.distinct()
                        points+= differentAnimals.size
                    }else{
                        var numCouple=0
                        val alreadyCount=mutableListOf<WildlifeToken>()
                        for(oneAnimal in neighbourAnimals){
                            if(alreadyCount.contains(oneAnimal))continue
                            val present= neighbourAnimals.count { it == oneAnimal }
                            if(present>=2){
                                numCouple+=1
                            }
                            alreadyCount.add(oneAnimal)
                        }
                        points+=numCouple*3
                    }
                }
            }
        }
        return points
    }
    //calculating total points corridor+animals
    private fun allPoints(board: Map<Triple<Int, Int, Int>, Tile>,scoringCards: List<Boolean>): Int{
        /*var points= calculateCorridorPoints(board)
        for( tier in WildlifeToken.entries){
            points += calculateAnimalPoints(board, tier, scoringCards[tier.ordinal])
        }
        return points*/
        return rootService.gameService.calculateScores(true).single().second.sum()
    }
    //chooses one of the given pairs
    private fun chooseBestPair(currentGame: CascadiaGame, player: Player){
        val freePlaces= findFreePlace(player.board)
        val startPoints= allPoints(player.board,currentGame.scoringCards)

        var bestIndex = 0
        var bestWinner= Int.MIN_VALUE
        for(i in currentGame.choices.indices){
            val tile= currentGame.choices[i].first
            var bestPointsForThisTile= startPoints
            for(p in freePlaces){
                for(s in 0..5){
                    val rotatedTile= rotatedTile(tile,s)
                    val simulateBoard= player.board.toMutableMap()
                    simulateBoard[p]=rotatedTile
                    player.board[p] = rotatedTile
                    val points=allPoints(simulateBoard,currentGame.scoringCards)
                    player.board.remove(p)
                    if(points> bestPointsForThisTile){
                        bestPointsForThisTile=points
                    }
                }
            }
            val winner=bestPointsForThisTile-startPoints
            if(winner>bestWinner){
                bestWinner = winner
                bestIndex=i
            }
        }
        if(player.natureTokens>0) {
            var bestFreeSelectionPoints = 0
            var bestTileIdx = 0
            var bestWildlifeIdx = 0

            for (i in currentGame.choices.indices) {
                val tile = currentGame.choices[i].first
                var bestScoreForThisTile = startPoints
                for (p in freePlaces) {
                    for (s in 0..5) {
                        val possibleRotatedTile = rotatedTile(tile, s)
                        val simulateBoard = player.board.toMutableMap()
                        simulateBoard[p] = possibleRotatedTile
                        player.board[p] = possibleRotatedTile
                        val score = allPoints(simulateBoard, currentGame.scoringCards)
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
                        val simulateBoard = player.board.toMutableMap()
                        val simulatedTile = Tile(simulateBoard[p]!!)
                        simulatedTile.occupant = wildlife
                        simulateBoard[p] = simulatedTile
                        player.board[p]!!.occupant = wildlife
                        val score = allPoints(simulateBoard, currentGame.scoringCards)
                        player.board[p]!!.occupant = null
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
            if (bestFreeSelectionPoints > bestWinner + 5) {
                rootService.playerActionService.freeSelection(bestTileIdx, bestWildlifeIdx)
                return
            }
        }
        rootService.playerActionService.selectColumn(bestIndex)
    }
    private fun placeBestPlace(currentGame: CascadiaGame, player: Player){
        val chosenTile= currentGame.choices[currentGame.selectedChoice.first].first
        val freePlaces=findFreePlace(player.board)
        if(freePlaces.isEmpty()){
            //currentGame.gameState= GameState.END_OF_TURN
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
                val points= allPoints(simulateBoard,currentGame.scoringCards)
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
            //currentGame.gameState= GameState.END_OF_TURN
            isFinished = true
            return
        }
        var bestPlace=possiblePlaces[0]
        var bestScore= Int.MIN_VALUE
        for(p in possiblePlaces){
            val simulateBoard= player.board.toMutableMap()
            val simulateTile= Tile(simulateBoard[p]!!)
            simulateTile.occupant=chosenToken
            simulateBoard[p]=simulateTile

            player.board[p]!!.occupant = chosenToken
            val points=allPoints(simulateBoard,currentGame.scoringCards)
            player.board[p]!!.occupant = null
            if(points>bestScore){
                bestScore=points
                bestPlace=p
            }
        }
        if (bestScore < 0) {
            //currentGame.gameState= GameState.END_OF_TURN
            isFinished = true
            return
        }

        bot.coordinatesWildlifeToken = bestPlace
        println(bestPlace)
        rootService.playerActionService.placeWildlife(bestPlace)
    }

}