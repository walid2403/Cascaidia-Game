package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.animation.*
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.CheckBox
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.components.uicomponents.UIComponent
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.BoardGameApplication.Companion.runOnGUIThread
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual
import tools.aqua.bgw.visual.Visual

class LobbyScene(private val app: SopraApplication, private val rootService: RootService) : MenuScene(1920, 1080), Refreshable {

    private val sceneWidth = 1920
    private val sceneHeight = 1080
    private val paneWidth = 750
    private val paneHeight = 620
    private val paneX = (sceneWidth - paneWidth) / 2
    private val paneY = (sceneHeight - paneHeight) / 2
    private val tabHeight = 130
    private val tabWidth = 130
    private val nameWidth = 560
    private val nameHeight = 70
    private val nameX = (paneWidth - nameWidth) / 2
    private val nameY = paneHeight / 2 - 80
    private val nameDistance = 25
    private val movementDistance = paneWidth / 2 - 90
    private val buttonSize = 80
    private val iconSize = 63
    private val iconDistance = 30


    private val logo = Label(
        posX = 0,
        posY = 0,
        width = sceneWidth,
        height = sceneHeight,
        visual = ImageVisual("GameConfigMenuBackground.png")
    )

    private val hostPanel = Pane<UIComponent>(
        posX = paneX, posY = paneY,
        width = paneWidth, height = paneHeight
    ).apply {
        visual = ImageVisual("LocalLobbyMenuBackground.png").apply {
            style.borderRadius = BorderRadius(35)
        }
    }


    private val sidePanel = Pane<UIComponent>(
        posX = paneX, posY = paneY,
        width = paneWidth, height = paneHeight
    ).apply {
        visual = ImageVisual("ScoreCardPaneBackground.png").apply {
            style.borderRadius = BorderRadius(35)
        }
    }

    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val p1Input: TextField = TextField(
        width = nameWidth - 126, height = nameHeight - 1,
        posX = nameX + 126, posY = nameY + 1,
        prompt = "Player 1",
        font = Font(size = 31)

    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
        onTextChanged = {
            //startButton.isDisabled = !(this.text.trim().isNotEmpty() && p2Input.text.trim().isNotEmpty())
            if (this.text.isBlank()) {
                this.prompt = "Player 1"
            }
        }
    }

    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val p2Input: TextField = TextField(
        width = nameWidth - 126, height = nameHeight - 1,
        posX = nameX + 126, posY = nameY + nameHeight + nameDistance + 1,
        prompt = "Player 2",
        font = Font(size = 31),

        ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
        onTextChanged = {
            //startButton.isDisabled = !(p1Input.text.trim().isNotEmpty() && this.text.trim().isNotEmpty())
            if (this.text.isBlank()) {
                this.prompt = "Player 2"
            }
        }
    }

    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val p3Input: TextField = TextField(
        width = nameWidth - 126, height = nameHeight - 1,
        posX = nameX + 126, posY = nameY + 2 * nameHeight + 2 * nameDistance + 1,
        prompt = "Player3",
        font = Font(size = 31)
    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
        onTextChanged = {
            //wenn nichts in den ersten beiden Feldern drin steht, dann funktioniert der Start Button nicht
            //startButton.isDisabled = !(p1Input.text.trim().isNotEmpty() && p2Input.text.trim().isNotEmpty())
            if (this.text.isBlank()) {
                this.prompt = "Player3"
            }
        }
        isDisabled = true
        isVisible = false
    }

    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val p4Input: TextField = TextField(
        width = nameWidth - 126, height = nameHeight - 1,
        posX = nameX + 126, posY = nameY + 3 * nameHeight + 3 * nameDistance + 1,
        prompt = "Player4",
        font = Font(size = 31)
    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
        onTextChanged = {
            //startButton.isDisabled = !(p1Input.text.trim().isNotEmpty() && p2Input.text.trim().isNotEmpty())
            if (this.text.isBlank()) {
                this.prompt = "Player4"
            }
        }
        isDisabled = true
        isVisible = false
    }

    //variables to adjust playerType icon correctly, will be adjusted by +/-1 when buttons are clicked
    //0 = human, 1 = easy bot, 2 = hard bot, 3 = not playing. variables%4 will be used to determine shown icon
    private var p1Type = 0
    private var p2Type = 0
    private var p3Type = 3
    private var p4Type = 3

