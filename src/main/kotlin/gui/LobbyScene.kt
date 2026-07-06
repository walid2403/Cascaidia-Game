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
import kotlin.Pair
import kotlin.String

/**
 * This scene is the Lobby scene for a local game in the HotSeat mode. The number of players, their names and
 * [PlayerType] can be configured here. The player order can be shuffled randomly. The scorecards used in the game can
 * be selected manually or randomly. The game can be started when 2-4 players are set to play with names and types
 * entered and a scorecard has been selected for each animal type.
 * @param app The [SopraApplication] of this game
 * @param rootService The [RootService] instance to access the other service methods and entity layer
 */
class LobbyScene(private val app: SopraApplication, private val rootService: RootService) :
    MenuScene(1920, 1080), Refreshable {

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
            orderNames[0] = Pair(this.text, orderNames[0].second)
            warning.isVisible = false
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
            orderNames[1] = Pair(this.text,orderNames[1].second)
            warning.isVisible = false
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
            orderNames[2] = Pair(this.text,orderNames[2].second)
            warning.isVisible = false
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
            orderNames[3] = Pair(this.text,orderNames[3].second)
            warning.isVisible = false
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
            warning.isVisible = false
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
            warning.isVisible = false
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
            warning.isVisible = false
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
            warning.isVisible = false
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
            warning.isVisible = false
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
            warning.isVisible = false
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
            warning.isVisible = false
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
            warning.isVisible = false
        }
    }

    //the pXIcons are the active player Icons displayed in the text entry box to the left of the player name
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

    //the noPX Icons are the Icons used for not participating/disabled player spots. The icons are displayed in the
    //middle of the text entry box
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
            warning.isVisible = false
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
            warning.isVisible = false
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
            warning.isVisible = false
        }
    }

    private val fox = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = elk.posX + iconDistance + iconSize + 15, posY = 30,        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("fox.png", iconSize, iconSize)

        onMouseClicked = {
            showScoreCards(foxCardA, foxCardB)
            warning.isVisible = false
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
            warning.isVisible = false
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
        posX = cardBX, posY = cardY
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

    private val checkBoxSalmonA = CheckBox(
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
                warning.isVisible = false
            }
        }
    }

    private val checkBoxSalmonB = CheckBox(
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
                warning.isVisible = false
            }
        }
    }


    private val checkBoxHawkA = CheckBox(
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
                warning.isVisible = false
            }
        }
    }

    private val checkBoxHawkB = CheckBox(
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
                warning.isVisible = false
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
                warning.isVisible = false
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
                warning.isVisible = false
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
                warning.isVisible = false
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
                warning.isVisible = false
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
                warning.isVisible = false
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
                warning.isVisible = false
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
            warning.isVisible = false
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
            warning.isVisible = false
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
            resizeScoreCards()
            hawkCardA.isVisible = true
            hawkCardB.isVisible = true
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

    /**
     * Informs the player why he is not able to start the game. Is invisible unless 'start game' is clicked but not all
     * conditions to start playing are met.
     */
    private val warning = Label(
        width = paneWidth - 80,
        height = 100,
        posX = paneX + 160 + movementDistance,
        posY = paneY + paneHeight + 45,
        visual = ColorVisual(204, 78, 0).apply {
            style.borderRadius = BorderRadius(15)
        },
        font = Font(size = 25, color = Color.WHITE, family = "Canva Sans", fontWeight = Font.FontWeight.BOLD),
        alignment = Alignment.CENTER,
        isWrapText = true
    ).apply {
        isVisible = false
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
            warning
        )
    }

    /**
     * This function moves the side panel containing the scorecard selection and images to the right and
     * the main panel to the left. The onClick action for the Tab [tabLabel] is changed to startGame
     */

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
                        tabLabel.apply {
                            visual = ImageVisual("StartGameTab.png").apply {
                                style.borderRadius = BorderRadius(15)
                            }
                            onMouseClicked = {
                                if(checkStartReady()) {
                                    rootService.gameService.startNewGame(getFinalPlayers(), getFinalScoreCards())
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * This function shuffles the entered names along with their playerType Icons. Slots currently set to "not playing"
     * will be sorted to the bottom slots.
     */

    private fun shuffleNames() {
        // shuffle all entries and save in a separate list
        val shuffledNames: List<Pair<String, Int>> = orderNames.shuffled()
        //sort into playing and not playing/disabled player spots, put disabled entries in the lowest player spots
        val sortedNames = mutableListOf(Pair("", 3), Pair("", 3), Pair("", 3), Pair("", 3))
        var endOfList = 3
        var startOfList = 0
        for(i in orderNames.indices) {

            if(shuffledNames[i].second == 3) {
                sortedNames[endOfList] = shuffledNames[i]
                endOfList -= 1
            } else {
                sortedNames[startOfList] = shuffledNames[i]
                startOfList += 1
            }
        }
        //copy the shuffled and sorted list into our orderNames list
        for(i in orderNames.indices) {
            orderNames[i] = sortedNames[i]
        }

        // enter the names and playerTypes into their respective fields according to the position they were shuffled to
        p1Input.text = sortedNames[0].first
        p1Type = sortedNames[0].second
        p2Input.text = sortedNames[1].first
        p2Type = sortedNames[1].second
        p3Input.text = sortedNames[2].first
        p3Type = sortedNames[2].second
        p4Input.text = sortedNames[3].first
        p4Type = sortedNames[3].second

        //adjust the icons and whether the text entry is enabled or not
        changePlayerType(1,false,sortedNames[0].second,false)
        changePlayerType(2,false,sortedNames[1].second,false)
        changePlayerType(3,false,sortedNames[2].second,false)
        changePlayerType(4,false,sortedNames[3].second,false)
    }

    /**
     * This function unchecks the complimentary checkbox to the one given as a String. The checkbox cannot be given as
     * a parameter directly because of issues with recursive function calls.
     * @param checkBox a [String] of the name of the [CheckBox] that is now checked. Its complement will be unchecked.
     */
    private fun checkBox(checkBox: String) {

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

    /**
     * This function updates a player's player type and player type icon according to the input parameters.
     * @param playerNum [Int] specifying which player's icon will be updated. 1 = first player slot, 2 = second...
     * @param leftButton [Boolean] indicating whether the left Button was pressed to initiate the icon change
     * @param playerType [Int] of the player's previous player Type
     * @param changeType [Boolean] indicating if the player type will be adjusted as well or only the player type icon
     */

    private fun changePlayerType(playerNum: Int, leftButton: Boolean, playerType: Int, changeType: Boolean): Int {

        val newType: Int = if(changeType) {
            if (leftButton) {
                (playerType + 3) % 4
            } else {
                (playerType + 1)%4
            }
        } else {
            playerType
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

    /**
     * This function assigns the [newVisual] into the left or right Label depending on the [leftIconVisible] value
     * @param leftLabel the player's left [Label], used for the human, easy and hard bot icons
     * @param rightLabel the player's right [Label], used for the "not playing"/slot disabled icon
     * @param newVisual the [ImageVisual] that will be assigned to one of the [Label]s
     * @param leftIconVisible [Boolean] if true, newVisual will be saved in the leftLabel, if false in the rightLabel.
     * The other Label will be made invisible
     */

    private fun adjustIcons(leftLabel: Label, rightLabel: Label, newVisual: ImageVisual, leftIconVisible: Boolean) {
        if(leftIconVisible) {
            leftLabel.isVisible = true
            rightLabel.isVisible = false
            leftLabel.visual = newVisual
        } else {
            leftLabel.isVisible = false
            rightLabel.isVisible = true
            rightLabel.visual = newVisual
        }
    }

    /**
     * This function adjusts whether a player's text entry field is visible and entry is enabled.
     * @param disableEntry [Boolean], if true entry will be disabled and the field will be set to not visible
     * @param playerNum [Int] number of the player who's text entry field will be adjusted
     */

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

    /**
     * This function makes the given [Label]s visible and all other scoring card labels invisible
     */

    private fun showScoreCards(cardA: Label, cardB: Label) {
        val cards = listOf(salmonCardA, salmonCardB, hawkCardA, hawkCardB, foxCardA, foxCardB,
            bearCardA, bearCardB, elkCardA, elkCardB)
        cards.forEach { card ->
            if(card == cardA || card == cardB) card.isVisible = true
            else card.isVisible = false
        }
    }

    /**
     * This function randomly selects A or B for each pair of scoring cards
     */

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

    /**
     * This function resizes all scoring cards to a height of 370 pixels, the width is respectively
     * resized proportionally
     */

    private fun resizeScoreCards() {
        elkCardA.resize(192, 370)
        elkCardB.resize(192, 370)
        hawkCardA.resize(170, 370)
        hawkCardB.resize(170, 370)
        salmonCardA.resize(170, 370)
        salmonCardB.resize(170, 370)
        foxCardA.resize(170, 370)
        foxCardB.resize(170, 370)
        bearCardA.resize(194, 370)
        bearCardB.resize(194, 370)
    }

    /**
     * This function returns true, if all conditions to start the game are met. Returns false otherwise.
     * The conditions: a scorecard is selected for each animal, at least 2 player slots are set to play, and every
     * player slot set to play has a distinct name entered
     */
    private fun checkStartReady(): Boolean {
        var ready = scoreCardsSelected()
        val names = listOf(p1Input.text, p2Input.text, p3Input.text, p4Input.text)
        val types = listOf(p1Type, p2Type, p3Type, p4Type)
        var counter = 0
        for(i in 0..3) {
            if(names[i] == "" && types[i] != 3) {
                ready = false
                warning.text = "Enter a name for all participating players and set unused slots to 'not playing'."
                warning.isVisible = true
            }
            if(types[i] != 3) {
                counter++
            }
        }
        if (counter < 2) {
            //the message to enter names for all players set to play is prioritized. The too few player warning will
            //only be shown if player names are entered where needed.
            if (ready) {
                warning.text = "You need at least 2 players to play."
                warning.isVisible = true
                ready = false
            }
        }

        if(names.filter{it != ""}.size != names.filter{it != ""}.distinct().size) {
            warning.text = "All player names must be unique."
            warning.isVisible = true
            ready = false
        }
        return ready
    }

    /**
     * This function returns true if exactly 5 scorecard [CheckBox]es are checked. As no two boxes
     * of the same animal type can be checked at once, this means that a scorecard is selected for each animal.
     * Returns false otherwise.
     */
    private fun scoreCardsSelected(): Boolean {
        var allSelected = true
        var checkedBoxes = 0
        val checkBoxList = listOf(checkBoxBearA, checkBoxBearB, checkBoxFoxA, checkBoxFoxB, checkBoxSalmonA,
            checkBoxSalmonB, checkBoxHawkA, checkBoxHawkB, checkBoxElkA, checkBoxElkB)
        checkBoxList.forEach { checkBox -> if (checkBox.isChecked) checkedBoxes++ }
        if(checkedBoxes != 5) {
            warning.text = "Select a Scoring Card for each animal type."
            warning.isVisible = true
            allSelected = false
        }
        return allSelected
    }

    /**
     * This function returns a [List] of [Pair]s of [String] and [PlayerType] containing the name and type of all
     * occupied player slots.
     */
    private fun getFinalPlayers(): List<Pair<String, PlayerType>> {
        val names = getFinalPlayerNames()
        val types = getFinalPlayerTypes()
        val finalList = mutableListOf<Pair<String, PlayerType>>()
        for(i in names.indices) {
            finalList.add(Pair(names[i], types[i]))
        }
        return finalList
    }

    /**
     * This function returns a [List] of [String] containing the name of each participating player, empty player
     * slots will be ignored
     */
    private fun getFinalPlayerNames(): List<String> {
        val list = mutableListOf<String>()
        if(p1Input.text != "" && p1Type != 3) list.add(p1Input.text)
        if(p2Input.text != "" && p2Type != 3) list.add(p2Input.text)
        if(p3Input.text != "" && p3Type != 3) list.add(p3Input.text)
        if(p4Input.text != "" && p4Type != 3) list.add(p4Input.text)
        return list.toList()
    }

    /**
     * This function returns a [List] of [PlayerType] containing the type of each participating player, empty player
     * slots will be ignored
     */
    private fun getFinalPlayerTypes(): List<PlayerType> {
        val list = mutableListOf<PlayerType>()
        if(p1Input.text != "" && p1Type != 3) list.add(getPlayerType(p1Type))
        if(p2Input.text != "" && p2Type != 3) list.add(getPlayerType(p2Type))
        if(p3Input.text != "" && p3Type != 3) list.add(getPlayerType(p3Type))
        if(p4Input.text != "" && p4Type != 3) list.add(getPlayerType(p4Type))
        return list.toList()
    }

    /**
     * This function returns the [PlayerType] corresponding to the [Int] given in [type]
     */
    private fun getPlayerType(type: Int): PlayerType {
        return when (type) {
            0 -> PlayerType.HUMAN
            1 -> PlayerType.EASY_BOT
            2 -> PlayerType.HARD_BOT
            else -> throw IllegalArgumentException("Player $type is not valid")
        }
    }

    /**
     * This function returns a [List] of [Boolean], one for each animal type. When true, scorecard A was selected for
     * this animal, otherwise card B. Order of animals: bear, elk, salmon, hawk, fox
     */
    private fun getFinalScoreCards(): List<Boolean> {
        val list = mutableListOf<Boolean>()
        list.add(0, checkBoxBearA.isChecked)
        list.add(1, checkBoxElkA.isChecked)
        list.add(2, checkBoxSalmonA.isChecked)
        list.add(3, checkBoxHawkA.isChecked)
        list.add(4, checkBoxFoxA.isChecked)
        return list.toList()
    }

    override fun refreshAfterStartGame() {
        app.hideMenuScene()
    }
}



