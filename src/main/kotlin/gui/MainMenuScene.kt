package gui


import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.*

/**
 * This is the Main Menu Scene of the game, the first thing that is shown when starting the application.
 * A player can decide between loading a previously started game, starting a new local game, starting a game lobby as
 * host or joining another players lobby from here.
 * @param app The [SopraApplication] of this game
 * @param rootService The [RootService] instance to access the other service methods and entity layer
 */
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
        visual = ImageVisual("backgrounds/ScoreSceneBackground.png")
    )
    private val menuBackground = Pane<StaticComponentView<*>>(
        posX = paneX,
        posY = paneY,
        width = paneWidth,
        height = paneHeight,
        visual = ImageVisual("backgrounds/MainMenuPaneBackground.png").apply {
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

    private var loadActive = false

    val loadButton = Button(
        width = 260, height = 70,
        //posX = 1920/2 -270, posY = 1080/2 + 150,
        posX = (paneWidth - (2*260))/4,
        posY = paneHeight/2 + 2*(paneHeight/2 - 2*70)/3 + 70,
        text = "Load",font = Font(size = 31)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(15) }
        onMouseClicked = {
            if(loadActive) {
                enableSaveEntry(false)
            } else {

                enableSaveEntry(true)
            }
            loadActive = !loadActive
        }
    }

    private val saveNameTextField = TextField(
        width = 260,
        height = 55,
        posX = (paneWidth - (2*260))/4,
        posY = paneHeight/2 + 2*(paneHeight/2 - 2*70)/3 + 70 + 80,
        prompt = "Enter the name of your save",
        font = Font(size = 18, color = Color.DARK_GRAY),
        visual = ColorVisual(Color.WHITE).apply {
            style.borderRadius = BorderRadius(8)
        }
    ).apply {
        isVisible = false
        onTextChanged = {
            enableConfirmButton(this.text.isNotBlank())
        }
    }

    private val confirmButton = Button(
        width = 80,
        height = 55,
        posX = (paneWidth - (2*260))/4 + 270,
        posY = loadButton.posY + 80,
        text = "confirm",
        font = Font(size = 18, family = "Canva Sans", color = Color.WHITE, fontWeight = Font.FontWeight.BOLD),
        visual = ColorVisual(173, 208, 75).apply {
            style.borderRadius = BorderRadius(8)
        }
    ).apply {
        isVisible = false
        isDisabled = true
        onMouseClicked = {
            rootService.gameService.loadGame(saveNameTextField.text)
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
        visual = ImageVisual("icons/EasyBotIcon.png")
    )
    private val hardBotIcon = Label(
        posX = sceneWidth/2,
        posY = sceneHeight/2,
        height = 62,
        width = 63,
        visual = ImageVisual("icons/HardBotIcon.png")
    )

    private val easyBotIcon2 = Label(
        posX = sceneWidth/2,
        posY = sceneHeight/2,
        height = 70,
        width = 110,
        visual = ImageVisual("icons/EasyBotIcon2.png")
    )
    private val hardBotIcon2 = Label(
        posX = sceneWidth/2,
        posY = sceneHeight/2,
        height = 70,
        width = 110,
        visual = ImageVisual("icons/HardBotIcon2.png")
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
            saveNameTextField,
            confirmButton,
        )
    }

    private fun enableSaveEntry(enable: Boolean) {
        if(enable) {
            saveNameTextField.isVisible = true
            confirmButton.isVisible = true
        } else {
            saveNameTextField.isVisible = false
            confirmButton.isVisible = false
        }
    }

    private fun enableConfirmButton(enable: Boolean) {
        confirmButton.isDisabled = !enable
    }
}