    private val p1TypeButtonLeft = Button(
        width = buttonSize, height = buttonSize,
        posX = 63, posY = p1Input.posY - (buttonSize - nameHeight) / 2,
        text = "←",
        font = Font(size = 28),
        visual = ColorVisual(153, 172, 255).apply {
            style.borderRadius = BorderRadius(buttonSize / 2)
        }
    ).apply {
        onMouseClicked = {
            p1Type = changePlayerType(1, true, p1Type)
        }
    }

    private val p2TypeButtonLeft = Button(
        width = buttonSize, height = buttonSize,
        posX = 63, posY = p2Input.posY - (buttonSize - nameHeight) / 2,
        text = "←",
        font = Font(size = 28),
        visual = ColorVisual(153, 172, 255).apply {
            style.borderRadius = BorderRadius(buttonSize / 2)
        }
    ).apply {
        onMouseClicked = {
            p2Type = changePlayerType(2, true, p2Type)
        }
    }

    private val p3TypeButtonLeft = Button(
        width = buttonSize, height = buttonSize,
        posX = 63, posY = p3Input.posY - (buttonSize - nameHeight) / 2,
        text = "←",
        font = Font(size = 28),
        visual = ColorVisual(153, 172, 255).apply {
            style.borderRadius = BorderRadius(buttonSize / 2)
        }
    ).apply {
        onMouseClicked = {
            p3Type = changePlayerType(3, true, p3Type)
        }
    }

    private val p4TypeButtonLeft = Button(
        width = buttonSize, height = buttonSize,
        posX = 63, posY = p4Input.posY - (buttonSize - nameHeight) / 2,
        text = "←",
        font = Font(size = 28),
        visual = ColorVisual(153, 172, 255).apply {
            style.borderRadius = BorderRadius(buttonSize / 2)
        }
    ).apply {
        onMouseClicked = {
            p4Type = changePlayerType(4, true, p4Type)
        }
    }

    private val p1TypeButtonRight = Button(
        width = buttonSize, height = buttonSize,
        posX = nameX + nameWidth - 32, posY = p1Input.posY - (buttonSize - nameHeight) / 2,
        text = "←",
        font = Font(size = 28),
        visual = ColorVisual(153, 172, 255).apply {
            style.borderRadius = BorderRadius(buttonSize / 2)
        }
    ).apply {
        onMouseClicked = {
            p1Type = changePlayerType(1, false, p1Type)
        }
        rotation = 180.0
    }

    private val p2TypeButtonRight = Button(
        width = buttonSize, height = buttonSize,
        posX = nameX + nameWidth - 32, posY = p2Input.posY - (buttonSize - nameHeight) / 2,
        text = "←",
        font = Font(size = 28),
        visual = ColorVisual(153, 172, 255).apply {
            style.borderRadius = BorderRadius(buttonSize / 2)
        }
    ).apply {
        onMouseClicked = {
            p2Type = changePlayerType(2, false, p2Type)
        }
        rotation = 180.0
    }

    private val p3TypeButtonRight = Button(
        width = buttonSize, height = buttonSize,
        posX = nameX + nameWidth - 32, posY = p3Input.posY - (buttonSize - nameHeight) / 2,
        text = "←",
        font = Font(size = 28),
        visual = ColorVisual(153, 172, 255).apply {
            style.borderRadius = BorderRadius(buttonSize / 2)
        }
    ).apply {
        onMouseClicked = {
            p3Type = changePlayerType(3, false, p3Type)
        }
        rotation = 180.0
    }

    private val p4TypeButtonRight = Button(
        width = buttonSize, height = buttonSize,
        posX = nameX + nameWidth - 32, posY = p4Input.posY - (buttonSize - nameHeight) / 2,
        text = "←",
        font = Font(size = 28),
        visual = ColorVisual(153, 172, 255).apply {
            style.borderRadius = BorderRadius(buttonSize / 2)
        }
    ).apply {
        onMouseClicked = {
            p4Type = changePlayerType(4, false, p4Type)
        }
        rotation = 180.0
    }

    private val p1Icon = Label(
        width = iconSize, height = iconSize,
        posX = p4TypeButtonLeft.posX + buttonSize + 5,
        posY = p1Input.posY - (iconSize - nameHeight) / 2,
        visual = ImageVisual("HumanIcon.png")
    )

