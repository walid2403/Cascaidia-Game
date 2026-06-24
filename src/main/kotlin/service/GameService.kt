package service

import entity.*

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
        checkNotNull(game) {"No current game"}

        val checkCondition = game.gameState == GameState.PLAYED_TILE || game.gameState == GameState.END_OF_TURN
        check(checkCondition) {"Current Turn can not be ended"}

        val currentPlayer = game.playerQueue.poll()
        game.playerQueue.add(currentPlayer)

        game.gameState = GameState.START_OF_TURN

        game.selectedChoice = Pair(-1, -1)

        val nextPlayer = game.playerQueue.peek()

        if (nextPlayer.board.size == 23) {
            calculateScores()
            return
        }

        if (nextPlayer.type == PlayerType.HUMAN && game.isLocal) {
            rootService.history.prevMoves.push(game)
        }

        onAllRefreshables { refreshAfterChangeTurn(nextPlayer.board.size == 22) }
    }
}