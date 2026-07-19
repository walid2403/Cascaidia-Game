package gui

import entity.PlayerType
import service.Refreshable
import service.RootService
import tools.aqua.bgw.animation.DelayAnimation
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
 * In this scene a player can enter a name, select a [entity.PlayerType] and enter a Lobby Code before joining
 * a Lobby as a participant.
 * @param app The [SopraApplication] of this game
 * @param rootService The [RootService] instance to access the other service methods and entity layer
 */
class JoinOnlineScene(private val app: SopraApplication,private val rootService: RootService) :
    MenuScene(1920, 1080), Refreshable  {

    private val sceneWidth = 1920
    private val sceneHeight = 1080
    private val paneWidth = 750
    private val paneHeight = 620
    private val paneX = (sceneWidth-paneWidth)/2
    private val paneY = (sceneHeight-paneHeight)/2

    private val logo = Label(
        posX = 0,
        posY = 0,
        width = sceneWidth,
        height = sceneHeight,
        visual = ImageVisual("backgrounds/LoadingScreenBackground.png")
    )

    private val menuBackground = Label(
        posX = paneX,
        posY = paneY,
        width = paneWidth,
        height = paneHeight,
        visual = ImageVisual("backgrounds/JoinMenuBackground.png").apply{
            style.borderRadius = BorderRadius(35)
        }
    )

    private val moreButton = Button(
        width = 85,
        height = 21,
        posX = paneX + 641,
        posY = paneY + 115,
        visual = Visual.EMPTY
    ).apply {
        onMouseEntered = {
            playerTypeOverview.isVisible = true
        }
        onMouseExited = {
            playerTypeOverview.isVisible = false
        }
    }

    private val playerTypeOverview = Label(
        width = 460,
        height = 742,
        posX = 1367,
        posY = 169,
        visual = ImageVisual("assets/PlayerTypeOverview.png").apply {
            style.borderRadius = BorderRadius(51)
        }
    ).apply {
        isVisible = false
    }

    private val exitButton = Button(
        width = 78, height = 78,
        posX = paneX + 33, posY = paneY + 23,
        visual = Visual.EMPTY
    ).apply {
        onMouseClicked = {
            rootService.networkService.disconnect()
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
        }
    }

    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val nameInput: TextField = TextField(
        width = 400, height = 50,
        posX = 1920/2 - 180, posY = 450,
        prompt = "",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
    }

    private val lobbyCodeInput: TextField = TextField(
        width = 400, height = 50,
        posX = 1920/2 - 180, posY = 600,
        prompt = "",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
    }

    private val joinButton = Button(
        width = 100, height = 60,
        posX = 1920/2 + 230, posY = 1080/2 + 215,
        text = "Join",
        font = Font( size = 20,fontWeight = Font.FontWeight.BOLD)).apply {
        visual = ColorVisual(153, 172, 255).apply {
            style.borderRadius = BorderRadius(8)
        }
        onMouseClicked = {
            if(nameInput.text.isBlank()) {
                showWarning("You must enter a name before joining a lobby.")
            } else if(lobbyCodeInput.text.isBlank()) {
                showWarning("You must enter a lobby code to enter a lobby.")
            } else {
                rootService.networkService.joinGame(nameInput.text, getPlayerType(playerType), lobbyCodeInput.text)
            }
        }
    }

    private val warning = Label(
        width = 350,
        height = 100,
        posX = paneX + (joinButton.posX - paneX)/2 - 175,
        posY = joinButton.posY - 40,
        font = Font(size = 23, color = Color.WHITE, family = "Canva Sans"),
        isWrapText = true,
        visual = ColorVisual(204, 78, 0).apply {
            style.borderRadius = BorderRadius(15)
        }
    ).apply {
        isVisible = false
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

    init {
        backgroundOpacity = .5
        addComponents(
            logo,
            menuBackground,
            exitButton,
            nameInput,
            lobbyCodeInput,
            name,
            lobbyCode,
            joinButton,
            playerTypeIcon,
            switchTypeLeftButton,
            switchTypeRightButton,
            warning,
            playerTypeOverview,
            moreButton,
        )
    }

    private fun resetScene() {
        lobbyCodeInput.text = ""
        nameInput.text = ""
        warning.isVisible = false
        playerType = 0
        playerTypeIcon.visual = ImageVisual("icons/HumanIcon2.png")
    }

    private fun changePlayerType(leftButton: Boolean, playerType: Int): Int {
        val newType = if (leftButton) {
            (playerType + 7) % 8
        } else {
            (playerType + 1) % 8
        }

        val newVisual = when (newType) {

            0 -> ImageVisual("icons/HumanIcon2.png")

            1 -> ImageVisual("icons/EasyBotIcon2.png")

            2 -> ImageVisual("icons/HardBotIcon2.png")

            3 -> ImageVisual("icons/MonteCarloBotIcon2.png")

            4 -> ImageVisual("icons/GreedyBotIcon2.png")

            5 -> ImageVisual("icons/HeuristicBotIcon2.png")

            6 -> ImageVisual("icons/GreedyHeuristicBotIcon2.png")

            7 -> ImageVisual("icons/NeuralNetworkBotIcon2.png")

            else -> throw IllegalArgumentException("Only possible visuals, $newType is invalid")
        }

        playerTypeIcon.visual = newVisual

        return newType
    }

    private fun getPlayerType(playerTypeInt: Int): PlayerType {
        return when (playerTypeInt) {
            //TODO("Greedy Heuristic Bot wieder einkommentieren wenn der implementiert ist")

            0 -> PlayerType.HUMAN
            1 -> PlayerType.EASY_BOT
            2 -> PlayerType.HARD_BOT
            3 -> PlayerType.MONTE_BOT
            4 -> PlayerType.GREEDY_BOT
            5 -> PlayerType.HEURISTIC_BOT
            6 -> PlayerType.GREEDY_HEURISTIC_BOT
            7 -> PlayerType.NEURAL_BOT
            else -> throw IllegalArgumentException("Only numbers between 0 and 3 are valid")
        }
    }

    /**
     * This function sets the text of [warning] to [text] and makes [warning] visible for 3 seconds
     */
    private fun showWarning(text: String) {
        warning.text = text
        warning.isVisible = true
        playAnimation(
            DelayAnimation(3000).apply {
                onFinished = {
                    warning.isVisible = false
                }
            }
        )
    }

    override fun refreshAfterJoinGame(lobbyCode: String, playerName: String, playerType: PlayerType) {
        resetScene()
        app.showMenuScene(app.joinOnlineLobbyScene)
    }

    override fun refreshAfterConnectionError(errorMessage: String) {
        showWarning(errorMessage)
    }
}