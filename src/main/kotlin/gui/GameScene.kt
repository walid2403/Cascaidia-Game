package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.animation.DelayAnimation
import tools.aqua.bgw.animation.RotationAnimation
import tools.aqua.bgw.animation.ScaleAnimation
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.components.container.HexagonGrid
import tools.aqua.bgw.components.layoutviews.CameraPane
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.HexOrientation
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.BidirectionalMap
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual
import entity.*
import tools.aqua.bgw.animation.FadeAnimation
import tools.aqua.bgw.animation.MovementAnimation
import tools.aqua.bgw.components.gamecomponentviews.HexagonView
import tools.aqua.bgw.components.uicomponents.ComboBox
import tools.aqua.bgw.net.common.response.SpectatorJoinGameResponse
import tools.aqua.bgw.visual.Visual
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Die Klasse GameScene ist die Hauptszene des Cascadia Spiels
 * @param rootService ein Objekt des Typs [RootService], hier wird der Rootservice übergeben,
 * um auf die Serviceschicht zugreifen zu können
 * @property rootService ein Objekt des Typs [RootService], hier wird der Rootservice übergeben,
 *  * um auf die Serviceschicht zugreifen zu können
 *  @property selectAnimal ein Objekt des Typs [Int], Index der aktuellen Tierauswahl (-1 = keins ausgewählt)
 *  @property selectTile ein Objekt des Typs [Int], Index der aktuellen Tileauswahl (-1 = keins ausgewählt)
 *  @property customChoiceActive ein Objekt des Typs [Boolean], ob Custom Auswahl in dem Zug aktiv ist
 *  @property changeWildlifeActive ein Objekt des Typs [Boolean], ob Change Wildlife gerade genutzt wird
 *  @property changeAnimalsArray ein Objekt des Typs [booleanArrayOf], speichert für jedes Tier, ob es getauscht wird
 *  @property player ein Objekt des Typs [Int], speichert den aktullen Spieler Index (-1 = Rundenanfang)
 */
class GameScene(private val app: SopraApplication,private val rootService: RootService) : BoardGameScene(1920, 1080), Refreshable {
    private var selectAnimal = -1
    private var selectTile = -1
    private var customChoiceActive = false
    private var changeWildlifeActive = false
    private var changeAnimalsArray = booleanArrayOf(false,false,false,false)
    private var player = -1

    private var allButtonsAllowed = true
    var animationsEnabled = true
    private var isPlayerHuman = true
    private var animationSpeed = 1.0

    //Neuerungen 06.07
    private val shop = arrayOfNulls<HexagonViewExtended>(4)
    private var selectedGridX: Int? = null
    private var selectedGridY: Int? = null



    //Hintergrundbild
    private val logo = Label(posX = 0,posY = 0,width = 1920,height = 1080,visual = ImageVisual("backgrounds/CascadiaHintergrund.png"))

    //Graue Box um Auswahl
    private val grayBox = Label(width = 950, height = 300, posX = 485, posY = -40).apply {
        visual= ColorVisual(170,170,170, 170).apply { style.borderRadius = BorderRadius(20) }
    }

    //Buttons in grauer Box
    private val customChoiceButton = Button(width = 200, height = 60, posX = 550, posY = 25, text = "Custom Choice",
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

    private val changeWildlifeButton = Button(width = 200, height = 60, posX = 550, posY = 100, text = "Change Wildlife",
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
                updateNatureTokenCount(1)

                //Alle Tiles-OnClicks und Rotate-Buttons deaktivieren und alle runterskalieren und Auswahl zurücksetzten
                enableTilesInShop(false)
                scaleDownOtherTiles(0)
                scaleDownOtherAnimals(0)
                //deactivateRotateButtons()
                selectTile = -1
                selectAnimal = -1
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
                enableTilesInShop(true)
                scaleDownOtherAnimals(0)
                rootService.playerActionService.changeWildlife(listOf(0, 1, 2, 3).filter {changeAnimalsArray[it]})
            }
            changeWildlifeActive = !changeWildlifeActive
        }
    }

    private val clearOverpopulationButton = Button(
        width = 200, height = 60, posX = 550,
        posY = 175, text = "Clear Overpopulation",
        font = Font(size = 16, color = Color(255, 255, 255, 127))
    ).apply {
        visual = ColorVisual(0, 0, 0, 127).apply {
            style.borderRadius = BorderRadius(10)
        }
        this.isDisabled = true
        onMouseClicked = {
            //Button kann nur angeklickt werden, wenn 3 gleiche existieren
            //Nach onClick werden die Tiere entfernt und der Button wieder durchsichtig
            rootService.gameService.exterminate(true)
            this.visual = ColorVisual(0, 0, 0, 127).apply {
                style.borderRadius = BorderRadius(10)
            }
            this.font = Font(size = 16, color = Color(255, 255, 255, 127))
        }
    }

    //Tannenzapfen Symbole an Buttons
    private val pineCone1 = Label(posX = 525,posY = 35,width = 40,height = 40,visual = ImageVisual("tokens/pinecone.png"))
    private val pineCone2 = Label(posX = 525,posY = 110,width = 40,height = 40,visual = ImageVisual("tokens/pinecone.png"))


    //Auswahl Tiles
    private val tileChoice1 = Label(
        width = 100,
        height = 116,
        posX = 822,
        posY = 25,
    ).apply {
        onMouseClicked = {
            chooseTile(0)
        }
    }

    private val tileChoice2 = Label(
        width = 100,
        height = 116,
        posX = 976,
        posY = 25,
    ).apply {
        onMouseClicked = {
            chooseTile(1)
        }
    }

    private val tileChoice3 = Label(
        width = 100,
        height = 116,
        posX = 1130,
        posY = 25,
    ).apply {
        onMouseClicked = {
            chooseTile(2)
        }
    }

    private val tileChoice4 = Label(
        width = 100,
        height = 116,
        posX = 1284,
        posY = 25,
    ).apply {
        onMouseClicked = {
            chooseTile(3)
        }
    }

    private val tileShop = listOf(tileChoice1, tileChoice2, tileChoice3, tileChoice4)

    //Auswahl Tiere
    private val animalChoice1 = Label(
        width = 60, height = 60, posX = 842, posY = 175
    ).apply {
        visual= ColorVisual(170,170,170)
        onMouseClicked = {
            chooseAnimal(0, this)
        }
    }
    private val animalChoice2 = Label(
        width = 60, height = 60, posX = 996, posY = 175
    ).apply {
        visual= ColorVisual(170,170,170)
        onMouseClicked = {
            chooseAnimal(1, this)
        }
    }
    private val animalChoice3 = Label(
        width = 60, height = 60, posX = 1150, posY = 175
    ).apply {
        visual= ColorVisual(170,170,170)
        onMouseClicked = {
            chooseAnimal(2, this)
        }
    }
    private val animalChoice4 = Label(
        width = 60, height = 60, posX = 1304, posY = 175
    ).apply {
        visual= ColorVisual(170,170,170)
        onMouseClicked = {
            chooseAnimal(3, this)
        }
    }

    private val animalShop = listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4)

    //Rotations Knöpfe oben
