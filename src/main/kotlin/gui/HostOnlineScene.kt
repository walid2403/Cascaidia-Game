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

class HostOnlineScene(private val app: SopraApplication,private val rootService: RootService) : MenuScene(1920, 1080), Refreshable  {


    private val logo = Label(
        posX = 0,
        posY = 0,
        width = 1920,
        height = 1080,
        visual = ImageVisual("HostHintergrund.png")

    )

    val exitButton = Button(
        width = 100, height = 60,
        posX = 1920/2-330, posY = 1080/2-300,
        text = "←",font = Font(size = 70, color = Color(0xFFFFFF))
    ).apply {
        visual = ColorVisual(64, 98, 70).apply { style.borderRadius = BorderRadius(8) }

        onMouseClicked = {
            app.showMenuScene(MainMenuScene(app,rootService))
        }
    }


    private val joinButton = Button(
        width = 100, height = 60,
        posX = 1920/2 + 230, posY = 1080/2 + 215,
        text = "Next",
        font = Font( size = 20,fontWeight = Font.FontWeight.BOLD)).apply {
        visual = ColorVisual(color = Color(0x99acff)).apply { style.borderRadius = BorderRadius(8) }
        isDisabled = true
        onMouseClicked = {
            app.showMenuScene(HostOnlineLobbyScene(app,rootService))
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
            if (this.text.isBlank()) {
                joinButton.isDisabled = true
            } else{
                joinButton.isDisabled = false
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


//    private val infoButton : Label  = Label(
//        posX =1920/2 - 200 , posY = 550,
//        width = 50, height = 50,
//        text = "",
//        visual = ColorVisual(color = Color(0xFFFFFF))
//    ).apply {
//        onMouseEntered ={
//            infoBox.isVisible = true
//        }
//        onMouseExited = { infoBox.isVisible = false }
//    }
//
//    private val infoBox : Label  = Label(
//        posX =1920/2 - 200 , posY = 550,
//        width = 500, height = 30,
//        text = "the Lobby-Code is optional. If you don't enter on a random one will be assigned to you",
//        visual = ColorVisual(color = Color(0xFFFFFF))
//            .apply {
//                transparency = 0.7
//            }
//    ).apply {
//        isVisible = false
//    }





    init {
        backgroundOpacity = .5
        addComponents(
            logo,
            exitButton,
            p2Input,
            p4Input,
            name,
//            infoButton,
//            infoBox,
            lobbyCode,
            joinButton,
        )
    }
}