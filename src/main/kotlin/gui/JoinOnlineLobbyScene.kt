package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.animation.MovementAnimation
import tools.aqua.bgw.animation.ParallelAnimation
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual

class JoinOnlineLobbyScene (
    private val app: SopraApplication,
    private val rootService: RootService) : MenuScene(1920, 1080), Refreshable  {

    private val sceneWidth = 1920
    private val sceneHeight = 1080
    private val paneWidth = 800
    private val paneHeight = 670
    private val tabHeight = 130
    private val tabWidth = 200
    private val borderThickness = 4
    private val paneX = (sceneWidth-paneWidth)/2
    private val paneY = (sceneHeight-paneHeight)/2
    private val movementDistance = paneWidth/2 - 150
    private val iconSize = 60
    private val iconDistance = 30

    private var panelsOut = false

    private val backgroundImage = Label(
        posX = 0,
        posY = 0,
        width = sceneWidth,
        height = sceneHeight,
        visual = ImageVisual("ScoreSceneBackground.png")
    )

    private val playerViewPane = Pane<StaticComponentView<*>>(
        posX = paneX,
        posY = paneY,
        width = paneWidth,
        height = paneHeight,
        visual = ColorVisual(64, 98, 70).apply{
            style.borderRadius = BorderRadius(35)
        }
    )

    private val scoreCardSelectionPane = Pane<StaticComponentView<*>>(
        posX = paneX,
        posY = paneY,
        width = paneWidth,
        height = paneHeight,
        visual = ColorVisual(153, 172, 255).apply{
            style.borderRadius = BorderRadius(35)
        }
    )

    private val playerPaneBorder = Label(
        posX = paneX - borderThickness,
        posY = paneY - borderThickness,
        width = paneWidth + borderThickness*2,
        height = paneHeight + borderThickness*2,
        visual = ColorVisual(255, 255, 255).apply{
            style.borderRadius = BorderRadius(35)
        }
    )

    private val scoreCardPaneBorder = Label(
        posX = paneX - borderThickness,
        posY = paneY - borderThickness,
        width = paneWidth + borderThickness*2,
        height = paneHeight + borderThickness*2,
        visual = ColorVisual(255, 255, 255).apply{
            style.borderRadius = BorderRadius(35)
        }
    )

    private val foldOutTab = Label(
        posX = paneX + paneWidth - tabWidth + 100,
        posY = paneY + paneHeight - tabHeight,
        width = tabWidth,
        height = tabHeight,
        visual = ColorVisual(153, 172, 255).apply {
            style.borderRadius = BorderRadius(15.0)
        }
    ).apply {
        onMouseClicked = {
            if(!panelsOut) {
                movePanelsOut()
                //moveContentsOut()
                panelsOut = true
            } else {
                movePanelsIn()
                //moveContentsIn()
                panelsOut = false
            }
        }
    }

    private val tabBorder = Label(
        posX = paneX + paneWidth - tabWidth + 100 - borderThickness,
        posY = paneY + paneHeight - tabHeight - borderThickness,
        width = tabWidth + 2*borderThickness,
        height = tabHeight + 2*borderThickness,
        visual = ColorVisual(255, 255, 255).apply {
            style.borderRadius = BorderRadius(15.0)
        }
    )

    val backArrow = Label(
        posX = 30,
        posY = 30,
        width = 60,
        height = 40,
        text = "⇐",
        font = Font(size = 50, color = Color.WHITE)
    ).apply {
        onMouseClicked = {
            app.showMenuScene(JoinOnlineScene(app, rootService))
        }
    }

    private val hawkIcon = Label(
        posX = paneX + paneWidth - (45 + 4*iconDistance + 5*iconSize),
        posY = paneY + 30,
        width = iconSize + 15,
        height = iconSize + 15,
        visual = ImageVisual("hawk.png", iconSize, iconSize)
    )

    private val salmonIcon = Label(
        posX = paneX + paneWidth - (45 + 3*iconDistance + 4*iconSize),
        posY = paneY + 30,
        width = iconSize + 15,
        height = iconSize + 15,
        visual = ImageVisual("salmon.png", iconSize, iconSize)
    )

    private val foxIcon = Label(
        posX = paneX + paneWidth - (45 + iconDistance + 2*iconSize),
        posY = paneY + 30,
        width = iconSize + 15,
        height = iconSize + 15,
        visual = ImageVisual("fox.png", iconSize, iconSize)
    )

    private val bearIcon = Label(
        posX = paneX + paneWidth - (45 + iconSize),
        posY = paneY + 30,
        width = iconSize + 15,
        height = iconSize + 15,
        visual = ImageVisual("bear.png", iconSize, iconSize)
    )

    private val elkIcon = Label(
        posX = paneX + paneWidth - (45 + 2*iconDistance + 3*iconSize),
        posY = paneY + 30,
        width = iconSize + 15,
        height = iconSize + 15,
        visual = ImageVisual("elk.png", iconSize, iconSize)
    )

    init {
        addComponents(
            backgroundImage,
            tabBorder,
            scoreCardPaneBorder,
            scoreCardSelectionPane,
            foxIcon,
            elkIcon,
            bearIcon,
            hawkIcon,
            salmonIcon,
            foldOutTab,
            playerPaneBorder,
            playerViewPane,
        )
        playerViewPane.add(
            backArrow,
        )

    }

    private fun movePanelsOut() {
        playAnimation(
            ParallelAnimation(
                MovementAnimation(
                    playerViewPane,
                    byX = -movementDistance,
                ),
                MovementAnimation(
                    playerPaneBorder,
                    byX = -movementDistance,
                ),
                MovementAnimation(
                    scoreCardPaneBorder,
                    byX = movementDistance,
                ),
                MovementAnimation(
                    scoreCardSelectionPane,
                    byX = movementDistance,
                ),
                MovementAnimation(
                    tabBorder,
                    byX = movementDistance,
                ),
                MovementAnimation(
                    foldOutTab,
                    byX = movementDistance,
                ),
                MovementAnimation(
                    elkIcon,
                    byX = movementDistance,
                ),
                MovementAnimation(
                    hawkIcon,
                    byX = movementDistance,
                ),
                MovementAnimation(
                    salmonIcon,
                    byX = movementDistance,
                ),
                MovementAnimation(
                    bearIcon,
                    byX = movementDistance,
                ),
                MovementAnimation(
                    foxIcon,
                    byX = movementDistance,
                )
            )
        )
    }

    private fun movePanelsIn() {
        playAnimation(
            ParallelAnimation(
                MovementAnimation(
                    playerViewPane,
                    byX = movementDistance,
                ),
                MovementAnimation(
                    playerPaneBorder,
                    byX = movementDistance,
                ),
                MovementAnimation(
                    scoreCardPaneBorder,
                    byX = -movementDistance,
                ),
                MovementAnimation(
                    scoreCardSelectionPane,
                    byX = -movementDistance,
                ),
                MovementAnimation(
                    tabBorder,
                    byX = -movementDistance,
                ),
                MovementAnimation(
                    foldOutTab,
                    byX = -movementDistance,
                ),
                MovementAnimation(
                    elkIcon,
                    byX = -movementDistance,
                ),
                MovementAnimation(
                    hawkIcon,
                    byX = -movementDistance,
                ),
                MovementAnimation(
                    salmonIcon,
                    byX = -movementDistance,
                ),
                MovementAnimation(
                    bearIcon,
                    byX = -movementDistance,
                ),
                MovementAnimation(
                    foxIcon,
                    byX = -movementDistance,
                )
            )
        )
    }
}