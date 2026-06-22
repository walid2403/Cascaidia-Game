package gui


import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.*


class MainMenuScene(private val app: SopraApplication,private val rootService: RootService) : MenuScene(1920, 1080), Refreshable {

    private val logo = Label(
        posX = 0,
        posY = 0,
        width = 1920,
        height = 1080,
        visual = ImageVisual("MenuHintergrund.png")

    )


    val newGameButton = Button(
        width = 200, height = 50,
        posX = 1920/2 -270, posY = 1080/2 + 50,
        text = "New Game",font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }

        onMouseClicked = {
            app.showMenuScene(LobbyScene(app,rootService))
        }
    }

    val loadButton = Button(
        width = 200, height = 50,
        posX = 1920/2 -270, posY = 1080/2 + 150,
        text = "Load",font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
        onMouseClicked = {

        }
    }

    val hostButton = Button(
        width = 200, height = 50,
        posX = 1920/2 +100, posY = 1080/2 + 50,
        text = "Host",font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
        onMouseClicked = {

        }
    }
    val joinButton = Button(
        width = 200, height = 50,
        posX = 1920/2 + 100, posY = 1080/2 + 150,
        text = "Join",font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }

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


    init {
        backgroundOpacity = .5
        addComponents(
            logo,
            hostButton,
            loadButton,
            newGameButton,
            joinButton,
            exitButton
        )
    }
}