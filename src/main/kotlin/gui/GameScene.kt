package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.animation.DelayAnimation
import tools.aqua.bgw.animation.RotationAnimation
import tools.aqua.bgw.animation.ScaleAnimation
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.components.container.HexagonGrid
import tools.aqua.bgw.components.gamecomponentviews.HexagonView
import tools.aqua.bgw.components.layoutviews.CameraPane
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.HexOrientation
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual

/**
 * Die Klasse GameScene ist die Hauptszene des Cascadia Spiels
 * @param rootService ein Objekt des Typs [RootService], hier wird der Rootservice übergeben,
 * um auf die Serviceschicht zugreifen zu können
 * @property rootService ein Objekt des Typs [RootService], hier wird der Rootservice übergeben,
 *  * um auf die Serviceschicht zugreifen zu können
 *  @property selectAnimal ein Objekt des Typs [Int], Index der aktuellen Tierauswahl (0 = keins ausgewählt)
 *  @property selectTile ein Objekt des Typs [Int], Index der aktuellen Tileauswahl (0 = keins ausgewählt)
 *  @property customChoiceActive ein Objekt des Typs [Boolean], ob Custom Auswahl in dem Zug aktiv ist
 *  @property changeWildlifeActive ein Objekt des Typs [Boolean], ob Change Wildlife gerade genutzt wird
 *  @property changeAnimalsArray ein Objekt des Typs [booleanArrayOf], speichert für jedes Tier, ob es getauscht wird
 *  @property player ein Objekt des Typs [Int], speichert den aktullen Spieler Index (0 = Rundenanfang)
 */
class GameScene(private val rootService: RootService) : BoardGameScene(1920, 1080), Refreshable {

    private var selectAnimal = 0
    private var selectTile = 0
    private var customChoiceActive = false
    private var changeWildlifeActive = false
    private var changeAnimalsArray = booleanArrayOf(false,false,false,false)
    private var player = 0

    private var allButtonsAllowed = true


    //Hintergrundbild
    private val logo = Label(posX = 0,posY = 0,width = 1920,height = 1080,visual = ImageVisual("CascadiaHintergrund.png"))

    //Graue Box um Auswahl
    private val grayBox = Label(width = 950, height = 260, posX = 485, posY = 0).apply {
        visual= ColorVisual(170,170,170, 170).apply { style.borderRadius = BorderRadius(20) }
    }

