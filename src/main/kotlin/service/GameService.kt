package service

import entity.PlayerType

/**
 * The game service class of the Cascadia Game. It includes all functions which work mostly on the system-logic side
 */

class GameService(private val rootService: RootService): AbstractRefreshingService() {
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
    private fun calculateScores() {

    }

    /**
     * the function changes the current Player by rotating the [entity.CascadiaGame.playerQueue]
     * and setting the [entity.CascadiaGame.gameState] to [entity.GameState.START_OF_TURN]
     *
     * it also checks if the [entity.CascadiaGame.tileStack] is empty
     * if the condition is true, [calculateScores] is executed
     *
     *@throws IllegalStateException if Game is not in [entity.GameState.END_OF_TURN]
     *@throws IllegalArgumentException if the [entity.CascadiaGame.playerQueue] is empty
     *
     *
     */
    fun changeTurn() {

    }

    /**
     * this functions resolves the overpopulation of the wildlife tokens
     *
     * If all four wildlife tokens are identical, they are automatically
     * removed and replaced. If only three identical wildlife tokens are present,
     * the current player may choose whether to remove and replace them.
     *
     * Removed wildlife tokens are temporarily set aside and returned to the
     * wildlife bag after the replacement process has been completed.
     */
    fun exterminate() {

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
     * A function to start a new game from scratch
     *
     * @param playerList A list of the players to be entered into the game. The list must not be longer then four
     * elements or shorter than two elements. The [List] object contains a [Pair] object for every player with the
     * players name as a [String] and the players Type as a [PlayerType]
     * @param scoringCards The scoring cards to be used in the game, as a [List] of [Boolean] objects
     * @see [entity.CascadiaGame.scoringCards]
     *
     * @throws IllegalArgumentException If the list-size is not 2 - 4, if there are duplicate names or if there are
     * not exactly five scoringCards
     * @throws IllegalStateException If there is currently a game running
     */
    fun startNewGame(playerList: List<Pair<String, PlayerType>>, scoringCards: List<Boolean>) {

    }
}