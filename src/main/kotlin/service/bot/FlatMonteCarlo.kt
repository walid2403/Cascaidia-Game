package service.bot

import entity.CascadiaGame
import entity.GameState
import entity.PlayerType
import service.RootService
import service.Refreshable

/**
 * Diese Klasse ist eine schwächere Version des vollständigen FlatMonteCarloBot
 * das Verfahren bleibt gleich aber statt alle Teilzüge miteinzubeziehen, werden nur Shoppicks,Rotation und Tile- und
 * Tiersetzung betrachten, da das Wipen der Tiertokens nicht speicherbar ist (bzw. wir wissen nicht wie)
 */
class FlatMonteCarlo(private val rootService: RootService, private val bot: Bot) {
    /**
     * Hilfsklasse, die ein [Refreshable] zur Verfügung stellt, um die Punkte nach einem Spiel zu speichern
     * nachdem [service.GameService.calculateScores] [refreshAfterEndGame] aufgerufen hat, werden hier die scores der
     * Spieler einer Partie in receivedScores gespeichert
     */
    private class ScoreRefreshable: Refreshable {
        var receiveScores: List<Pair<String, List<Int>>>? = null

        override fun refreshAfterEndGame(scores: List<Pair<String, List<Int>>>) {
            receiveScores = scores
        }
    }

    /**
     * Hilfsklasse für Züge
     * @param shopTileIndex Position des Tiles welches ausgewählt wird
     * @param shopWildLifeIndex Position des Tiers welches ausgewählt wird
     * @param tilePlacementPosition (gültige) Cubekoordinate für die Tileplatzierung
     * @param tileRotation Anzahl der 60 Grad Drehungen (0-5 mal)
     * @param wildLifePlacement (mögliche) Platzierung des Tiertoken aufs Board (darf nullable sein)
     */
    private data class Move(
        val shopTileIndex:Int=-1,
        val shopWildLifeIndex:Int=-1,
        val tilePlacementPosition: Triple<Int, Int, Int> = Triple(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE),
        val tileRotation: Int=0,
        val wildLifePlacement: Triple<Int, Int, Int>?=null,
        )

/**
 * Methode, die den Botzug nach FlatMonteCarlo durchführt
 */

fun turn(){
    val game=rootService.currentGame
    checkNotNull(game){"Das Spiel läuft nicht"}

    // den besten Zug nach MonteCarlo berechnen und diesen durchführen
    val bestMove= getBestMove()

    bot.coordinatesTile = bestMove.tilePlacementPosition
    if (bestMove.wildLifePlacement != null) {
        bot.coordinatesWildlifeToken = bestMove.wildLifePlacement
    } else {
        bot.coordinatesWildlifeToken = Triple(null, null, null)
    }

    executeMove(bestMove,rootService)

}
    /**
     * Methode, die den besten aktuellen Zug nach FlatMonteCarlo bestimmt
     * @return gibt den besten Move zurück
     */
    private fun getBestMove(): Move{
        val game=rootService.currentGame
        checkNotNull(game){"Das Spiel läuft nicht"}

        //Liste aller möglichen Züge
        val allMoves: List<Move> = getAllLegalMoves()
        check(allMoves.isNotEmpty()){"keine Züge gefunden"}

        // bestScore speichern, damit wir später vergleichen können
        // Initialwert ist - undendlich (ein ungültiger Wert)
        var bestScore: Double= Double.NEGATIVE_INFINITY
        // index in der Liste des besten Moves
        var indexBestMove = 0

        //iterieren über die Listenelemente: Vergleich des iten scores mit dem aktuell besten
        for (i in allMoves.indices){
            val move = allMoves[i]
            //Durchschnittscore des aktuellen Moves
            val localScore: Double = getAverageScore(move)

            //falls der lokale Score der aktuell größte ist, diesen speichern (und den dazugehörigen Index)
            if(localScore > bestScore){
                bestScore = localScore
                indexBestMove = i
            }
        }

        return allMoves[indexBestMove]
    }
    /**
     * Funktion, die den durschnittlichen Score eines Zugs anhand simulierter Spiele berechnet
     * @param move der Zug für den der Score berechnet wird
     * @return der Durchschnittsscore
     */
    private fun getAverageScore(move : Move): Double{
        val game=rootService.currentGame
        checkNotNull(game)

        //Spielername des Bots
        val botName=game.playerQueue.first().name
        //Summe der Scores pro Simulation
        var sumScore=0

        //auf 5 Spiele(kann man später anpassen):
        //einen neuen Rootservice erstellen, das aktuelle Spiel kopieren, dieses dem neuen Rootservice zuweisen
        //(dies geschieht um nicht auf dem "echten" Rootservice und CascadiaGame zuarbeiten)
        //dann wird auf dem kopierten Spiel der Zug ausgeführt und das spiel bis zum ende simuliert
        //am Ende addieren wir den Score des simulierten Spiel auf die bisherige Summe
        repeat(5){
            val simulatedRootService= RootService()
            val copy= CascadiaGame(game)
            simulatedRootService.currentGame = copy
            executeMove(move,simulatedRootService)
            sumScore+=simulateAndCalculateOneGame(simulatedRootService,botName)
        }
        //arithmetisches Mittel aller Spieler
        return sumScore / 5.0
    }

