package gui

import tools.aqua.bgw.core.BoardGameApplication
import service.RootService
import service.Refreshable

/**
 * Represents the main application for the Cascadia board game.
 * The application initializes the [RootService] and displays the scenes.
 * @property rootService ein Objekt des Typs [RootService], Verbindung zum RootService
 * @property gameScene ein Objekt des Typs [GameScene], die GameScene des Spiels
 * @property mainMenuScene ein Objekt des Typs [MainMenuScene], die MainMenuScene des Spiels
 * @property pauseMenu ein Objekt des Typs [pauseMenu], die pauseMenuScene des Spiels
 * @property scoreScene ein Objekt des Typs [scoreScene], die ScoreScene des Spiels
 * @property joinOnlineLobbyScene ein Objekt des Typs [joinOnlineLobbyScene], die JoinOnlineLobbyScene des Spiels
 * @property hostOnlineLobbyScene ein Objekt des Typs [hostOnlineLobbyScene], die HostOnlineLobbyScene des Spiels
 * @property hostOnlineScene ein Objekt des Typs [hostOnlineScene], die HostOnlineScene des Spiels
 * @property joinOnlineScene ein Objekt des Typs [joinOnlineScene], die JoinOnlineScene des Spiels
 * @property lobbyScene ein Objekt des Typs [lobbyScene], die LobbyScene des Spiels
 */
class SopraApplication : BoardGameApplication("SoPra Game"), Refreshable {

    /**
     * The root service instance. This is used to call service methods and access the entity layer.
     */
    val rootService: RootService = RootService()

    /**
     * The main game scene displayed in the application.
     */
    val gameScene = GameScene(this@SopraApplication, rootService)

    val mainMenuScene = MainMenuScene(this@SopraApplication, rootService)

    val pauseMenu = PauseMenuScene(this@SopraApplication, rootService)

    val scoreScene = ScoreScene(this@SopraApplication, rootService)

    val joinOnlineLobbyScene = JoinOnlineLobbyScene(this@SopraApplication)

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
            mainMenuScene, pauseMenu, scoreScene, joinOnlineLobbyScene, hostOnlineLobbyScene, hostOnlineScene,
            joinOnlineScene, lobbyScene,
        )
        this.showGameScene(gameScene)
        this.showMenuScene(mainMenuScene )
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

