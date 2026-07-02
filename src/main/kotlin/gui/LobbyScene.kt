package gui

import entity.PlayerType
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
import gui.HostOnlineLobbyScene
import kotlin.Pair
import kotlin.String

class LobbyScene(private val app: SopraApplication, private val rootService: RootService) : MenuScene(1920, 1080), Refreshable {

    private val sceneWidth = 1920
    private val sceneHeight = 1080

    private val paneWidth = 750
    private val paneHeight = 620
    private val paneX = (sceneWidth - paneWidth) / 2
    private val paneY = (sceneHeight - paneHeight) / 2

    private val tabHeight = 130
    private val tabWidth = 130

    private val nameWidth = 374
    private val nameHeight = 66
    private val nameX = 225
    private val nameY = 232
    private val nameDistance = 29

    private val movementDistance = paneWidth / 2 - 90

    private val buttonHeight = 88
    private val buttonWidth = 88
    private val buttonY = 218
    private val leftButtonX = 66
    private val rightButtonX = 600

    private val iconSize = 63
    private val iconDistance = 30
    private val cardY = 230
    private val cardAX = 245
    private val cardBX = 505
    private val orderNames = mutableListOf<Pair<String,Int>>(Pair("",0),Pair("",0),Pair("",3),Pair("",3))


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
        width = nameWidth, height = nameHeight,
        posX = nameX, posY = nameY,
        prompt = "Player 1",
        font = Font(size = 31)

    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
        onTextChanged = {
            //startButton.isDisabled = !(this.text.trim().isNotEmpty() && p2Input.text.trim().isNotEmpty())
            if (this.text.isBlank()) {
                this.prompt = "Player 1"
            }
            orderNames[0] = Pair(this.text,p1Type)
        }
    }

    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val p2Input: TextField = TextField(
        width = nameWidth, height = nameHeight,
        posX = nameX, posY = nameY + nameHeight + nameDistance,
        prompt = "Player 2",
        font = Font(size = 31),

        ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
        onTextChanged = {
            //startButton.isDisabled = !(p1Input.text.trim().isNotEmpty() && this.text.trim().isNotEmpty())
            if (this.text.isBlank()) {
                this.prompt = "Player 2"
            }
            orderNames[1] = Pair(this.text,p2Type)
        }
    }

    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val p3Input: TextField = TextField(
        width = nameWidth, height = nameHeight,
        posX = nameX, posY = nameY + 2 * nameHeight + 2 * nameDistance,
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
            orderNames[2] = Pair(this.text,p3Type)
        }
        isDisabled = true
        isVisible = false
    }

    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val p4Input: TextField = TextField(
        width = nameWidth, height = nameHeight,
        posX = nameX, posY = nameY + 3 * nameHeight + 3 * nameDistance,
        prompt = "Player4",
        font = Font(size = 31)
    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
        onTextChanged = {
            //startButton.isDisabled = !(p1Input.text.trim().isNotEmpty() && p2Input.text.trim().isNotEmpty())
            if (this.text.isBlank()) {
                this.prompt = "Player4"
            }
            orderNames[3] = Pair(this.text,p4Type)
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

    private val p1TypeButtonLeft = Label(
        width = buttonWidth, height = buttonHeight,
        posX = leftButtonX,
        posY = buttonY,
        visual = Visual.EMPTY
    ).apply {
        onMouseClicked = {
            p1Type = changePlayerType(1, true, p1Type,true)
            orderNames[0] = Pair(p1Input.text, p1Type)
        }
    }

    private val p2TypeButtonLeft = Label(
        width = buttonWidth, height = buttonHeight,
        posX = leftButtonX,
        posY = p2Input.posY - 1 - (84 - nameHeight) / 2,
        visual = Visual.EMPTY

    ).apply {
        onMouseClicked = {
            p2Type = changePlayerType(2, true, p2Type,true)
            orderNames[1] = Pair(p2Input.text, p2Type)
        }
    }

    private val p3TypeButtonLeft = Label(
        width = buttonWidth, height = buttonHeight,
        posX = leftButtonX,
        posY = p3Input.posY - 1 - (84 - nameHeight) / 2,
        visual = Visual.EMPTY

    ).apply {
        onMouseClicked = {
            p3Type = changePlayerType(3, true, p3Type,true)
            orderNames[2] = Pair(p3Input.text, p3Type)
        }
    }

    private val p4TypeButtonLeft = Label(
        width = buttonWidth, height = buttonHeight,
        posX = leftButtonX,
        posY = p4Input.posY - 1 - (84 - nameHeight) / 2,
        visual = Visual.EMPTY

    ).apply {
        onMouseClicked = {
            p4Type = changePlayerType(4, true, p4Type,true)
            orderNames[3] = Pair(p4Input.text, p4Type)
        }
    }

    private val p1TypeButtonRight = Label(
        width = buttonWidth, height = buttonHeight,
        posX = rightButtonX,
        posY = 218,
        visual = Visual.EMPTY
    ).apply {
        onMouseClicked = {
            p1Type = changePlayerType(1, false, p1Type,true)
            orderNames[0] = Pair(p1Input.text, p1Type)
        }
    }

    private val p2TypeButtonRight = Label(
        width = buttonWidth, height = buttonHeight,
        posX = rightButtonX,
        posY = p2Input.posY - 1 -  (84 - nameHeight) / 2,
        visual = Visual.EMPTY
    ).apply {
        onMouseClicked = {
            p2Type = changePlayerType(2, false, p2Type,true)
            orderNames[1] = Pair(p2Input.text, p2Type)
        }
    }

    private val p3TypeButtonRight = Label(
        width = buttonWidth, height = buttonHeight,
        posX = rightButtonX,
        posY = p3Input.posY - 1 - (84 - nameHeight) / 2,
        visual = Visual.EMPTY
    ).apply {
        onMouseClicked = {
            p3Type = changePlayerType(3, false, p3Type,true)
            orderNames[2] = Pair(p3Input.text, p3Type)
        }
    }

    private val p4TypeButtonRight = Label(
        width = buttonWidth, height = buttonHeight,
        posX = rightButtonX,
        posY = p4Input.posY - 1 - (84 - nameHeight) / 2,
        visual = Visual.EMPTY
    ).apply {
        onMouseClicked = {
            p4Type = changePlayerType(4, false, p4Type,true)
            orderNames[3] = Pair(p4Input.text, p4Type)
        }
    }

    private val p1Icon = Label(
        width = iconSize, height = iconSize,
        posX = 155,
        posY = 234,
        visual = ImageVisual("HumanIcon.png")
    )

    private val p2Icon = Label(
        width = iconSize, height = iconSize,
        posX = p4TypeButtonLeft.posX + buttonWidth + 5,
        posY = p2Input.posY - (iconSize - nameHeight) / 2,
        visual = ImageVisual("HumanIcon.png")
    )

    private val p3Icon = Label(
        width = iconSize, height = iconSize,
        posX = p4TypeButtonLeft.posX + buttonWidth + 5,
        posY = p3Input.posY - (iconSize - nameHeight) / 2,
    ).apply {
        isVisible = false
    }

    private val p4Icon = Label(
        width = iconSize, height = iconSize,
        posX = p4TypeButtonLeft.posX + buttonWidth + 5,
        posY = p4Input.posY - (iconSize - nameHeight) / 2,
    ).apply {
        isVisible = false
    }

    private val noP1 = Label(
        width = iconSize, height = iconSize,
        posX = 343, posY = p1Input.posY - (iconSize - nameHeight) / 2,
    ).apply {
        isVisible = false
    }

    private val noP2 = Label(
        width = iconSize, height = iconSize,
        posX = 343, posY = p2Input.posY - (iconSize - nameHeight) / 2,
        visual = ImageVisual("NotPlayingIcon.png")
    ).apply {
        isVisible = false
    }

    private val noP3 = Label(
        width = iconSize, height = iconSize,
        posX = 343, posY = p3Input.posY - (iconSize - nameHeight) / 2,
        visual = ImageVisual("NotPlayingIcon.png")
    )

    private val noP4 = Label(
        width = iconSize, height = iconSize,
        posX = 343, posY = p4Input.posY - (iconSize - nameHeight) / 2,
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
        }
    }

    private val fox = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = elk.posX + iconDistance + iconSize + 15, posY = 30,        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("fox.png", iconSize, iconSize)

        onMouseClicked = {
            showScoreCards(foxCardA, foxCardB)
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
        posX = cardAX - 5, posY = cardY,
    ).apply {
        visual = ImageVisual("Scoring_Bear_A.png")
        isVisible = false
    }

    private val bearCardB = Label(
        width = 207, height = 394,
        posX = cardBX - 5, posY = cardY,
        ).apply {
        visual = ImageVisual("Scoring_Bear_B.png")
        isVisible = false
    }

    private val elkCardA = Label(
        width = 262, height = 504,
        posX = cardAX, posY = cardY,
    ).apply {
        visual = ImageVisual("Scoring_Elk_A.png")
        isVisible = false
    }

    private val elkCardB = Label(
        width = 262, height = 504,
        posX = cardBX - 5, posY = cardY,

        ).apply {
        visual = ImageVisual("Scoring_Elk_B.png")
        isVisible = false
    }

    private val foxCardA = Label(
        width = 229, height = 497,
        posX = cardAX, posY = cardY,
    ).apply {
        visual = ImageVisual("Scoring_Fox_A.png")
        isVisible = false
    }

    private val foxCardB = Label(
        width = 229, height = 497,
        posX = cardBX, posY = cardY,
        ).apply {
        visual = ImageVisual("Scoring_Fox_B.png")
        isVisible = false
    }


    private val hawkCardA = Label(
        width = 229, height = 497,
        posX = cardAX, posY = cardY,
    ).apply {
        visual = ImageVisual("Scoring_Hawk_A.png")
        isVisible = false
    }

    private val hawkCardB = Label(
        width = 229, height = 497,
        posX = cardBX, posY = cardY,

        ).apply {
        visual = ImageVisual("Scoring_Hawk_B.png")
        isVisible = false
    }

    private val salmonCardA = Label(
        width = 229, height = 497,
        posX = cardAX, posY = cardY,
    ).apply {
        visual = ImageVisual("Scoring_Salmon_A.png")
        isVisible = false
    }

    private val salmonCardB = Label(
        width = 229, height = 497,
        posX = cardBX, posY = cardY,
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
                checkBox("checkBoxSalmonA")
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

    private val randomScoreCards = Button(
        width = 100, height = 30,
        posX = paneWidth - 130,
        posY = checkBoxElkB.posY + checkBoxElkA.height,
        text = "Random",
        font = Font(size = 18, color = Color.WHITE),
        visual = ColorVisual(101, 130, 255).apply {
            style.borderRadius = BorderRadius(15)
        }
    ).apply {
        onMouseClicked = {
            randomizeScoreCards()
        }
    }

    private val exitButton = Button(
        width = 78, height = 78,
        posX = 33, posY = 23,
    ).apply {
        visual = Visual.EMPTY
        onMouseClicked = {
            app.showMenuScene(MainMenuScene(app, rootService))
        }
    }


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


    private val tabLabel = Label(
        posX = paneX + paneWidth - tabWidth + 80,
        posY = paneY + paneHeight - tabHeight,
        width = tabWidth,
        height = tabHeight,
        visual = ImageVisual("FoldOutTab.png").apply {
            style.borderRadius = BorderRadius(15.0)
        }
    ).apply {
        onMouseClicked = {
            expandPanel()
            app.hostOnlineLobbyScene.resizeScoreCards()
        }
    }

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
            shuffleButton, exitButton, p1TypeButtonLeft, p2TypeButtonLeft, p3TypeButtonLeft, p4TypeButtonLeft,
            p1TypeButtonRight, p2TypeButtonRight, p3TypeButtonRight, p4TypeButtonRight,
            noP1, noP2, noP3, noP4, p1Icon, p2Icon, p3Icon, p4Icon,
        ).forEach { hostPanel.add(it) }

        listOf(
            startLabel, startButton, bear, elk, hawk, salmon, fox,
            bearCardA, bearCardB, elkCardA, elkCardB, foxCardA, foxCardB,
            hawkCardA, hawkCardB, salmonCardA, salmonCardB,
            checkBoxHawkA, checkBoxHawkB, checkBoxElkA, checkBoxElkB, checkBoxBearA, checkBoxBearB, checkBoxFoxA,
            checkBoxFoxB, checkBoxSalmonA, checkBoxSalmonB, randomScoreCards
        ).forEach { sidePanel.add(it) }


        backgroundOpacity = .5
        addComponents(
            logo,
            sidePanel,
            tabLabel,
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
                    tabLabel,
                    byX = movementDistance,
                )
            ).apply {
                onFinished = {
                    runOnGUIThread {
                        //arrowButton.isVisible = false
                        //arrowLabel.isVisible = false
                        //startLabel.isVisible = true
                        //startButton.isVisible = true
                        tabLabel.apply {
                            visual = ImageVisual("StartGameTab.png").apply {
                                style.borderRadius = BorderRadius(15)
                            }
                            onMouseClicked = {
                                TODO("Start game muss hier aufgerufen werden")
//                                rootService.gameService.startNewGame()
                            }
                        }
                    }
                }
            }
        )
    }






    private fun shuffleNames() {
        // ich filter die Liste nach allem die nicht leer sind und dann shuffle ich diese und packe sie in eine
        // Liste
        println(orderNames.toString())
        orderNames.shuffle()
        println(orderNames.toString())

        // wird wieder ins Feld geschieben
        p1Input.text = orderNames[0].first
        println(orderNames[0].second)
        println(orderNames[0].first)
        p2Input.text = orderNames[1].first
        p3Input.text = orderNames[2].first
        p4Input.text = orderNames[3].first

        changePlayerType(1,false,orderNames[0].second,false)
        changePlayerType(2,false,orderNames[1].second,false)
        changePlayerType(3,false,orderNames[2].second,false)
        changePlayerType(4,false,orderNames[3].second,false)
    }