    /**
     * FUnktion die ein Spiel bis zum Ende simuliert und den Score dieses Spiels berechnet
     * @param simRootService der simulierende Rootservice
     * @param botName Name des Bots (braucht die getScore Funktion)
     */
    private fun simulateAndCalculateOneGame(simRootService: RootService, botName: String): Int {

        val game = simRootService.currentGame
        checkNotNull(game)

        val randomBot = Bot(simRootService)

        val startSize=game.playerQueue.minOf{it.board.size}
        val targetSize=minOf(startSize+2,23)

        if (!gameIsFinished(game,targetSize)) {
            simRootService.gameService.changeTurn()
        }

        while (!gameIsFinished(game,targetSize)) {
            randomBot.makeTurn(PlayerType.EASY_BOT)

            if (!gameIsFinished(game,targetSize)) {
                simRootService.gameService.changeTurn()
            }
        }

        return getScore(simRootService, botName)

        }



    /**
     * Hilfsmethode die aussagt ob das Spiel am Ende ist
     * @param game ist das aktuell simulierte Spiel
     * @return Wahrheitswert ob das Spiel im Endzustand ist
     */
    private fun gameIsFinished(game: CascadiaGame, targetSize:Int): Boolean
    {  // (aus changeturn): bei dieser Anzahl von Tiles pro Board ist das Spiel zuende
        return game.playerQueue.all {it.board.size >= targetSize}
    }

    /**
     * Funktion, die den Score eines Spiels berechnet
     * wichtig: haben uns für die Differenz des Botscores von dem des besten Spielers entschieden
     * dieser Wert ist aussagekräftiger für die "Stärke" eines Zugs
     * @param simRootService der simulierte Rootservice
     * @param name Name des Botspielers
     * @return die Scoredifferenz des jeweiligen Spiels
     */
    private fun getScore(simRootService: RootService, name: String): Int{
        //den Refreshable erzeugen der die Scores bekommt und beim rootservice registieren
        val refreshable= ScoreRefreshable()
        simRootService.addRefreshable(refreshable)
        //calculatescores aufrufen
        simRootService.gameService.calculateScores()
        //scores bekommt nun die scores als Paar(Name, Summe aller Teilscores)

        val scores=(refreshable.receiveScores ?: error("")).map { pair->
            val playerName= pair.first
            val partialScore = pair.second
            Pair(playerName, partialScore.sum())
        }

        //score des bots
        val botScore=(scores.first { it.first==name }).second
        // alle anderen Pairs der Liste
        val restPairs=scores.filter { it.first!=name }
        // der beste Score der anderen Spieler
        val best=(restPairs.maxBy { it.second }).second
        //Rückgabe: Differenz
        return botScore-best

    }
    /**
     * Methode die alle legalen Züge auswählt
     * @return alle legalen Züge als Liste
     */
    private fun getAllLegalMoves(): List<Move> {
        val game = rootService.currentGame
        checkNotNull(game) { "Spiel läuft nicht" }

        // Liste aller vollständigen legalen Züge
        val moves = mutableListOf<Move>()

        // Normale Auswahl einer Shopspalte
        addNormalShopPicks(moves)

        // Mit mindestens einem Naturmarker ist eine freie Auswahl möglich
        if (game.playerQueue.first().natureTokens > 0) {
            addTokenShopPicks(moves)
        }

        // Die bisherigen Teilzüge um alle weiteren Möglichkeiten erweitern
        addAllRotations(moves)
        addAllTilePlacements(moves)
        addAllWildlifePlacements(moves)

        return moves
    }
    /**
     * Methode fügt die normalen Shoppicks zu (auch kein Wipen per Token vorher)
     * @param moves die eigentliche Liste der Moves, welche später genutzt wird
     */
    private fun addNormalShopPicks(moves: MutableList<Move>) {
            //gehe die Shopindizies durch
            for(shopIndex in 0..3){
                //füge zu den Zügen den jeweiligen Zug hinzu
                //(Kopie der Grundzüge mit jeweiligen shopIndizies)
                moves.add(
                    Move(
                        shopTileIndex = shopIndex,
                        shopWildLifeIndex = shopIndex
                    )
                )
            }
        }
    /**
     * Methode die Shoppicks mit ungleichen Indizes hinzufügt (bei token)
     * @param moves die eigentliche Liste der Moves, welche später genutzt wird
     */

