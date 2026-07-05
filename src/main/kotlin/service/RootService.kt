package service

import entity.*
import service.bot.Bot
import service.bot.HeuristicBot


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
    val bot = Bot(this)
    val heuristicBot = HeuristicBot(this)

    var currentGame : CascadiaGame ?= null
    val history = CascadiaGames()

    /**
     * Adds the provided [newRefreshable] to all services connected
     * to this root service
     */
    fun addRefreshable(newRefreshable: Refreshable) {
        gameService.addRefreshable(newRefreshable)
        playerActionService.addRefreshable(newRefreshable)
    }
}