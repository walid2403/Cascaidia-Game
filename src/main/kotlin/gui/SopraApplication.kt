package gui

import tools.aqua.bgw.core.BoardGameApplication
import service.RootService
import service.Refreshable

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

    val mainMenuScene = MainMenuScene(this@SopraApplication,rootService)

    val pauseMenu = PauseMenuScene(this@SopraApplication,rootService)

    private val scoreScene = ScoreScene(this@SopraApplication, rootService)

    val joinOnlineLobbyScene = JoinOnlineLobbyScene(this@SopraApplication, rootService)

    val hostOnlineLobbyScene = HostOnlineLobbyScene(this@SopraApplication, rootService)

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
        this.hideMenuScene()
        this.showGameScene(gameScene)
    }

    override fun refreshAfterSaveGame() {
        this.showMenuScene(mainMenuScene)
    }

    override fun refreshAfterLoadGame() {
        this.hideMenuScene()
    }

    override fun refreshAfterEndGame(scores: List<Pair<String, List<Int>>>) {
        this.showMenuScene(scoreScene)
    }
}

