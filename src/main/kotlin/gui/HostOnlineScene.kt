package gui

import entity.PlayerType
import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual
import tools.aqua.bgw.visual.Visual

/**
 * In this scene a player can enter a name, select a [entity.PlayerType] and chose a Lobby Code before opening
 * a Lobby in the role of Host.
 * @param app The [SopraApplication] of this game
 * @param rootService The [RootService] instance to access the other service methods and entity layer
 */
class HostOnlineScene(private val app: SopraApplication, private val rootService: RootService) :
    MenuScene(1920, 1080), Refreshable  {

    private val sceneWidth = 1920
    private val sceneHeight = 1080
    private val paneX = (sceneWidth - 750) / 2
    private val paneY = (sceneHeight - 620) / 2
    private val paneWidth = 750
    private val paneHeight = 620

    private val logo = Label(
        posX = 0,
        posY = 0,
        width = 1920,
        height = 1080,
        visual = ImageVisual("backgrounds/GameConfigMenuBackground.png")
    )

    private val menuBackground = Label(
        posX = paneX,
        posY = paneY,
        width = paneWidth,
        height = paneHeight,
        visual = ImageVisual("backgrounds/HostMenuBackground.png").apply {
            style.borderRadius = BorderRadius(35)
        }
    )

    val exitButton = Button(
        width = 78, height = 78,
        posX = paneX + 33, posY = paneY + 23,
        visual = Visual.EMPTY
    ).apply {
        onMouseClicked = {
            resetScene()
            app.showMenuScene(app.mainMenuScene)
        }
    }

    private var playerType = 0

    private val playerTypeIcon = Label(
        posX = sceneWidth/2 - 195 - 110,
        posY = 440,
        width = 110,
        height = 70,
        visual = ImageVisual("icons/HumanIcon2.png")
    )

    private val switchTypeLeftButton = Button(
        posX = playerTypeIcon.posX,
        posY = playerTypeIcon.posY,
        width = 45,
        height = 110,
        visual = Visual.EMPTY
    ).apply {
        onMouseClicked = {
            playerType = changePlayerType(true, playerType)
            warning.isVisible = false
            warning.text = ""
        }
    }

    private val switchTypeRightButton = Button(
        posX = playerTypeIcon.posX + playerTypeIcon.width - 45,
        posY = playerTypeIcon.posY,
        width = 45,
        height = 110,
        visual = Visual.EMPTY
    ).apply {
        onMouseClicked = {
            playerType = changePlayerType(false, playerType)
            warning.isVisible = false
            warning.text = ""
        }
    }

    private val joinButton = Button(
        width = 100, height = 60,
        posX = 1920/2 + 230, posY = 1080/2 + 215,
        text = "Next",
        font = Font( size = 20,fontWeight = Font.FontWeight.BOLD)).apply {
        visual = ColorVisual(color = Color(0x99acff)).apply { style.borderRadius = BorderRadius(8) }
        isDisabled = false
        onMouseClicked = {
            var name = nameInput.text
            if(name.isBlank()) {
                warning.text = "Enter a name before opening the lobby."
                warning.isVisible = true
            } else {
                rootService.networkService.hostGame(name, getPlayerType(playerType), lobbyInput.text)
            }
        }
    }

    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val nameInput: TextField = TextField(
        width = 400, height = 50,
        posX = 1920/2 - 180, posY = 450,
        prompt = "Enter Name",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(204, 212, 209).apply {
            style.borderRadius = BorderRadius(8)
        }
        onTextChanged = {
            warning.isVisible = false
            warning.text = ""
        }
    }

    private val lobbyInput: TextField = TextField(
        width = 400, height = 50,
        posX = 1920/2 - 180, posY = 600,
        prompt = "(optional)",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(204, 212, 209).apply {
            style.borderRadius = BorderRadius(8)
        }
        onTextChanged = {
            warning.isVisible = false
            warning.text = ""
        }
    }

    private val name : Label  = Label(
        posX =1920/2 - 180 , posY = 400,
        width = 120, height = 30,
        text = "Name:",
        font = Font(
            size = 28,
            color = Color(0xFFFFFF),
            family = "Arial",
        ),
        alignment = Alignment.CENTER,
        isWrapText = false,
        visual = ColorVisual(color = Color(0xFFFFFF))
            .apply {
                transparency = 0.0
            }
    )

    private val lobbyCode : Label  = Label(
        posX =1920/2 - 180 , posY = 550,
        width = 200, height = 30,
        text = "Lobby-Code:",
        font = Font(
            size = 28,
            color = Color(0xFFFFFF),
            family = "Arial",
            fontWeight = Font.FontWeight.NORMAL,
            fontStyle = Font.FontStyle.NORMAL
        ),
        alignment = Alignment.CENTER,
        isWrapText = false,
        visual = ColorVisual(color = Color(0xFFFFFF)).apply {
            transparency = 0.0
        }
    )

    private val warning = Label(
        width = 350,
        height = 100,
        posX = paneX + ((1920/2 + 230) - paneX)/2 - 175,
        posY = (1080/2 + 215) - 40,
        font = Font(size = 23, color = Color.WHITE, family = "Canva Sans"),
        isWrapText = true,
        visual = ColorVisual(204, 78, 0).apply {
            style.borderRadius = BorderRadius(15)
        }
    ).apply {
        isVisible = false
        text = ""
    }

    init {
        backgroundOpacity = .5
        addComponents(
            logo,
            menuBackground,
            exitButton,
            nameInput,
            lobbyInput,
            name,
            lobbyCode,
            joinButton,
            playerTypeIcon,
            switchTypeLeftButton,
            switchTypeRightButton,
            warning,
        )
    }

    private fun resetScene() {
        lobbyInput.text = ""
        nameInput.text = ""
        warning.isVisible = false
        playerType = 0
        playerTypeIcon.visual = ImageVisual("icons/HumanIcon2.png")
    }

    /**
     * This function adjusts the playerType and the players icon according to if the left or right button was pressed
     */
    private fun changePlayerType(leftButton: Boolean, playerType: Int): Int {
        val newType = if (leftButton) {
            (playerType + 2) % 3
        } else {
            (playerType + 1) % 3
        }

        val newVisual = when (newType) {
            0 -> ImageVisual("icons/HumanIcon2.png")

            1 -> ImageVisual("icons/EasyBotIcon2.png")

            2 -> ImageVisual("icons/HardBotIcon2.png")

            else -> throw IllegalArgumentException("Only possible visuals, $newType is invalid")
        }

        playerTypeIcon.visual = newVisual

        return newType
    }

    private fun getPlayerType(playerTypeInt: Int): PlayerType {
        return when (playerTypeInt) {
            0 -> PlayerType.HUMAN
            1 -> PlayerType.EASY_BOT
            2 -> PlayerType.HARD_BOT
            else -> throw IllegalArgumentException("Only numbers between 0 and 3 are valid")
        }
    }

    override fun refreshAfterConnectionError(errorMessage: String) {
        warning.text = errorMessage
        warning.isVisible = true
    }

    override fun refreshAfterHostGame(lobbyCode: String, playerName: String, playerType: PlayerType) {
        resetScene()
        app.showMenuScene(app.hostOnlineLobbyScene)
    }
}