package gui

import tools.aqua.bgw.core.BoardGameApplication
import service.RootService


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
    private val helloScene = GameScene(this@SopraApplication,rootService)


    private val mainMenuScene = MainMenuScene(this@SopraApplication,rootService)

    private val scoreScene = ScoreScene(this@SopraApplication, rootService)

    private val joinOnlineLobbyScene = JoinOnlineLobbyScene(this@SopraApplication, rootService)

    private val hostOnlineLobbyScene = HostOnlineLobbyScene(this@SopraApplication, rootService)

    /**
     * Initializes the application by displaying the [MainMenuScene].
     */
    init {
        this.showGameScene(helloScene)
        this.showMenuScene(mainMenuScene )
        //this.showMenuScene(scoreScene)
    }
}

