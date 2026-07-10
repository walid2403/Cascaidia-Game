package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.CheckBox
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual


/**
 * This scene is the in-game pause scene. The player can turn on or off the animations from here, save and exit the
 * game, exit to the main menu without saving or exit the application entirely from here.
 * @param app The [SopraApplication] of this game
 * @param rootService The [RootService] instance to access the other service methods and entity layer
 */
class PauseMenuScene (private val app: SopraApplication, private val rootService: RootService):
    MenuScene(530, 590),
    Refreshable {

    private val paneWidth = 530
    private val paneHeight = 590
    private val textHeight = 200
    private val textWidth = paneWidth - 20

    private val menuPane = Pane<StaticComponentView<*>>(
        posX = 0,
        posY = 0,
        width = paneWidth,
        height = paneHeight,
        visual = ImageVisual("backgrounds/PauseMenuBackground.png").apply {
            style.borderRadius = BorderRadius(51)
        }
    )

    private val animationsEnabled = CheckBox(
        posX = 70,
        posY = 130,
        width = paneWidth - 140,
        height = 20,
        text = "Animations Enabled",
        alignment = Alignment.CENTER_LEFT,
        font = Font(38.0),
        isChecked = true
    ).apply {
        onCheckedChanged = {
            app.gameScene.animationsEnabled = isChecked
        }
    }

    private val mainMenuButton = Button(
        posX = 70,
        posY = 310,
        width = paneWidth - 140,
        height = 100,
        text = "Main Menu",
        font = Font(30.0,Color.WHITE,fontWeight = Font.FontWeight.BOLD,family = "Poppins"),
        visual = ColorVisual(236, 142, 14).apply {
            style.borderRadius = BorderRadius(31)
        }
    ).apply {
        onMouseClicked ={
            app.showMenuScene(MainMenuScene(app,rootService))
        }
    }

    val saveAndExitButton = Button(
        posX = 70,
        posY = 180,
        width = paneWidth - 140,
        height = 100,
        text = "Save and Exit",
        font = Font(30.0,Color.WHITE,fontWeight = Font.FontWeight.BOLD,family = "Poppins"),
        visual = ColorVisual(236, 142, 14).apply {
            style.borderRadius = BorderRadius(31)
        }
    ).apply {
        onMouseClicked = {
            textPane.isVisible = true
            listOf(exitButton, mainMenuButton, this).forEach { it.isDisabled = true}
//            rootService.playerActionService.saveGame("")
        }
    }

    private val exitButton = Button(
        posX = 70,
        posY = 440,
        width = paneWidth - 140,
        height = 100,
        text = "Exit",
        font = Font(30.0,Color.WHITE,fontWeight = Font.FontWeight.BOLD,family = "Poppins"),
        visual = ColorVisual(196, 75, 0).apply {
            style.borderRadius = BorderRadius(31)
        }
    ).apply {
        onMouseClicked ={
            app.exit()
        }
    }

    private val textPane = Pane<StaticComponentView<*>>(
        posX = 10,
        posY = (paneHeight - textHeight)/2 + 100,
        width = textWidth,
        height = textHeight,
        visual = ColorVisual(153, 172, 255).apply {
            style.borderRadius = BorderRadius(20)
        }
    ).apply {
        isVisible = false
    }

    private val saveButton = Button(
        posX = textWidth - 100,
        posY = textHeight - 50,
        width = 80,
        height = 40,
        text = "Save",
        font = Font(15.0,Color.WHITE,fontWeight = Font.FontWeight.BOLD,family = "Poppins"),
        visual = ColorVisual(196, 75, 0).apply {
            style.borderRadius = BorderRadius(15)
        }
    ).apply {
        onMouseClicked = {
            if(textField.text.isNotBlank()) {
                rootService.playerActionService.saveGame(textField.text.trim())
            }
        }
    }


    private val textField: TextField = TextField(
        width = textWidth - 40, height = 50,
        posX = (textWidth - (textWidth - 40))/2 , posY = (textHeight - 50)/2,
        prompt = "",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
    }

    private val fileNameLabel = Label(
        width = textWidth - 40,
        height = 50,
        posX = (textWidth - (textWidth - 40))/2 ,
        posY = (textHeight - 150)/2,
        text = "File Name:",
        font = Font(size = 28)
    )



    private val backButton = Button(
        posX = 440,
        posY = 30,
        width = 60,
        height = 60,
        text = "x",
        font = Font(30.0,Color.WHITE,fontWeight = Font.FontWeight.BOLD,family = "Poppins"),
        visual = ColorVisual(115, 115, 115).apply {
            style.borderRadius = BorderRadius(30)
        }
    ).apply {
        onMouseClicked ={
            app.hideMenuScene()
        }
    }

    init {

        listOf(textField,saveButton, fileNameLabel).forEach {  textPane.add(it)  }
        backgroundOpacity = 0.0
        addComponents(
            menuPane,
            mainMenuButton,
            animationsEnabled,
            saveAndExitButton,
            exitButton,
            backButton,
            textPane,
        )
    }

}