    private val p2Icon = Label(
        width = iconSize, height = iconSize,
        posX = p4TypeButtonLeft.posX + buttonSize + 5,
        posY = p2Input.posY - (iconSize - nameHeight) / 2,
        visual = ImageVisual("HumanIcon.png")
    )

    private val p3Icon = Label(
        width = iconSize, height = iconSize,
        posX = p4TypeButtonLeft.posX + buttonSize + 5,
        posY = p3Input.posY - (iconSize - nameHeight) / 2,
    ).apply {
        isVisible = false
    }

    private val p4Icon = Label(
        width = iconSize, height = iconSize,
        posX = p4TypeButtonLeft.posX + buttonSize + 5,
        posY = p4Input.posY - (iconSize - nameHeight) / 2,
    ).apply {
        isVisible = false
    }

    private val noP1 = Label(
        width = iconSize, height = iconSize,
        posX = nameX + (nameWidth - iconSize)/2, posY = p1Input.posY - (iconSize - nameHeight) / 2,
    ).apply {
        isVisible = false
    }

    private val noP2 = Label(
        width = iconSize, height = iconSize,
        posX = nameX + (nameWidth - iconSize)/2, posY = p2Input.posY - (iconSize - nameHeight) / 2,
        visual = ImageVisual("NotPlayingIcon.png")
    ).apply {
        isVisible = false
    }

    private val noP3 = Label(
        width = iconSize, height = iconSize,
        posX = nameX + (nameWidth - iconSize)/2, posY = p3Input.posY - (iconSize - nameHeight) / 2,
        visual = ImageVisual("NotPlayingIcon.png")
    )

    private val noP4 = Label(
        width = iconSize, height = iconSize,
        posX = nameX + (nameWidth - iconSize)/2, posY = p4Input.posY - (iconSize - nameHeight) / 2,
        visual = ImageVisual("NotPlayingIcon.png")
    )

