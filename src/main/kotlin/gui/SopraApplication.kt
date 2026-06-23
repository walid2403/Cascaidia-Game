package gui

import tools.aqua.bgw.core.BoardGameApplication
import service.RootService
import tools.aqua.bgw.core.MenuScene

/**
 * Represents the main application for the SoPra board game.
 * The application initializes the [RootService] and displays the scenes.
 */
class SopraApplication : BoardGameApplication("SoPra Game") {

    /**
     * The root service instance. This is used to call service methods and access the entity layer.
     */
    val rootService: RootService = RootService()

    /**
     * The main game scene displayed in the application.
     */
    private val helloScene = GameScene(rootService)

    private val lobbyScene = LobbyScene(this@SopraApplication,rootService)

    private val joinScene = JoinOnlineScene(this@SopraApplication,rootService)


    private val mainMenuScene = MainMenuScene(this@SopraApplication,rootService).apply {
        hostButton.onMouseClicked = {
            this@SopraApplication.showMenuScene(lobbyScene)
        }
    }

    private val scoreScene = ScoreScene().apply {
        exitButton.onMouseClicked = {
            exit()
        }

        newGameButton.onMouseClicked = {
            this@SopraApplication.showMenuScene(mainMenuScene)
        }
    }

    /**
     * Initializes the application by displaying the [HelloScene].
     */
    init {
        this.showGameScene(helloScene)
        this.showMenuScene(mainMenuScene)
    }

}