//    private val rotateOneCW = Button(width = 40, height = 40, posX = 782, posY = 155, text = "<",
//        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
//        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
//        this.isVisible = false
//        onMouseClicked = { rotateInSelection(true, shop[0] as HexagonViewExtended) }
//    }
//    private val rotateOneCCW = Button(width = 40, height = 40, posX = 922, posY = 155, text = ">",
//        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
//        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
//        this.isVisible = false
//        onMouseClicked = { rotateInSelection(false, shop[0] as HexagonViewExtended) }
//    }
//    private val rotateTwoCW = Button(width = 40, height = 40, posX = 936, posY = 155, text = "<",
//        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
//        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
//        this.isVisible = false
//        onMouseClicked = { rotateInSelection(true, shop[1] as HexagonViewExtended) }
//    }
//    private val rotateTwoCCW = Button(width = 40, height = 40, posX = 1076, posY = 155, text = ">",
//        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
//        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
//        this.isVisible = false
//        onMouseClicked = { rotateInSelection(false, shop[1] as HexagonViewExtended) }
//    }
//    private val rotateThreeCW = Button(width = 40, height = 40, posX = 1090, posY = 155, text = "<",
//        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
//        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
//        this.isVisible = false
//        onMouseClicked = { rotateInSelection(true, shop[2] as HexagonViewExtended) }
//    }
//    private val rotateThreeCCW = Button(width = 40, height = 40, posX = 1230, posY = 155, text = ">",
//        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
//        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
//        this.isVisible = false
//        onMouseClicked = { rotateInSelection(false, shop[2] as HexagonViewExtended) }
//    }
//    private val rotateFourCW = Button(width = 40, height = 40, posX = 1244, posY = 155, text = "<",
//        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
//        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
//        this.isVisible = false
//        onMouseClicked = { rotateInSelection(true, shop[3] as HexagonViewExtended) }
//    }
//    private val rotateFourCCW = Button(width = 40, height = 40, posX = 1384, posY = 155, text = ">",
//        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
//        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
//        this.isVisible = false
//        onMouseClicked = { rotateInSelection(false, shop[3] as HexagonViewExtended) }
//    }

    //Bereiche der Spieler
    private val playerOneArea = HexagonGrid<HexagonViewExtended>(
        width = 30, height = 30, posX = 1140, posY = 404,
        coordinateSystem = HexagonGrid.CoordinateSystem.AXIAL,
        orientation = HexOrientation.POINTY_TOP
    ).apply {
        //visual = ColorVisual(170,170, 170, 127).apply { style.borderRadius = BorderRadius(10) }
    }
    private val playerTwoArea = HexagonGrid<HexagonViewExtended>(
        width = 30, height = 30, posX = 1550, posY = 729,
        coordinateSystem = HexagonGrid.CoordinateSystem.AXIAL,
        orientation = HexOrientation.POINTY_TOP
    ).apply {
        //visual = ColorVisual(170,170, 170, 127).apply { style.borderRadius = BorderRadius(10) }
    }
    private val playerThreeArea = HexagonGrid<HexagonViewExtended>(
        width = 30, height = 30, posX = 1000, posY = 854,
        coordinateSystem = HexagonGrid.CoordinateSystem.AXIAL,
        orientation = HexOrientation.POINTY_TOP
    ).apply {
        //visual = ColorVisual(170,170, 170, 127).apply { style.borderRadius = BorderRadius(10) }
    }
    private val playerFourArea = HexagonGrid<HexagonViewExtended>(
        width = 30, height = 30, posX = 590, posY = 529,
        coordinateSystem = HexagonGrid.CoordinateSystem.AXIAL,
        orientation = HexOrientation.POINTY_TOP
    ).apply {
        //visual = ColorVisual(170,170, 170, 127).apply { style.borderRadius = BorderRadius(10) }
    }

    //Buttons unten rechts
    //TODO("text vor erstem Spielzug zu start game o.ä. ändern")
    private val endTurn = Button(width = 185, height = 60, posX = 1700, posY = 985, text = "End Turn",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(10) }
        onMouseClicked = {
//            if(player == 3) player = 0
//            else player++
            val game = rootService.currentGame
            checkNotNull(game)
            rootService.gameService.changeTurn()
//            zoomOnNextPlayer()
        }
    }

    private val confirm = Button(width = 120, height = 60, posX = 1410, posY = 985, text = "Confirm",
        font = Font(size = 16, color = Color(255, 255, 255, 255)),
        visual = ColorVisual(0,0, 0).apply {
            style.borderRadius = BorderRadius(10)
        }
    ).apply {
        isVisible = false
        onMouseClicked = {
            println("on Click Test ")
            val s = ((selectedGridX!! + selectedGridY!!) * (-1))
            rootService.playerActionService.placeTile(Triple(selectedGridX!!, selectedGridY!!, s))
        }
    }

    private val rotateTileLeft = Button(width = 60, height = 60, posX = 1325, posY = 985, text = "->",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(30) }
        isVisible = false
        onMouseClicked = {
            rootService.playerActionService.rotateTile(false)
        }
    }

    private val rotateTileRight = Button(width = 60, height = 60, posX = 1240, posY = 985, text = "<-",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(30) }
        isVisible = false
        onMouseClicked = {
            rootService.playerActionService.rotateTile(true)
        }
    }


    //Neue Zoomfunktion

    private var zoomFactor = 4.32

    private val zoomIn = Button(width = 60, height = 60, posX = 1325, posY = 900, text = "+",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(30) }
        onMouseClicked = {
            zoomFactor += 0.3
            changeZoom()
        }
    }

    private val zoomOut = Button(width = 60, height = 60, posX = 1240, posY = 900, text = "-",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(30) }
        onMouseClicked = {
            zoomFactor -= 0.3
            changeZoom()
        }
    }





    //Buttons unten links
    //TODO("visibility bei online spielen, aufrufe der Methoden der Service Schicht")
    private val undo = Button(width = 130, height = 60, posX = 35, posY = 985, text = "Undo",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(10) }
        onMouseClicked = {
            rootService.playerActionService.undo()
        }
    }
    private val redo = Button(width = 130, height = 60, posX = 190, posY = 985, text = "Redo",
        font = Font(size = 16, color = Color(255, 255, 255, 127))).apply {
        visual = ColorVisual(0,0, 0, 127).apply { style.borderRadius = BorderRadius(10) }
        onMouseClicked = {
            rootService.playerActionService.redo()
        }
    }

    private val animationSpeedControl = ComboBox<Double>(
        posX = 354,
        posY = 985,
        width = 200,
        height = 60,
        items = listOf(1.0, 0.5, 2.0, 5.0, 10.0),
        disallowUnselect = true,
        formatFunction = { "Animation Speed: $it" },
        font = Font(size = 16, color = Color.WHITE),
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(10) }
    ).apply {
        onItemSelected = {
            newValue -> if(newValue != null) animationSpeed = newValue
        }
    }

    //Panel an der Seite mit einzelnen Views
    private val viewPanel = Label(width = 285, height = 912, posX = 35, posY = 35).apply {
        visual= ColorVisual(170,170,170, 170).apply { style.borderRadius = BorderRadius(20) }
    }

    //Kameras
    private val world = Pane<ComponentView>(width = 1920, height = 1080)
    private val cameraPane = CameraPane(posX = 0, posY = 0, width = 1920, height = 1080, target = world)
    private val cameraPaneOneSide = CameraPane(posX = 55, posY = 65, width = 245, height = 130, target = world)
    private val cameraPaneTwoSide = CameraPane(posX = 55, posY = 295, width = 245, height = 130, target = world)
    private val cameraPaneThreeSide = CameraPane(posX = 55, posY = 525, width = 245, height = 130, target = world)
    private val cameraPaneFourSide = CameraPane(posX = 55, posY = 755, width = 245, height = 130, target = world)

    //Labels über den Kameras an der Seite für die Hover Funktion
    private val lableAboveCamOne = Label(posX = 55, posY = 65, width = 245, height = 130).apply {
        onMouseEntered = {
            zoomIn.isVisible = false
            zoomOut.isVisible = false
            showOtherPlayer(0)
        }
        onMouseExited = {
            zoomIn.isVisible = true
            zoomOut.isVisible = true
            showOtherPlayer(getPlayerId())
        }
    }
    private val lableAboveCamTwo = Label(posX = 55, posY = 295, width = 245, height = 130).apply {
        onMouseEntered = {
            zoomIn.isVisible = false
            zoomOut.isVisible = false
            showOtherPlayer(1)
        }
        onMouseExited = {
            zoomIn.isVisible = true
            zoomOut.isVisible = true
            showOtherPlayer(getPlayerId())
        }
    }
    private val lableAboveCamThree = Label(posX = 55, posY = 525, width = 245, height = 130).apply {
        onMouseEntered = {
            zoomIn.isVisible = false
            zoomOut.isVisible = false
            showOtherPlayer(2)
        }
        onMouseExited = {
            zoomIn.isVisible = true
            zoomOut.isVisible = true
            showOtherPlayer(getPlayerId())
        }
    }
    private val lableAboveCamFour = Label(posX = 55, posY = 755, width = 245, height = 130).apply {
        onMouseEntered = {
            zoomIn.isVisible = false
            zoomOut.isVisible = false
            showOtherPlayer(3)
        }
        onMouseExited = {
            zoomIn.isVisible = true
            zoomOut.isVisible = true
            showOtherPlayer(getPlayerId())
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
        visual= ImageVisual("tokens/pinecone.png")
    }
    private val natureTokenTwoSide = Label(width = 30, height = 30, posX = 220, posY = 443).apply {
        visual= ImageVisual("tokens/pinecone.png")
    }
    private val natureTokenThreeSide = Label(width = 30, height = 30, posX = 220, posY = 673).apply {
        visual= ImageVisual("tokens/pinecone.png")
    }
    private val natureTokenFourSide = Label(width = 30, height = 30, posX = 220, posY = 903).apply {
        visual= ImageVisual("tokens/pinecone.png")
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
    }

    //Tiere für Scoring Cards
    private val bear = Label(width = 60, height = 60, posX = 1825, posY = 175).apply {
        visual= ImageVisual("tokens/bear.png")
        onMouseEntered = { bearScoringCard.isVisible = true }
        onMouseExited = { bearScoringCard.isVisible = false }
    }
    private val elk = Label(width = 60, height = 60, posX = 1825, posY = 275).apply {
        visual= ImageVisual("tokens/elk.png")
        onMouseEntered = { elkScoringCard.isVisible = true }
        onMouseExited = { elkScoringCard.isVisible = false }
    }
    private val fox = Label(width = 60, height = 60, posX = 1825, posY = 375).apply {
        visual= ImageVisual("tokens/fox.png")
        onMouseEntered = { foxScoringCard.isVisible = true }
        onMouseExited = { foxScoringCard.isVisible = false }
    }
    private val salmon = Label(width = 60, height = 60, posX = 1825, posY = 475).apply {
        visual= ImageVisual("tokens/salmon.png")
        onMouseEntered = { salmonScoringCard.isVisible = true }
        onMouseExited = { salmonScoringCard.isVisible = false }
    }
    private val hawk = Label(width = 60, height = 60, posX = 1825, posY = 575).apply {
        visual= ImageVisual("tokens/hawk.png")
        onMouseEntered = { hawkScoringCard.isVisible = true }
        onMouseExited = { hawkScoringCard.isVisible = false }
    }

    //Graue Box um Tiere
    private val grayBoxScoringAnimals = Label(width = 90, height = 700, posX = 1810, posY = -40).apply {
        visual= ColorVisual(170,170,170, 170).apply { style.borderRadius = BorderRadius(45) }
    }

    //Scoring Karten
    private val bearScoringCard = Label(width = 229, height = 435, posX = 1556, posY = 160).apply {
        visual= ImageVisual("scoringCards/Scoring_Bear_A.png")
        this.isVisible = false
    }
    private val elkScoringCard = Label(width = 229, height = 440, posX = 1556, posY = 160).apply {
        visual= ImageVisual("scoringCards/Scoring_Elk_A.png")
        this.isVisible = false
    }
    private val foxScoringCard = Label(width = 229, height = 497, posX = 1556, posY = 160).apply {
        visual= ImageVisual("scoringCards/Scoring_Fox_A.png")
        this.isVisible = false
    }
    private val salmonScoringCard = Label(width = 229, height = 497, posX = 1556, posY = 160).apply {
        visual= ImageVisual("scoringCards/Scoring_Salmon_A.png")
        this.isVisible = false
    }
    private val hawkScoringCard = Label(width = 229, height = 497, posX = 1556, posY = 160).apply {
        visual= ImageVisual("scoringCards/Scoring_Hawk_A.png")
        this.isVisible = false
    }

    //Pause Button
    private val pause = Button(width = 60, height = 60, posX = 1825, posY = 35, text = "||",
        font = Font(size = 16, color = Color(255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(30) }
    }.apply {
        onMouseClicked = {
            app.showMenuScene(PauseMenuScene(app,rootService))
        }
    }


    //Testweise
    private val hex = HexagonViewExtended(size = 14.0, visual = ImageVisual("tiles/clear/tile2.png"))
    private val greyHex = HexagonViewExtended(size = 20.0, visual = ColorVisual(170,170,170).apply { transparency = 1.0 })
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

    private val tileMap = BidirectionalMap<Tile, HexagonViewExtended>()

    init {
        //Container für CameraPane erstellen
        world.add(logo)
        listOf(label1, label2, label3, label4, playerOneArea, playerTwoArea, playerThreeArea, playerFourArea, hex).forEach { world.add(it) }

        addComponents(
            cameraPane,
            grayBox, customChoiceButton, changeWildlifeButton, clearOverpopulationButton, pineCone1, pineCone2,

            animalChoice1, animalChoice2, animalChoice3, animalChoice4,
//            rotateOneCW, rotateOneCCW, rotateTwoCW, rotateTwoCCW,
//            rotateThreeCW, rotateThreeCCW, rotateFourCW, rotateFourCCW,
            endTurn, undo, redo, viewPanel, confirm, rotateTileLeft, rotateTileRight,
            grayBoxScoringAnimals, pause, bear, elk, salmon, hawk, fox,
            barOne, barTwo, barThree, nameOneSide, nameTwoSide, nameThreeSide, nameFourSide,
            natureTokenOneSide, natureTokenTwoSide, natureTokenThreeSide, natureTokenFourSide,
            natureTokenCountOneSide, natureTokenCountTwoSide, natureTokenCountThreeSide, natureTokenCountFourSide,
            cameraPaneOneSide, cameraPaneTwoSide, cameraPaneThreeSide, cameraPaneFourSide,
            lableAboveCamOne, lableAboveCamTwo, lableAboveCamThree, lableAboveCamFour, playerName,
            bearScoringCard, elkScoringCard, salmonScoringCard, hawkScoringCard, foxScoringCard,
            zoomIn, zoomOut,
            tileChoice1, tileChoice2, tileChoice3, tileChoice4,
            animationSpeedControl,
        )
    }


    private fun startFirstTurn() {
        listOf(lableAboveCamOne, lableAboveCamTwo, lableAboveCamThree, lableAboveCamFour).forEach {
            it.isDisabled = false
        }
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

//    /**
//     * Zoomt mit dem CameraPane aus dem letzten Spieler heraus und auf den aktuellen
//     * Wenn noch kein Spieler dran war zoomt sie nur auf den ersten
//     */
//    private fun showPlayer(player: Int, fromMiniMap: Boolean, zoom: Boolean) {
//        //X und Y für den Zoom auswählen
//        var x = 0.0
//        var y = 0.0
//        when(player) {
//            0 -> {
//                x = 1130.0
//                y = 384.0
//            }
//            1 -> {
//                x = 1540.0
//                y = 709.0
//            }
//            2 -> {
//                x = 990.0
//                y = 834.0
//            }
//            3 -> {
//                x = 580.0
//                y = 509.0
//            }
//        }
//
//
//        if(zoom) {
//            cameraPane.pan(x = x, y = y, zoom = zoomFactor, smooth = true)
//        } else if(!fromMiniMap) {
//            //Aus dem alten Spieler rauszoomen, eine Sekunde delay und in den aktuellen Spieler reinzoomen
//            if (this.player != -1) {
//                //playerName.isVisible = false
//                cameraPane.pan(x = 0, y = 0, zoom = 1.0, smooth = true)
//                playAnimation(
//                    DelayAnimation(duration = (1000/animationSpeed).toInt()).apply {
//                        onFinished = {
//                            cameraPane.pan(x = x, y = y, zoom = 4.32, smooth = true)
//                            //playerName.isVisible = true
//                        }
//                    }
//                )
//            }
//            //Wenn noch keiner dran war in den ersten Spieler rein zoomen
//            else {
//                cameraPane.pan(x = x, y = y, zoom = 4.32, smooth = true)
//                //playerName.isVisible = true
//            }
//        } else {
//            cameraPane.pan(x = x, y = y, zoom = 4.32, smooth = true)
//        }
//        playerName.text = listOf(nameOneSide, nameTwoSide, nameThreeSide, nameFourSide).elementAt(player).text
//    }


    private fun zoomOnNextPlayer() {
        val currentPlayerID = getPlayerId()
        val coordinates = getCameraCoordinates(currentPlayerID)
        cameraPane.pan(x = 0, y = 0, zoom = 1.0, smooth = true)
        playAnimation(
            DelayAnimation(duration = (1000/animationSpeed).toInt()).apply {
                onFinished = {
                    cameraPane.pan(x = coordinates.first, y = coordinates.second, zoom = 4.32, smooth = true)
                }
            }
        )
    }

    private fun showOtherPlayer(playerID: Int) {
        val coordinates = getCameraCoordinates(playerID)
        println(playerID.toString())
        println(coordinates.toString())
        cameraPane.pan(x = coordinates.first, y = coordinates.second, zoom = 4.32, smooth = true)
        zoomFactor = 4.32
    }

    private fun zoomOnFirstPlayer() {
        val coordinates = getCameraCoordinates(0)
        cameraPane.pan(x = coordinates.first, y = coordinates.second, zoom = 4.32, smooth = true)
    }

    private fun changeZoom() {
        val currentPlayerID = getPlayerId()
        val coordinates = getCameraCoordinates(currentPlayerID)
        cameraPane.pan(x = coordinates.first+5, y = coordinates.second, zoom = zoomFactor, smooth = true)
    }

    private fun getCameraCoordinates(playerID: Int): Pair<Double, Double> {
        var x = 0.0
        var y = 0.0
        when(playerID) {
            0 -> {
                x = 1130.0
                y = 384.0
            }
            1 -> {
                x = 1540.0
                y = 709.0
            }
            2 -> {
                x = 990.0
                y = 834.0
            }
            3 -> {
                x = 580.0
                y = 509.0
            }
        }
        return Pair(x, y)
    }






    /**
     * 
     */
    private fun chooseTile(index: Int) {
        //deactivateRotateButtons()
        val tile = when (index) {
            0 -> tileChoice1
            1 -> tileChoice2
            2 -> tileChoice3
            3 -> tileChoice4
            else -> throw IllegalArgumentException("Invalid index given: $index")
        }
        if(selectTile == index) selectTile = -1
        else {
            selectTile = index
            scaleDownOtherTiles(selectTile)
            //listOf(rotateOneCW, rotateTwoCW, rotateThreeCW, rotateFourCW).elementAt(selectTile).isVisible = true
            //listOf(rotateOneCCW, rotateTwoCCW, rotateThreeCCW, rotateFourCCW).elementAt(selectTile).isVisible = true
            if(!customChoiceActive) {
                scaleDownOtherAnimals(selectTile)
            }
        }
        if(!customChoiceActive) {
            scaleTile(selectTile != -1, tile)
            val animal = listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).elementAt(index)
            selectAnimal = selectTile
            scaleAnimal(selectAnimal != -1, animal)
        } else {
            scaleTile(selectTile != -1, tile)
        }
    }

    private fun chooseAnimal(index: Int, animal: Label) {
        if(!customChoiceActive && !changeWildlifeActive) {
            chooseTile(index)
        } else {
            if(!changeWildlifeActive) {
                if (selectAnimal == index) selectAnimal = -1
                else {
                    selectAnimal = index
                    scaleDownOtherAnimals(selectAnimal)
                }
                scaleAnimal(selectAnimal != -1, animal)
            } else {
                if(!changeAnimalsArray[index]) {
                    changeAnimalsArray[index] = true
                    scaleAnimal(true, animal)
                } else {
                    changeAnimalsArray[index] = false
                    scaleAnimal(false, animal)
                }
            }
        }
    }

//    private fun deactivateRotateButtons() {
//        listOf(rotateOneCW, rotateOneCCW, rotateTwoCW, rotateTwoCCW,
//            rotateThreeCW, rotateThreeCCW, rotateFourCW, rotateFourCCW).forEach {
//                it.isVisible = false
//        }
//    }


    private fun scaleDownOtherTiles(select: Int) {
        tileShop.forEachIndexed { i, tile ->
            if(i != select) {
                scaleTile(false, tile)
            }
        }
    }

    private fun scaleDownOtherAnimals(select: Int) {
        listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).forEachIndexed { i, animal ->
            if(i != select) {
                scaleAnimal(false, animal)
            }
        }
    }

    private fun scaleTile(sizeUp: Boolean, tileLabel: Label) {
        val targetScale = if(sizeUp) {100.0 / 80.0 } else { 1.0 }
        playAnimation(
            ScaleAnimation(
                componentView = tileLabel,
                fromScaleX = tileLabel.scaleX,
                fromScaleY = tileLabel.scaleY,
                toScaleX = targetScale,
                toScaleY = targetScale,
                duration = (300/animationSpeed).toInt(),
                persist = true
            )
        )
    }

    private fun scaleAnimal(sizeUp: Boolean, animalView: Label) {
        val targetScale = if(sizeUp) {100.0 / 81.0 } else { 1.0 }
        playAnimation(
            ScaleAnimation(
                componentView = animalView,
                fromScaleX = animalView.scaleX,
                fromScaleY = animalView.scaleY,
                toScaleX = targetScale,
                toScaleY = targetScale,
                duration = (300/animationSpeed).toInt(),
                persist = true
            )
        )
    }

    private fun rotateInSelection(clockwise: Boolean, tile: HexagonViewExtended) {
        val rotation = if(clockwise) 60.0 else -60.0
        if(animationsEnabled) {
            playAnimation(
                RotationAnimation(
                    componentView = tile,
                    byAngle = rotation,
                    duration = (300/animationSpeed).toInt(),
                    persist = true
                )
            )
        } else {
            tile.rotation += rotation
        }
    }

    private fun enableTilesInShop(enabled: Boolean) {
        shop.forEach {
            if (it != null) {
                it.isDisabled = !enabled
            }
        }
    }

    private fun enableWildlifeInShop(enabled: Boolean) {
        listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).forEach { it.isDisabled = !enabled }
    }

    /**
     * This function creates [HexagonViewExtended] for every players' start tiles, adds them to the players grid
     * and adds the tile and tile view to the [tileMap]
     */
    private fun createTileView() {
        val game = rootService.currentGame
        checkNotNull(game)

        val tileList = mutableListOf<Tile>()

        for (player in game.playerQueue) {
            tileList.addAll(player.board.values)
        }

        for(tile in tileList) {
            val hexagon = HexagonViewExtended(size = 14.0, visual =
                ImageVisual("tiles/choices/tile_${tile.id}.png")).apply {
                onMouseClicked = {
                    onClickForTiles(this)
                }
            }
            tileMap.add(tile to hexagon)
        }
        tileMap.entries.forEach {
           entry -> println("value: ${entry.first.id}, coordinates: ${entry.second.toString()}")
        }
    }

    private fun onClickForTiles(tile: HexagonViewExtended) {
        val game = rootService.currentGame
        checkNotNull(game)
        //wenn auf ein graues Randhexagon geklickt wird, wird select tile aufgerufen, sonst place wildlife
        if (tile.visual == ColorVisual(170, 170, 170, 127)) {
            selectedGridX = game.playerQueue.peek().board.entries.find {
                it.value == tileMap.backward(tile)}?.key?.first
            selectedGridY = game.playerQueue.peek().board.entries.find {
                it.value == tileMap.backward(tile)}?.key?.second

            if (customChoiceActive) rootService.playerActionService.freeSelection(selectTile, selectAnimal)
            else rootService.playerActionService.selectColumn(selectTile)

        } else {
            println("place wildlife soll aufgerufen werden")
            val s = ((selectedGridX!! + selectedGridY!!) * (-1))
            rootService.playerActionService.placeWildlife(Triple(selectedGridX!!, selectedGridY!!, s))
        }
    }

    private fun createAnimalView(wildlifeToken: WildlifeToken): ImageVisual {
        val image = when (wildlifeToken) {
            WildlifeToken.FOX -> ImageVisual("tokens/fox.png")
            WildlifeToken.HAWK -> ImageVisual("tokens/hawk.png")
            WildlifeToken.ELK -> ImageVisual("tokens/elk.png")
            WildlifeToken.BEAR -> ImageVisual("tokens/bear.png")
            WildlifeToken.SALMON -> ImageVisual("tokens/salmon.png")
        }
        return image
    }

    private fun resetGame() {
        listOf(nameOneSide, nameTwoSide, nameThreeSide, nameFourSide, playerName).forEach { it.text = "" }
        listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).forEach { it.visual = Visual.EMPTY }
        listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).forEach { it.visual = Visual.EMPTY }
        listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).forEach { it.clear() }
        listOf(natureTokenCountOneSide, natureTokenCountTwoSide, natureTokenCountThreeSide, natureTokenCountFourSide).
        forEach { it.text = "0" }

        selectAnimal = -1
        selectTile = -1
        animationSpeed = 1.0
        animationSpeedControl.selectedItem = animationSpeedControl.items[0]
        customChoiceActive = false
        changeWildlifeActive = false
        changeAnimalsArray = booleanArrayOf(false,false,false,false)
        player = 0

        allButtonsAllowed = true
        animationsEnabled = true
        isPlayerHuman = true
        animationSpeed = 1.0
        shop[0] = null
        shop[1] = null
        shop[2] = null
        shop[3] = null
        selectedGridX = null
        selectedGridY = null
    }


    override fun refreshAfterStartGame() {
        val game = rootService.currentGame
        checkNotNull(game)

        playerListAtStart = game.playerQueue.toMutableList()

        resetGame()

        createTileView()
        initializeShop()

        loadScoreCards(game.scoringCards)

        for(i in 0..3) {
            val animal = game.choices.elementAt(i).second
            listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).elementAt(i).visual =
                createAnimalView(animal)
        }

        loadStartTiles()
        initializeCamerasOnSide()