    private fun addTokenShopPicks(moves: MutableList<Move>) {
        for(tileIndex in 0..3){
           for(wildlifeIndex in 0..3){
               if(tileIndex != wildlifeIndex){
                   moves.add(
                       Move(
                           shopTileIndex = tileIndex,
                           shopWildLifeIndex = wildlifeIndex
                       )
                   )
               }
           }
        }
    }

    private fun addAllRotations(moves: MutableList<Move>){
        val baseMoves=moves.toList()
        moves.clear()

        //für jeden bsiherigen move 0..5 rotation hinzufügen
        for(move in baseMoves){
            for(rotation in 0..5){
                moves.add(
                    move.copy(
                        tileRotation = rotation,
                    )
                )
            }
        }
    }

    /**
     * bisherigen Moves auf alle Möglichen Tilepositions erweitern
     */

    private fun addAllTilePlacements(moves: MutableList<Move>){
        val baseMoves=moves.toList()
        moves.clear()
        //legale Positionen bestimmen
        val positions=getTilePositions()

        //Erweitern wie gewohnt
        for(move in baseMoves){
            for(position in positions){
                moves.add(
                    move.copy(
                        tilePlacementPosition = position,
                    )
                )
            }
        }
    }

    /**
     * Hilfsmethode um alle legalen Positionen zubekommen
     * @return Liste der Platzierungen (Cubekoordinaten)
     */

    private fun getTilePositions(): List<Triple<Int, Int, Int>> {
        val game=rootService.currentGame
        checkNotNull(game){"Spiel läüft nicht"}


        val player=game.playerQueue.first()
        //die besetzten Positionen
        val occupiedPositions=player.board.keys
        val legalPositions=mutableListOf<Triple<Int, Int,Int>>()

        //jede besetze Position durch gehen
        for(position in occupiedPositions){
            //jeden Nachbar durchgehen
            for(neighbor in getNeighbors(position)){
                //falls der Nachbar nicht im Board ist, hinzufügen
                if(neighbor !in occupiedPositions){
                    legalPositions.add(neighbor)
                }
            }
        }
        //mehrfache koordinaten vermeiden
        return legalPositions.toList().distinct()

    }

    /**
     * Hilfsmethode um die Nachbarkoordinaten einer Cubekoordinate zubestimmen
     * @param position Koordinate der Position
     * @return die Nachbarn der Koordinate
     */

