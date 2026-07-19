package service.bot

import entity.*
import service.*
import kotlin.random.Random

/**
 * Eine Klasse, in der alle Aufrufe der Bot Methoden gebündelt sind
 */
class Bot (private val rootService: RootService) : AbstractRefreshingService() {

    /**
     * Wichtig: Beide müssen in jedem Bot gesetzt werden!
     */
    var coordinatesTile: Triple<Int?, Int?, Int?> = Triple(null, null, null)
    var coordinatesWildlifeToken: Triple<Int?, Int?, Int?> = Triple(null, null, null)

    private var isFinished = false

    /**
     * Hier eine Kopie von eurem Bot erstellen
     */
    private val greedyBot = BotLocaleOptimum(rootService, this)
    private val heuristicBot = HeuristicBot(rootService, this)
    private val monteCarloBot = FlatMonteCarlo(rootService, this)
    private val greedyHeuristicBot = GreedyHeuristicBot(rootService, this)
    var greedyHeuristicTest = GreedyHeuristicBot(rootService, this)

    private fun resetCoordinates() {
        coordinatesTile = Triple(null, null, null)
        coordinatesWildlifeToken = Triple(null, null, null)
    }

    /**
     * Die Schnittstelle für die GUI
     */
    fun makeTurn(playerType: PlayerType) {
        require(playerType != PlayerType.HUMAN) { "Die Methode sollte nur für Bot Züge aufgerufen werden" }
        require(playerType != PlayerType.NETWORK) { "Die Methode sollte nur für Bot Züge aufgerufen werden" }
        resetCoordinates()
        when (playerType) {
            PlayerType.EASY_BOT -> {
                isFinished = false
                randomBotTurn()
            }
            PlayerType.HEURISTIC_BOT -> {
                heuristicBot.isFinished = false
                while(!heuristicBot.isFinished) {
                    heuristicBot.makeTurn()
                }
            }
            PlayerType.GREEDY_BOT -> {
                greedyBot.isFinished = false
                while(!greedyBot.isFinished) {
                    greedyBot.makeTurn()
                }
            }
            PlayerType.MONTE_BOT -> {
                monteCarloBot.turn()
            }
            PlayerType.NEURAL_BOT -> {
                isFinished = false
                randomBotTurn()
            }
            PlayerType.GREEDY_HEURISTIC_BOT -> {
                greedyHeuristicBot.makeTurn()
            }
            PlayerType.HARD_BOT -> {
                greedyHeuristicBot.makeTurn()
            }
        }
    }
    private fun randomBotTurn() {
        val currentGame = rootService.currentGame
        checkNotNull(currentGame) { "Es existiert kein Spiel" }
        val player = currentGame.playerQueue.peek()
        checkNotNull(player) { "Es existiert kein Spiel" }

        val legalTurns = mutableListOf(TurnOptions.MAKE_SELECTION)
        if (player.natureTokens > 0) {
            legalTurns += TurnOptions.NATURE_TOKEN_FREE_SELECTION
            legalTurns += TurnOptions.NATURE_TOKEN_CHANGE_WILDLIFE
        }
        if (currentGame.choices.map { it.second }.groupBy { it }.entries.maxOfOrNull { it.value.size } == 3) {
            legalTurns += TurnOptions.CLEAR_SEMIPOPULATION
        }

        while (legalTurns.isNotEmpty()) {
            val turnIndex = randomBotChooseOption(legalTurns)
            val turn = legalTurns[turnIndex]
            when (turn) {
                TurnOptions.NATURE_TOKEN_CHANGE_WILDLIFE -> randomBotNatureTokenExchangeWildlife()
                TurnOptions.NATURE_TOKEN_FREE_SELECTION -> randomBotNatureTokenFreeSelection()
                TurnOptions.CLEAR_SEMIPOPULATION -> randomBotClearSemipopulation()
                TurnOptions.MAKE_SELECTION -> randomBotMakeSelection()
                TurnOptions.PLACE_HABITAT_TILE -> randomBotPlaceHabitatTile(player)
                TurnOptions.PLACE_WILDLIFE_TOKEN -> randomBotPlaceWildlifeToken(player)
                TurnOptions.DISCARD_WILDLIFE_TOKEN -> randomBotDiscardWildlifeToken()
                TurnOptions.ROTATE -> randomBotRotate()
            }
            newLegalTurns(legalTurns)
        }

        //rootService.gameService.changeTurn()
    }

