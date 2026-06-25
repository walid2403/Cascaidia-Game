package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.animation.MovementAnimation
import tools.aqua.bgw.animation.ParallelAnimation
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.CheckBox
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Alignment
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
    private val paneWidth = 750
    private val paneHeight = 620
    private val tabHeight = 130
    private val tabWidth = 130
    private val borderThickness = 4
    private val paneX = (sceneWidth-paneWidth)/2
    private val paneY = (sceneHeight-paneHeight)/2
    private val movementDistance = paneWidth/2 - 90
    private val iconSize = 60
    private val iconDistance = 30
    private val nameWidth = 400
    private val nameHeight = 50
    private val nameY = paneHeight/2 - 100
    private val nameDistance = 35

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
            checkBoxHawkA.isDisabled = true
            checkBoxHawkB.isDisabled = true
            checkBoxElkA.isDisabled = true
            checkBoxElkB.isDisabled = true
            checkBoxBearA.isDisabled = true
            checkBoxBearB.isDisabled = true
            checkBoxFoxA.isDisabled = true
            checkBoxFoxB.isDisabled = true
            checkBoxSalmonA.isDisabled = true
            checkBoxSalmonB.isDisabled = true
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
        width = 80,
        height = 60,
        text = "⇐",
        font = Font(size = 50, color = Color.WHITE)
    ).apply {
        onMouseClicked = {
            app.showMenuScene(JoinOnlineScene(app, rootService))
        }
    }

    private val playerTypeOverview = Label(
        width = 200,
        height = 60,
        posX = 500,
        posY = 45,
        font = Font(size = 40)
    ).apply { visual = ImageVisual("Auswahl.png") }



    private val header = Label(
        width = 300, height = 100,
        posX = 750.0/2 - 150, posY = 30,
        text = "Join",
        font = Font(
            size = 50,
            color = Color(0xFFFFFF),
            family = "Canva Sans",
            fontWeight = Font.FontWeight.BOLD)
    )


    private val p1Input = Label(
        width = nameWidth,
        height = nameHeight,
        posX = (paneWidth - nameWidth)/2,
        posY = nameY,
        text = "Player 1",
        font = Font(size = 28),
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
    )

    private val p2Input = Label(
        width = nameWidth,
        height = nameHeight,
        posX = (paneWidth - nameWidth)/2,
        posY = nameY + nameHeight + nameDistance,
        text = "Empty Player Slot",
        font = Font(size = 28, color = Color.DARK_GRAY),
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
    )

    private val p3Input = Label(
        width = nameWidth,
        height = nameHeight,
        posX = (paneWidth - nameWidth)/2,
        posY = nameY + 2*nameHeight + 2*nameDistance,
        text = "Empty Player Slot",
        font = Font(size = 28, color = Color.DARK_GRAY),
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
    )

    private val p4Input = Label(
        width = nameWidth,
        height = nameHeight,
        posX = (paneWidth - nameWidth)/2,
        posY = nameY + 3*nameHeight + 3*nameDistance,
        text = "Empty Player Slot",
        font = Font(size = 28, color = Color.DARK_GRAY),
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
    )

    private val elkIcon = Label(
//        posX = paneX + (paneWidth)/2 + 90 - (iconSize + 15)/2,
//        posY = paneY + 30,
        posX = (paneWidth)/2 + 90 - (iconSize + 15)/2,
        posY = 30,
        width = iconSize + 15,
        height = iconSize + 15,
        visual = ImageVisual("elk.png", iconSize, iconSize)
    ).apply {
        visual = ImageVisual("elk.png")
        onMouseClicked = {
            bearCardA.isVisible = false
            bearCardB.isVisible = false
            elkCardA.isVisible = true
            elkCardB.isVisible = true
            foxCardA.isVisible = false
            foxCardB.isVisible = false
            salmonCardA.isVisible = false
            salmonCardB.isVisible = false
            hawkCardA.isVisible = false
            hawkCardB.isVisible = false
            elkCardA.resize(209, 403)
            elkCardB.resize(209, 403)
        }
    }

    private val hawkIcon = Label(
        posX = elkIcon.posX - 2*iconDistance - 2*(iconSize + 15),
//        posY = paneY + 30,
        posY = 30,
        width = iconSize + 15,
        height = iconSize + 15,
        visual = ImageVisual("hawk.png", iconSize, iconSize)
    ).apply {
        visual = ImageVisual("hawk.png")
        onMouseClicked = {
            bearCardA.isVisible = false
            bearCardB.isVisible = false
            elkCardA.isVisible = false
            elkCardB.isVisible = false
            foxCardA.isVisible = false
            foxCardB.isVisible = false
            salmonCardA.isVisible = false
            salmonCardB.isVisible = false
            hawkCardA.isVisible = true
            hawkCardB.isVisible = true
            hawkCardA.resize(183, 397)
            hawkCardB.resize(183, 397)        }
    }

    private val salmonIcon = Label(
        posX = elkIcon.posX - iconDistance - (iconSize + 15),
//        posY = paneY + 30,
        posY = 30,
        width = iconSize + 15,
        height = iconSize + 15,
        visual = ImageVisual("salmon.png", iconSize, iconSize)
    ).apply {
        onMouseClicked = {
            onMouseClicked = {
                bearCardA.isVisible = false
                bearCardB.isVisible = false
                elkCardA.isVisible = false
                elkCardB.isVisible = false
                foxCardA.isVisible = false
                foxCardB.isVisible = false
                salmonCardA.isVisible = true
                salmonCardB.isVisible = true
                hawkCardA.isVisible = false
                hawkCardB.isVisible = false
                salmonCardA.resize(183, 397)
                salmonCardB.resize(183, 397)
            }
        }
    }

    private val foxIcon = Label(
        posX = elkIcon.posX + iconDistance + iconSize + 15,
//        posY = paneY + 30,
        posY = 30,
        width = iconSize + 15,
        height = iconSize + 15,
        visual = ImageVisual("fox.png", iconSize, iconSize)
    ).apply {
        visual = ImageVisual("fox.png")
        onMouseClicked = {
            bearCardA.isVisible = false
            bearCardB.isVisible = false
            elkCardA.isVisible = false
            elkCardB.isVisible = false
            foxCardA.isVisible = true
            foxCardB.isVisible = true
            salmonCardA.isVisible = false
            salmonCardB.isVisible = false
            hawkCardA.isVisible = false
            hawkCardB.isVisible = false
            foxCardA.resize(183, 397)
            foxCardB.resize(183, 397)        }
    }

    private val bearIcon = Label(
        posX = elkIcon.posX + 2*iconDistance + 2*(iconSize + 15),
//        posY = paneY + 30,
        posY = 30,
        width = iconSize + 15,
        height = iconSize + 15,
        visual = ImageVisual("bear.png", iconSize, iconSize)
    ).apply {
        visual = ImageVisual("bear.png")
        onMouseClicked = {
            bearCardA.isVisible = true
            bearCardB.isVisible = true
            elkCardA.isVisible = false
            elkCardB.isVisible = false
            foxCardA.isVisible = false
            foxCardB.isVisible = false
            salmonCardA.isVisible = false
            salmonCardB.isVisible = false
            hawkCardA.isVisible = false
            hawkCardB.isVisible = false
        }
    }

    val checkBoxSalmonA = CheckBox(
        posX = salmonIcon.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked ={
            if (!isChecked){
                checkBox("checkBoxSalmonA")
            }
        }
    }


    val checkBoxSalmonB = CheckBox(
        posX = salmonIcon.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked ={
            if (!isChecked){
                checkBox("checkBoxSalmonB")
            }
        }
    }



    val checkBoxHawkA = CheckBox(
        posX = hawkIcon.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked ={
            if (!isChecked){
                checkBox("checkBoxHawkA")
            }
        }
    }

    val checkBoxHawkB = CheckBox(
        posX = hawkIcon.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked ={
            if (!isChecked){
                checkBox("checkBoxHawkB")
            }
        }

    }

    val checkBoxBearA = CheckBox(
        posX = bearIcon.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked ={
            if (!isChecked){
                checkBox("checkBoxBearA")
            }
        }

    }

    val checkBoxBearB = CheckBox(
        posX = bearIcon.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked ={
            if (!isChecked){
                checkBox("checkBoxBearB")
            }
        }

    }


    val checkBoxFoxA = CheckBox(
        posX = foxIcon.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked ={
            if (!isChecked){
                checkBox("checkBoxFoxA")
            }
        }

    }


    val checkBoxFoxB = CheckBox(
        posX = foxIcon.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked ={
            if (!isChecked){
                checkBox("checkBoxFoxB")
            }
        }

    }



    val checkBoxElkA = CheckBox(
        posX = elkIcon.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked ={
            if (!isChecked){
                checkBox("checkBoxElkA")
            }
        }

    }

    val checkBoxElkB = CheckBox(
        posX = elkIcon.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked ={
            if (!isChecked){
                checkBox("checkBoxElkB")
            }
        }

    }

    private val bearCardA = Label(
        width = 207,
        height = 394,
        posX = 230,
        posY = 200,
    ).apply {
        visual = ImageVisual("Scoring_Bear_A.png")
        isVisible = false
    }

    private val bearCardB = Label(
        width = 207,
        height = 394,
        posX = 490,
        posY = 200,
        ).apply {
        visual = ImageVisual("Scoring_Bear_B.png")
        isVisible = false
    }


    private val elkCardA = Label(
        width = 262,
        height = 504,
        posX = 230,
        posY = 200,
    ).apply {
        visual = ImageVisual("Scoring_Elk_A.png")
        isVisible = false
    }

    private val elkCardB = Label(
        width = 262,
        height = 504,
        posX = 490,
        posY = 200,
        ).apply {
        visual = ImageVisual("Scoring_Elk_B.png")
        isVisible = false
    }


    private val foxCardA = Label(
        width = 229,
        height = 497,
        posX = 230,
        posY = 200,
    ).apply {
        visual = ImageVisual("Scoring_Fox_A.png")
        isVisible = false
    }

    private val foxCardB = Label(
        width = 229,
        height = 497,
        posX = 490,
        posY = 200,
        ).apply {
        visual = ImageVisual("Scoring_Fox_B.png")
        isVisible = false
    }


    private val hawkCardA = Label(
        width = 229,
        height = 497,
        posX = 230,
        posY = 200,
    ).apply {
        visual = ImageVisual("Scoring_Hawk_A.png")
        isVisible = false
    }

    private val hawkCardB = Label(
        width = 229,
        height = 497,
        posX = 490,
        posY = 200,
        ).apply {
        visual = ImageVisual("Scoring_Hawk_B.png")
        isVisible = false
    }

    private val salmonCardA = Label(
        width = 229,
        height = 497,
        posX = 230,
        posY = 200,
    ).apply {
        visual = ImageVisual("Scoring_Salmon_A.png")
        isVisible = false
    }

    private val salmonCardB = Label(
        width = 229,
        height = 497,
        posX = 490,
        posY = 200,
        ).apply {
        visual = ImageVisual("Scoring_Salmon_B.png")
        isVisible = false
    }

    init {
        addComponents(
            backgroundImage,
            tabBorder,
            scoreCardPaneBorder,
            scoreCardSelectionPane,
//            foxIcon,
//            elkIcon,
//            bearIcon,
//            hawkIcon,
//            salmonIcon,
            foldOutTab,
            playerPaneBorder,
            playerViewPane,
        )
        playerViewPane.addAll(
            backArrow,
            playerTypeOverview,
            header,
            p1Input,
            p2Input,
            p3Input,
            p4Input,
        )
        scoreCardSelectionPane.addAll(
            foxIcon,
            elkIcon,
            bearIcon,
            hawkIcon,
            salmonIcon,
            elkCardA,
            elkCardB,
            hawkCardA,
            hawkCardB,
            salmonCardA,
            salmonCardB,
            foxCardA,
            foxCardB,
            bearCardA,
            bearCardB,
            checkBoxHawkA,
            checkBoxHawkB,
            checkBoxElkA,
            checkBoxElkB,
            checkBoxBearA,
            checkBoxBearB,
            checkBoxFoxA,
            checkBoxFoxB,
            checkBoxSalmonA,
            checkBoxSalmonB
        )

    }

    private fun checkBox(checkBox : String){

        when (checkBox){
            "checkBoxHawkA" -> checkBoxHawkB.isChecked = false
            "checkBoxHawkB" -> checkBoxHawkA.isChecked = false

            "checkBoxElkA" -> checkBoxElkB.isChecked = false
            "checkBoxElkB" -> checkBoxElkA.isChecked = false

            "checkBoxFoxA" -> checkBoxFoxB.isChecked = false
            "checkBoxFoxB" -> checkBoxFoxA.isChecked = false

            "checkBoxSalmonA" -> checkBoxSalmonB.isChecked = false
            "checkBoxSalmonB" -> checkBoxSalmonA.isChecked = false

            "checkBoxBearA" -> checkBoxBearB.isChecked = false
            "checkBoxBearB" -> checkBoxBearA.isChecked = false
        }
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
//                MovementAnimation(
//                    elkIcon,
//                    byX = movementDistance,
//                ),
//                MovementAnimation(
//                    hawkIcon,
//                    byX = movementDistance,
//                ),
//                MovementAnimation(
//                    salmonIcon,
//                    byX = movementDistance,
//                ),
//                MovementAnimation(
//                    bearIcon,
//                    byX = movementDistance,
//                ),
//                MovementAnimation(
//                    foxIcon,
//                    byX = movementDistance,
//                )
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
//                MovementAnimation(
//                    elkIcon,
//                    byX = -movementDistance,
//                ),
//                MovementAnimation(
//                    hawkIcon,
//                    byX = -movementDistance,
//                ),
//                MovementAnimation(
//                    salmonIcon,
//                    byX = -movementDistance,
//                ),
//                MovementAnimation(
//                    bearIcon,
//                    byX = -movementDistance,
//                ),
//                MovementAnimation(
//                    foxIcon,
//                    byX = -movementDistance,
//                )
            )
        )
    }
}