//    private fun shuffleTypes(typePlayer: Int,typeNum: Int){
//        val newVisual = when (typeNum) {
//            0 -> ImageVisual("HumanIcon.png")
//
//            1 -> ImageVisual("EasyBotIcon.png")
//
//            2 -> ImageVisual("HardBotIcon.png")
//
//            3 -> ImageVisual("NotPlayingIcon.png")
//
//            else -> throw IllegalArgumentException("Only 4 possible visuals, $typeNum is invalid")
//        }
//
//
//        val leftLabel = when (typePlayer) {
//            1 -> p1Icon
//            2 -> p2Icon
//            3 -> p3Icon
//            4 -> p4Icon
//            else -> throw IllegalArgumentException("Player $typePlayer is not valid")
//        }
//
//        val rightLabel = when (typePlayer) {
//            1 -> noP1
//            2 -> noP2
//            3 -> noP3
//            4 -> noP4
//            else -> throw IllegalArgumentException("Player $typePlayer is not valid")
//        }
//
//        nameEntryDisabled(typeNum == 3, typePlayer)
//        adjustIcons(leftLabel, rightLabel, newVisual, typeNum != 3)
//
//    }

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

    private fun changePlayerType(playerNum: Int, left: Boolean, playerType: Int, changeType: Boolean): Int {
        val newType: Int
        if(changeType) {
            newType = if (left) {
                (playerType + 3) % 4
            } else {
                (playerType + 1)%4
            }
        } else {
            newType = playerType
        }


        val newVisual = when (newType) {
            0 -> ImageVisual("HumanIcon.png")

            1 -> ImageVisual("EasyBotIcon.png")

            2 -> ImageVisual("HardBotIcon.png")

            3 -> ImageVisual("NotPlayingIcon.png")

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

    private fun randomizeScoreCards() {
        val randomizerList = mutableListOf(true, false)
        val scoreCards = listOf(Pair(checkBoxBearA, checkBoxBearB), Pair(checkBoxSalmonA, checkBoxSalmonB),
            Pair(checkBoxHawkA, checkBoxHawkB), Pair(checkBoxFoxA, checkBoxFoxB), Pair(checkBoxElkA, checkBoxElkB))
        for (pair in scoreCards) {
            randomizerList.shuffle()
            pair.first.isChecked = randomizerList[0]
            pair.second.isChecked = randomizerList[1]
        }
    }

//    private fun resizeScoreCards() {
//        elkCardA.resize(192, 370)
//        elkCardB.resize(192, 370)
//        hawkCardA.resize(170, 370)
//        hawkCardB.resize(170, 370)
//        salmonCardA.resize(170, 370)
//        salmonCardB.resize(170, 370)
//        foxCardA.resize(170, 370)
//        foxCardB.resize(170, 370)
//        bearCardA.resize(194, 370)
//        bearCardB.resize(194, 370)
//    }
}