//        disableAllButtons()
        setNames()
        adjustAreas()

        //animateDealTile()
        changeGreyVisibility(true, 0)
        changeGreyVisibility(false, 1)
        changeGreyVisibility(false, 2)
        changeGreyVisibility(false, 3)

        zoomOnFirstPlayer()

        //Testblock
//        shop[0]?.isVisible = true
//        shop[0]?.posX = 500.0
//        shop[0]?.posY = 500.0
//        addComponents(shop[0] as HexagonView)

    }

    private fun setNames() {
        val game = rootService.currentGame
        checkNotNull(game)

        for(i in game.playerQueue.indices) {
            listOf(nameOneSide, nameTwoSide, nameThreeSide, nameFourSide).elementAt(i).text =
                game.playerQueue.elementAt(i).name
        }
    }

    private fun loadStartTiles() {
        val game = rootService.currentGame
        checkNotNull(game)

        listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).forEachIndexed { index, grid ->
            if(index < game.playerQueue.size) {
                var tile = game.playerQueue.elementAt(index).board[Triple(0,0,0)]
                checkNotNull(tile)
                var hexView = tileMap.forward(tile)
                grid[0,0] = hexView
                println("current Index: $index")
                addGreyHexagon(hexView, index)

                tile = game.playerQueue.elementAt(index).board[Triple(0,1,-1)]
                checkNotNull(tile)
                hexView = tileMap.forward(tile)
                grid[0,1] = hexView
                addGreyHexagon(hexView, index)

                tile = game.playerQueue.elementAt(index).board[Triple(-1,1,0)]
                checkNotNull(tile)
                hexView = tileMap.forward(tile)
                grid[-1,1] = hexView
                addGreyHexagon(hexView, index)
            }
        }
    }


    private fun placeChosenTile() {
        val game = rootService.currentGame
        checkNotNull(game)

        val playerID = getPlayerId()
        val currentArea = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(playerID)

        val chosenTile = tileShop[selectTile]
        scaleTile( false, chosenTile)
        //chosenTile.size = 14.0
        //TODO("Größe vom tile Label im shop scalen auf Größe von tiles im Player Grid")

        val x = selectedGridX
        val y = selectedGridY
        checkNotNull(x)
        checkNotNull(y)
        val greyTile = currentArea[x,y]
        checkNotNull(greyTile)

        //bewege das shop label des ausgewählten Tiles an die gewählte Position im Grid, übertrage das tile visual in
        //das angeklickte graue Hexagon, ergänze das hexagon in der tileMap und mache das shop Label unsichtbar (in change Turn wird es zurück in den shop
        //bewegt, mit dem nachgelegten Tile befüllt und wieder sichtbar gemacht)

//        playAnimation(
//            MovementAnimation(
//                tileShop[selectTile],
//                toX = greyTile.posX,
//                toY = greyTile.posY,
//                duration = (500/animationSpeed).toInt()
//            ).apply {
//                onFinished = {
//                    println("post animation")
//                    greyTile.visual = chosenTile.visual
//                    chosenTile.isVisible = false
//
//                    tileMap.add(game.choices[selectTile].first to greyTile)
//                    addGreyHexagon(greyTile)
//                }
//            }
//        )


        //das soll eigentlich alles im onFinished der Animation passieren, da die nicht funktioniert habe ich das
        // rausgezogen. Wenn die Animation gefixt wird bitte löschen
        greyTile.visual = chosenTile.visual
        chosenTile.isVisible = false
        tileMap.add(game.choices[selectTile].first to greyTile)


        //deaktiviere nach dem Legen den Shop und alle Shop Buttons
        enableTilesInShop(false)
        enableWildlifeInShop(false)
        customChoiceButton.isDisabled = true
        changeWildlifeButton.isDisabled = true
        clearOverpopulationButton.isDisabled = true

        activatesTileButtons()
        changeGreyVisibility(false, playerID, greyTile)
        //deactivateRotateButtons()

        greyTile.isDisabled = false
        greyTile.onMouseClicked = { onClickForTiles(greyTile) }
        greyTile.choiceHex = false

        confirm.isDisabled = false
    }

    private fun putTileInGrid(x: Int, y: Int) {
        val game = rootService.currentGame
        checkNotNull(game)
        val currentArea = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(player)
        val selectTile = shop[selectTile]
        checkNotNull(selectTile)
        currentArea[x,y] = selectTile
        selectTile.isPlaced = true
        selectTile.inShop = false
    }

    /**
     * This function activates the confirm, rotateTileLeft and rotateTileRight Buttons
     */
    private fun activatesTileButtons() {
        if(isPlayerHuman) {
            listOf(confirm, rotateTileLeft, rotateTileRight).forEach {
                it.isVisible = true
                it.isDisabled = false
            }
        }
    }

    /**
     * This function deactivates the confirm, rotateTileLeft and rotateTileRight Buttons
     */
    private fun deactivateTileButtons() {
        listOf(confirm, rotateTileLeft, rotateTileRight).forEach { it.isVisible = false }
    }

    override fun refreshAfterChangeTurn(lastTurn: Boolean) {
        val game = rootService.currentGame
        checkNotNull(game)

        deactivateTileButtons()
        //player++
        player = (player + 1)%game.playerQueue.size

        zoomOnNextPlayer()
        changeGreyVisibility(true, player)

        if(isHuman()) {
            unlock()
        } else {
            lock()
        }

        checkRemoveWildlifeButton()
        checkExterminateButton()

        //load new visual into Shop Label of the Tile selected by the last player, then move it back into the Shop
        //position and make it visible again
        tileShop[selectTile].visual = getShopVisual(selectTile)
        moveLabelToShop(selectTile)

        animalShop[selectAnimal].visual = createAnimalView(game.choices[selectAnimal].second)
        chooseAnimal(selectAnimal, animalShop[selectAnimal])

        selectTile = -1
        selectAnimal = -1
        for(tile in game.playerQueue.peek().board){
            //if(tileMap.forward(tile.value).visual != ColorVisual(170, 170, 170, 127)) {
            println("calling addGreyHexagon for tile ${tile.value.id}")
            addGreyHexagon(tileMap.forward(tile.value))
            //}
        }

        //refreshShop()       //Game Ende testen wenn TileStack leer ist oder zu wenig animal Tokens
        //saveGameState()
    }

    /**
     * This function moves the Tile Label corresponding to the [shopIndex] back into its position in the shop and makes
     * it visible again
     */
    private fun moveLabelToShop(shopIndex: Int) {
        var label = tileChoice1
        var posX = 0.0
        val posY = 25.0

        when(shopIndex) {
            0 -> {
                label = tileChoice1
                posX = 822.0
            }
            1 -> {
                label = tileChoice2
                posX = 976.0
            }
            2 -> {
                label = tileChoice3
                posX = 1130.0
            }
            3 -> {
                label = tileChoice4
                posX = 1284.0
            }
            else -> throw IllegalArgumentException("Invalid shop index $shopIndex")
        }

        label.posX = posX
        label.posY = posY

        playAnimation(
            FadeAnimation(
                label,
                toOpacity = 1.0,
                duration = (300/animationSpeed).toInt()
            ).apply {
                onFinished = {
                    label.isVisible = true
                    println("label was moved")
                }
            }
        )
    }

    private fun isHuman(): Boolean {
        val game = rootService.currentGame
        checkNotNull(game)
        return game.playerQueue.peek().type == PlayerType.HUMAN
    }


    private fun initializeShop() {
        val game = rootService.currentGame
        checkNotNull(game)

        for(i in 0..3) {
            tileShop[i].visual = getShopVisual(i)
            tileShop[i].isVisible = true
            tileShop[i].isDisabled = false
        }
    }

    private fun getShopVisual(shopIndex: Int): ImageVisual {
        val game = rootService.currentGame
        checkNotNull(game)

        val tileID = game.choices[shopIndex].first.id
        return ImageVisual("tiles/choices/tile_$tileID.png")
    }

    private fun animateDealTile() {
//        for(i in 0..3) {
//            playAnimation(
//                MovementAnimation(
//                    componentView = shop[i] as HexagonView,   //Kp warum er da ComponentView haben will und sonst reicht HexagonViewExtended
//                    fromX = 1500.0,                             //Hinterher aus dem "TileStack"
//                    fromY = 700.0,
//                    toX = getShopTileCoordinateX(i),
//                    toY = 30.0,
//                    duration = (500/animationSpeed).toInt()
//                )
//            )
//            println("Animation")
//            println(getShopTileCoordinateX(i))
//            println(shop[i].toString())
//        }

        shop[0]?.posX = 820.0
        shop[1]?.posX = 974.0
        shop[2]?.posX = 1128.0
        shop[3]?.posX = 1282.0

        shop[0]?.posY = 30.0
        shop[1]?.posY = 30.0
        shop[2]?.posY = 30.0
        shop[3]?.posY = 30.0

        shop[0]?.size = 60.0
        shop[1]?.size = 60.0
        shop[2]?.size = 60.0
        shop[3]?.size = 60.0
    }

    private fun getShopTileCoordinateX(index: Int): Double {
        when(index) {
            0 -> return 820.0
            1 -> return 974.0
            2 -> return 1128.0
            3 -> return 1282.0
        }
        return 0.0
    }

