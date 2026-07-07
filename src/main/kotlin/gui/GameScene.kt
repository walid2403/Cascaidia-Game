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
import tools.aqua.bgw.animation.MovementAnimation
import tools.aqua.bgw.visual.Visual

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
class GameScene(private val app: SopraApplication,private val rootService: RootService) : BoardGameScene(1920, 1080), Refreshable {
    private var selectAnimal = 0
    private var selectTile = 0
    private var customChoiceActive = false
    private var changeWildlifeActive = false
    private var changeAnimalsArray = booleanArrayOf(false,false,false,false)
    private var player = 0

    private var allButtonsAllowed = true
    var animationsEnabled = true
    private var isPlayerHuman = true

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
                rootService.playerActionService.changeWildlife(listOf(0, 1, 2, 3).filter {changeAnimalsArray[it]})
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
            rootService.gameService.exterminate(true)
            clearOverPopulation()
            this.visual = ColorVisual(0, 0, 0, 127).apply {
                style.borderRadius = BorderRadius(10)
            }
            this.font = Font(size = 16, color = Color(255, 255, 255, 127))
        }
    }

    //Tannenzapfen Symbole an Buttons
    private val pineCone1 = Label(posX = 525,posY = 35,width = 40,height = 40,visual = ImageVisual("tokens/pinecone.png"))
    private val pineCone2 = Label(posX = 525,posY = 110,width = 40,height = 40,visual = ImageVisual("tokens/pinecone.png"))

