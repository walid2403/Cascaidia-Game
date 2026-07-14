package gui

import entity.PlayerType
import service.Refreshable
import service.RootService
import tools.aqua.bgw.animation.MovementAnimation
import tools.aqua.bgw.animation.ParallelAnimation
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.CheckBox
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual
import tools.aqua.bgw.visual.Visual

/**
 * This scene shows the Join Lobby of the game. It shows all players in the Lobby with their name and playerType icon
 * as well as the host's current scoreCard selection.
 * @param app The [SopraApplication] of the game
 * @param [rootService] The [RootService] instance to access the other service methods and entity layer
 */
class JoinOnlineLobbyScene (private val app: SopraApplication, private val rootService: RootService) :
    MenuScene(1920, 1080), Refreshable  {

    private val sceneWidth = 1920
    private val sceneHeight = 1080
    private val paneWidth = 750
    private val paneHeight = 620
    private val tabHeight = 130
    private val tabWidth = 130
    private val paneX = (sceneWidth-paneWidth)/2
    private val paneY = (sceneHeight-paneHeight)/2

    private val movementDistance = paneWidth/2 - 90

    private val iconSize = 60
    private val iconDistance = 30

    private val nameWidth = 400
    private val nameHeight = 65
    private val nameY = paneHeight/2 - 170
    private val nameDistance = 35

    private var playerType: PlayerType = PlayerType.HUMAN
    private var playerName: String = "Name"


    private var panelsOut = false

    private val backgroundImage = Label(
        posX = 0,
        posY = 0,
        width = sceneWidth,
        height = sceneHeight,
        visual = ImageVisual("backgrounds/GameConfigMenuBackground.png")
    )

    private val playerViewPane = Pane<StaticComponentView<*>>(
        posX = paneX,
        posY = paneY,
        width = paneWidth,
        height = paneHeight,
        visual = ImageVisual("backgrounds/JoinMenuBackground.png").apply{
            style.borderRadius = BorderRadius(35)
        }
    )

    private val scoreCardSelectionPane = Pane<StaticComponentView<*>>(
        posX = paneX,
        posY = paneY,
        width = paneWidth,
        height = paneHeight,
        visual = ImageVisual("assets/ScoreCardPaneBackground.png").apply{
            style.borderRadius = BorderRadius(35)
        }
    )

    private val foldOutTab = Label(
        posX = paneX + paneWidth - tabWidth + 80,
        posY = paneY + paneHeight - tabHeight,
        width = tabWidth,
        height = tabHeight,
        visual = ImageVisual("assets/FoldOutTab.png").apply {
            style.borderRadius = BorderRadius(15.0)
        }
    ).apply {
        onMouseClicked = {
            resizeScoreCards()
            hawkCardA.isVisible = true
            hawkCardB.isVisible = true
            if(!panelsOut) {
                movePanelsOut()
                panelsOut = true
            } else {
                movePanelsIn()
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

    val backArrow = Button(
        width = 78, height = 78,
        posX = 33, posY = 23,
        visual = Visual.EMPTY
    ).apply {
        onMouseClicked = {
            resetScene()
            app.showMenuScene(app.joinOnlineScene)
        }
    }

    private val p1Input = Label(
        width = nameWidth,
        height = nameHeight,
        posX = (paneWidth - nameWidth)/2,
        posY = nameY,
        text = "Player 1",
        font = Font(size = 28),
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
    )

    private val p1Icon = Label(
        height = nameHeight,
        width = nameHeight,
        posX = p1Input.posX - nameHeight - 20,
        posY = p1Input.posY,
        visual = ImageVisual("icons/NetworkIcon.png").apply{
            style.borderRadius = BorderRadius(8)
        }
    )

    private val p2Input = Label(
        width = nameWidth,
        height = nameHeight,
        posX = (paneWidth - nameWidth)/2,
        posY = nameY + nameHeight + nameDistance,
        font = Font(size = 28, color = Color.DARK_GRAY),
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
    )

    private val p2Icon = Label(
        height = nameHeight,
        width = nameHeight,
        posX = p2Input.posX - nameHeight - 20,
        posY = p2Input.posY,
        visual = ImageVisual("icons/NetworkIcon.png").apply {
            style.borderRadius = BorderRadius(8)
        }
    )

    private val p3Input = Label(
        width = nameWidth,
        height = nameHeight,
        posX = (paneWidth - nameWidth)/2,
        posY = nameY + 2*nameHeight + 2*nameDistance,
        text = "Empty Player Slot",
        font = Font(size = 28, color = Color.DARK_GRAY),
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
    ).apply {
        isVisible = false
    }

    private val p3Icon = Label(
        height = nameHeight,
        width = nameHeight,
        posX = p3Input.posX - nameHeight - 20,
        posY = p3Input.posY,
        visual = ImageVisual("icons/NetworkIcon.png").apply {
            style.borderRadius = BorderRadius(8)
        }
    ).apply {
        isVisible = false
    }

    private val p4Input = Label(
        width = nameWidth,
        height = nameHeight,
        posX = (paneWidth - nameWidth)/2,
        posY = nameY + 3*nameHeight + 3*nameDistance,
        text = "Empty Player Slot",
        font = Font(size = 28, color = Color.DARK_GRAY),
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
    ).apply {
        isVisible = false
    }

    private val p4Icon = Label(
        height = nameHeight,
        width = nameHeight,
        posX = p4Input.posX - nameHeight - 20,
        posY = p4Input.posY,
        visual = ImageVisual("icons/NetworkIcon.png").apply {
            style.borderRadius = BorderRadius(8)
        }
    ).apply {
        isVisible = false
    }

    private val waitingToStart = Label(
        height = 60,
        width = nameWidth + nameHeight + 10,
        posX = p4Input.posX - 37,
        posY = p4Icon.posY + nameHeight + 20,
        text = "Waiting for the host to start the game...",
        font = Font(size = 20, fontWeight = Font.FontWeight.BOLD, family = "Canva Sans"),
        visual = ColorVisual(243, 197, 39).apply {
            style.borderRadius = BorderRadius(30)
        }
    )

    private val elkIcon = Label(
        posX = (paneWidth)/2 + 90 - (iconSize + 15)/2,
        posY = 30,
        width = iconSize + 15,
        height = iconSize + 15
    ).apply {
        visual = ImageVisual("tokens/elk.png")
        onMouseClicked = {
            showScoreCards(elkCardA, elkCardB)
        }
    }

    private val hawkIcon = Label(
        posX = elkIcon.posX - 2*iconDistance - 2*(iconSize + 15),
        posY = 30,
        width = iconSize + 15,
        height = iconSize + 15
    ).apply {
        visual = ImageVisual("tokens/hawk.png")
        onMouseClicked = {
            showScoreCards(hawkCardA, hawkCardB)
        }
    }

    private val salmonIcon = Label(
        posX = elkIcon.posX - iconDistance - (iconSize + 15),
        posY = 30,
        width = iconSize + 15,
        height = iconSize + 15
    ).apply {
        visual = ImageVisual("tokens/salmon.png")
        onMouseClicked = {
            showScoreCards(salmonCardA, salmonCardB)
        }
    }

    private val foxIcon = Label(
        posX = elkIcon.posX + iconDistance + iconSize + 15,
        posY = 30,
        width = iconSize + 15,
        height = iconSize + 15
    ).apply {
        visual = ImageVisual("tokens/fox.png")
        onMouseClicked = {
            showScoreCards(foxCardA, foxCardB)
        }
    }

    private val bearIcon = Label(
        posX = elkIcon.posX + 2*iconDistance + 2*(iconSize + 15),
        posY = 30,
        width = iconSize + 15,
        height = iconSize + 15
    ).apply {
        visual = ImageVisual("tokens/bear.png")
        onMouseClicked = {
            showScoreCards(bearCardA, bearCardB)
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
    )

    val checkBoxSalmonB = CheckBox(
        posX = salmonIcon.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    )

    val checkBoxHawkA = CheckBox(
        posX = hawkIcon.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    )

    val checkBoxHawkB = CheckBox(
        posX = hawkIcon.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    )

    val checkBoxBearA = CheckBox(
        posX = bearIcon.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    )

    val checkBoxBearB = CheckBox(
        posX = bearIcon.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    )

    val checkBoxFoxA = CheckBox(
        posX = foxIcon.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    )

    val checkBoxFoxB = CheckBox(
        posX = foxIcon.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    )

    val checkBoxElkA = CheckBox(
        posX = elkIcon.posX + 10,
        posY = 100,
        width = 30,
        height = 50,
        text = "A",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    )

    val checkBoxElkB = CheckBox(
        posX = elkIcon.posX + 10,
        posY = 140,
        width = 30,
        height = 50,
        text = "B",
        alignment = Alignment.CENTER_LEFT,
        font = Font(20.0, Color.WHITE),
    )

    private val bearCardA = Label(
        width = 207,
        height = 394,
        posX = 230,
        posY = 200,
    ).apply {
        visual = ImageVisual("scoringCards/Scoring_Bear_A.png")
        isVisible = false
    }

    private val bearCardB = Label(
        width = 207,
        height = 394,
        posX = 490,
        posY = 200,
        ).apply {
        visual = ImageVisual("scoringCards/Scoring_Bear_B.png")
        isVisible = false
    }

    private val elkCardA = Label(
        width = 262,
        height = 504,
        posX = 230,
        posY = 200,
    ).apply {
        visual = ImageVisual("scoringCards/Scoring_Elk_A.png")
        isVisible = false
    }

    private val elkCardB = Label(
        width = 262,
        height = 504,
        posX = 490,
        posY = 200,
        ).apply {
        visual = ImageVisual("scoringCards/Scoring_Elk_B.png")
        isVisible = false
    }

    private val foxCardA = Label(
        width = 229,
        height = 497,
        posX = 230,
        posY = 200,
    ).apply {
        visual = ImageVisual("scoringCards/Scoring_Fox_A.png")
        isVisible = false
    }

    private val foxCardB = Label(
        width = 229,
        height = 497,
        posX = 490,
        posY = 200,
        ).apply {
        visual = ImageVisual("scoringCards/Scoring_Fox_B.png")
        isVisible = false
    }

    private val hawkCardA = Label(
        width = 229,
        height = 497,
        posX = 230,
        posY = 200,
    ).apply {
        visual = ImageVisual("scoringCards/Scoring_Hawk_A.png")
        isVisible = false
    }

    private val hawkCardB = Label(
        width = 229,
        height = 497,
        posX = 490,
        posY = 200
    ).apply {
        visual = ImageVisual("scoringCards/Scoring_Hawk_B.png")
        isVisible = false
    }

    private val salmonCardA = Label(
        width = 229,
        height = 497,
        posX = 230,
        posY = 200,
    ).apply {
        visual = ImageVisual("scoringCards/Scoring_Salmon_A.png")
        isVisible = false
    }

    private val salmonCardB = Label(
        width = 229,
        height = 497,
        posX = 490,
        posY = 200,
        ).apply {
        visual = ImageVisual("scoringCards/Scoring_Salmon_B.png")
        isVisible = false
    }

    private val lobbyCodeLabel = Label(
        posX = 35,
        posY = paneHeight - 70,
        width = 500,
        height = 40,
        alignment = Alignment.TOP_LEFT,
        font = Font(24.0, family = "Canva Sans"),
        text = "Lobby Code: UNKNOWN",
    )

    init {
        addComponents(
            backgroundImage,
            scoreCardSelectionPane,
            foldOutTab,
            playerViewPane,
        )
        playerViewPane.addAll(
            backArrow,
            p1Input,
            p2Input,
            p3Input,
            p4Input,
            p1Icon,
            p2Icon,
            p3Icon,
            p4Icon,
            waitingToStart,
            lobbyCodeLabel,
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

    private fun resetScene() {

        listOf(checkBoxSalmonA, checkBoxSalmonB, checkBoxElkA, checkBoxElkB, checkBoxBearA, checkBoxBearB, checkBoxFoxA,
            checkBoxFoxB, checkBoxHawkA, checkBoxHawkB).forEach { it.isChecked = false }

        listOf(p3Input, p4Input).forEach { name ->
            name.text = ""
            name.isVisible = false
        }

        listOf(p3Icon, p4Icon).forEach { icon ->
            icon.isVisible = false
            icon.visual = ImageVisual("icons/NetworkIcon.png")
        }

        panelsOut = false
        movePanelsIn()
    }

    /**
     * This function moves the side panel containing the scorecard selection and images to the right and
     * the main panel to the left. The onClick action for the Tab [foldOutTab] is changed to [movePanelsIn]
     */

    private fun movePanelsOut() {
        playAnimation(
            ParallelAnimation(
                MovementAnimation(
                    playerViewPane,
                    byX = -movementDistance,
                ),
                MovementAnimation(
                    scoreCardSelectionPane,
                    byX = movementDistance,
                ),
                MovementAnimation(
                    foldOutTab,
                    byX = movementDistance,
                )
            ).apply {
                onFinished = {
                    foldOutTab.visual = ImageVisual("assets/FoldInTab.png").apply {
                        style.borderRadius = BorderRadius(15)
                    }
                }
            }
        )
    }

    /**
     * This function moves the side panel containing the scorecards as well as the main panel back to the middle.
     * The onClick action for the Tab [foldOutTab] is changed to [movePanelsOut]
     */

    private fun movePanelsIn() {
        playAnimation(
            ParallelAnimation(
                MovementAnimation(
                    playerViewPane,
                    byX = movementDistance,
                ),
                MovementAnimation(
                    scoreCardSelectionPane,
                    byX = -movementDistance,
                ),
                MovementAnimation(
                    foldOutTab,
                    byX = -movementDistance,
                )
            ).apply {
                onFinished = {
                    foldOutTab.visual = ImageVisual("assets/FoldOutTab.png").apply {
                        style.borderRadius = BorderRadius(15)
                    }
                }
            }
        )
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
     * This function fills the [String]s in [players] into the corresponding player name [Label] and the player's
     * player type into the player's player icon [Label]
     * @param players [List] [String] containing the players' names in the order they are set to play in
     */

    private fun updatePlayers(players: List<String>) {
        if(players.size !in 2..4) {
            throw IllegalArgumentException("Invalid number of players: ${players.size}")
        }
        if (!duplicateFree(players)) {
            throw IllegalArgumentException("Duplicate names are not allowed")
        }
        for (i in 1..4) {
            when (i) {
                1 -> {
                    p1Input.text = players[0]
                    p1Icon.visual = getVisual(players[0])
                }
                2 -> {
                    if (players.size > 1) {
                        p2Input.text = players[1]
                        p2Icon.visual = getVisual(players[1])

                        p2Icon.isVisible = true
                        p2Input.isVisible = true
                    } else {
                        p2Icon.isVisible = false
                        p2Input.isVisible = false
                        p2Input.text = ""
                    }
                }
                3 -> {
                    if (players.size > 2) {
                        p3Input.text = players[2]
                        p3Icon.visual = getVisual(players[2])

                        p3Icon.isVisible = true
                        p3Input.isVisible = true
                    } else {
                        p3Icon.isVisible = false
                        p3Input.isVisible = false
                        p3Input.text = ""
                    }
                }
                4 -> {
                    if (players.size > 3) {
                        p4Input.text = players[3]
                        p4Icon.visual = getVisual(players[3])

                        p4Icon.isVisible = true
                        p4Input.isVisible = true
                    } else {
                        p4Icon.isVisible = false
                        p4Input.isVisible = false
                        p4Input.text = ""
                    }
                }
            }
        }
    }

    /**
     * This function updates the selected scorecards according to the [Boolean] values in the [List]
     * @param [selection] [List] of [Boolean] with the new scorecard values: if true A is selected,
     * if false B is selected
     */
    private fun updateScoreCards(selection: List<Boolean?>) {
        if(selection.size != 5) throw IllegalArgumentException("List scoreCards must have exactly one Boolean for " +
                "every animal type, invalid List length: ${selection.size}")

        for ( i in selection.indices) {
            var boxA = checkBoxFoxA
            var boxB = checkBoxFoxB
            when (i) {
                0 -> {
                    boxA = checkBoxBearA
                    boxB = checkBoxBearB
                }
                1 -> {
                    boxA = checkBoxElkA
                    boxB = checkBoxElkB
                }
                2 -> {
                    boxA = checkBoxFoxA
                    boxB = checkBoxFoxB
                }
                3 -> {
                    boxA = checkBoxHawkA
                    boxB = checkBoxHawkB
                }
                4 -> {
                    boxA = checkBoxSalmonA
                    boxB = checkBoxSalmonB
                }
            }
            if(selection[i] == true) {
                boxA.isChecked = true
                boxB.isChecked = false
            } else if(selection[i] == false) {
                boxB.isChecked = true
                boxA.isChecked = false
            } else {
                boxB.isChecked = false
                boxA.isChecked = false
            }
        }
    }

    /**
     * returns true if the given [List] [String] is duplicate free, false otherwise
     */
    private fun duplicateFree(players: List<String>): Boolean {
        return players.size == players.distinct().size
    }

    /**
     * This function returns the [ImageVisual] corresponding to the [playerType], if the parameter [name] is identical
     * to the [playerName] of the local player and the NetworkIcon otherwise
     */

    private fun getVisual(name: String): ImageVisual {
        return if (name == playerName) {
            when(playerType) {
                PlayerType.HUMAN -> ImageVisual("icons/HumanIcon3.png").apply {
                    style.borderRadius = BorderRadius(8)
                }
                PlayerType.EASY_BOT -> ImageVisual("icons/EasyBotIcon3.png").apply {
                    style.borderRadius = BorderRadius(8)
                }
                PlayerType.HARD_BOT -> ImageVisual("icons/HardBotIcon3.png").apply {
                    style.borderRadius = BorderRadius(8)
                }
                else -> throw IllegalArgumentException("Player type must be PlayerType Object and can't be NETWORK, " +
                        "$playerType not supported")
            }
        } else {
            ImageVisual("icons/NetworkIcon.png").apply {
                style.borderRadius = BorderRadius(8)
            }
        }
    }

    private fun resizeScoreCards() {
        foxCardA.resize(183, 397)
        foxCardB.resize(183, 397)
        salmonCardA.resize(183, 397)
        salmonCardB.resize(183, 397)
        hawkCardA.resize(183, 397)
        hawkCardB.resize(183, 397)
        elkCardA.resize(209, 403)
        elkCardB.resize(209, 403)
    }

    /**
     * This function updates the games configurations (players, their order, and the selected scorecards) after a
     * GameConfigMessage
     * @param playerList [List] [String] containing the players' names in the order they are set to play in
     * @param scoringCards [List] [Boolean] contains a Boolean for each animal type, if true card A is selected,
     * card B otherwise. Order of Booleans: hawk, salmon, elk, fox, bear
     */
    override fun refreshAfterGameConfigUpdate(playerList: List<String>, scoringCards: List<Boolean?>) {
        updatePlayers(playerList)
        updateScoreCards(scoringCards)
    }

    override fun refreshAfterStartGame() {
        resetScene()
        app.hideMenuScene()
    }

    override fun refreshAfterPlayerJoined(playerName: String) {
        if(!p1Input.isVisible) {
            addNewPlayer(playerName, p1Input, p1Icon)
        } else if (!p2Input.isVisible) {
            addNewPlayer(playerName, p2Input, p2Icon)
        } else if(!p3Input.isVisible) {
            addNewPlayer(playerName, p3Input, p3Icon)
        } else if(!p4Input.isVisible) {
            addNewPlayer(playerName, p4Input, p4Icon)
        } else {
            throw IllegalArgumentException("All 4 Lobby Slots are already occupied. No new player can join.")
        }
    }

    override fun refreshAfterJoinGame(lobbyCode: String, playerName: String, playerType: PlayerType) {
        this.playerName = playerName
        this.playerType = playerType
        this.lobbyCodeLabel.text = "Lobby Code: $lobbyCode"
    }

    private fun addNewPlayer(name: String, nameField: Label, icon: Label) {
        nameField.text = name
        icon.visual = ImageVisual("icons/NetworkIcon.png").apply {
            style.borderRadius = BorderRadius(8)
        }
        nameField.isVisible = true
        icon.isVisible = true
    }
}