package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.CheckBox
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
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
        width = 45,
        height = 45,
        text = "Animations Enabled",
        isChecked = true
    )

    private val mainMenuButton = Button(
        posX = 70,
        posY = 180,
        width = paneWidth - 140,
        height = 100,
        text = "Main Menu",
        visual = ColorVisual(236, 142, 14).apply {
            style.borderRadius = BorderRadius(31)
        }
    )

    init {
        addComponents(
            menuPane,
            mainMenuButton,
            animationsEnabled,
        )
    }

}