package service

import entity.*


/**
 * The root service class is responsible for managing services and the entity layer reference.
 * This class acts as a central hub for every other service within the application and holds the [currentGame] state for
 * these services to access.
 * It also holds the [history] of all moves.
 *
 */
class RootService {

    val gameService = GameService(this)
    val playerActionService = PlayerActionService(this)

    var currentGame : CascadiaGame ?= null
    val history = CascadiaGames()

    /**
     * Companion object for the `RootService` class that contains constant values
     * related to the saving and loading of game data.
     *
     * - `SAVE_DIRECTORY`: Specifies the directory where game save files are stored.
     * - `SAVE_EXTENSION`: Specifies the file extension used for save files.
     */
    companion object {
        const val SAVE_DIRECTORY = "SavedGames"
        const val SAVE_EXTENSION = ".cascadia"
    }

    /**
     * Adds the provided [newRefreshable] to all services connected
     * to this root service
     */
    fun addRefreshable(newRefreshable: Refreshable) {
        gameService.addRefreshable(newRefreshable)
        playerActionService.addRefreshable(newRefreshable)
    }
}

