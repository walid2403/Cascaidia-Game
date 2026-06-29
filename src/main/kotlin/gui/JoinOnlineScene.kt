package gui

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

class JoinOnlineScene(private val app: SopraApplication,private val rootService: RootService) : MenuScene(1920, 1080), Refreshable  {

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
        visual = ImageVisual("GameConfigMenuBackground.png")
    )

    private val menuBackground = Label(
        posX = paneX,
        posY = paneY,
        width = paneWidth,
        height = paneHeight,
        visual = ImageVisual("JoinMenuBackground.png").apply{
            style.borderRadius = BorderRadius(35)
        }
    )

//    val exitButton = Button(
//        width = 100, height = 60,
//        posX = sceneWidth/2-330, posY = sceneHeight/2-300,
//        //text = "←",font = Font(size = 70, color = Color(0xFFFFFF))
//    ).apply {
//        visual = ColorVisual(64, 98, 70).apply { style.borderRadius = BorderRadius(8) }
//
//        onMouseClicked = {
//            app.showMenuScene(MainMenuScene(app,rootService))
//        }
//    }
    val exitButton = Button(
        width = 78, height = 78,
        posX = paneX + 33, posY = paneY + 23,
        visual = Visual.EMPTY
    ).apply {
        onMouseClicked = {
            app.showMenuScene(MainMenuScene(app,rootService))
        }
    }


    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val p2Input: TextField = TextField(
        width = 400, height = 50,
        posX = 1920/2 - 180, posY = 450,
        prompt = "",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
        onTextChanged = {
            if (!this.text.isBlank() && !p4Input.text.isBlank()) {
                joinButton.isDisabled = false
            } else {
                joinButton.isDisabled = true
            }

        }
    }

    private val p4Input: TextField = TextField(
        width = 400, height = 50,
        posX = 1920/2 - 180, posY = 600,
        prompt = "",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
        onTextChanged = {

            if (!this.text.isBlank() && !p2Input.text.isBlank()) {
                joinButton.isDisabled = false
            } else {
                joinButton.isDisabled = true
            }

        }

    }

    private val joinButton = Button(
        width = 100, height = 60,
        posX = 1920/2 + 230, posY = 1080/2 + 215,
        text = "Join",
        font = Font( size = 20,fontWeight = Font.FontWeight.BOLD)).apply {
        visual = ColorVisual(color = Color(0x99acff)).apply { style.borderRadius = BorderRadius(8) }
        isDisabled = true
        onMouseClicked = {
            app.showMenuScene(JoinOnlineLobbyScene(app,rootService))
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
        visual = ColorVisual(color = Color(0xFFFFFF))
            .apply {
                transparency = 0.0
            }
    )





    init {
        backgroundOpacity = .5
        addComponents(
            logo,
            menuBackground,
            exitButton,
            p2Input,
            p4Input,
            name,
            lobbyCode,
            joinButton,
        )
    }
}