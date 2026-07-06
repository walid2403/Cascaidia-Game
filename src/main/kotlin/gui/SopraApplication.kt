package gui

import service.Refreshable
import tools.aqua.bgw.core.BoardGameApplication
import service.RootService

/**
 * Represents the main application for the Cascadia board game.
 * The application initializes the [RootService] and displays the scenes.
 */
class SopraApplication : BoardGameApplication("SoPra Game"), Refreshable {

    /**
     * The root service instance. This is used to call service methods and access the entity layer.
     */
    val rootService: RootService = RootService()

    /**
     * The main game scene displayed in the application.
     */
    val gameScene = GameScene(this@SopraApplication,rootService)

    private val mainMenuScene = MainMenuScene(this@SopraApplication,rootService)

    private val pauseMenu = PauseMenuScene(this@SopraApplication,rootService)

    private val scoreScene = ScoreScene(this@SopraApplication, rootService)

    val joinOnlineLobbyScene = JoinOnlineLobbyScene(this@SopraApplication, rootService, "name", 0)

    val hostOnlineLobbyScene = HostOnlineLobbyScene(this@SopraApplication, rootService, "Name", 0)

    val hostOnlineScene = HostOnlineScene(this@SopraApplication, rootService)
    val joinOnlineScene = JoinOnlineScene(this@SopraApplication, rootService)
    val lobbyScene = LobbyScene(this@SopraApplication, rootService)

    /**
     * Initializes the application by displaying the [MainMenuScene].
     */
    init {
        rootService.addRefreshables(
            this,
            gameScene,
            mainMenuScene, pauseMenu, scoreScene, joinOnlineLobbyScene, hostOnlineLobbyScene, hostOnlineScene, joinOnlineScene, lobbyScene,
        )
        this.showGameScene(gameScene)
        this.showMenuScene(mainMenuScene )
        //this.showMenuScene(scoreScene)
        //this.showMenuScene(testing(this@SopraApplication, rootService))
    }

    override fun refreshAfterStartGame() {
        println("refresh aufgerufen")
        this.hideMenuScene()
    }
}

