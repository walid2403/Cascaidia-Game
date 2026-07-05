package gui


import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.*


class MainMenuScene(private val app: SopraApplication,private val rootService: RootService) :
    MenuScene(1920, 1080), Refreshable {

    private val sceneWidth = 1920
    private val sceneHeight = 1080
    private val paneX = (sceneWidth - 750) / 2
    private val paneY = (sceneHeight - 620) / 2
    private val paneWidth = 750
    private val paneHeight = 620

    private val logo = Label(
        posX = 0,
        posY = 0,
        width = sceneWidth,
        height = sceneHeight,
        visual = ImageVisual("ScoreSceneBackground.png")
    )
    private val menuBackground = Pane<StaticComponentView<*>>(
        posX = paneX,
        posY = paneY,
        width = paneWidth,
        height = paneHeight,
        visual = ImageVisual("MainMenuPaneBackground.png").apply {
            style.borderRadius = BorderRadius(35)
        }
    )

    val newGameButton = Button(
        width = 260, height = 70,
        //posX = 1920/2 -270, posY = 1080/2 + 50,
        posX = (paneWidth - (2*260))/4,
        posY = paneHeight/2 + (paneHeight/2 - 2*70)/3,
        text = "New Game",font = Font(size = 31)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(15) }

        onMouseClicked = {
            app.showMenuScene(LobbyScene(app,rootService))
        }
    }

    val loadButton = Button(
        width = 260, height = 70,
        //posX = 1920/2 -270, posY = 1080/2 + 150,
        posX = (paneWidth - (2*260))/4,
        posY = paneHeight/2 + 2*(paneHeight/2 - 2*70)/3 + 70,
        text = "Load",font = Font(size = 31)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(15) }
        onMouseClicked = {

        }
    }

    val hostButton = Button(
        width = 260, height = 70,
        //posX = 1920/2 +100, posY = 1080/2 + 50,
        posX = 3*(paneWidth - (2*260))/4 + 260,
        posY = paneHeight/2 + (paneHeight/2 - 2*70)/3,
        text = "Host",font = Font(size = 31)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(15) }
        onMouseClicked = {
            app.showMenuScene(HostOnlineScene(app,rootService))
        }
    }
    val joinButton = Button(
        width = 260, height = 70,
        //posX = 1920/2 + 100, posY = 1080/2 + 150,
        posX = 3*(paneWidth - (2*260))/4 + 260,
        posY = paneHeight/2 + 2*(paneHeight/2 - 2*70)/3 + 70,
        text = "Join",font = Font(size = 31)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(15) }

        onMouseClicked = {
            app.showMenuScene(JoinOnlineScene(app, rootService))
        }

    }

    val exitButton = Button(
        width = 200, height = 50,
        posX = 20, posY = 1010,
        text = "Exit",font = Font(size = 28)
    ).apply {
        visual = ColorVisual(204, 78, 0).apply { style.borderRadius = BorderRadius(8) }
            onMouseClicked = {
                app.exit()
            }
    }

    //
    // these labels will not ever be visible but having them in the main menu scene will preload the images
    // this will make the user experience in the following menu scenes smoother, because the blip between images
    // loading is eliminated
    //

    private val easyBotIcon = Label(
        posX = sceneWidth/2,
        posY = sceneHeight/2,
        height = 63,
        width = 63,
        visual = ImageVisual("EasyBotIcon.png")
    )
    private val hardBotIcon = Label(
        posX = sceneWidth/2,
        posY = sceneHeight/2,
        height = 62,
        width = 63,
        visual = ImageVisual("HardBotIcon.png")
    )

    private val easyBotIcon2 = Label(
        posX = sceneWidth/2,
        posY = sceneHeight/2,
        height = 70,
        width = 110,
        visual = ImageVisual("EasyBotIcon2.png")
    )
    private val hardBotIcon2 = Label(
        posX = sceneWidth/2,
        posY = sceneHeight/2,
        height = 70,
        width = 110,
        visual = ImageVisual("HardBotIcon2.png")
    )

    init {
        backgroundOpacity = .5
        addComponents(
            easyBotIcon,
            hardBotIcon,
            easyBotIcon2,
            hardBotIcon2,
            logo,
            menuBackground,
            exitButton
        )
        menuBackground.addAll(
            hostButton,
            loadButton,
            newGameButton,
            joinButton,
        )
    }
}