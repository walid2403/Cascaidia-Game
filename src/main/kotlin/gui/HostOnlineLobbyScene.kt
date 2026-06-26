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

class HostOnlineLobbyScene(private val app: SopraApplication, private val rootService: RootService) : MenuScene(1920, 1080), Refreshable  {

    private val sceneWidth = 1920
    private val sceneHeight = 1080
    private val paneWidth = 750
    private val paneHeight = 620
    private val tabHeight = 130
    private val tabWidth = 158
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
    private val buttonWidth = 50
    private val buttonHeight = 50
    private val downUpButtonDistance = 20


    private val logo = Label(
        posX = 0,
        posY = 0,
        width = 1920,
        height = 1080,
        visual = ImageVisual("ScoreSceneBackground.png")

    )
    private val hostPanel = Pane<UIComponent>(
        posX = paneX, posY = paneY,
        width = paneWidth, height = paneHeight,
    ).apply {
        visual = ColorVisual(64, 98, 70).apply { style.borderRadius = BorderRadius(35) }

    }

    private val sidePanel = Pane<UIComponent>(
        posX = paneX, posY = paneY,
        width = paneWidth, height = paneHeight,
    ).apply {
        visual = ColorVisual(153, 172, 255).apply { style.borderRadius = BorderRadius(35) }
    }

    private val hostPaneBorder = Label(
        posX = paneX - borderThickness,
        posY = paneY - borderThickness,
        width = paneWidth + borderThickness*2,
        height = paneHeight + borderThickness*2,
        visual = ColorVisual(255, 255, 255).apply{
            style.borderRadius = BorderRadius(35)
        }
    )

    private val sidePaneBorder = Label(
        posX = paneX - borderThickness,
        posY = paneY - borderThickness,
        width = paneWidth + borderThickness*2,
        height = paneHeight + borderThickness*2,
        visual = ColorVisual(255, 255, 255).apply{
            style.borderRadius = BorderRadius(35)
        }
    )

    private val tabBorder = Label(
        posX = paneX + paneWidth - tabWidth + 100 - borderThickness,
        posY = paneY + paneHeight - tabHeight - borderThickness,
        width = tabWidth + 2*borderThickness,
        height = tabHeight + 2*borderThickness,
        visual = ColorVisual(255, 255, 255).apply {
            style.borderRadius = BorderRadius(15.0)
        }
    )

    private val tab2Border = Label(
        posX = tabBorder.posX + movementDistance,
        posY = paneY + paneHeight - tabHeight - borderThickness,
        width = tabWidth + 2*borderThickness,
        height = tabHeight + 2*borderThickness,
        visual = ColorVisual(255, 255, 255).apply {
            style.borderRadius = BorderRadius(15.0)
        }
    ).apply {
        isVisible = false
    }

    private val downButtonP1 = Button(
        width = buttonWidth, height = buttonHeight,
        posX = (paneWidth+nameWidth)/2 + downUpButtonDistance ,posY = nameY,
        text = "↓",font = Font(size = 28)
    ).apply{
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
        onMouseClicked ={
                if((p1Input.posY.toInt() == nameY) &&
                    (p2Input.posY.toInt() == nameY + nameHeight + nameDistance)) {
                    buttonsDown(p1Input,p2Input)
                }
        }
    }

    private val downButtonP2 = Button(
        width = buttonWidth, height = buttonHeight,
        posX = (paneWidth+nameWidth)/2 + downUpButtonDistance ,posY = nameY + nameHeight + nameDistance,
        text = "↓",font = Font(size = 28)
    ).apply{
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }

    }

    private val downButtonP3 = Button(
        width = buttonWidth, height = buttonHeight,
        posX = (paneWidth+nameWidth)/2 + downUpButtonDistance ,posY = nameY + 2*nameHeight + 2*nameDistance,
        text = "↓",font = Font(size = 28)
    ).apply{
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }

    }

//    private val downButtonP4 = Button(
//        width = buttonWidth, height = buttonHeight,
//        posX = (paneWidth+nameWidth)/2 + downUpButtonDistance ,posY = nameY + 3*nameHeight + 3*nameDistance,
//        text = "↓",font = Font(size = 28)
//    ).apply{
//        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
//
//    }


