package service

import entity.CascadiaGame


/**
 * The root service class is responsible for managing services and the entity layer reference.
 * This class acts as a central hub for every other service within the application and holds the [currentGame] state for
 * these services to access.
 *
 */
class RootService {

    val gameService = GameService(this)
    val playerActionService = PlayerActionService(this)

    var currentGame : CascadiaGame?= null

    /**
     * Adds the provided [newRefreshable] to all services connected
     * to this root service
     */
    fun addRefreshable(newRefreshable: Refreshable) {
        gameService.addRefreshable(newRefreshable)
        playerActionService.addRefreshable(newRefreshable)
    }
}