//    private fun disableAllButtons() {
//        listOf(undo, redo,
//            confirm, rotateTileLeft, rotateTileRight).forEach {
//                it.isDisabled = true
//        }
//        deactivateRotateButtons()
//    }

    private fun getTileWithAnimal(tile: Tile): Pair<ImageVisual, Int> {
        var path = "tilesWithWildlife/"
        path += when(tile.id) {
            0,1,2,3,4,5,420 -> "PF"
            6,7,8,9,10,16,120 -> "WF"
            11,12,13,14,15,500 -> "F"
            17,18,19,20,21,200 -> "W"
            22,23,24,25,26,27,510 -> "WP"
            28,29,30,31,32,300 -> "P"
            33 -> "WM"
            34,35,36,37,38,400 -> "R"
            39,40,41,42,43,44,320 -> "RW"
            45,46,47,48,49,50,520 -> "MR"
            51,63 -> "FM"
            52,53,54 -> "PM"
            55,58,59,60,61,62,220 -> "RF"
            56,57,110 -> "RP"
            64,65,66,67 -> "RP"
            68,69,70,71,72,100 -> "M"
            73,74 -> "PM"
            75,76,77,78,81,410 -> "WM"
            79,210 -> "PM"
            80,82,83,84,310 -> "FM"

            else -> throw IllegalArgumentException("Invalid tile id: ${tile.id}")
        }

        if(tile.occupant != null) {
            path += when(tile.occupant) {
                WildlifeToken.ELK -> "_E"
                WildlifeToken.FOX -> "_F"
                WildlifeToken.BEAR -> "_B"
                WildlifeToken.HAWK -> "_H"
                WildlifeToken.SALMON -> "_S"
                else -> throw IllegalArgumentException("Invalid tile occupant: ${tile.occupant}")
            }
        } else {
            throw IllegalArgumentException("Tile has no occupant! Tile ID: {$tile.id")
        }

        val rotation = when(tile.id) {
            110 -> 300
            120 -> 60
            210 -> 300
            220 -> 240
            310 -> 300
            320 -> 60
            410 -> 120
            420 -> 240
            510 -> 300
            520 -> 240
            else -> 0
        }
        return Pair(ImageVisual("$path.png"), rotation)
    }

    private fun addGreyHexagon(tileView: HexagonViewExtended, playerIndex: Int = 0) {
        val game = rootService.currentGame
        checkNotNull(game)
        println("index in addHexagons: $playerIndex")

        val currentArea = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(player)
        println("area selected")

        val nbhs = listOf(Pair(1, -1), Pair(1, 0), Pair(0, 1), Pair(-1, 1), Pair(-1, 0), Pair(0, -1))

        val tile = tileMap.backward(tileView)
        checkNotNull(tile)
        println("tile being processed: ${tile.id}")
        val tilePos = game.playerQueue.elementAt(playerIndex).board.entries.find { it.value == tile }?.key
        checkNotNull(tilePos)
        val tileViewPos = Pair(tilePos.first, tilePos.second)

        nbhs.forEach { nbh ->
            var nbhTileView = currentArea[tileViewPos.first + nbh.first, tileViewPos.second + nbh.second]
            if (nbhTileView == null) {
                nbhTileView = HexagonViewExtended(tileView.size, ColorVisual(170, 170, 170, 127))
                nbhTileView.apply {
                    onMouseClicked = {
                        selectedGridX = tileViewPos.first + nbh.first
                        selectedGridY = tileViewPos.second + nbh.second

                        if (customChoiceActive) rootService.playerActionService.freeSelection(selectTile, selectAnimal)
                        else rootService.playerActionService.selectColumn(selectTile)
                    }
                }

                nbhTileView.choiceHex = true
                currentArea[tileViewPos.first + nbh.first, tileViewPos.second + nbh.second] = nbhTileView
            }
        }
    }

    private fun changeGreyVisibility(visible: Boolean, player: Int, exception: HexagonViewExtended? = null) {
        listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(player).components.filter {
            it.choiceHex }.forEach {
                if(exception == null || it != exception) {
                    it.isVisible = visible
                }
            }
    }


    override fun refreshAfterChangeWildlife(indices: List<Int>) {
        val game = rootService.currentGame
        checkNotNull(game)
        for (i in indices){
            val image = createAnimalView(game.choices.elementAt(i).second)
            listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).elementAt(i).visual = image
        }

        checkRemoveWildlifeButton()
        checkExterminateButton()
    }

    private fun checkRemoveWildlifeButton() {
        val game = rootService.currentGame
        checkNotNull(game)

        changeWildlifeButton.isDisabled = true

        if (game.gameState !in listOf(GameState.START_OF_TURN, GameState.HAS_EXTERMINATED)) return

        if (game.playerQueue.peek().natureTokens < 1) return

        changeWildlifeButton.visual = ColorVisual(0,0,0,255).apply {
            style.borderRadius = BorderRadius(10)
        }
        changeWildlifeButton.font = Font(size = 16, color = Color(255, 255, 255, 255))

        changeWildlifeButton.isDisabled = false
    }

    private fun checkExterminateButton() {
        val game = rootService.currentGame
        checkNotNull(game)

        clearOverpopulationButton.isDisabled = true

        if (game.gameState != GameState.START_OF_TURN) return

        val choices = game.choices.map {it.second}

        if (choices.distinct().size != 2) return

        if (choices.count {it == choices[0]} != 3 && choices.count {it == choices[0]} != 3) return

        clearOverpopulationButton.visual = ColorVisual(0,0,0,255).apply {
            style.borderRadius = BorderRadius(10)
        }
        clearOverpopulationButton.font = Font(size = 16, color = Color(255, 255, 255, 255))

        clearOverpopulationButton.isDisabled = false
    }


    override fun refreshAfterExterminate() {
        //deactivateRotateButtons()
        selectTile = -1
        selectAnimal = -1
        scaleDownOtherTiles(selectTile)
        scaleDownOtherAnimals(selectAnimal)

        refreshAfterChangeWildlife(listOf(0,1,2,3))
    }


    override fun refreshAfterUndo() {
        refreshAfterStartGame()
        println("Test")
    }

    override fun refreshAfterSelectColumn(index: Int) {
        placeChosenTile()
    }

    override fun refreshAfterRedo() {
        refreshAfterStartGame()
    }


    override fun refreshAfterFreeSelection() {
        placeChosenTile()
    }


    override fun refreshAfterLoadGame() {
        refreshAfterStartGame()
    }


    override fun refreshAfterRotate(right: Boolean) {
        val game = rootService.currentGame
        checkNotNull(game)
        rotateInSelection(right, tileMap.forward(game.choices.elementAt(game.selectedChoice.first).first))
    }


    override fun refreshAfterPlaceTile(index: Triple<Int, Int, Int>) {

        val game = rootService.currentGame
        checkNotNull(game)

        confirm.isVisible = false
        rotateTileRight.isVisible = false
        rotateTileLeft.isVisible = false

        //TODO("eig. wird das Tile jetzt zu früh (nach select statt nach place Tile) ins Grid gepackt. Macht anders nur keinen Sinn?")


        //putTileInGrid(index.first, index.second)
    }


    override fun refreshAfterPlaceWildlife(index: Triple<Int, Int, Int>) {
        val game = rootService.currentGame
        checkNotNull(game)
        val currentArea = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(getPlayerId())

        val tile = currentArea[index.first, index.second]
        checkNotNull(tile)
        val newVisual = getTileWithAnimal(tileMap.backward(tile))
        tile.visual = newVisual.first
        tile.isDisabled = true
        tile.rotate(newVisual.second)

        animalShop[selectAnimal].visual = Visual.EMPTY

        updateNatureTokenCount()
    }

    private fun updateNatureTokenCount(removeManually: Int = 0) {
        val game = rootService.currentGame
        checkNotNull(game)
        val tokenCount = listOf(natureTokenCountOneSide, natureTokenCountTwoSide,
            natureTokenCountThreeSide, natureTokenCountFourSide).elementAt(player)

        val newCount = game.playerQueue.peek().natureTokens - removeManually
        tokenCount.text = "$newCount"
    }

    override fun refreshAfterChatMessage(messageSender: String, message: String) {

    }

    override fun refreshAfterUseNatureToken() {
        customChoiceButton.visual = ColorVisual(256, 181, 0).apply {
            style.borderRadius = BorderRadius(10)
        }
        customChoiceButton.font = Font(size = 16, color = Color(0, 0, 0))
        customChoiceActive = true
        updateNatureTokenCount()
    }

    //Liste wird bei refreshAfterStartGame mit der Startreihenfolge befüllt
    private var playerListAtStart = mutableListOf<Player>()

    /**
     * Die Methode gibt den Index des aktuellen Spielers zurück, bezogen auf die Startreihenfolge.
     * Sollte ein Spieler das Spiel verlassen wird dieser nicht weiter berücksichtigt.
     * Es gibt eine Fehlermeldung, falls der Index nicht in der Range der Queue liegt.
     * @return Gibt den Index des Spielers zurück
     * */
    private fun getPlayerId(): Int {
        val game = rootService.currentGame
        checkNotNull(game)

        //Passt die Liste an, falls Spieler das Spiel verlassen haben
        if(playerListAtStart.size != game.playerQueue.size) {
            playerListAtStart.removeAll{ it !in game.playerQueue}
        }

        return playerListAtStart.indexOf(game.playerQueue.peek())
    }

    private fun loadScoreCards(selection: List<Boolean>) {
        for (i in selection.indices) {
            if(!selection[i]) {
                when (i) {
                    0 -> bearScoringCard.visual = ImageVisual("scoringCards/Scoring_Bear_B.png")
                    1 -> elkScoringCard.visual = ImageVisual("scoringCards/Scoring_Elk_B.png")
                    2 -> salmonScoringCard.visual = ImageVisual("scoringCards/Scoring_Salmon_B.png")
                    3 -> hawkScoringCard.visual = ImageVisual("scoringCards/Scoring_Hawk_B.png")
                    4 -> foxScoringCard.visual = ImageVisual("scoringCards/Scoring_Fox_B.png")
                    else -> throw IllegalArgumentException("scoreCard list has too many indices: " +
                            "${selection.indices} indices")
                }
            }
        }
    }


    private fun renderImage(tileName: String, tokenName: String, tileRotation: Int): String {
        val background = ImageIO.read(File("tiles/clear/$tileName.png"))
        val foreground = ImageIO.read(File("tokens/$tokenName.png"))

        val result = BufferedImage(
            background.width,
            background.height,
            BufferedImage.TYPE_INT_RGB
        )

        val g = result.createGraphics()

        g.drawImage(background, 0, 0, background.width, background.height, null)

        val angle = Math.toRadians(60.0 * tileRotation.toDouble())

        val x = 50 // Abstand vom linken Rand des Hintergrundbildes
        val y = 50 // Abstand vom oberen Rand des Hintergrundbildes

        val centerX = x + foreground.width / 2.0
        val centerY = y + foreground.height / 2.0

        g.rotate(angle, centerX, centerY)
        g.drawImage(foreground, x, y, foreground.width * 1, foreground.height * 1, null)

        g.dispose()

        val tmpFile = File.createTempFile("cascadia_renderedTile_", ".png")
        ImageIO.write(result, "png", tmpFile)

        return tmpFile.absolutePath
    }




    //Nur solange die Service Schicht noch nicht vorhanden ist um die Tiles etc. am Anfang zu laden

    private fun initializeTest() {
        animalChoice1.visual = ImageVisual("tokens/bear.png")
        animalChoice2.visual = ImageVisual("tokens/elk.png")
        animalChoice3.visual = ImageVisual("tokens/hawk.png")
        animalChoice4.visual = ImageVisual("tokens/fox.png")

//        tileChoice1.visual = ImageVisual("tile1.png")
//        tileChoice2.visual = ImageVisual("tile2.png")
//        tileChoice3.visual = ImageVisual("tile3.png")
//        tileChoice4.visual = ImageVisual("tile4.png")

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

//        nameOneSide.text = "Luca"
//        nameTwoSide.text = "Theresa"
//        nameThreeSide.text = "Philipp"
//        nameFourSide.text = "Nicolas"

        natureTokenCountOneSide.text = "3"
        natureTokenCountTwoSide.text = "2"
        natureTokenCountThreeSide.text = "0"
        natureTokenCountFourSide.text = "4"

//        playerName.text = "Aktuell"

        initializeCamerasOnSide()
        startFirstTurn()
//        showPlayer(1,false, false)
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




//    if(customChoiceActive){
//        rootService.playerActionService.freeSelection(selectTile -1, selectAnimal -1)
//    }
//    else{
//        rootService.playerActionService.selectColumn(selectTile -1)
//    }


}