    //Buttons in grauer Box
    private val customChoice = Button(width = 200, height = 60, posX = 550, posY = 25, text = "Custom Choice",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0, 0, 0).apply {
            style.borderRadius = BorderRadius(10)
        }
        //Beim ersten mal anlicken wird customChoiceActive auf true gesetzt und die Button Farbe ändert sich
        onMouseClicked = {
            if(!customChoiceActive) {
                this.visual = ColorVisual(256, 181, 0).apply {
                    style.borderRadius = BorderRadius(10)
                }
                this.font = Font(size = 16, color = Color(0, 0, 0))
                customChoiceActive = true
            }
        }
    }

    private val changeWildlife = Button(width = 200, height = 60, posX = 550, posY = 100, text = "Change Wildlife",
        font = Font(size = 16, color = Color(255, 255, 255))).apply {
        visual = ColorVisual(0, 0, 0).apply {
            style.borderRadius = BorderRadius(10)
        }
        onMouseClicked = {
            //Wenn es gerade aktiviert wird
            if(!changeWildlifeActive) {
                //Visuelle Änderung am Button
                this.visual = ColorVisual(256, 181, 0).apply {
                    style.borderRadius = BorderRadius(10)
                }
                this.font = Font(size = 16, color = Color(0, 0, 0))
                this.text = "Remove selection"

                //Alle Tiles-OnClicks und Rotate-Buttons deaktivieren und alle runterskalieren und Auswahl zurücksetzten
                selectionTileClick(false)
                scaleDownOtherTiles(0)
                scaleDownOtherAnimals(0)
                deactivateRotateButtons()
                selectTile = 0
                selectAnimal = 0
            }
            //Wenn die Tiere ausgetauscht werden sollen
            else {
                //Visuelle Änderung am Button
                this.visual = ColorVisual(0, 0, 0).apply {
                    style.borderRadius = BorderRadius(10)
                }
                this.font = Font(size = 16, color = Color(255, 255, 255))
                this.text = "Change Wildlife"

                //Alle Tiere wieder runterskalieren und Tiles-OnClick wieder aktivieren und Tierauswahl entfernen
                selectionTileClick(true)
                scaleDownOtherAnimals(0)
                removeChosenWildlife()
            }
            changeWildlifeActive = !changeWildlifeActive
        }
    }

    private val clearOverpopulation = Button(width = 200, height = 60, posX = 550,
        posY = 175, text = "Clear Overpopulation",
        font = Font(size = 16, color = Color(255, 255, 255, 127))).apply {
        visual = ColorVisual(0, 0, 0, 127).apply {
            style.borderRadius = BorderRadius(10)
        }
        this.isDisabled = true
        onMouseClicked = {
            //Button kann nur angeklickt werden, wenn 3 gleiche existieren
            //Nach onClick werden die Tiere entfernt und der Button wieder durchsichtig
            clearOverPopulation()
            this.visual = ColorVisual(0, 0, 0, 127).apply {
                style.borderRadius = BorderRadius(10)
            }
            this.font = Font(size = 16, color = Color(255, 255, 255, 127))
        }
    }

    //Tannenzapfen Symbole an Buttons
    private val pineCone1 = Label(posX = 525,posY = 35,width = 40,height = 40,visual = ImageVisual("pinecone.png"))
    private val pineCone2 = Label(posX = 525,posY = 110,width = 40,height = 40,visual = ImageVisual("pinecone.png"))

    //Auswahl Habitate
    private val tileChoice1 = HexagonView(posX = 820, posY = 30, size = 60, visual = ColorVisual(170,170,170)).apply {
        onMouseClicked = { chooseTile(1, this) }
    }
    private val tileChoice2 = HexagonView(posX = 974, posY = 30, size = 60, visual = ColorVisual(170,170,170)).apply {
        onMouseClicked = { chooseTile(2, this) }
    }
    private val tileChoice3 = HexagonView(posX = 1128, posY = 30, size = 60, visual = ColorVisual(170,170,170)).apply {
        onMouseClicked = { chooseTile(3, this) }
    }
    private val tileChoice4 = HexagonView(posX = 1282, posY = 30, size = 60, visual = ColorVisual(170,170,170)).apply {
        onMouseClicked = { chooseTile(4, this) }
    }

    //Auswahl Tiere
    private val animalChoice1 = Label(width = 60, height = 60, posX = 842, posY = 175).apply {
        visual= ColorVisual(170,170,170)
        onMouseClicked = { chooseAnimal(1, this) }
    }
    private val animalChoice2 = Label(width = 60, height = 60, posX = 996, posY = 175).apply {
        visual= ColorVisual(170,170,170)
        onMouseClicked = { chooseAnimal(2, this) }
    }
    private val animalChoice3 = Label(width = 60, height = 60, posX = 1150, posY = 175).apply {
        visual= ColorVisual(170,170,170)
        onMouseClicked = { chooseAnimal(3, this) }
    }
    private val animalChoice4 = Label(width = 60, height = 60, posX = 1304, posY = 175).apply {
        visual= ColorVisual(170,170,170)
        onMouseClicked = { chooseAnimal(4, this) }
    }

    //Rotations Knöpfe oben
    private val rotateOneCW = Button(width = 40, height = 40, posX = 782, posY = 155, text = "<",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(true, tileChoice1) }
    }
    private val rotateOneCCW = Button(width = 40, height = 40, posX = 922, posY = 155, text = ">",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(false, tileChoice1) }
    }
    private val rotateTwoCW = Button(width = 40, height = 40, posX = 936, posY = 155, text = "<",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(true, tileChoice2) }
    }
    private val rotateTwoCCW = Button(width = 40, height = 40, posX = 1076, posY = 155, text = ">",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(false, tileChoice2) }
    }
    private val rotateThreeCW = Button(width = 40, height = 40, posX = 1090, posY = 155, text = "<",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(true, tileChoice3) }
    }
    private val rotateThreeCCW = Button(width = 40, height = 40, posX = 1230, posY = 155, text = ">",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(false, tileChoice3) }
    }
    private val rotateFourCW = Button(width = 40, height = 40, posX = 1244, posY = 155, text = "<",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(true, tileChoice4) }
    }
    private val rotateFourCCW = Button(width = 40, height = 40, posX = 1384, posY = 155, text = ">",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(false, tileChoice4) }
    }

    //Bereiche der Spieler
    private val playerOneArea = HexagonGrid<HexagonView>(
        width = 30, height = 30, posX = 1140, posY = 404,
        coordinateSystem = HexagonGrid.CoordinateSystem.AXIAL,
        orientation = HexOrientation.POINTY_TOP
    ).apply {
        //visual = ColorVisual(170,170, 170, 127).apply { style.borderRadius = BorderRadius(10) }
    }
    private val playerTwoArea = HexagonGrid<HexagonView>(
        width = 30, height = 30, posX = 1550, posY = 729,
        coordinateSystem = HexagonGrid.CoordinateSystem.AXIAL,
        orientation = HexOrientation.POINTY_TOP
    ).apply {
        //visual = ColorVisual(170,170, 170, 127).apply { style.borderRadius = BorderRadius(10) }
    }
    private val playerThreeArea = HexagonGrid<HexagonView>(
        width = 30, height = 30, posX = 1000, posY = 854,
        coordinateSystem = HexagonGrid.CoordinateSystem.AXIAL,
        orientation = HexOrientation.POINTY_TOP
    ).apply {
        //visual = ColorVisual(170,170, 170, 127).apply { style.borderRadius = BorderRadius(10) }
    }
    private val playerFourArea = HexagonGrid<HexagonView>(
        width = 30, height = 30, posX = 590, posY = 529,
        coordinateSystem = HexagonGrid.CoordinateSystem.AXIAL,
        orientation = HexOrientation.POINTY_TOP
    ).apply {
        //visual = ColorVisual(170,170, 170, 127).apply { style.borderRadius = BorderRadius(10) }
    }

    //Buttons unten rechts
    private val endTurn = Button(width = 185, height = 60, posX = 1700, posY = 985, text = "End Turn",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(10) }
        onMouseClicked = {
            if(player == 0) {
                startFirstTurn()
            }

            if(player == 4) player = 1
            else player++
            showPlayer(player, false)
            initializeCamerasOnSide()
        }
    }

    //Temp. test variable für Undo funktion Add Hex
    var row = -2

    //Buttons unten links
    private val undo = Button(width = 130, height = 60, posX = 35, posY = 985, text = "Undo",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(10) }
        onMouseClicked = {
            val area = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(player-1)
            area[0,row]=hex
            row = --row
            adjustTileSize(player)
        }
    }
    private val redo = Button(width = 130, height = 60, posX = 190, posY = 985, text = "Redo",
        font = Font(size = 16, color = Color(255, 255, 255, 127))).apply {
        visual = ColorVisual(0,0, 0, 127).apply { style.borderRadius = BorderRadius(10) }
    }

    //Panel an der Seite mit einzelnen Views
    private val viewPanel = Label(width = 285, height = 912, posX = 35, posY = 35).apply {
        visual= ColorVisual(170,170,170, 170).apply { style.borderRadius = BorderRadius(40) }
    }

    //Kameras
    private val world = Pane<ComponentView>(width = 1920, height = 1080)
    private val cameraPane = CameraPane(posX = 0, posY = 0, width = 1920, height = 1080, target = world)
    private val cameraPaneOneSide = CameraPane(posX = 55, posY = 65, width = 245, height = 130, target = world)
    private val cameraPaneTwoSide = CameraPane(posX = 55, posY = 295, width = 245, height = 130, target = world)
    private val cameraPaneThreeSide = CameraPane(posX = 55, posY = 525, width = 245, height = 130, target = world)
    private val cameraPaneFourSide = CameraPane(posX = 55, posY = 755, width = 245, height = 130, target = world)

    //Labels über den Kameras an der Seite für die Hover Funktion
    private val LableAboveCamOne = Label(posX = 55, posY = 65, width = 245, height = 130).apply {
        isDisabled = true
        onMouseEntered = {
            showPlayer(1, true)
            playerName.text = "Luca"
        }
        onMouseExited = {
            showPlayer(player, true)
            playerName.text = "Aktuell"
        }
    }
    private val LableAboveCamTwo = Label(posX = 55, posY = 295, width = 245, height = 130).apply {
        isDisabled = true
        onMouseEntered = {
            showPlayer(2, true)
            playerName.text = "Theresa"
        }
        onMouseExited = {
            showPlayer(player, true)
            playerName.text = "Aktuell"
        }
    }
    private val LableAboveCamThree = Label(posX = 55, posY = 525, width = 245, height = 130).apply {
        isDisabled = true
        onMouseEntered = {
            showPlayer(3, true)
            playerName.text = "Philipp"
        }
        onMouseExited = {
            showPlayer(player, true)
            playerName.text = "Aktuell"
        }
    }
    private val LableAboveCamFour = Label(posX = 55, posY = 755, width = 245, height = 130).apply {
        isDisabled = true
        onMouseEntered = {
            showPlayer(4, true)
            playerName.text = "Nicolas"
        }
        onMouseExited = {
            showPlayer(player, true)
            playerName.text = "Aktuell"
        }
    }

    //Schwarze Leisten zwischen kleinen Kameras an der Seite
    private val barOne = Label(width = 245, height = 4, posX = 55, posY = 261).apply { visual= ColorVisual(0,0,0) }
    private val barTwo = Label(width = 245, height = 4, posX = 55, posY = 491).apply { visual= ColorVisual(0,0,0) }
    private val barThree = Label(width = 245, height = 4, posX = 55, posY = 721).apply { visual= ColorVisual(0,0,0) }

    //Namen an der Seite unter den kleinen Kameras
    private val nameOneSide = Label(width = 155, height = 30, posX = 55, posY = 213).apply { font = Font(size = 25) }
    private val nameTwoSide = Label(width = 155, height = 30, posX = 55, posY = 443).apply { font = Font(size = 25) }
    private val nameThreeSide = Label(width = 155, height = 30, posX = 55, posY = 673).apply { font = Font(size = 25) }
    private val nameFourSide = Label(width = 155, height = 30, posX = 55, posY = 903).apply { font = Font(size = 25) }

    //NatureToken Symbole unter den kleinen Kameras
    private val natureTokenOneSide = Label(width = 30, height = 30, posX = 220, posY = 213).apply {
        visual= ImageVisual("pinecone.png")
    }
    private val natureTokenTwoSide = Label(width = 30, height = 30, posX = 220, posY = 443).apply {
        visual= ImageVisual("pinecone.png")
    }
    private val natureTokenThreeSide = Label(width = 30, height = 30, posX = 220, posY = 673).apply {
        visual= ImageVisual("pinecone.png")
    }
    private val natureTokenFourSide = Label(width = 30, height = 30, posX = 220, posY = 903).apply {
        visual= ImageVisual("pinecone.png")
    }

    //Anzahl NatureTokens unter den kleinen Kameras
    private val natureTokenCountOneSide = Label(width = 50, height = 30, posX = 250, posY = 213).apply {
        font = Font(size = 25)
    }
    private val natureTokenCountTwoSide = Label(width = 50, height = 30, posX = 250, posY = 443).apply {
        font = Font(size = 25)
    }
    private val natureTokenCountThreeSide = Label(width = 50, height = 30, posX = 250, posY = 673).apply {
        font = Font(size = 25)
    }
    private val natureTokenCountFourSide = Label(width = 50, height = 30, posX = 250, posY = 903).apply {
        font = Font(size = 25)
    }

    //Name des Spielers unten mittig
    private val playerName = Label(width = 310, height = 60, posX = 845, posY = 985).apply {
        visual= ColorVisual(170,170,170, 170).apply { style.borderRadius = BorderRadius(30) }
        font = Font(size = 50)
        isVisible = false
    }








    //Testweise
    private val hex = HexagonView(size = 14, visual = ImageVisual("tile2.png"))
    private val greyHex = HexagonView(size = 20, visual = ColorVisual(170,170,170).apply { transparency = 1.0 })
    private val label1 = Label(width = 300, height = 158, posX = 990, posY = 325).apply {
        //visual= ColorVisual(170,170,170, 127)
    }
    private val label2 = Label(width = 300, height = 158, posX = 1400, posY = 650).apply {
        //visual= ColorVisual(170,170,170, 127)
    }
    private val label3 = Label(width = 300, height = 158, posX = 850, posY = 775).apply {
        //visual= ColorVisual(170,170,170, 127)
    }
    private val label4 = Label(width = 300, height = 158, posX = 440, posY = 450).apply {
        //visual= ColorVisual(170,170,170, 127)
    }



    init {
        //Container für CameraPane erstellen
        world.add(logo)
        listOf(label1, label2, label3, label4, playerOneArea, playerTwoArea, playerThreeArea, playerFourArea, hex).forEach { world.add(it) }

        addComponents(
            cameraPane,
            grayBox, customChoice, changeWildlife, clearOverpopulation, pineCone1, pineCone2,
            tileChoice1, tileChoice2, tileChoice3, tileChoice4,
            animalChoice1, animalChoice2, animalChoice3, animalChoice4,
            rotateOneCW, rotateOneCCW, rotateTwoCW, rotateTwoCCW,
            rotateThreeCW, rotateThreeCCW, rotateFourCW, rotateFourCCW,
            endTurn, undo, redo, viewPanel,
            barOne, barTwo, barThree, nameOneSide, nameTwoSide, nameThreeSide, nameFourSide,
            natureTokenOneSide, natureTokenTwoSide, natureTokenThreeSide, natureTokenFourSide,
            natureTokenCountOneSide, natureTokenCountTwoSide, natureTokenCountThreeSide, natureTokenCountFourSide,
            cameraPaneOneSide, cameraPaneTwoSide, cameraPaneThreeSide, cameraPaneFourSide,
            LableAboveCamOne, LableAboveCamTwo, LableAboveCamThree, LableAboveCamFour, playerName
        )

        initializeTest()
        adjustAreas()
    }


    private fun startFirstTurn() {
        listOf(LableAboveCamOne, LableAboveCamTwo, LableAboveCamThree, LableAboveCamFour).forEach {
            it.isDisabled = false
        }
        LableAboveCamOne.isDisabled = false
    }

    private fun initializeCamerasOnSide() {
        cameraPaneOneSide.pan(x = 1140.0, y = 404.0, zoom = 0.8, smooth = true)
        cameraPaneTwoSide.pan(x = 1550.0, y = 729.0, zoom = 0.8, smooth = true)
        cameraPaneThreeSide.pan(x = 1000.0, y = 854.0, zoom = 0.8, smooth = true)
        cameraPaneFourSide.pan(x = 590.0, y = 529.0, zoom = 0.8, smooth = true)
    }

    /**
     * Zentriert alle HexagonGrids in ihren jeweiligen Bereichen
     * */
    private fun adjustAreas() {
        playerOneArea.posX = 1140 - playerOneArea.width / 2
        playerOneArea.posY = 404 - playerOneArea.height / 2

        playerTwoArea.posX = 1550 - playerTwoArea.width / 2
        playerTwoArea.posY = 729 - playerTwoArea.height / 2

        playerThreeArea.posX = 1000 - playerThreeArea.width / 2
        playerThreeArea.posY = 854 - playerThreeArea.width / 2

        playerFourArea.posX = 590 - playerFourArea.width / 2
        playerFourArea.posY = 529 - playerFourArea.width / 2
    }

    /**
     * Reduziert die Größe der Tiles in den HexagonGrids, sobald diese größer werden als der Bereich
     * @param areaIndex von dem Typ [Int], gibt den Index (1-4) des aktuellen Spielers und damit Spielbereich an
     */
    private fun adjustTileSize(areaIndex: Int) {
        val area = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(areaIndex-1)
        if(area.height > 158 || area.width > 300) {
            area.components.forEach { it.size -= 5 }
        }

        //Nach der Größenveränderung muss neu zentriert werden
        adjustAreas()
    }

    /**
     * Zoomt mit dem CameraPane aus dem letzten Spieler heraus und auf den aktuellen
     * Wenn noch kein Spieler dran war zoomt sie nur auf den ersten
     */
    private fun showPlayer(player: Int, fromMiniMap: Boolean) {
        //X und Y für den Zoom auswählen
        var x = 0.0
        var y = 0.0
        when(player) {
            1 -> {
                x = 1130.0
                y = 384.0
            }
            2 -> {
                x = 1540.0
                y = 709.0
            }
            3 -> {
                x = 990.0
                y = 834.0
            }
            4 -> {
                x = 580.0
                y = 509.0
            }
        }

        if(!fromMiniMap) {
            //Aus dem alten Spieler rauszoomen, eine Sekunde delay und in den aktuellen Spieler reinzoomen
            if (this.player != 0) {
                playerName.isVisible = false
                cameraPane.pan(x = 0, y = 0, zoom = 1.0, smooth = true)
                playAnimation(
                    DelayAnimation(duration = 1000).apply {
                        onFinished = {
                            cameraPane.pan(x = x, y = y, zoom = 4.32, smooth = true)
                            playerName.isVisible = true
                        }
                    }
                )
            }
            //Wenn noch keiner dran war in den ersten Spieler rein zoomen
            else {
                cameraPane.pan(x = x, y = y, zoom = 4.32, smooth = true)
                playerName.isVisible = true
            }
        } else {
            cameraPane.pan(x = x, y = y, zoom = 4.32, smooth = true)
        }
    }

    /**
     * 
     */
    private fun chooseTile(index: Int, tile: HexagonView) {
        deactivateRotateButtons()
        if(selectTile == index) selectTile = 0
        else {
            selectTile = index
            scaleDownOtherTiles(selectTile)
            listOf(rotateOneCW, rotateTwoCW, rotateThreeCW, rotateFourCW).elementAt(selectTile-1).isVisible = true
            listOf(rotateOneCCW, rotateTwoCCW, rotateThreeCCW, rotateFourCCW).elementAt(selectTile-1).isVisible = true
            if(!customChoiceActive) {
                scaleDownOtherAnimals(selectTile)
            }
        }
        if(!customChoiceActive) {
            scaleTile(selectTile, tile)
            val animal = listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).elementAt(index-1)
            selectAnimal = selectTile
            scaleAnimal(selectAnimal, animal)
        } else {
            scaleTile(selectTile, tile)
        }
    }

    private fun chooseAnimal(index: Int, animal: Label) {
        if(!customChoiceActive && !changeWildlifeActive) {
            val tile = listOf(tileChoice1, tileChoice2, tileChoice3, tileChoice4).elementAt(index-1)
            chooseTile(index, tile)
        } else {
            if(!changeWildlifeActive) {
                if (selectAnimal == index) selectAnimal = 0
                else {
                    selectAnimal = index
                    scaleDownOtherAnimals(selectAnimal)
                }
                scaleAnimal(selectAnimal, animal)
            } else {
                if(changeAnimalsArray[index-1] == false) {
                    changeAnimalsArray[index-1] = true
                    scaleAnimal(index, animal)
                } else {
                    changeAnimalsArray[index-1] = false
                    scaleAnimal(0, animal)
                }
            }
        }
    }

    private fun deactivateRotateButtons() {
        listOf(rotateOneCW, rotateOneCCW, rotateTwoCW, rotateTwoCCW,
            rotateThreeCW, rotateThreeCCW, rotateFourCW, rotateFourCCW).forEach {
                it.isVisible = false
        }
    }

    private fun scaleDownOtherTiles(select: Int) {
        listOf(tileChoice1, tileChoice2, tileChoice3, tileChoice4).forEachIndexed { i, tile ->
            if(i != select-1) {
                scaleTile(0, tile)
            }
        }
    }

    private fun scaleDownOtherAnimals(select: Int) {
        listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).forEachIndexed { i, animal ->
            if(i != select-1) {
                scaleAnimal(0, animal)
            }
        }
    }


    private fun scaleTile(select: Int, hexView: HexagonView) {
        val targetScale = if(select !=0) {100.0 / 80.0 } else { 1.0 }
        playAnimation(
            ScaleAnimation(
                componentView = hexView,
                fromScaleX = hexView.scaleX,
                fromScaleY = hexView.scaleY,
                toScaleX = targetScale,
                toScaleY = targetScale,
                duration = 300,
                persist = true
            )
        )
    }

    private fun scaleAnimal(select: Int, animalView: Label) {
        val targetScale = if(select !=0) {100.0 / 81.0 } else { 1.0 }
        playAnimation(
            ScaleAnimation(
                componentView = animalView,
                fromScaleX = animalView.scaleX,
                fromScaleY = animalView.scaleY,
                toScaleX = targetScale,
                toScaleY = targetScale,
                duration = 300,
                persist = true
            )
        )
    }

    private fun rotateInSelection(clockwise: Boolean, tile: HexagonView) {
        val rotation = if(clockwise) 60.0 else -60.0
        playAnimation(
            RotationAnimation(
                componentView = tile,
                byAngle = rotation,
                duration = 300,
                persist = true
            )
        )
    }

    private fun selectionTileClick(onClick: Boolean) {
        listOf(tileChoice1, tileChoice2, tileChoice3, tileChoice4).forEach { it.isDisabled = !onClick }
    }

    private fun removeChosenWildlife() {
        for(i in 0..3) {
            changeAnimalsArray[i] = false
        }

        //Nur zur visualisierung
        testNearOverpopulation()
        animalChoice1.visual =  ImageVisual("fox.png")
        animalChoice2.visual =  ImageVisual("fox.png")
    }

    private fun testNearOverpopulation() {
        //Nur zur visualisierung
        clearOverpopulation.visual = ColorVisual(0,0,0,255).apply {
            style.borderRadius = BorderRadius(10)
        }
        clearOverpopulation.font = Font(size = 16, color = Color(255, 255, 255, 255))
        clearOverpopulation.isDisabled = false
    }

    private fun clearOverPopulation() {
        deactivateRotateButtons()
        scaleDownOtherTiles(0)
        scaleDownOtherAnimals(0)
        selectTile = 0
        selectAnimal = 0

        //Nur zur visualisierung
        animalChoice1.visual = ImageVisual("bear.png")
        animalChoice2.visual = ImageVisual("elk.png")
    }













    //Nur solange die Service Schicht noch nicht vorhanden ist um die Tiles etc. am Anfang zu laden

    private fun initializeTest() {
        animalChoice1.visual = ImageVisual("bear.png")
        animalChoice2.visual = ImageVisual("elk.png")
        animalChoice3.visual = ImageVisual("hawk.png")
        animalChoice4.visual = ImageVisual("fox.png")

        tileChoice1.visual = ImageVisual("tile1.png")
        tileChoice2.visual = ImageVisual("tile2.png")
        tileChoice3.visual = ImageVisual("tile3.png")
        tileChoice4.visual = ImageVisual("tile4.png")

        playerOneArea[0,0]=hex
        playerOneArea[-1,0]=hex
        playerOneArea[1,0]=hex
        playerOneArea[0,-1]=hex
        playerOneArea[1,-1]=hex
        playerOneArea[-1,1]=hex
        playerOneArea[0,1]=hex

        playerTwoArea[0,0]=hex
        playerTwoArea[-1,0]=hex
        playerTwoArea[1,0]=hex
        playerTwoArea[0,-1]=hex
        playerTwoArea[1,-1]=hex
        playerTwoArea[-1,1]=hex
        playerTwoArea[0,1]=hex

        playerThreeArea[0,0]=hex
        playerThreeArea[-1,0]=hex
        playerThreeArea[1,0]=hex
        playerThreeArea[0,-1]=hex
        playerThreeArea[1,-1]=hex
        playerThreeArea[-1,1]=hex
        playerThreeArea[0,1]=hex

        playerFourArea[0,0]=hex
        playerFourArea[-1,0]=hex
        playerFourArea[1,0]=hex
        playerFourArea[0,-1]=hex
        playerFourArea[1,-1]=hex
        playerFourArea[-1,1]=hex
        playerFourArea[0,1]=hex

        nameOneSide.text = "Luca"
        nameTwoSide.text = "Theresa"
        nameThreeSide.text = "Philipp"
        nameFourSide.text = "Nicolas"

        natureTokenCountOneSide.text = "3"
        natureTokenCountTwoSide.text = "2"
        natureTokenCountThreeSide.text = "0"
        natureTokenCountFourSide.text = "4"

        playerName.text = "Aktuell"

        initializeCamerasOnSide()
    }





    /** Zum Anzeigen lassen der globalen Variablen*/

//    private val label1 = Label(posX = 500,posY = 500,width = 100,height = 30)
//    private val label2 = Label(posX = 500,posY = 540,width = 100,height = 30)
//    private val label3 = Label(posX = 500,posY = 580,width = 100,height = 30)
//    private val label4 = Label(posX = 500,posY = 620,width = 100,height = 30)
//    private val label5 = Label(posX = 500,posY = 660,width = 200,height = 30)
//
//    label1, label2, label3, label4, label5
//
//    label1.text = selectTile.toString()
//    label2.text = selectAnimal.toString()
//    label3.text = customChoiceActive.toString()
//    label4.text = changeWildlifeActive.toString()
//    label5.text = changeAnimalsArray[0].toString() + changeAnimalsArray[1].toString() +
//                  changeAnimalsArray[2].toString() + changeAnimalsArray[3].toString()


}