    private val elk = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = (paneWidth)/2 + 90 - (iconSize + 15)/2, posY = 30,
        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("elk.png", iconSize, iconSize)
        onMouseClicked = {
            showScoreCards(elkCardA, elkCardB)
            elkCardA.resize(209, 403)
            elkCardB.resize(209, 403)
        }
    }

    private val hawk = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = elk.posX - 2*iconDistance - 2*(iconSize + 15), posY = 30,
        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("hawk.png", iconSize, iconSize)
        onMouseClicked = {
            showScoreCards(hawkCardA, hawkCardB)
            hawkCardA.resize(183, 397)
            hawkCardB.resize(183, 397)
        }
    }

    private val salmon = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = elk.posX - iconDistance - (iconSize + 15), posY = 30,
        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("salmon.png", iconSize, iconSize)
        onMouseClicked = {
            showScoreCards(salmonCardA, salmonCardB)
            salmonCardA.resize(183, 397)
            salmonCardB.resize(183, 397)
        }
    }

    private val fox = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = elk.posX + iconDistance + iconSize + 15, posY = 30,        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("fox.png", iconSize, iconSize)

        onMouseClicked = {
            showScoreCards(foxCardA, foxCardB)
            foxCardA.resize(183, 397)
            foxCardB.resize(183, 397)
        }
    }

    private val bear = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = elk.posX + 2*iconDistance + 2*(iconSize + 15), posY = 30,
        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("bear.png", iconSize, iconSize)
        onMouseClicked = {
            showScoreCards(bearCardA, bearCardB)
        }
    }

    private val bearCardA = Label(
        width = 207, height = 394,
        posX = 230, posY = 200,
    ).apply {
        visual = ImageVisual("Scoring_Bear_A.png")
        isVisible = false
    }

    private val bearCardB = Label(
        width = 207, height = 394,
        posX = 490, posY = 200,

        ).apply {
        visual = ImageVisual("Scoring_Bear_B.png")
        isVisible = false
    }


    private val elkCardA = Label(
        width = 262, height = 504,
        posX = 230, posY = 200,
    ).apply {
        visual = ImageVisual("Scoring_Elk_A.png")
        isVisible = false
    }

    private val elkCardB = Label(
        width = 262, height = 504,
        posX = 490, posY = 200,

        ).apply {
        visual = ImageVisual("Scoring_Elk_B.png")
        isVisible = false
    }


    private val foxCardA = Label(
        width = 229, height = 497,
        posX = 230, posY = 200,
    ).apply {
        visual = ImageVisual("Scoring_Fox_A.png")
        isVisible = false
    }

    private val foxCardB = Label(
        width = 229, height = 497,
        posX = 490, posY = 200,

        ).apply {
        visual = ImageVisual("Scoring_Fox_B.png")
        isVisible = false
    }


    private val hawkCardA = Label(
        width = 229, height = 497,
        posX = 230, posY = 200,
    ).apply {
        visual = ImageVisual("Scoring_Hawk_A.png")
        isVisible = false
    }

    private val hawkCardB = Label(
        width = 229, height = 497,
        posX = 490, posY = 200,

        ).apply {
        visual = ImageVisual("Scoring_Hawk_B.png")
        isVisible = false
    }

    private val salmonCardA = Label(
        width = 229, height = 497,
        posX = 230, posY = 200,
    ).apply {
        visual = ImageVisual("Scoring_Salmon_A.png")
        isVisible = false
    }

    private val salmonCardB = Label(
        width = 229, height = 497,
        posX = 490, posY = 200,

        ).apply {
        visual = ImageVisual("Scoring_Salmon_B.png")
        isVisible = false
    }


    val checkBoxSalmonA = CheckBox(
        posX = salmon.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked = {
            if (!isChecked) {
                uncheckBox(checkBoxSalmonB)
            }
        }
    }

    val checkBoxSalmonB = CheckBox(
        posX = salmon.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked = {
            if (!isChecked) {
                checkBox("checkBoxSalmonB")
            }
        }
    }


    val checkBoxHawkA = CheckBox(
        posX = hawk.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked = {
            if (!isChecked) {
                checkBox("checkBoxHawkA")
            }
        }
    }

    val checkBoxHawkB = CheckBox(
        posX = hawk.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked = {
            if (!isChecked) {
                checkBox("checkBoxHawkB")
            }
        }

    }

    val checkBoxBearA = CheckBox(
        posX =  bear.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked = {
            if (!isChecked) {
                checkBox("checkBoxBearA")
            }
        }

    }

    val checkBoxBearB = CheckBox(
        posX =  bear.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked = {
            if (!isChecked) {
                checkBox("checkBoxBearB")
            }
        }

    }


    val checkBoxFoxA = CheckBox(
        posX = fox.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked = {
            if (!isChecked) {
                checkBox("checkBoxFoxA")
            }
        }

    }


    val checkBoxFoxB = CheckBox(
        posX = fox.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked = {
            if (!isChecked) {
                checkBox("checkBoxFoxB")
            }
        }

    }


    val checkBoxElkA = CheckBox(
        posX = elk.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked = {
            if (!isChecked) {
                checkBox("checkBoxElkA")
            }
        }

    }

    val checkBoxElkB = CheckBox(
        posX = elk.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    ).apply {
        onMouseClicked = {
            if (!isChecked) {
                checkBox("checkBoxElkB")
            }
        }

    }

//    private val barOne = Label(
//        width = 650, height = 4,
//        posX = 55, posY = 210).apply { visual= ColorVisual(0,0,0) }
//
//    private val barTwo = Label(
//        width = 3, height = 45,
//        posX = 200, posY = 160).apply { visual= ColorVisual(0,0,0) }
//
//    private val barThree = Label(
//        width = 3, height = 45,
//        posX = 360, posY = 160).apply { visual= ColorVisual(0,0,0) }
//
//    private val barFour = Label(
//        width = 3, height = 45,
//        posX = 535, posY = 160).apply { visual= ColorVisual(0,0,0) }


//    private val human = Label(
//        width = 130, height = 45,
//        posX = 50, posY = 160,
//        ).apply { visual = ImageVisual("Mensch.png") }


//    private val easyBot = Label(
//        width = 140, height = 45,
//        posX = 210, posY = 160,
//    ).apply { visual = ImageVisual("EasyBot.png") }


//    private val hardBot = Label(
//        width = 150, height = 45,
//        posX = 365, posY = 160,
//    ).apply { visual = ImageVisual("HardBot.png") }
//
//
//    private val emptySlot = Label(
//        width = 150, height = 45,
//        posX = 545, posY = 160,
//    ).apply { visual = ImageVisual("EmptySlot.png") }


