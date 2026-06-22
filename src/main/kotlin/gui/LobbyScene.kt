package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.animation.*
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.components.uicomponents.UIComponent
import tools.aqua.bgw.core.BoardGameApplication.Companion.runOnGUIThread
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual

class LobbyScene(private val app: SopraApplication, private val rootService: RootService) : MenuScene(1920, 1080), Refreshable  {



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
        visual = ColorVisual(64, 98, 70).apply { style.borderRadius = BorderRadius(8) }
    }

    private val sidePanel = Pane<UIComponent>(
        posX = 635, posY = 230,
        width = 750.0, height = 620.0
    ).apply {
        visual = ColorVisual(160, 150, 210).apply { style.borderRadius = BorderRadius(8) }
    }


    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val p1Input: TextField = TextField(
        width = 400, height = 50,
        posX = 750/2 - 200, posY = 450,
        prompt = "Player 1",
        font = Font(size = 28)

    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
        onTextChanged ={
            //startButton.isDisabled = !(this.text.trim().isNotEmpty() && p2Input.text.trim().isNotEmpty())
            if (this.text.isBlank()){
                this.prompt = "\uD83D\uDC64 Player 1"
            }
        }
    }

    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val p2Input: TextField = TextField(
        width = 400, height = 50,
        posX = 750/2 - 200, posY = 530,
        prompt = "\uD83D\uDC64 Player 2",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
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
        width = 400, height = 50,
        posX = 750/2 - 200, posY = 610,
        prompt = "+",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }
        onTextChanged ={
            //wenn nichts in den ersten beiden Feldern drin steht, dann funktioniert der Start Button nicht
            //startButton.isDisabled = !(p1Input.text.trim().isNotEmpty() && p2Input.text.trim().isNotEmpty())
            if (this.text.isBlank()){
                this.prompt = "+"
            }
        }
    }

    // type inference fails here, so explicit  ": TextField" is required
    // see https://discuss.kotlinlang.org/t/unexpected-type-checking-recursive-problem/6203/14
    private val p4Input: TextField = TextField(
        width = 400, height = 50,
        posX = 750/2 - 200, posY = 690,
        prompt = "+",
        font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(8) }

        onTextChanged ={
            //startButton.isDisabled = !(p1Input.text.trim().isNotEmpty() && p2Input.text.trim().isNotEmpty())
            if (this.text.isBlank()){
                this.prompt = "+"
            }
        }
    }

    private val barOne = Label(
        width = 650, height = 4,
        posX = 55, posY = 210).apply { visual= ColorVisual(0,0,0) }

    private val barTwo = Label(
        width = 3, height = 45,
        posX = 216, posY = 160).apply { visual= ColorVisual(0,0,0) }

    private val barThree = Label(
        width = 3, height = 45,
        posX = 378, posY = 160).apply { visual= ColorVisual(0,0,0) }

    private val barFour = Label(
        width = 3, height = 45,
        posX = 540, posY = 160).apply { visual= ColorVisual(0,0,0) }


    private val easyBot = Label(
        width = 150, height = 45,
        posX = 57, posY = 160,
        ).apply { visual = ImageVisual("EasyBot.png") }




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


    private val shuffleButton = Button(
        width = 50, height = 50,
        posX = 750/2 + 250, posY = 620/2-260,
        text = "⤮",font = Font(size = 28)
    ).apply {
        visual = ColorVisual(192, 192, 192).apply { style.borderRadius = BorderRadius(360) }
        onMouseClicked = {
            shuffleNames()
        }
    }




//    private val toggleButton = Button(
//        posX = 1385, posY = 850 - 55,
//        width = 80.0, height = 60.0,
//        text = "→",
//        font = Font(size = 22, color = Color.WHITE)
//    ).apply {
//        visual = ColorVisual(Color(140, 130, 200))
//
//        onMouseClicked = {
//            expandPanel()
//        }
//    }


    private val toggleButton = Button(
        posX = 1377, posY = 770,
        width = 80.0, height = 80.0,
        text = "→",
        font = Font(size = 22, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(160, 150, 210).apply { style.borderRadius = BorderRadius(8) }
        onMouseClicked = {
            expandPanel()

        }
    }

    private val startButton = Button(
        posX = 750 -8, posY = 620 - 80,
        width = 80.0, height = 80.0,
        text = "▶",
        font = Font(size = 22, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(160, 150, 210).apply { style.borderRadius = BorderRadius(8) }
        this.isVisible = false
    }


//    private fun expandPanel() {
//        // Panel erst unsichtbar machen, damit es nicht kurz irgendwo aufbitzt
//        sidePanel.opacity = 0.0
//
//        playAnimation(
//            SequentialAnimation(
//                // 1. Einschieben von rechts
//                MovementAnimation( // bewegt das sidePanel
//                    componentView = sidePanel,
//                    fromX = PANEL_HIDDEN_X, // von wo nach wo
//                    toX = PANEL_VISIBLE_X,
//                    duration = 350 // dauer
//                ),
//                // 2. Einblenden (gleichzeitig via parallele Nutzung)
//                FadeAnimation( //blendet das sidePanel ein
//                    componentView = sidePanel,
//                    fromOpacity = 0.0,
//                    toOpacity = 0.95,
//                    duration = 200
//                )
//            ).apply {
//                onFinished = {
//                    runOnGUIThread {
//                        sidePanel.posX = PANEL_VISIBLE_X //Position dauerhaft fixieren
//                        sidePanel.opacity = 0.95
//                        toggleButton.text = "▶"
//                        toggleButton.opacity = 0.0
//                        isPanelExpanded = true
//                    }
//                }
//            }
//        )
//    }

    private fun expandPanel() {

        playAnimation(
            ParallelAnimation(
                 MovementAnimation( // bewegt das sidePanel
                    componentView = hostPanel,
                    fromX = hostPanel.actualPosX,
                    toX = hostPanel.actualPosX - (hostPanel.width/2),
                    duration = 500 // dauer
                ),
                MovementAnimation( // bewegt das sidePanel
                    componentView = sidePanel,
                    fromX = sidePanel.actualPosX,
                    toX = sidePanel.actualPosX + (sidePanel.width/2),
                    duration = 500 // dauer
                ),

            ).apply {
                onFinished = {
                    runOnGUIThread {
                        toggleButton.isVisible = false
                        startButton.isVisible = true

                    }
                }
            }
        )
    }


    init {

        listOf(p1Input,p2Input,p3Input,p4Input,
            barOne,barTwo,barThree,barFour,easyBot,
            shuffleButton,exitButton,).forEach { hostPanel.add(it) }

        sidePanel.add(startButton)
        backgroundOpacity = .5
        addComponents(
            logo,
            toggleButton,
            sidePanel,
            hostPanel,

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

}