    private fun newLegalTurns(legalTurns: MutableList<TurnOptions>) {
        val currentGame = rootService.currentGame
        checkNotNull(currentGame) { "Es existiert kein Spiel" }
        val player = currentGame.playerQueue.peek()
        checkNotNull(player) { "Es existiert kein Spiel" }

        legalTurns.clear()
        when (currentGame.gameState) {
            GameState.START_OF_TURN -> {
                legalTurns += TurnOptions.MAKE_SELECTION
                if (currentGame.choices.map { it.second }.groupBy { it }.entries.maxOfOrNull { it.value.size } == 3) {
                    legalTurns += TurnOptions.CLEAR_SEMIPOPULATION
                }
                if (player.natureTokens > 0) {
                    legalTurns += TurnOptions.NATURE_TOKEN_FREE_SELECTION
                    legalTurns += TurnOptions.NATURE_TOKEN_CHANGE_WILDLIFE
                }
            }

            GameState.HAS_EXTERMINATED -> {
                legalTurns += TurnOptions.MAKE_SELECTION
                if (player.natureTokens > 0) {
                    legalTurns += TurnOptions.NATURE_TOKEN_FREE_SELECTION
                    legalTurns += TurnOptions.NATURE_TOKEN_CHANGE_WILDLIFE
                }
            }

            GameState.MADE_CHOICE -> {
                legalTurns += TurnOptions.PLACE_HABITAT_TILE
                legalTurns += TurnOptions.ROTATE
            }

            GameState.PLAYED_TILE -> {
                legalTurns += mutableListOf(TurnOptions.PLACE_WILDLIFE_TOKEN, TurnOptions.DISCARD_WILDLIFE_TOKEN)
            }

            GameState.END_OF_TURN -> {

            }
        }
        if (isFinished) legalTurns.clear()
    }

    private fun randomBotChooseOption(legalTurns: List<TurnOptions>): Int {
        return Random.nextInt(legalTurns.size)
    }

    private fun randomBotNatureTokenExchangeWildlife() {
        var count = Random.nextInt(5)
        val indices = mutableListOf<Int>()
        while (count > 0) {     //Geht so lange durch, bis er count einzigartige Indices erstellt hat
            val randomIndex = Random.nextInt(4)
            if (randomIndex !in indices) {
                indices += randomIndex
                count--
            }
        }
        //println("Bot changed Wildlife")
        rootService.playerActionService.changeWildlife(indices)
    }

    private fun randomBotNatureTokenFreeSelection() {
        val tileIndex = Random.nextInt(4)
        val wildlifeIndex = Random.nextInt(4)
        //println("Bot made Custom Choice")
        rootService.playerActionService.freeSelection(tileIndex, wildlifeIndex)
    }

    private fun randomBotClearSemipopulation() {
        //println("Bot cleared Semipopulation")
        rootService.gameService.exterminate(true)
    }

    private fun randomBotMakeSelection() {
        val index = Random.nextInt(4)
        rootService.playerActionService.selectColumn(index)
    }

    private fun randomBotPlaceHabitatTile(player: Player) {
        val possiblePositions = mutableListOf<Triple<Int, Int, Int>>()
        for (entry in player.board) {
            for (i in listOf(-1, 1)) {   //Geht alle Nachbarn durch und fügt neue leere Nachbarn zur Liste hinzu
                var option = Triple(entry.key.first, entry.key.second + i, entry.key.third - i)
                if (option !in possiblePositions && player.board[option] == null) possiblePositions += option
                option = Triple(entry.key.first + i, entry.key.second, entry.key.third - i)
                if (option !in possiblePositions && player.board[option] == null) possiblePositions += option
                option = Triple(entry.key.first + i, entry.key.second - i, entry.key.third)
                if (option !in possiblePositions && player.board[option] == null) possiblePositions += option
            }
        }
        val position = Random.nextInt(possiblePositions.size)
        coordinatesTile = possiblePositions[position]
        rootService.playerActionService.placeTile(possiblePositions[position])
    }

    private fun randomBotPlaceWildlifeToken(player: Player) {
        val currentGame = rootService.currentGame
        checkNotNull(currentGame)
        //welche Tiere besitzt der Bot gerade
        val selectedWildlife = currentGame.choices[currentGame.selectedChoice.second].second
        val possiblePositions = player.board.entries
            .filter { it.value.occupant == null && selectedWildlife in it.value.possibles }
            .map { it.key } //freie plätze
        // falls die Liste leer ist, müssen wir das Tier wegwerfen
        if (possiblePositions.isEmpty()) {
            randomBotDiscardWildlifeToken()
            return
        }
        val position = Random.nextInt(possiblePositions.size)
        coordinatesWildlifeToken = possiblePositions[position]
        rootService.playerActionService.placeWildlife(possiblePositions[position])
    }

    private fun randomBotDiscardWildlifeToken() {
        isFinished = true
    }

    private fun randomBotRotate() {
        val rotation = Random.nextBoolean()
        rootService.playerActionService.rotateTile(rotation)
    }
}


