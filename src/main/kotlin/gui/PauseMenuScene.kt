package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.MenuScene

class PauseMenuScene(private val app: SopraApplication,private val rootService: RootService) : MenuScene(530, 590), Refreshable {

//    private val sceneWidth = 1920
//    private val sceneHeight = 1080
    private val paneWidth = 530
    private val paneHeight = 590

    private val menuPane = Pane<StaticComponentView<*>>(
        posX = 0,
        posY = 0,
        width = paneWidth, height = paneHeight
    )


}