    private fun getNeighbors(position: Triple<Int,Int,Int>): List<Triple<Int,Int,Int>> {
        val x=position.first
        val y=position.second
        val z=position.third

        //bitte schauen ob richtig so:
        return  listOf(
            Triple(x+1,y-1,z),
            Triple(x+1,y,z-1),
            Triple(x,y+1,z-1),
            Triple(x-1,y+1,z),
            Triple(x-1,y,z+1),
            Triple(x,y-1,z+1),
        )

    }

    /**
     * Methode die alle moves um (eventuellen) Tierpositionen erweitert
     * @param moves die Liste der Züge von exterminate bis tile platzieren)
     *
     */

    private fun addAllWildlifePlacements(moves:MutableList<Move>){
        val baseMoves = moves.toList()
        moves.clear()
        //erst alle um keine Platzierung, dann alle um alle Tierplatzierungen erweitern
        addAllMovesWithoutWildlifePlacement(baseMoves,moves)
        addAllMovesWithWildlifePlacement(baseMoves,moves)
    }

    /**
     * Methode die alle moves um Tierpositionen erweitert
     *  @param moves die Liste aller möglichen Züge
     *  @param baseMoves die Liste der Züge von exterminate bis tile platzieren)
     */

    private fun addAllMovesWithoutWildlifePlacement(baseMoves: List<Move>, moves:MutableList<Move>)
    {
        //jeden move um keine Platzierung erweitern
        for(move in baseMoves){
            moves.add(move.copy(wildLifePlacement = null))
        }
    }
    /**
     * Methode die alle moves um echten Tierpositionen erweitert
     *  @param moves die Liste aller möglichen Züge
     *  @param baseMoves die Liste der Züge von exterminate bis tile platzieren)
     */

    private  fun addAllMovesWithWildlifePlacement(baseMoves: List<Move>, moves:MutableList<Move>){
        val game=rootService.currentGame
        checkNotNull(game)

        for(move in baseMoves){
            val positions=getWildlifePositions(move,game)

            for(position in positions){
                moves.add(
                    move.copy(
                        wildLifePlacement = position,
                    )
                )
            }
        }

    }
    /**
     * Methode berechnet mögliche Tierplatzierungen
     * @param move (weil wir den shopindex brauchen, um das gezogene Tier zubestimmen)
     * @param game das simulierte Spiel
     * @return mögliche Cubekoordinaten
     */
    private fun getWildlifePositions(move: Move, game: CascadiaGame): List<Triple<Int,Int,Int>>{

        //das gezogene Tier und Tile
        val wildlife=game.choices[move.shopWildLifeIndex].second
        val tile=game.choices[move.shopTileIndex].first
        //die Position des Tiles im board
        val newPosition=move.tilePlacementPosition


        val legalPositions=mutableListOf<Triple<Int,Int,Int>>()

        //mögliche positionen im board durchgehen
        for((position,tile) in game.playerQueue.first().board){
            //falls besetzt nicht möglich
            if(tile.occupant!=null) continue
            //falls nicht besetzt und für das Tier valide, dann platzieren
            if(wildlife in tile.possibles){
                legalPositions.add(position)
            }
        }
        //nochmal prüfen ob der gezogene Tile das tier beinhalten darf
        if(wildlife in tile.possibles){
            legalPositions.add(newPosition)
        }

        return legalPositions

    }

    private fun executeMove(move: Move, rootService:  RootService){
        val game=rootService.currentGame
        checkNotNull(game){"Spiel läuft nicht"}

        // je nachdem ob die Shopindizes gleich sind: freeselection oder selectcolumn
        if(move.shopTileIndex!=move.shopWildLifeIndex){
            rootService.playerActionService.freeSelection(move.shopTileIndex,move.shopWildLifeIndex)
        }else{
            rootService.playerActionService.selectColumn(move.shopTileIndex)
        }

        // tilerotation mal rotieren
        repeat(move.tileRotation){
            rootService.playerActionService.rotateTile(true)
        }

        //tile platzieren
        rootService.playerActionService.placeTile(move.tilePlacementPosition)
        // falls gewollt: tiertoken platzieren
        if(move.wildLifePlacement!=null){
            rootService.playerActionService.placeWildlife(move.wildLifePlacement)
        }else{
            game.gameState= GameState.END_OF_TURN
        }
    }

}
