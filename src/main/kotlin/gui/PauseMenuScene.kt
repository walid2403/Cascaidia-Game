package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.CheckBox
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual

class PauseMenuScene (private val app: SopraApplication, private val rootService: RootService): MenuScene(530, 590),
    Refreshable {

    private val paneWidth = 530
    private val paneHeight = 590

    private val menuPane = Pane<StaticComponentView<*>>(
        posX = 0,
        posY = 0,
        width = paneWidth,
        height = paneHeight,
        visual = ImageVisual("PauseMenuBackground.png").apply {
            style.borderRadius = BorderRadius(51)
        }
    )

    private val animationsEnabled = CheckBox(
        posX = 70,
        posY = 115,
        width = 300,
        height = 20,
        text = "Animations Enabled",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0),
        isChecked = true
    )

    private val mainMenuButton = Button(
        posX = 70,
        posY = 180,
        width = paneWidth - 140,
        height = 100,
        text = "Main Menu",
        font = Font(30.0,Color.WHITE,fontWeight = Font.FontWeight.BOLD,family = "Poppins"),
        visual = ColorVisual(236, 142, 14).apply {
            style.borderRadius = BorderRadius(31)
        }
    )

    private val saveAndExitButton = Button(
        posX = 70,
        posY = 310,
        width = paneWidth - 140,
        height = 100,
        text = "Save and Exit",
        font = Font(30.0,Color.WHITE,fontWeight = Font.FontWeight.BOLD,family = "Poppins"),
        visual = ColorVisual(236, 142, 14).apply {
            style.borderRadius = BorderRadius(31)
        }
    )

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
    )

    init {
        backgroundOpacity = 0.0
        addComponents(
            menuPane,
            mainMenuButton,
            animationsEnabled,
            saveAndExitButton,
            exitButton
        )
    }

}