//    private val player = Label(
//        width = 300, height = 100,
//        posX = 750.0/2 - 150, posY = 30,
//        text = "Add Player",
//        font = Font(
//            size = 50,
//            color = Color(0xFFFFFF),
//            family = "Canva Sans",
//            fontWeight = Font.FontWeight.BOLD)
//    )


    private val exitButton = Button(
        width = 78, height = 78,
        posX = 33, posY = 23,
        //text = "←",font = Font(size = 70,color = Color(0xFFFFFF))
    ).apply {
        //visual = ColorVisual(64, 98, 70).apply { style.borderRadius = BorderRadius(8) }
        visual = Visual.EMPTY
        onMouseClicked = {
            app.showMenuScene(MainMenuScene(app, rootService))
        }
    }

//    "◀──",


    private val shuffleButton = Button(
        width = 50, height = 50,
        posX = 750 / 2 + 250, posY = 620 / 2 - 260,
        text = "⤮", font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(360) }
        onMouseClicked = {
            shuffleNames()
        }
    }


    private val arrowLabel = Label(
        posX = paneX + paneWidth - tabWidth + 80,
        posY = paneY + paneHeight - tabHeight,
        width = tabWidth,
        height = tabHeight,
        visual = ImageVisual("FoldOutTab.png").apply {
            style.borderRadius = BorderRadius(15.0)
        }
    ).apply {
        //visual = ColorVisual(160, 150, 210).apply { style.borderRadius = BorderRadius(15) }
        onMouseClicked = {
            expandPanel()
        }
    }

