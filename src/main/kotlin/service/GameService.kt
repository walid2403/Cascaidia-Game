package service

import entity.PlayerType

/**
 * The game service class of the Cascadia Game. It includes all functions which work mostly on the system-logic side
 */

class GameService(private val rootService: RootService): AbstractRefreshingService() {

    private fun calculateScores() {

    }

    fun changeTurn() {

    }

    fun exterminate() {

    }

    /**
     * A function to load a previously saved game. The saved game is identified by the name Parameter.
     *
     * @param name The name of the previously saved game
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
     */
    fun startNewGame(playerList: List<Pair<String, PlayerType>>, scoringCards: List<Boolean>) {

    }
}