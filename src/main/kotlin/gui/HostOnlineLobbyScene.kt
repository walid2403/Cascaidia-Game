package gui

import entity.PlayerType
import service.Refreshable
import service.RootService
import tools.aqua.bgw.animation.DelayAnimation
import tools.aqua.bgw.animation.MovementAnimation
import tools.aqua.bgw.animation.ParallelAnimation
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.CheckBox
import tools.aqua.bgw.components.uicomponents.Label
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


/**
 * This scene shows the Host Lobby of the game. All players joining the Lobby will be shown with their name and a
 * Network Player Icon, the Host will be shown with the name [playerName] he entered in the [HostOnlineScene] and the
 * PlayerIcon for the [PlayerType] he selected, [playerType].
 * The Host can manually reorder the players or shuffle them. The Host can manually select the scoringcards used for
 * each animal type or chose a random selection. When there are 2-4 players in the Lobby and scorecards were selected,
 * the Host can start the game.
 *
 * @param app The [SopraApplication] of the game
 * @param [rootService] The [RootService] instance to access the other service methods and entity layer
 * @param playerName The [String] that was entered in the [HostOnlineScene]
 * @param playerType The [Int] corresponding to the [PlayerType] selected in [HostOnlineScene]
 */
class HostOnlineLobbyScene(private val app: SopraApplication,
                           private val rootService: RootService,
                           private val playerName: String = "Name",
                           private val playerType: PlayerType = PlayerType.HUMAN,
                           private val lobbyCode: String = "Code"
) : MenuScene(1920, 1080), Refreshable  {

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

    private val buttonWidth = 50
    private val buttonHeight = 50
    private val downUpButtonDistance = 20

    private val shuffleButtonWidth = 50
    private val shuffleButtonHeight = 50
    private val shuffleHeightPanel = 45
    private val shuffleWidthPanel = (paneWidth- 2*shuffleButtonWidth)/3 -30

    private val cardY = 230
    private val cardAX = 245
    private val cardBX = 505


    private val logo = Label(
        posX = 0,
        posY = 0,
        width = 1920,
        height = 1080,
        visual = ImageVisual("backgrounds/GameConfigMenuBackground.png")
    )
    private val hostPanel = Pane<UIComponent>(
        posX = paneX, posY = paneY,
        width = paneWidth, height = paneHeight,
    ).apply {
        visual = ImageVisual("backgrounds/HostMenuBackground.png").apply {
            style.borderRadius = BorderRadius(35)
        }
    }

    private val sidePanel = Pane<UIComponent>(
        posX = paneX, posY = paneY,
        width = paneWidth, height = paneHeight,
    ).apply {
        visual = ImageVisual("assets/ScoreCardPaneBackground.png").apply {
            style.borderRadius = BorderRadius(35)
        }
    }

    private val downButtonP1 = Button(
        width = buttonWidth, height = buttonHeight,
        posX = (paneWidth+nameWidth)/2 + downUpButtonDistance ,posY = nameY + (nameHeight - buttonHeight)/2,
        text = "↓",font = Font(size = 28),
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
    ).apply{
        onMouseClicked ={
            switchNames(1)
        }
        isVisible = false
    }

    private val downButtonP2 = Button(
        width = buttonWidth, height = buttonHeight,
        posX = (paneWidth+nameWidth)/2 + downUpButtonDistance ,posY = nameY + nameHeight + nameDistance +
                (nameHeight - buttonHeight)/2,
        text = "↓",font = Font(size = 28)
    ).apply{
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
        onMouseClicked ={
            switchNames(2)
        }
        isVisible = false
    }

    private val downButtonP3 = Button(
        width = buttonWidth, height = buttonHeight,
        posX = (paneWidth+nameWidth)/2 + downUpButtonDistance ,posY = nameY + 2*nameHeight + 2*nameDistance +
                (nameHeight - buttonHeight)/2,
        text = "↓",font = Font(size = 28)
    ).apply{
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
        onMouseClicked ={
            switchNames(3)
        }
        isVisible = false
    }

    private val shuffleButton = Button(
        width = shuffleButtonWidth, height = shuffleButtonHeight,
        posX = shuffleWidthPanel, posY = shuffleHeightPanel,
        text = "⤮",font = Font(size = 28)
    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(360) }
        onMouseClicked = {
            shuffleNames()
        }
    }

    private val p1Input = Label(
        width = nameWidth, height = nameHeight,
        posX = (paneWidth-nameWidth)/2, posY = nameY,
        text = playerName,
        font = Font(size = 28)

    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
    }

    private val p1Icon = Label(
        height = nameHeight,
        width = nameHeight,
        posX = p1Input.posX - nameHeight - 20,
        posY = p1Input.posY,
        visual = getVisual(playerName)
    )

    private val p2Input = Label(
        width = nameWidth, height = nameHeight,
        posX = (paneWidth - nameWidth)/2, posY = nameY + nameHeight + nameDistance,
        text = "",
        font = Font(size = 28),
        visual = ColorVisual(204, 212, 209).apply {
            style.borderRadius = BorderRadius(8)
        }
    ).apply {
        isVisible = false
    }

    private val p2Icon = Label(
        height = nameHeight,
        width = nameHeight,
        posX = p2Input.posX - nameHeight - 20,
        posY = p2Input.posY,
        visual = ImageVisual("icons/NetworkIcon.png").apply {
            style.borderRadius = BorderRadius(8)
        }
    ).apply {
        isVisible = false
    }

    private val p3Input = Label(
        width = nameWidth, height = nameHeight,
        posX = (paneWidth - nameWidth)/2, posY = nameY + 2*nameHeight + 2*nameDistance,
        text = "",
        font = Font(size = 28),
        visual = ColorVisual(204, 212, 209).apply {
            style.borderRadius = BorderRadius(8)
        }
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
        width = nameWidth, height = nameHeight,
        posX = (paneWidth - nameWidth)/2, posY = nameY + 3*nameHeight + 3*nameDistance,
        text = "",
        font = Font(size = 28),
        visual = ColorVisual(204, 212, 209).apply {
            style.borderRadius = BorderRadius(8)
        }
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

    private val orderOfNames = mutableListOf(p1Input, p2Input, p3Input, p4Input)
    private val orderOfTypes = mutableListOf(p1Icon, p2Icon, p3Icon, p4Icon)

    private val elk = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = (paneWidth)/2 + 90 - (iconSize + 15)/2, posY = 30,
        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("tokens/elk.png", iconSize, iconSize)
        onMouseClicked = {
            showScoreCards(elkCardA, elkCardB)
        }
    }

     val hawk = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = elk.posX - 2*iconDistance - 2*(iconSize + 15), posY = 30,
        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("tokens/hawk.png", iconSize, iconSize)
        onMouseClicked = {
            showScoreCards(hawkCardA, hawkCardB)
        }
    }

    private val salmon = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = elk.posX - iconDistance - (iconSize + 15), posY = 30,
        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("tokens/salmon.png", iconSize, iconSize)
        onMouseClicked = {
            showScoreCards(salmonCardA, salmonCardB)
        }
    }

    private val fox = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = elk.posX + iconDistance + iconSize + 15, posY = 30,        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("tokens/fox.png", iconSize, iconSize)

        onMouseClicked = {
            showScoreCards(foxCardA, foxCardB)
        }
    }

    private val bear = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = elk.posX + 2*iconDistance + 2*(iconSize + 15), posY = 30,
        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("tokens/bear.png", iconSize, iconSize)
        onMouseClicked = {
            showScoreCards(bearCardA, bearCardB)
        }
    }

    private val bearCardA = Label(
        width = 207, height = 394,
        posX = cardAX - 5, posY = cardY,
    ).apply {
        visual = ImageVisual("scoringCards/Scoring_Bear_A.png")
        isVisible = false
    }

    private val bearCardB = Label(
        width = 207, height = 394,
        posX = cardBX - 5, posY = cardY,

    ).apply {
        visual = ImageVisual("scoringCards/Scoring_Bear_B.png")
        isVisible = false
    }

    private val elkCardA = Label(
        width = 262, height = 504,
        posX = cardAX, posY = cardY,
    ).apply {
        visual = ImageVisual("scoringCards/Scoring_Elk_A.png")
        isVisible = false
    }

    private val elkCardB = Label(
        width = 262, height = 504,
        posX = cardBX - 5, posY = cardY,

        ).apply {
        visual = ImageVisual("scoringCards/Scoring_Elk_B.png")
        isVisible = false
    }

    private val foxCardA = Label(
        width = 183, height = 383,
        posX = cardAX, posY = cardY,
    ).apply {
        visual = ImageVisual(path = "scoringCards/Scoring_Fox_A.png")
        isVisible = false
    }

    private val foxCardB = Label(
        width = 229, height = 497,
        posX = cardBX, posY = cardY,

        ).apply {
        visual = ImageVisual("scoringCards/Scoring_Fox_B.png")
        isVisible = false
    }

    private val hawkCardA = Label(
        width = 229, height = 497,
        posX = cardAX, posY = cardY,
    ).apply {
        visual = ImageVisual("scoringCards/Scoring_Hawk_A.png")
        isVisible = false
    }

    private val hawkCardB = Label(
        width = 229, height = 497,
        posX = cardBX, posY = cardY
    ).apply {
        visual = ImageVisual("scoringCards/Scoring_Hawk_B.png")
        isVisible = false
    }

    private val salmonCardA = Label(
        width = 229, height = 497,
        posX = cardAX, posY = cardY,
    ).apply {
        visual = ImageVisual("scoringCards/Scoring_Salmon_A.png")
        isVisible = false
    }

    private val salmonCardB = Label(
        width = 229, height = 497,
        posX = cardBX, posY = cardY,

        ).apply {
        visual = ImageVisual("scoringCards/Scoring_Salmon_B.png")
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
        onMouseClicked ={
            if (!isChecked){
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
        onMouseClicked ={
            if (!isChecked){
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
        onMouseClicked ={
            if (!isChecked){
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
        onMouseClicked ={
            if (!isChecked){
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
        onMouseClicked ={
            if (!isChecked){
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
        onMouseClicked ={
            if (!isChecked){
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
        onMouseClicked ={
            if (!isChecked){
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
        onMouseClicked ={
            if (!isChecked){
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
        onMouseClicked ={
            if (!isChecked){
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
        onMouseClicked ={
            if (!isChecked){
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
        visual = Visual.EMPTY
    ).apply {
        onMouseClicked = {
            app.showMenuScene(HostOnlineScene(app,rootService))
        }
    }

    private val tabLabel = Label(
        posX = paneX + paneWidth - tabWidth + 80,
        posY = paneY + paneHeight - tabHeight,
        width = tabWidth,
        height = tabHeight,
        visual = ImageVisual("assets/FoldOutTab.png").apply {
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

    private val lobbyCodeLabel = Label(
        posX = 35,
        posY = paneHeight - 70,
        width = 500,
        height = 40,
        alignment = Alignment.TOP_LEFT,
        font = Font(24.0, family = "Canva Sans"),
        text = "Lobby Code: $lobbyCode",
    )

    private val warning = Label(
        width = paneWidth - 180,
        height = 100,
        posX = paneX + 210 + movementDistance,
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

        listOf(p1Input,p2Input,p3Input,p4Input, downButtonP1, downButtonP2, downButtonP3, shuffleButton,
            exitButton, p1Icon, p2Icon, p3Icon, p4Icon, lobbyCodeLabel).forEach { hostPanel.add(it) }

        listOf(bear,elk,hawk,salmon,fox,
            bearCardA,bearCardB,elkCardA,elkCardB,foxCardA,foxCardB,
            hawkCardA,hawkCardB,salmonCardA,salmonCardB,
            checkBoxHawkA,checkBoxHawkB, checkBoxElkA,checkBoxElkB,checkBoxBearA,checkBoxBearB,checkBoxFoxA,
            checkBoxFoxB,checkBoxSalmonA,checkBoxSalmonB, randomScoreCards
            ).forEach { sidePanel.add(it) }



        backgroundOpacity = .5
        addComponents(
            logo,
            sidePanel,
            tabLabel,
            hostPanel,
            warning,
            )
    }

    /**
     * This function moves the side panel containing the scorecard selection and images to the right and
     * the main panel to the left. The onClick action for the Tab [tabLabel] is changed to startGame
     */

    private fun expandPanel() {

        playAnimation(
            ParallelAnimation(
                MovementAnimation( // bewegt das hostPanel
                    componentView = hostPanel,
//                    fromX = hostPanel.actualPosX,
//                    toX = hostPanel.actualPosX - (hostPanel.width/2)+90,
                    byX = -movementDistance,
                    duration = 1000 // dauer
                ),
                MovementAnimation( // bewegt das sidePanel
                    componentView = sidePanel,
//                    fromX = sidePanel.actualPosX,
//                    toX = sidePanel.actualPosX + (sidePanel.width/2)-90,
                    byX = movementDistance,
                    duration = 1000 // dauer
                ),
                MovementAnimation( // bewegt das Tab
                    componentView = tabLabel,
//                    fromX = sidePanel.actualPosX,
//                    toX = sidePanel.actualPosX + (sidePanel.width/2)-90,
                    byX = movementDistance,
                    duration = 1000 // dauer
                )

            ).apply {
                onFinished = {
                    updateTab()
//                    runOnGUIThread {
//                        tabLabel.apply {
//                            visual = ImageVisual("StartGameTab.png").apply {
//                                style.borderRadius = BorderRadius(15)
//                            }
//                            onMouseClicked = {
//                                if(allScoreCardsSelected() && enoughPlayers()) {
//                                    rootService.gameService.startNewGame(getFinalPlayerList(), getFinalScoreCards())
//                                } else {
//                                    val delay = DelayAnimation(5000)
//                                    playAnimation(delay).apply {
//                                        onFinished = {
//                                            warning.isVisible = false
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
                }
            }
        )
    }

    /**
     * This function updates the [Visual] and onMouseClicked functionality of the [tabLabel].
     */
    private fun updateTab() {
        runOnGUIThread {
            tabLabel.apply {
                visual = ImageVisual("assets/StartGameTab.png").apply {
                    style.borderRadius = BorderRadius(15)
                }
                onMouseClicked = {
                    if(allScoreCardsSelected() && enoughPlayers()) {
                        rootService.networkService.startNewHostedGame()
                    } else {
                        warning.isVisible = true
                        playAnimation(
                            DelayAnimation(3000).apply {
                            onFinished = {
                                warning.isVisible = false
                            }
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * This function returns true if a [CheckBox] is checked for each animal type, false otherwise. If false, a player
     * warning becomes visible.
     */
    private fun allScoreCardsSelected(): Boolean {
        val bear = (checkBoxBearA.isChecked || checkBoxBearB.isChecked)
        val hawk = (checkBoxHawkA.isChecked || checkBoxHawkB.isChecked)
        val fox = (checkBoxFoxA.isChecked || checkBoxFoxB.isChecked)
        val salmon = (checkBoxSalmonA.isChecked || checkBoxSalmonB.isChecked)
        val elk = (checkBoxElkA.isChecked || checkBoxElkB.isChecked)
        if(!(bear && hawk && fox && salmon && elk)) {
            //warning.isVisible = true
            warning.text = "You need to select a Score Card for each animal type to play."
            return false
        } else {
            return true
        }
    }

    /**
     * This function returns true if there is a second player in the Lobby, false otherwise. If false, a player warning
     * becomes visible.
     */
    private fun enoughPlayers(): Boolean {
        if(p2Input.isVisible) {
            return true
        } else {
            //warning.isVisible = true
            warning.text = "You need at least 2 players to play."
            return false
        }
    }

    /**
     * This function returns a [List] of [Pair]s of [String] and [PlayerType] containing the name and type of all
     * occupied player slots.
     */

    private fun getFinalPlayerList(): List<Pair<String, PlayerType>> {
        val list: MutableList<Pair<String, PlayerType>> = mutableListOf()
        var type = PlayerType.HUMAN
        var name = ""
        for(i in 0..3) {
            when (i) {
                0 -> {
                    type = getPlayerType(orderOfTypes[0])
                    name = orderOfNames[0].text
                }
                1 -> {
                    type = getPlayerType(orderOfTypes[1])
                    name = orderOfNames[1].text
                }
                2 -> {
                    type = getPlayerType(orderOfTypes[2])
                    name = orderOfNames[2].text
                }
                3 -> {
                    type = getPlayerType(orderOfTypes[3])
                    name = orderOfNames[3].text
                }
            }
            if(name != "") list.add(Pair(name,type))
        }
        return list.toList()
    }

    /**
     * This function returns the [PlayerType] corresponding to the [ImageVisual] saved in the given [Label] [icon]
     */

    private fun getPlayerType(icon: Label): PlayerType {
        return when(icon.visual) {
            ImageVisual("icons/HumanIcon3.png") -> PlayerType.HUMAN
            ImageVisual("icons/EasyBotIcon3.png") -> PlayerType.EASY_BOT
            ImageVisual("icons/HardBotIcon3.png") -> PlayerType.HARD_BOT
            ImageVisual("icons/NetworkIcon.png") -> PlayerType.NETWORK
            else -> throw IllegalArgumentException("Unknown player type")
        }
    }

    /**
     * This function returns a [List] of [Boolean], one for each animal type. When true, scorecard A was selected for
     * this animal, otherwise card B. Order of animals: bear, elk, salmon, hawk, fox
     */

    private fun getScoreCards(final: Boolean): List<Boolean?> {
        val scoreCards = listOf(Pair(checkBoxBearA, checkBoxBearB), Pair(checkBoxElkA, checkBoxElkB),
            Pair(checkBoxSalmonA, checkBoxSalmonB), Pair(checkBoxHawkA, checkBoxHawkB),
            Pair(checkBoxFoxA, checkBoxFoxB))

        val res = mutableListOf<Boolean?>()

        scoreCards.forEach {
            if (final) res.add(it.first.isChecked)
            else if (it.first.isChecked) {
                res.add(true)
            }
            else if (it.second.isChecked) {
                res.add(false)
            }
            else res.add(null)
        }
        return res
    }

    /**
     * This function shuffles the [String] saved in the players' name slots and the corresponding [Visual] in the
     * players' icon. Empty/unused slots will be sorted to the bottom slots.
     */
    private fun shuffleNames() {

        //pair up the players' names and icons into a list
        val nameAndType = getNameAndTypePairs()

        //shuffle the list
        val shuffledList = nameAndType.shuffled(
        )

        //sort list so that unused/empty player slots are at the end of the list. fill the list with temporary values
        //to start and then overwrite with the correct ones
        val sortedList: MutableList<Pair<String, Visual>> = mutableListOf(Pair("", ImageVisual("icons/NetworkIcon.png")),
            Pair("", ImageVisual("icons/NetworkIcon.png")), Pair("", ImageVisual("icons/NetworkIcon.png")),
            Pair("", ImageVisual("icons/NetworkIcon.png")))
        var endOfList = 3
        var startOfList = 0

        for(i in 0..3) {
            if(shuffledList[i].first == "") {
                sortedList[endOfList] = shuffledList[i]
                endOfList -= 1
            } else {
                sortedList[startOfList] = shuffledList[i]
                startOfList += 1
            }
        }

        //update the lists orderOfNames and orderOfTypes according to the shuffle results
        for (i in 0..3) {
            orderOfNames[i].text = sortedList[i].first
            orderOfTypes[i].visual = sortedList[i].second
        }

        rootService.networkService.sendGameConfig(orderOfNames.filter {!it.text.isBlank()}.map {it.text}, getScoreCards(false))
    }

    /**
     * This function pairs up the [String] and [Visual] of all player slots and returns them in a [List]
     */
    private fun getNameAndTypePairs(): List<Pair<String, Visual>> {
        val list = mutableListOf<Pair<String, Visual>>()
        var name = p1Input
        var type = p1Icon.visual
        for(i in 0..3) {
            when(i) {
                0 -> {
                    name = p1Input
                    type = p1Icon.visual

                }
                1 -> {
                    name = p2Input
                    type = p2Icon.visual
                }
                2 -> {
                    name = p3Input
                    type = p3Icon.visual
                }
                3 -> {
                    name = p4Input
                    type = p4Icon.visual
                }
            }

            list.add(Pair(name.text, type))

        }
        return list.toList()
    }
    /**
     * This function unchecks the complimentary checkbox to the one given as a String. The checkbox cannot be given as
     * a parameter directly because of issues with recursive function calls.
     * @param checkBox a [String] of the name of the [CheckBox] that is now checked. Its complement will be unchecked.
     */

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

        rootService.networkService.sendGameConfig(orderOfNames.filter {!it.text.isBlank()}.map {it.text}, getScoreCards(false))
    }

    /**
     * This function switches the position of two [Label]s containing player names and the corresponding player type
     * icons via Movement Animations.
     * @param button [Int] number of the button that initiated this function call, determines which labels get
     * switched. Button 1 -> the first two labels/players get switched; Button 2 -> players 2 and 3 get switched...
     */

    private fun switchNames(button: Int){
        val name1 : Label
        val name2 : Label
        val typeIcon1: Label
        val typeIcon2: Label
        val pos1 : Double
        val pos2 : Double
        when(button) {
            1 -> {
                name1 = orderOfNames[0]
                name2 = orderOfNames[1]
                typeIcon1 = orderOfTypes[0]
                typeIcon2 = orderOfTypes[1]
                pos1 = name1.posY
                pos2 = name2.posY
                orderOfNames[0] = name2
                orderOfNames[1] = name1
                orderOfTypes[0] = typeIcon2
                orderOfTypes[1] = typeIcon1
            }
            2 -> {
                name1 = orderOfNames[1]
                name2 = orderOfNames[2]
                typeIcon1 = orderOfTypes[1]
                typeIcon2 = orderOfTypes[2]
                pos1 = name1.posY
                pos2 = name2.posY
                orderOfNames[1] = name2
                orderOfNames[2] = name1
                orderOfTypes[1] = typeIcon2
                orderOfTypes[2] = typeIcon1
            }
            3 -> {
                name1 = orderOfNames[2]
                name2 = orderOfNames[3]
                typeIcon1 = orderOfTypes[2]
                typeIcon2 = orderOfTypes[3]
                pos1 = name1.posY
                pos2 = name2.posY
                orderOfNames[2] = name2
                orderOfNames[3] = name1
                orderOfTypes[2] = typeIcon2
                orderOfTypes[3] = typeIcon1
            }
            else -> throw IllegalArgumentException("Invalid button for this function: $button")
        }

        rootService.networkService.sendGameConfig(orderOfNames.filter {!it.text.isBlank()}.map {it.text}, getScoreCards(false))

        playAnimation(
            ParallelAnimation(
                MovementAnimation(
                    componentView = name1,
                    toY = pos2,
                    duration = 500 // dauer
                ),
                MovementAnimation(
                    componentView = name2,
                    toY = pos1,
                    duration = 500 // dauer
                ),
                MovementAnimation(
                    componentView = typeIcon1,
                    toY = pos2,
                    duration = 500 // dauer
                ),
                MovementAnimation(
                    componentView = typeIcon2,
                    toY = pos1,
                    duration = 500 // dauer
                )
            )
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

        rootService.networkService.sendGameConfig(orderOfNames.filter {!it.text.isBlank()}.map {it.text}, getScoreCards(false))
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

    override fun refreshAfterGameConfigUpdate(playerList: List<String>, scoringCards: List<Boolean?>) {
        updatePlayers(playerList)
    }

    /**
     * Closes the MenuScene when called.
     */
    override fun refreshAfterStartGame() {
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

    private fun addNewPlayer(name: String, nameField: Label, icon: Label) {
        nameField.text = name
        icon.visual = ImageVisual("icons/NetworkIcon.png").apply {
            style.borderRadius = BorderRadius(8)
        }
        nameField.isVisible = true
        icon.isVisible = true
    }

    /**
     * This function returns true when the given [List] [players] is duplicate free, false otherwise
     */
    private fun duplicateFree(players: List<String>): Boolean {
        return players.size == players.distinct().size
    }

    /**
     * This function updates the player name slots with the entries in [players] and the player icons with the
     * corresponding icons/visuals. Unused name slots, icons and downButtons are set to be invisible.
     */
    private fun updatePlayers(players: List<String>) {
        if(players.isEmpty() || players.size > 4) {
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
                        downButtonP1.isVisible = true
                    } else {
                        p2Icon.isVisible = false
                        p2Input.isVisible = false
                        p2Input.text = ""
                        downButtonP1.isVisible = false
                    }
                }
                3 -> {
                    if (players.size > 2) {
                        p3Input.text = players[2]
                        p3Icon.visual = getVisual(players[2])
                        downButtonP2.isVisible = true

                        p3Icon.isVisible = true
                        p3Input.isVisible = true

                    } else {
                        p3Icon.isVisible = false
                        p3Input.isVisible = false
                        p3Input.text  = ""
                        downButtonP2.isVisible = false
                    }
                }
                4 -> {
                    if (players.size > 3) {
                        p4Input.text = players[3]
                        p4Icon.visual = getVisual(players[3])
                        downButtonP3.isVisible = true

                        p4Icon.isVisible = true
                        p4Input.isVisible = true
                    } else {
                        p4Icon.isVisible = false
                        p4Input.isVisible = false
                        p4Input.text = ""
                        downButtonP3.isVisible = false
                    }
                }
            }
        }
    }

    /**
     * This function returns the [ImageVisual] for [playerType] if [name] is the [playerName] and
     * the NetworkIcon otherwise
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
}