//    private val upButtonP1 = Button(
//        width = buttonWidth, height = buttonHeight,
//        posX = (paneWidth+nameWidth)/2 + 2*downUpButtonDistance + buttonWidth  ,posY = nameY,
//        text = "↑",font = Font(size = 28)
//    ).apply{
//        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
//
//    }

    private val upButtonP2 = Button(
        width = buttonWidth, height = buttonHeight,
        posX = (paneWidth+nameWidth)/2 +  2*downUpButtonDistance + buttonWidth ,
        posY = nameY + nameHeight + nameDistance,
        text = "↑",font = Font(size = 28)
    ).apply{
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }

    }

    private val upButtonP3 = Button(
        width = buttonWidth, height = buttonHeight,
        posX = (paneWidth+nameWidth)/2 + 2*downUpButtonDistance + buttonWidth ,
        posY = nameY + 2*nameHeight + 2*nameDistance,
        text = "↑",font = Font(size = 28)
    ).apply{
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }

    }

    private val upButtonP4 = Button(
        width = buttonWidth, height = buttonHeight,
        posX = (paneWidth+nameWidth)/2 +  2*downUpButtonDistance + buttonWidth  ,
        posY = nameY + 3*nameHeight + 3*nameDistance,
        text = "↑",font = Font(size = 28)
    ).apply{
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }

    }



    private val p1Input = Label(
        width = nameWidth, height = nameHeight,
        posX = (paneWidth-nameWidth)/2, posY = nameY,
        text = "Player 1",
        font = Font(size = 28)

    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
    }



    private val p2Input = Label(
        width = nameWidth, height = nameHeight,
        posX = (paneWidth - nameWidth)/2, posY = nameY + nameHeight + nameDistance,
        text = " Player 2",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }

    }



    private val p3Input = Label(
        width = nameWidth, height = nameHeight,
        posX = (paneWidth - nameWidth)/2, posY = nameY + 2*nameHeight + 2*nameDistance,
        text = "",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
    }




    private val p4Input = Label(
        width = nameWidth, height = nameHeight,
        posX = (paneWidth - nameWidth)/2, posY = nameY + 3*nameHeight + 3*nameDistance,
        text = "",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }

    }

    private val elk = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = (paneWidth)/2 + 90 - (iconSize + 15)/2, posY = 30,
        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("elk.png", iconSize, iconSize)
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

    private val hawk = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = elk.posX - 2*iconDistance - 2*(iconSize + 15), posY = 30,
        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("hawk.png", iconSize, iconSize)
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

    private val fox = Label(
        width = iconSize + 15, height = iconSize + 15,
        posX = elk.posX + iconDistance + iconSize + 15, posY = 30,        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("fox.png", iconSize, iconSize)

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
        posX =  230, posY = 200,
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
        width = 183, height = 383,
        posX =  230, posY = 200,
    ).apply {
        visual = ImageVisual(path = "Scoring_Fox_A.png")
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
        posX =  230, posY = 200,
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
        posX =  230, posY = 200,
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

    private val human = Label(
        width = 200, height = 60,
        posX = 500, posY = 45,
        font = Font(size = 40)
    ).apply { visual = ImageVisual("Auswahl.png") }

    private val player = Label(
        width = 300, height = 100,
        posX = 750.0/2 - 150, posY = 30,
        text = "Host",
        font = Font(
            size = 50,
            color = Color(0xFFFFFF),
            family = "Canva Sans",
            fontWeight = Font.FontWeight.BOLD)
    )

    private val exitButton = Button(
        width = 100, height = 60,
        posX = 750/2 -330, posY = 620/2-300,
        text = "←",font = Font(size = 70,color = Color(0xFFFFFF))
    ).apply {
        visual = ColorVisual(64, 98, 70).apply { style.borderRadius = BorderRadius(8) }
        onMouseClicked = {
            app.showMenuScene(MainMenuScene(app,rootService))
        }
    }

//    "◀──",



    private val arrowLabel = Label(
        posX = paneX + paneWidth - tabWidth + 100, posY = paneY + paneHeight - tabHeight,
        width = tabWidth, height = tabHeight,
        text = "",
        font = Font(size = 22, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(153, 172, 255).apply { style.borderRadius = BorderRadius(15) }
    }

    private val arrowButton = Label(
        posX = paneX + paneWidth + 5, posY = paneY + paneHeight -110,
        width = 90.0, height = 90.0,
        text = "→",
        font = Font(size = 40, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(153, 172, 255).apply { style.borderRadius = BorderRadius(8) }
        onMouseClicked = {
            expandPanel()

        }
    }


    private val startLabel = Label(
        posX = paneWidth - 42, posY = paneHeight - tabHeight,
        width = 142.0, height = 130.0,
        text = "",
        font = Font(size = 22, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(153, 172, 255).apply { style.borderRadius = BorderRadius(15) }
        this.isVisible = false
    }


    private val startButton = Label(
        posX = 750 -8, posY = 620 - 100,
        width = 80.0, height = 50.0,
        text = "▶",
        font = Font(size = 40, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(153, 172, 255).apply { style.borderRadius = BorderRadius(8) }
        this.isVisible = false
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
                MovementAnimation( // bewegt die Umrandung des hostPanel
                    componentView = hostPaneBorder,
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
                MovementAnimation( // bewegt die Umrandung des sidePanel
                    componentView = sidePaneBorder,
                    fromX = sidePanel.actualPosX,
                    toX = sidePanel.actualPosX + (sidePanel.width/2)-90,
                    duration = 1000 // dauer
                ),
//                MovementAnimation( // bewegt die Umrandung des Tabs an der Seite
//                    componentView = tabBorder,
//                    byX = movementDistance,
//                    duration = 1000 // dauer
//                ),

                ).apply {
                onFinished = {
                    runOnGUIThread {
                        arrowButton.isVisible = false
                        startButton.isVisible = true
                        arrowLabel.isVisible = false
                        startLabel.isVisible = true
                        tabBorder.isVisible = false
                        tab2Border.isVisible = true

                    }
                }
            }
        )
    }


    init {

        listOf(p1Input,p2Input,p3Input,p4Input, downButtonP1, downButtonP2, downButtonP3,
             upButtonP2, upButtonP3, upButtonP4,
            human,player, exitButton).forEach { hostPanel.add(it) }

        listOf(startLabel,startButton, bear,elk,hawk,salmon,fox,
            bearCardA,bearCardB,elkCardA,elkCardB,foxCardA,foxCardB,
            hawkCardA,hawkCardB,salmonCardA,salmonCardB,
            checkBoxHawkA,checkBoxHawkB, checkBoxElkA,checkBoxElkB,checkBoxBearA,checkBoxBearB,checkBoxFoxA,
            checkBoxFoxB,checkBoxSalmonA,checkBoxSalmonB
            ).forEach { sidePanel.add(it) }



        backgroundOpacity = .5
        addComponents(
            logo,
            tabBorder,
            tab2Border,
            sidePaneBorder,
            arrowLabel,
            arrowButton,
            sidePanel,
            hostPaneBorder,
            hostPanel,
            )
    }


    private fun names() : List<String> {
        return listOf(p1Input.text.trim(),p2Input.text.trim(),p3Input.text.trim(),p4Input.text.trim())
    }

//    private fun shuffleNames() {
//        // ich filter die Liste nach allem die nicht leer sind und dann shuffle ich diese und packe sie in eine
//        // Liste
//        val player = names().filter { it != "" }.shuffled().toMutableList()
//        // fügt bei allem leeren "" hinzu
//        repeat(4-player.size){
//            player.add("")
//        }
//        // wird wieder ins Feld geschieben
//        p1Input.text = player[0]
//        p2Input.text = player[1]
//        p3Input.text = player[2]
//        p4Input.text = player[3]
//
//    }

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


    private fun buttonsDown (label1 : Label, label2 : Label){
        val l1 = label1.posY
        val l2 = label2.posY

        playAnimation(
            ParallelAnimation(
                MovementAnimation(
                    componentView = label1,
                    fromY = l1,
                    toY = l2,
                    duration = 500 // dauer
                ),
                MovementAnimation(
                    componentView = label2,
                    fromY = l2,
                    toY = l1,
                    duration = 500 // dauer
                )
            )
        )
    }
}