//    private val arrowButton = Label(
//        posX = 1380, posY = 750,
//        width = 90.0, height = 90.0,
//        text = "→",
//        font = Font(size = 40, color = Color.BLACK)
//    ).apply {
//        visual = ColorVisual(160, 150, 210).apply { style.borderRadius = BorderRadius(8) }
//
//    }


    private val startLabel = Label(
        posX = 700 - 8, posY = 620 - 130,
        width = 142.0, height = 130.0,
        text = "",
        font = Font(size = 22, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(160, 150, 210).apply { style.borderRadius = BorderRadius(15) }
        this.isVisible = false
    }


    private val startButton = Label(
        posX = 750 - 8, posY = 620 - 100,
        width = 80.0, height = 50.0,
        text = "▶",
        font = Font(size = 40, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(160, 150, 210).apply { style.borderRadius = BorderRadius(8) }
        this.isVisible = false
    }


    init {

        listOf(
            p1Input, p2Input, p3Input, p4Input,
            //p1InputHuman, p2InputHuman, p3InputHuman, p4InputHuman,
            //easyBot,human,hardBot,emptySlot,barOne,barTwo,barThree,barFour,player,
            shuffleButton, exitButton, p1TypeButtonLeft, p2TypeButtonLeft, p3TypeButtonLeft, p4TypeButtonLeft,
            p1TypeButtonRight, p2TypeButtonRight, p3TypeButtonRight, p4TypeButtonRight,
            noP1, noP2, noP3, noP4, p1Icon, p2Icon, p3Icon, p4Icon,
        ).forEach { hostPanel.add(it) }

        listOf(
            startLabel, startButton, bear, elk, hawk, salmon, fox,
            bearCardA, bearCardB, elkCardA, elkCardB, foxCardA, foxCardB,
            hawkCardA, hawkCardB, salmonCardA, salmonCardB,
            checkBoxHawkA, checkBoxHawkB, checkBoxElkA, checkBoxElkB, checkBoxBearA, checkBoxBearB, checkBoxFoxA,
            checkBoxFoxB, checkBoxSalmonA, checkBoxSalmonB
        ).forEach { sidePanel.add(it) }


        backgroundOpacity = .5
        addComponents(
            logo,
            sidePanel,
            arrowLabel,
            //arrowButton,
            hostPanel,
        )
    }

    private fun expandPanel() {

        playAnimation(
            ParallelAnimation(
                MovementAnimation( // bewegt das sidePanel
                    componentView = hostPanel,
                    toX = hostPanel.actualPosX - (hostPanel.width / 2) + 90,
                    duration = 1000 // dauer
                ),
                MovementAnimation( // bewegt das sidePanel
                    componentView = sidePanel,
                    toX = sidePanel.actualPosX + (sidePanel.width / 2) - 90,
                    duration = 1000 // dauer
                ),
                MovementAnimation(
                    arrowLabel,
                    byX = movementDistance,
                )
            ).apply {
                onFinished = {
                    runOnGUIThread {
                        //arrowButton.isVisible = false
                        //arrowLabel.isVisible = false
                        //startLabel.isVisible = true
                        //startButton.isVisible = true
                        arrowLabel.apply {
                            visual = ImageVisual("StartGameTab.png").apply {
                                style.borderRadius = BorderRadius(15)
                            }
                            onMouseClicked = {
                                TODO("Start game muss hier aufgerufen werden")
                            }
                        }
                    }
                }
            }
        )
    }

    private fun names(): List<String> {
        return listOf(p1Input.text.trim(), p2Input.text.trim(), p3Input.text.trim(), p4Input.text.trim())
    }

    private fun shuffleNames() {
        // ich filter die Liste nach allem die nicht leer sind und dann shuffle ich diese und packe sie in eine
        // Liste
        val player = names().filter { it != "" }.shuffled().toMutableList()
        // fügt bei allem leeren "" hinzu
        repeat(4 - player.size) {
            player.add("")
        }
        // wird wieder ins Feld geschieben
        p1Input.text = player[0]
        p2Input.text = player[1]
        p3Input.text = player[2]
        p4Input.text = player[3]

    }

    private fun checkBox(checkBox: String) {
        //uncheckBox.isChecked = false

        when (checkBox) {
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

    private fun uncheckBox(uncheckBox: CheckBox) {
        uncheckBox.isChecked = false
    }

    private fun changePlayerType(playerNum: Int, left: Boolean, playerType: Int): Int {

        val newType = if (left) {
            (playerType + 3) % 4
        } else {
            (playerType + 5)%4
        }

        val newVisual = when (newType) {
            0 -> {
                ImageVisual("HumanIcon.png")
            }
            1 -> {
                ImageVisual("EasyBotIcon.png")
            }
            2 -> {
                ImageVisual("HardBotIcon.png")
            }
            3 -> {
                ImageVisual("NotPlayingIcon.png")
            }
            else -> throw IllegalArgumentException("Only 4 possible visuals, $newType is invalid")
        }

        val leftLabel = when (playerNum) {
            1 -> p1Icon
            2 -> p2Icon
            3 -> p3Icon
            4 -> p4Icon
            else -> throw IllegalArgumentException("Player $playerNum is not valid")
        }

        val rightLabel = when (playerNum) {
            1 -> noP1
            2 -> noP2
            3 -> noP3
            4 -> noP4
            else -> throw IllegalArgumentException("Player $playerNum is not valid")
        }

        nameEntryDisabled(newType == 3, playerNum)
        adjustIcons(leftLabel, rightLabel, newVisual, newType != 3)

        return newType
    }

    private fun adjustIcons(leftLabel: Label, rightLabel: Label, visual: ImageVisual, leftVisible: Boolean) {
        if(leftVisible) {
            leftLabel.isVisible = true
            rightLabel.isVisible = false
            leftLabel.visual = visual
        } else {
            leftLabel.isVisible = false
            rightLabel.isVisible = true
            rightLabel.visual = visual
        }
    }

    private fun nameEntryDisabled(disableEntry: Boolean, playerNum: Int) {
        when (playerNum) {
            1 -> {
                p1Input.isDisabled = disableEntry
                p1Input.isVisible = !disableEntry
            }
            2 -> {
                p2Input.isDisabled = disableEntry
                p2Input.isVisible = !disableEntry
            }
            3 -> {
                p3Input.isDisabled = disableEntry
                p3Input.isVisible = !disableEntry
            }
            4 -> {
                p4Input.isDisabled = disableEntry
                p4Input.isVisible = !disableEntry
            }
            else -> throw IllegalArgumentException("Player $playerNum is not valid")
        }
    }

    private fun showScoreCards(cardA: Label, cardB: Label) {
        val cards = listOf(salmonCardA, salmonCardB, hawkCardA, hawkCardB, foxCardA, foxCardB,
            bearCardA, bearCardB, elkCardA, elkCardB)
        cards.forEach { card ->
            if(card == cardA || card == cardB) card.isVisible = true
            else card.isVisible = false
        }
    }
}



