package gui

import service.Refreshable
import service.RootService
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

class HostOnlineLobbyScene(private val app: SopraApplication, private val rootService: RootService) : MenuScene(1920, 1080), Refreshable  {

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
        visual = ImageVisual("GameConfigMenuBackground.png")
    )
    private val hostPanel = Pane<UIComponent>(
        posX = paneX, posY = paneY,
        width = paneWidth, height = paneHeight,
    ).apply {
        visual = ImageVisual("HostMenuBackground.png").apply {
            style.borderRadius = BorderRadius(35)
        }
    }

    private val sidePanel = Pane<UIComponent>(
        posX = paneX, posY = paneY,
        width = paneWidth, height = paneHeight,
    ).apply {
        visual = ImageVisual("ScoreCardPaneBackground.png").apply {
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
        text = "Player 1",
        font = Font(size = 28)

    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
    }

    private val p1Icon = Label(
        height = nameHeight,
        width = nameHeight,
        posX = p1Input.posX - nameHeight - 20,
        posY = p1Input.posY,
        visual = ImageVisual("HumanIcon3.png").apply {
            style.borderRadius = BorderRadius(8)
        }
    )

    private val p2Input = Label(
        width = nameWidth, height = nameHeight,
        posX = (paneWidth - nameWidth)/2, posY = nameY + nameHeight + nameDistance,
        text = " Player 2",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }

    }

    private val p2Icon = Label(
        height = nameHeight,
        width = nameHeight,
        posX = p2Input.posX - nameHeight - 20,
        posY = p2Input.posY,
        visual = ImageVisual("EasyBotIcon3.png").apply {
            style.borderRadius = BorderRadius(8)
        }
    )

    private val p3Input = Label(
        width = nameWidth, height = nameHeight,
        posX = (paneWidth - nameWidth)/2, posY = nameY + 2*nameHeight + 2*nameDistance,
        text = "Player 3",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
    }

    private val p3Icon = Label(
        height = nameHeight,
        width = nameHeight,
        posX = p3Input.posX - nameHeight - 20,
        posY = p3Input.posY,
        visual = ImageVisual("HardBotIcon3.png").apply {
            style.borderRadius = BorderRadius(8)
        }
    )


    private val p4Input = Label(
        width = nameWidth, height = nameHeight,
        posX = (paneWidth - nameWidth)/2, posY = nameY + 3*nameHeight + 3*nameDistance,
        text = "Player 4",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(204, 212, 209).apply { style.borderRadius = BorderRadius(8) }
    }

    private val p4Icon = Label(
        height = nameHeight,
        width = nameHeight,
        posX = p4Input.posX - nameHeight - 20,
        posY = p4Input.posY
    ).apply {
        visual = ImageVisual("NetworkIcon.png").apply {
            style.borderRadius = BorderRadius(8)
        }
    }

//    private val waitingToStart = Label(
//        height = 60,
//        width = nameWidth + nameHeight + 10,
//        posX = p4Icon.posX,
//        posY = p4Icon.posY + nameHeight + 20,
//        text = "Waiting "
//    )

    private val orderOfNames = mutableListOf(p1Input, p2Input, p3Input, p4Input)

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
        width = 183, height = 383,
        posX = cardAX, posY = cardY,
    ).apply {
        visual = ImageVisual(path = "Scoring_Fox_A.png")
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
            app.showMenuScene(MainMenuScene(app,rootService))
        }
    }

//    "◀──",

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
        }
    }


    init {

        listOf(p1Input,p2Input,p3Input,p4Input, downButtonP1, downButtonP2, downButtonP3, shuffleButton,
//             upButtonP2, upButtonP3, upButtonP4,
            exitButton, p1Icon, p2Icon, p3Icon, p4Icon).forEach { hostPanel.add(it) }

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
            )
    }

    private fun expandPanel() {

        playAnimation(
            ParallelAnimation(
                MovementAnimation( // bewegt das hostPanel
                    componentView = hostPanel,
                    fromX = hostPanel.actualPosX,
                    toX = hostPanel.actualPosX - (hostPanel.width/2)+90,
                    duration = 1000 // dauer
                ),
                MovementAnimation( // bewegt das sidePanel
                    componentView = sidePanel,
                    fromX = sidePanel.actualPosX,
                    toX = sidePanel.actualPosX + (sidePanel.width/2)-90,
                    duration = 1000 // dauer
                ),
                MovementAnimation( // bewegt das Tab
                    componentView = tabLabel,
                    fromX = sidePanel.actualPosX,
                    toX = sidePanel.actualPosX + (sidePanel.width/2)-90,
                    duration = 1000 // dauer
                )

                ).apply {
                onFinished = {
                    runOnGUIThread {
                        //arrowButton.isVisible = false
                        tabLabel.apply {
                            visual = ImageVisual("StartGameTab.png").apply {
                                style.borderRadius = BorderRadius(15)
                            }
                            onMouseClicked = {
                                TODO("startGame muss aufgerufen werden")
                            }
                        }
                    }
                }
            }
        )
    }

    private fun names() : List<String> {
        return listOf(p1Input.text.trim(),p2Input.text.trim(),p3Input.text.trim(),p4Input.text.trim())
    }

    private fun shuffleNames() {
        // ich filter die Liste nach allem die nicht leer sind und dann shuffle ich diese und packe sie in eine
        // Liste
        val player = names().filter { it != "" }.shuffled().toMutableList()
        // fügt bei allem leeren "" hinzu
        repeat(4-player.size){
            player.add("")
        }
        // wird wieder ins Feld geschieben
        p1Input.text = player[0]
        p2Input.text = player[1]
        p3Input.text = player[2]
        p4Input.text = player[3]

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

    private fun switchNames(button: Int){
        val name1 : Label
        val name2 : Label
        val pos1 : Double
        val pos2 : Double
        when(button) {
            1 -> {
                name1 = orderOfNames[0]
                name2 = orderOfNames[1]
                pos1 = name1.posY
                pos2 = name2.posY
                orderOfNames[0] = name2
                orderOfNames[1] = name1
            }
            2 -> {
                name1 = orderOfNames[1]
                name2 = orderOfNames[2]
                pos1 = name1.posY
                pos2 = name2.posY
                orderOfNames[1] = name2
                orderOfNames[2] = name1
            }
            3 -> {
                name1 = orderOfNames[2]
                name2 = orderOfNames[3]
                pos1 = name1.posY
                pos2 = name2.posY
                orderOfNames[2] = name2
                orderOfNames[3] = name1
            }
            else -> throw IllegalArgumentException("Invalid button for this function: $button")
        }

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
                )
            )
        )
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

    fun resizeScoreCards() {
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
}