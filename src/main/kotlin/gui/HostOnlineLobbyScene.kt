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

    private val logo = Label(
        posX = 0,
        posY = 0,
        width = 1920,
        height = 1080,
        visual = ImageVisual("LobbyHintergrund.png")

    )
    private val hostPanel = Pane<UIComponent>(
        posX = 635, posY = 230,
        width = 750.0, height = 620.0
    ).apply {
        visual = ColorVisual(64, 98, 70).apply { style.borderRadius = BorderRadius(35) }

    }

    private val sidePanel = Pane<UIComponent>(
        posX = 635, posY = 230,
        width = 750.0, height = 620.0
    ).apply {
        visual = ColorVisual(160, 150, 210).apply { style.borderRadius = BorderRadius(35) }
    }



    private val p1Input = Label(
        width = 400, height = 50,
        posX = 750/2 - 200, posY = 620/2 - 100,
        text = "Player 1",
        font = Font(size = 28)

    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
    }



    private val p2Input = Label(
        width = 400, height = 50,
        posX = 750/2 - 200, posY = 620/2 - 15,
        text = " Player 2",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }

    }



    private val p3Input = Label(
        width = 400, height = 50,
        posX = 750/2 - 200, posY = 620/2 + 70,
        text = "",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
    }




    private val p4Input = Label(
        width = 400, height = 50,
        posX = 750/2 - 200, posY = 620/2 + 155,
        text = "",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }

    }

    private val salmon = Label(
        width = 60, height = 60,
        posX = 90, posY = 45,
        font = Font(size = 40)
    ).apply {
        visual = ImageVisual("salmon.png")
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


    private val hawk = Label(
        width = 60, height = 60,
        posX = 200, posY = 45,
        font = Font(size = 40)
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
            hawkCardB.resize(183, 397)
        }
    }


    private val fox = Label(
        width = 60, height = 60,
        posX = 310, posY = 45,
        font = Font(size = 40)
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
            foxCardB.resize(183, 397)
        }
    }



    private val elk = Label(
        width = 60, height = 60,
        posX = 420, posY = 45,
        font = Font(size = 40)
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



    private val bear = Label(
        width = 60, height = 60,
        posX = 530, posY = 45,
        font = Font(size = 40)
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
        posX = 90,
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
        posX = 90,
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
        posX = 200,
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
        posX = 200,
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
        posX = 530,
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
        posX = 530,
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
        posX = 310,
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
        posX = 310,
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
        posX = 420,
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
        posX = 420,
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
        posX = 1327, posY = 720,
        width = 158.0, height = 130.0,
        text = "",
        font = Font(size = 22, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(160, 150, 210).apply { style.borderRadius = BorderRadius(15) }
    }

    private val arrowButton = Label(
        posX = 1380, posY = 750,
        width = 90.0, height = 90.0,
        text = "→",
        font = Font(size = 40, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(160, 150, 210).apply { style.borderRadius = BorderRadius(8) }
        onMouseClicked = {
            expandPanel()

        }
    }


    private val startLabel = Label(
        posX = 700 -8, posY = 620 - 130,
        width = 142.0, height = 130.0,
        text = "",
        font = Font(size = 22, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(160, 150, 210).apply { style.borderRadius = BorderRadius(15) }
        this.isVisible = false
    }


    private val startButton = Label(
        posX = 750 -8, posY = 620 - 100,
        width = 80.0, height = 50.0,
        text = "▶",
        font = Font(size = 40, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(160, 150, 210).apply { style.borderRadius = BorderRadius(8) }
        this.isVisible = false
    }


    private fun expandPanel() {

        playAnimation(
            ParallelAnimation(
                MovementAnimation( // bewegt das sidePanel
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

                ).apply {
                onFinished = {
                    runOnGUIThread {
                        arrowButton.isVisible = false
                        startButton.isVisible = true
                        arrowLabel.isVisible = false
                        startLabel.isVisible = true

                    }
                }
            }
        )
    }


    init {

        listOf(p1Input,p2Input,p3Input,p4Input,
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
            arrowLabel,
            arrowButton,
            sidePanel,
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
}