//    //Auswahl Habitate
//    private val tileChoice1 = HexagonViewExtended(posX = 820, posY = 30, size = 60, visual = ColorVisual(170,170,170)).apply {
//        onMouseClicked = { chooseTile(1, this) }
//    }
//    private val tileChoice2 = HexagonViewExtended(posX = 974, posY = 30, size = 60, visual = ColorVisual(170,170,170)).apply {
//        onMouseClicked = { chooseTile(2, this) }
//    }
//    private val tileChoice3 = HexagonViewExtended(posX = 1128, posY = 30, size = 60, visual = ColorVisual(170,170,170)).apply {
//        onMouseClicked = { chooseTile(3, this) }
//    }
//    private val tileChoice4 = HexagonViewExtended(posX = 1282, posY = 30, size = 60, visual = ColorVisual(170,170,170)).apply {
//        onMouseClicked = { chooseTile(4, this) }
//    }

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
        onMouseClicked = { rotateInSelection(true, shop[0] as HexagonViewExtended) }
    }
    private val rotateOneCCW = Button(width = 40, height = 40, posX = 922, posY = 155, text = ">",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(false, shop[0] as HexagonViewExtended) }
    }
    private val rotateTwoCW = Button(width = 40, height = 40, posX = 936, posY = 155, text = "<",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(true, shop[1] as HexagonViewExtended) }
    }
    private val rotateTwoCCW = Button(width = 40, height = 40, posX = 1076, posY = 155, text = ">",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(false, shop[1] as HexagonViewExtended) }
    }
    private val rotateThreeCW = Button(width = 40, height = 40, posX = 1090, posY = 155, text = "<",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(true, shop[2] as HexagonViewExtended) }
    }
    private val rotateThreeCCW = Button(width = 40, height = 40, posX = 1230, posY = 155, text = ">",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(false, shop[2] as HexagonViewExtended) }
    }
    private val rotateFourCW = Button(width = 40, height = 40, posX = 1244, posY = 155, text = "<",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(true, shop[3] as HexagonViewExtended) }
    }
    private val rotateFourCCW = Button(width = 40, height = 40, posX = 1384, posY = 155, text = ">",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(20) }
        this.isVisible = false
        onMouseClicked = { rotateInSelection(false, shop[3] as HexagonViewExtended) }
    }

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
            if(player == 3) player = 0
            else player++
            //rootService.gameService.changeTurn()
            showPlayer(player, false, false)
            initializeCamerasOnSide()
        }
    }

    private val confirm = Button(width = 120, height = 60, posX = 1410, posY = 985, text = "Confirm",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(10) }
        isVisible = false
        onMouseClicked = {
            //TODO ("position auf dem board angeben")
//            rootService.playerActionService.placeTile()
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
            zoomFactor += 0.2
            showPlayer(player, false, true)
        }
    }

    private val zoomOut = Button(width = 60, height = 60, posX = 1240, posY = 900, text = "-",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(30) }
        onMouseClicked = {
            zoomFactor -= 0.2
            showPlayer(player, false, true)
        }
    }





    //Buttons unten links
    //TODO("visibility bei online spielen, aufrufe der Methoden der Service Schicht")
    private val undo = Button(width = 130, height = 60, posX = 35, posY = 985, text = "Undo",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(10) }
    }
    private val redo = Button(width = 130, height = 60, posX = 190, posY = 985, text = "Redo",
        font = Font(size = 16, color = Color(255, 255, 255, 127))).apply {
        visual = ColorVisual(0,0, 0, 127).apply { style.borderRadius = BorderRadius(10) }
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
        isDisabled = true
        onMouseEntered = {
            showPlayer(0, true, false)
        }
        onMouseExited = {
            showPlayer(player, true, false)
        }
    }
    private val lableAboveCamTwo = Label(posX = 55, posY = 295, width = 245, height = 130).apply {
        isDisabled = true
        onMouseEntered = {
            showPlayer(1, true, false)
        }
        onMouseExited = {
            showPlayer(player, true, false)
        }
    }
    private val lableAboveCamThree = Label(posX = 55, posY = 525, width = 245, height = 130).apply {
        isDisabled = true
        onMouseEntered = {
            showPlayer(2, true, false)
        }
        onMouseExited = {
            showPlayer(player, true, false)
        }
    }
    private val lableAboveCamFour = Label(posX = 55, posY = 755, width = 245, height = 130).apply {
        isDisabled = true
        onMouseEntered = {
            showPlayer(3, true, false)
        }
        onMouseExited = {
            showPlayer(player, true, false)
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

    //Slider
    private val sliderBar = Label(width = 200, height = 4, posX = 400, posY = 398).apply {
        visual = ColorVisual(170,170,170)
    }
    private val sliderPoint = Label(width = 50, height = 50, posX = 400, posY = 375).apply {
        visual = ColorVisual(170,170,170)
        onMousePressed = { event ->
            if(event.posX.toDouble() >= 400.0 && event.posX.toDouble() <= 600) {
                this.posX = event.posX.toDouble()
            }
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



    init {
        //Container für CameraPane erstellen
        world.add(logo)
        listOf(label1, label2, label3, label4, playerOneArea, playerTwoArea, playerThreeArea, playerFourArea, hex).forEach { world.add(it) }

        addComponents(
            cameraPane,
            grayBox, customChoice, changeWildlife, clearOverpopulation, pineCone1, pineCone2,
            //tileChoice1, tileChoice2, tileChoice3, tileChoice4,
            animalChoice1, animalChoice2, animalChoice3, animalChoice4,
            rotateOneCW, rotateOneCCW, rotateTwoCW, rotateTwoCCW,
            rotateThreeCW, rotateThreeCCW, rotateFourCW, rotateFourCCW,
            endTurn, undo, redo, viewPanel, confirm, rotateTileLeft, rotateTileRight,
            grayBoxScoringAnimals, pause, bear, elk, salmon, hawk, fox,
            barOne, barTwo, barThree, nameOneSide, nameTwoSide, nameThreeSide, nameFourSide,
            natureTokenOneSide, natureTokenTwoSide, natureTokenThreeSide, natureTokenFourSide,
            natureTokenCountOneSide, natureTokenCountTwoSide, natureTokenCountThreeSide, natureTokenCountFourSide,
            cameraPaneOneSide, cameraPaneTwoSide, cameraPaneThreeSide, cameraPaneFourSide,
            lableAboveCamOne, lableAboveCamTwo, lableAboveCamThree, lableAboveCamFour, playerName,
            bearScoringCard, elkScoringCard, salmonScoringCard, hawkScoringCard, foxScoringCard,
            zoomIn, zoomOut,

            sliderBar, sliderPoint
        )

        initializeTest()
        adjustAreas()
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

    /**
     * Zoomt mit dem CameraPane aus dem letzten Spieler heraus und auf den aktuellen
     * Wenn noch kein Spieler dran war zoomt sie nur auf den ersten
     */
    private fun showPlayer(player: Int, fromMiniMap: Boolean, zoom: Boolean) {
        //X und Y für den Zoom auswählen
        var x = 0.0
        var y = 0.0
        when(player) {
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


        if(zoom) {
            cameraPane.pan(x = x, y = y, zoom = zoomFactor, smooth = true)
        } else if(!fromMiniMap) {
            //Aus dem alten Spieler rauszoomen, eine Sekunde delay und in den aktuellen Spieler reinzoomen
            if (this.player != -1) {
                //playerName.isVisible = false
                cameraPane.pan(x = 0, y = 0, zoom = 1.0, smooth = true)
                playAnimation(
                    DelayAnimation(duration = 1000).apply {
                        onFinished = {
                            cameraPane.pan(x = x, y = y, zoom = 4.32, smooth = true)
                            //playerName.isVisible = true
                        }
                    }
                )
            }
            //Wenn noch keiner dran war in den ersten Spieler rein zoomen
            else {
                cameraPane.pan(x = x, y = y, zoom = 4.32, smooth = true)
                //playerName.isVisible = true
            }
        } else {
            cameraPane.pan(x = x, y = y, zoom = 4.32, smooth = true)
        }
        playerName.text = listOf(nameOneSide, nameTwoSide, nameThreeSide, nameFourSide).elementAt(player).text
    }

    /**
     * 
     */
    private fun chooseTile(index: Int, tile: HexagonViewExtended) {
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
            val tile = shop[index-1] as HexagonViewExtended
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
        shop.forEachIndexed { i, tile ->
            if(i != select-1) {
                scaleTile(0, tile as HexagonViewExtended)
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


    private fun scaleTile(select: Int, hexView: HexagonViewExtended) {
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

    private fun rotateInSelection(clockwise: Boolean, tile: HexagonViewExtended) {
        val rotation = if(clockwise) 60.0 else -60.0
        if(animationsEnabled) {
            playAnimation(
                RotationAnimation(
                    componentView = tile,
                    byAngle = rotation,
                    duration = 300,
                    persist = true
                )
            )
        } else {
            tile.rotation += rotation
        }
    }

    private fun selectionTileClick(onClick: Boolean) {
        shop.forEach {
            if (it != null) {
                it.isDisabled = !onClick
            }
        }
    }

    private fun removeChosenWildlife() {
        for(i in 0..3) {
            changeAnimalsArray[i] = false
        }

        //Nur zur visualisierung
        testNearOverpopulation()
        animalChoice1.visual =  ImageVisual("tokens/fox.png")
        animalChoice2.visual =  ImageVisual("tokens/fox.png")
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
        animalChoice1.visual = ImageVisual("tokens/bear.png")
        animalChoice2.visual = ImageVisual("tokens/elk.png")
    }



    //Ab hier neu

    private val tileMap = BidirectionalMap<Tile, HexagonViewExtended>()

    private fun createTileView() {
        val game = rootService.currentGame
        checkNotNull(game)

        val tileList = game.tileStack.peekAll().toMutableList()
        tileList.addAll(game.choices.map { it.first })

        for (player in game.playerQueue) {
            tileList.addAll(player.board.values)
        }

        for(tile in tileList) {
            val hexagon = HexagonViewExtended(size = 14.0, visual = ImageVisual("tiles/choices/tile_${tile.id}.png")).apply {
                //Neuerung 06.07
                var inShop = false
                var isPlaced = false
                onMouseClicked = {
                    if(inShop) {
                        chooseTile(shop.indexOf(this) + 1, this)
                    } else if (isPlaced) {
                        //Onclick für Tiere platzieren
                    }
                }
            }
            tileMap.add(tile to hexagon)
        }
    }

    private fun createAnimalView(wildlifeToken: WildlifeToken): ImageVisual {
        var image = when (wildlifeToken) {
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
    }


    override fun refreshAfterStartGame() {
        val game = rootService.currentGame
        checkNotNull(game)

        playerListAtStart = game.playerQueue.toMutableList()

        resetGame()

        createTileView()
        initializeShop()

        for(i in 0..3) {
            val animal = game.choices.elementAt(i).second
            listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).elementAt(i).visual =
                createAnimalView(animal)
        }

        loadStartTiles()
        initializeCamerasOnSide()
        disableAllButtons()
        setNames()

        animateDealTile()
        changeGreyVisibility(true, 0)
        changeGreyVisibility(false, 1)
        changeGreyVisibility(false, 2)
        changeGreyVisibility(false, 3)
    }

    private fun setNames() {
        val game = rootService.currentGame
        checkNotNull(game)

        for(i in 0 until game.playerQueue.size) {
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

    private fun placeChoosenTile() {
        val game = rootService.currentGame
        checkNotNull(game)
        val currentArea = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(player)
        val selectTile = shop[selectTile]
        checkNotNull(selectTile)

        val x = selectedGridX
        val y = selectedGridY
        checkNotNull(x)
        checkNotNull(y)

        val xTemp = currentArea[x,y]?.actualPosX
        val yTemp = currentArea[x,y]?.actualPosY
        checkNotNull(xTemp)
        checkNotNull(yTemp)

        if(animationsEnabled) {
            playAnimation(
                MovementAnimation(
                    componentView = selectTile as ComponentView,
                    toX = xTemp,
                    toY = yTemp,
                    duration = 500
                )
            )
        } else {
            selectTile.posX = xTemp
            selectTile.posY = yTemp
        }

        activatesTileButtons()
        changeGreyVisibility(false, player)
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

    private fun activatesTileButtons() {
        if(isPlayerHuman) {
            listOf(confirm, rotateTileLeft, rotateTileRight).forEach { it.isVisible = true }
        }
    }

    private fun deactivatesTileButtons() {
        listOf(confirm, rotateTileLeft, rotateTileRight).forEach { it.isVisible = false }
    }

    override fun refreshAfterChangeTurn(lastTurn: Boolean) {
        deactivatesTileButtons()
        player++
        showPlayer(player, false, false)
        changeGreyVisibility(true, player)

        if(isHuman()) {
            //activateButtons()
        } else {
            //deactivateAllButtons()
        }

        //refreshShop()       //Game Ende testen wenn TileStack leer ist oder zu wenig animal Tokens
        //saveGameState()
    }

    private fun isHuman(): Boolean {
        val game = rootService.currentGame
        checkNotNull(game)
        return if(game.playerQueue.peek().type == PlayerType.HUMAN) true else false
    }


    //Neuerungen 06.07
    private fun initializeShop() {
        val game = rootService.currentGame
        checkNotNull(game)

        for(i in 0..3) {
            shop[i] = tileMap.forward(game.choices.elementAt(i).first)
            shop[i]?.inShop = true
            addComponents(shop[i] as ComponentView)
        }
    }

    private fun animateDealTile() {
        for(i in 0..3) {
            playAnimation(
                MovementAnimation(
                    componentView = shop[i] as ComponentView,   //Kp warum er da ComponentView haben will und sonst reicht HexagonViewExtended
                    fromX = 1500.0,                             //Hinterher aus dem "TileStack"
                    fromY = 700.0,
                    toX = getShopTileCoordinateX(i),
                    toY = 30.0,
                    duration = 500
                )
            )
            println("Animation")
            println(getShopTileCoordinateX(i))
            println(shop[i].toString())
        }
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

    private fun disableAllButtons() {
        listOf(undo, redo, customChoice, changeWildlife, clearOverpopulation,
            confirm, rotateTileLeft, rotateTileRight).forEach {
                it.isDisabled = true
        }
        deactivateRotateButtons()
    }

    private fun getTileWithAnimal(tile: Tile): ImageVisual {
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

        var rotation = 0

        rotation = when(tile.rotation) {
            0 -> 0
            1 -> 60
            2 -> 120
            3 -> 180
            4 -> 240
            5 -> 300
            else -> throw IllegalArgumentException("Invalid rotation: ${tile.rotation}")
        }

        rotation += when(tile.id) {
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

        return ImageVisual("$path.png", rotation = rotation)
    }

    private fun addGreyHexagon(tileView: HexagonViewExtended, playerIndex: Int = 0) {
        val game = rootService.currentGame
        checkNotNull(game)

        val currentArea = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(player)

        val nbhs = listOf(Pair(1, -1), Pair(1, 0), Pair(0, 1), Pair(-1, 1), Pair(-1, 0), Pair(0, -1))

        val tile = tileMap.backward(tileView)
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

                currentArea[tileViewPos.first + nbh.first, tileViewPos.second + nbh.second] = nbhTileView
            }
        }
    }

    private fun changeGreyVisibility(visible: Boolean, player: Int) {
        listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(player).components.filter { it.choiceHex }.forEach { it.isVisible = visible }
    }


    override fun refreshAfterChangeWildlife(indices: List<Int>) {
        val game = rootService.currentGame
        checkNotNull(game)
        for (i in indices){
            val image = createAnimalView(game.choices.elementAt(i).second)
            listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).elementAt(i).visual = image
        }

    }



    override fun refreshAfterExterminate() {
        refreshAfterChangeWildlife(listOf(0,1,2,3))
    }


    override fun refreshAfterUndo() {
        refreshAfterStartGame()
    }

    override fun refreshAfterSelectColumn(index: Int) {
        placeChoosenTile()
    }

    override fun refreshAfterRedo() {
        refreshAfterStartGame()
    }


    override fun refreshAfterFreeSelection() {
        placeChoosenTile()
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
        putTileInGrid(index.first, index.second)
    }


    override fun refreshAfterPlaceWildlife(index: Triple<Int, Int, Int>) {
        val game = rootService.currentGame
        checkNotNull(game)
        val currentArea = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(player)

        val tile = currentArea[index.first, index.second]
        checkNotNull(tile)
        val image = getTileWithAnimal(tileMap.backward(tile))
        currentArea[index.first, index.second]?.visual = image
        currentArea[index.first, index.second]?.isDisabled = true
    }



    override fun refreshAfterChatMessage(messageSender: String, message: String) {

    }



    override fun refreshAfterUseNatureToken() {
        customChoice.visual = ColorVisual(256, 181, 0).apply {
            style.borderRadius = BorderRadius(10)
        }
        customChoice.font = Font(size = 16, color = Color(0, 0, 0))
        customChoiceActive = true
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

        //Philllip wird hier einen Rechtschreibfehler erwähnen
        val index = playerListAtStart.indexOf(game.playerQueue.peek())
        require(index >= 0 && index < game.playerQueue.size) { "Player-Index out of bounds (GUI)" }
        return index
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
        showPlayer(1,false, false)
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