package gui

import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.components.layoutviews.Pane

class PauseMenuScene {

    private val sceneWidth = 1920
    private val sceneHeight = 1080
    private val paneWidth = 530
    private val paneHeight = 590

    private val menuPane = Pane<StaticComponentView<*>>(
        posX = (sceneWidth - paneWidth) / 2,
        posY = (sceneHeight - paneHeight) / 2,
        width = paneWidth, height = paneHeight
    )


}