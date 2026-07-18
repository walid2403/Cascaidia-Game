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
import tools.aqua.bgw.components.gamecomponentviews.HexagonView
import tools.aqua.bgw.components.uicomponents.ComboBox
import tools.aqua.bgw.components.uicomponents.ListView
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.visual.Visual
import kotlin.collections.iterator
import kotlin.math.ceil
import kotlin.math.min

//import java.awt.image.BufferedImage
//import java.io.File
//import javax.imageio.ImageIO

/**
 * Die Klasse GameScene ist die Hauptszene des Cascadia Spiels
 * @param rootService ein Objekt des Typs [RootService], hier wird der RootService übergeben,
 * um auf die Serviceschicht zugreifen zu können
 * @property rootService ein Objekt des Typs [RootService], hier wird der RootService übergeben,
 *  * um auf die Serviceschicht zugreifen zu können
 *  @property selectAnimal ein Objekt des Typs [Int], Index der aktuellen Tierauswahl (-1 = keins ausgewählt)
 *  @property selectTile ein Objekt des Typs [Int], Index der aktuellen Tile Auswahl (-1 = keins ausgewählt)
 *  @property customChoiceActive ein Objekt des Typs [Boolean], ob Custom Auswahl in dem Zug aktiv ist
 *  @property changeWildlifeActive ein Objekt des Typs [Boolean], ob Change Wildlife gerade genutzt wird
 *  @property changeAnimalsArray ein Objekt des Typs [booleanArrayOf], speichert für jedes Tier, ob es getauscht wird
 *  @property player ein Objekt des Typs [Int], speichert den aktuellen Spieler Index (-1 = Rundenanfang)
 *  @property botRotation ein Objekt des Typs [Int], speichert die botRotation (+1 = +60 Grad)
 *  @property fromScoringScene ein Objekt des Typs [Boolean], speichert, ob man die GameScene von der ScoringScene aus
 *  anschaut, um die Boards noch einmal zu sehen
 *  @property animationsEnabled ein Objekt des Typs [Boolean], speichert, ob Animationen eingeschaltet sind
 */
class GameScene(private val app: SopraApplication,private val rootService: RootService) :
    BoardGameScene(1920, 1080), Refreshable {

    private var selectAnimal = -1
    private var selectTile = -1
    private var customChoiceActive = false
    private var changeWildlifeActive = false
    private var changeAnimalsArray = booleanArrayOf(false,false,false,false)
    private var player = -1
    private var botRotation = 0
    var fromScoringScene = false

    private var allButtonsAllowed = true
    var animationsEnabled = false
    private var isPlayerHuman = true
    private var animationSpeed = 1.0

    private var selectedGridX: Int? = null
    private var selectedGridY: Int? = null

    //Liste wird bei refreshAfterStartGame mit der Startreihenfolge befüllt
    private var playerListAtStart = mutableListOf<Player>()

    //Hintergrundbild
    private val logo = Label(posX = 0,posY = 0,width = 1920,height = 1080,visual =
        ImageVisual("backgrounds/CascadiaHintergrund.png"))

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
        //Beim ersten Mal anklicken wird customChoiceActive auf true gesetzt und die Button Farbe ändert sich
        onMouseClicked = {
            val game = rootService.currentGame
            checkNotNull(game)
            if(game.playerQueue.peek().natureTokens > 0) {
                if (!customChoiceActive) {
                    this.visual = ColorVisual(256, 181, 0).apply {
                        style.borderRadius = BorderRadius(10)
                    }
                    this.font = Font(size = 16, color = Color(0, 0, 0))
                    customChoiceActive = true
                    enableShopOnclick()

                    rootService.networkService.sendUseNatureToken()
                }
            }
        }
    }

    private val changeWildlifeButton = Button(
        width = 200, height = 60, posX = 550, posY = 100, text = "Change Wildlife",
        font = Font(size = 16, color = Color(255, 255, 255))).apply {
        visual = ColorVisual(0, 0, 0).apply {
            style.borderRadius = BorderRadius(10)
        }
        onMouseClicked = {
            val game = rootService.currentGame
            checkNotNull(game)
            if(game.playerQueue.peek().natureTokens > 0) {
                //Wenn es gerade aktiviert wird
                if (!changeWildlifeActive) {
                    //Visuelle Änderung am Button
                    this.visual = ColorVisual(256, 181, 0).apply {
                        style.borderRadius = BorderRadius(10)
                    }
                    this.font = Font(size = 16, color = Color(0, 0, 0))
                    this.text = "Remove selection"
                    updateNatureTokenCount(1)

                    //Alle Tiles-OnClicks und Rotate-Buttons deaktivieren und
                    // alle runterskalieren und Auswahl zurücksetzten
                    disableShopOnclick()
                    enableWildlifeInShop(true)
                    disableGreyHexagonOnClicks()
                    scaleDownOtherTiles(-1)
                    scaleDownOtherAnimals(-1)
                    selectTile = -1
                    selectAnimal = -1
                    disableAllTilesOnclick()
                    clearOverpopulationButton.isDisabled = true
                    customChoiceButton.isDisabled = true
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
                    scaleDownOtherAnimals(-1)
                    scaleDownOtherTiles(-1)
                    rootService.playerActionService.changeWildlife(listOf(0, 1, 2, 3).filter {changeAnimalsArray[it]})
                    //println("test button")
                    enableShopOnclick()
                    customChoiceButton.isDisabled = false
                    enableCurrentPlayerTilesOnClick()
                    if(game.playerQueue.peek().type == PlayerType.HUMAN) enableGreyHexagonOnClicks()
                }
                changeWildlifeActive = !changeWildlifeActive
            }
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
            //Button kann nur angeklickt werden, wenn 3 gleiche existieren.
            //Nach onClick werden die Tiere entfernt und der Button wieder durchsichtig
            rootService.gameService.exterminate(true)
            this.visual = ColorVisual(0, 0, 0, 127).apply {
                style.borderRadius = BorderRadius(10)
            }
            this.font = Font(size = 16, color = Color(255, 255, 255, 127))
        }
    }

    //Tannenzapfen Symbole an Buttons
    private val pineCone1 = Label(
        posX = 525,posY = 35,width = 40,height = 40,visual = ImageVisual("tokens/pinecone.png"))
    private val pineCone2 = Label(
        posX = 525,posY = 110,width = 40,height = 40,visual = ImageVisual("tokens/pinecone.png"))


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

    //Bereiche der Spieler
    private val playerOneArea = HexagonGrid<HexagonViewExtended>(
        width = 30, height = 30, posX = 1140, posY = 404,
        coordinateSystem = HexagonGrid.CoordinateSystem.AXIAL,
        orientation = HexOrientation.POINTY_TOP
    )
    private val playerTwoArea = HexagonGrid<HexagonViewExtended>(
        width = 30, height = 30, posX = 1550, posY = 729,
        coordinateSystem = HexagonGrid.CoordinateSystem.AXIAL,
        orientation = HexOrientation.POINTY_TOP
    )
    private val playerThreeArea = HexagonGrid<HexagonViewExtended>(
        width = 30, height = 30, posX = 1000, posY = 854,
        coordinateSystem = HexagonGrid.CoordinateSystem.AXIAL,
        orientation = HexOrientation.POINTY_TOP
    )
    private val playerFourArea = HexagonGrid<HexagonViewExtended>(
        width = 30, height = 30, posX = 590, posY = 529,
        coordinateSystem = HexagonGrid.CoordinateSystem.AXIAL,
        orientation = HexOrientation.POINTY_TOP
    )

    //Buttons unten rechts
    private val endTurn = Button(width = 185, height = 60, posX = 1700, posY = 985, text = "End Turn",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(10) }
        onMouseClicked = {

            val game = rootService.currentGame
            checkNotNull(game)
            if(game.gameState == GameState.PLAYED_TILE || game.gameState == GameState.END_OF_TURN) {
                rootService.gameService.changeTurn()
            }
        }
    }

    private val confirm = Button(width = 120, height = 60, posX = 1350, posY = 985, text = "Confirm",
        font = Font(size = 16, color = Color(255, 255, 255, 255)),
        visual = ColorVisual(0,0, 0).apply {
            style.borderRadius = BorderRadius(10)
        }
    ).apply {
        isVisible = false
        onMouseClicked = {
            val gridX = checkNotNull(selectedGridX)
            val gridY = checkNotNull(selectedGridY)
            val s = ((gridX + gridY) * (-1))
            rootService.playerActionService.placeTile(Triple(s, gridY, gridX))
        }
    }

    private val rotateTileLeft = Button(width = 60, height = 60, posX = 1265, posY = 985, text = "->",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(30) }
        isVisible = false
        onMouseClicked = {
            rootService.playerActionService.rotateTile(true)
        }
    }

    private val rotateTileRight = Button(width = 60, height = 60, posX = 1180, posY = 985, text = "<-",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(30) }
        isVisible = false
        onMouseClicked = {
            rootService.playerActionService.rotateTile(false)
        }
    }

    private var zoomFactor = 4.32

    private val zoomIn = Button(width = 60, height = 60, posX = 1597.5, posY = 985, text = "+",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(30) }
        onMouseClicked = {
            zoomFactor += 0.3
            changeZoom()
        }
    }

    private val zoomOut = Button(width = 60, height = 60, posX = 1512.5, posY = 985, text = "-",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(30) }
        onMouseClicked = {
            zoomFactor -= 0.3
            changeZoom()
        }
    }

    //Buttons unten links
    private val undo = Button(width = 130, height = 60, posX = 35, posY = 985, text = "Undo",
        font = Font(size = 16, color = Color(255, 255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(10) }
        onMouseClicked = {
            if(rootService.history.prevMoves.size > 1) {
                rootService.playerActionService.undo()
            }
        }
    }
    private val redo = Button(width = 130, height = 60, posX = 190, posY = 985, text = "Redo",
        font = Font(size = 16, color = Color(255, 255, 255, 127))).apply {
        visual = ColorVisual(0,0, 0, 127).apply { style.borderRadius = BorderRadius(10) }
        onMouseClicked = {
            if(rootService.history.undoneMoves.isNotEmpty()) {
                rootService.playerActionService.redo()
            }
        }
    }

    private val showScoreScene = Button(
        width = 400, height = 60, posX = 760, posY = 985, text = "Go Back To Scoring Screen",
        font = Font(size = 25, color = Color(255, 255, 255))).apply {
        visual = ColorVisual(0,0, 0).apply { style.borderRadius = BorderRadius(10) }
        onMouseClicked = {
            app.showMenuScene(app.scoreScene)
        }
    }

    private val animationSpeedControl = ComboBox(
        posX = 354,
        posY = 985,
        width = 200,
        height = 60,
        items = listOf(1.0, 0.5, 2.0, 5.0, 100.0),
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
            setPlayerNameAtBottom(0)
        }
        onMouseExited = {
            if(!fromScoringScene) {
                zoomIn.isVisible = true
                zoomOut.isVisible = true
                showOtherPlayer(getPlayerId())
                setPlayerNameAtBottom(null)
            }
        }
    }
    private val lableAboveCamTwo = Label(posX = 55, posY = 295, width = 245, height = 130).apply {
        onMouseEntered = {
            zoomIn.isVisible = false
            zoomOut.isVisible = false
            showOtherPlayer(1)
            setPlayerNameAtBottom(1)
        }
        onMouseExited = {
            if(!fromScoringScene) {
                zoomIn.isVisible = true
                zoomOut.isVisible = true
                showOtherPlayer(getPlayerId())
                setPlayerNameAtBottom(null)
            }
        }
    }
    private val lableAboveCamThree = Label(posX = 55, posY = 525, width = 245, height = 130).apply {
        onMouseEntered = {
            zoomIn.isVisible = false
            zoomOut.isVisible = false
            showOtherPlayer(2)
            setPlayerNameAtBottom(2)
        }
        onMouseExited = {
            if(!fromScoringScene) {
                zoomIn.isVisible = true
                zoomOut.isVisible = true
                showOtherPlayer(getPlayerId())
                setPlayerNameAtBottom(null)
            }
        }
    }
    private val lableAboveCamFour = Label(posX = 55, posY = 755, width = 245, height = 130).apply {
        onMouseEntered = {
            zoomIn.isVisible = false
            zoomOut.isVisible = false
            showOtherPlayer(3)
            setPlayerNameAtBottom(3)
        }
        onMouseExited = {
            if(!fromScoringScene) {
                zoomIn.isVisible = true
                zoomOut.isVisible = true
                showOtherPlayer(getPlayerId())
                setPlayerNameAtBottom(null)
            }
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
            app.showMenuScene(app.pauseMenu)
        }
    }

    //Chat
    private var chatOpened = false
    private var newMessage = false

    private val chatButton = Button(width = 70, height = 60, posX = 1710, posY = 35).apply {
        onMouseClicked = {
            if(!chatOpened) {
                openChat()
            } else {
                closeChat()
            }
        }
    }
    private val chatView = ListView<String>(posX = 1455, posY = 80, width = 240, height = 175,
        visual = ColorVisual(200,200,200).apply { style.borderRadius = BorderRadius(10) },
        font = Font(size = 20))
    private val chatBoxBackground = Label(posX = 1450, posY = 35, width = 250, height = 225,
        visual = ColorVisual(120,120,120).apply { style.borderRadius = BorderRadius(10) })
    private val messageInput = TextField(posX = 1455, posY = 40, width = 195, height = 35,
        visual = ColorVisual(200,200,200).apply { style.borderRadius = BorderRadius(17.5) },
        prompt = "Message:", font = Font(size = 20))
    private val sendButton = Button(posX = 1660, posY = 40, width = 35, height = 35,
        visual = ColorVisual(200,200,200).apply { style.borderRadius = BorderRadius(17.5) },
        text = ">", font = Font(size = 30)).apply {
            onMouseClicked = {
                rootService.networkService.sendChatMessage(messageInput.text)
                refreshAfterChatMessage("Me",
                        messageInput.text)
                messageInput.text = ""
            }
    }

    //Anzeigen über EndTurn Button
    private val roundCounterHex = HexagonView(
        posX = 1723, posY = 780, size = 80,
        visual = ColorVisual(170, 170, 170, 127), orientation = HexOrientation.POINTY_TOP)
    private val roundCounterText = Label(
        posX = 1742, posY = 800, width = 100, height = 40, font = Font(size = 25))
    private val roundCounterLabel1 = Label(
        posX = 1742, posY = 840, width = 100, height = 40, text = "Rounds", font = Font(size = 25))
    private val roundCounterLabel2 = Label(
        posX = 1742, posY = 880, width = 100, height = 40, text = "left", font = Font(size = 25))

    private val tileMap = BidirectionalMap<Tile, HexagonViewExtended>()

    init {
        //Container für CameraPane erstellen
        world.addAll(logo, playerOneArea, playerTwoArea, playerThreeArea, playerFourArea)

        addComponents(
            cameraPane,
            grayBox, customChoiceButton, changeWildlifeButton, clearOverpopulationButton, pineCone1, pineCone2,
            animalChoice1, animalChoice2, animalChoice3, animalChoice4,
            endTurn, undo, redo, viewPanel, confirm, rotateTileLeft, rotateTileRight,
            grayBoxScoringAnimals, pause, bear, elk, salmon, hawk, fox,
            barOne, barTwo, barThree, nameOneSide, nameTwoSide, nameThreeSide, nameFourSide,
            natureTokenOneSide, natureTokenTwoSide, natureTokenThreeSide, natureTokenFourSide,
            natureTokenCountOneSide, natureTokenCountTwoSide, natureTokenCountThreeSide, natureTokenCountFourSide,
            cameraPaneOneSide, cameraPaneTwoSide,
            lableAboveCamOne, lableAboveCamTwo, playerName,
            bearScoringCard, elkScoringCard, salmonScoringCard, hawkScoringCard, foxScoringCard,
            zoomIn, zoomOut,
            tileChoice1, tileChoice2, tileChoice3, tileChoice4,
            animationSpeedControl, roundCounterHex, roundCounterText, roundCounterLabel1, roundCounterLabel2,
            chatButton,
            chatBoxBackground, chatView, messageInput, sendButton, showScoreScene
        )
    }

    private fun initializeCamerasOnSide() {
        cameraPaneOneSide.pan(x = 1140.0, y = 404.0, zoom = 0.8, smooth = true)
        cameraPaneTwoSide.pan(x = 1550.0, y = 729.0, zoom = 0.8, smooth = true)
        cameraPaneThreeSide.pan(x = 1000.0, y = 854.0, zoom = 0.8, smooth = true)
        cameraPaneFourSide.pan(x = 590.0, y = 529.0, zoom = 0.8, smooth = true)
    }

    private fun adjustAreas() {
        playerOneArea.posX = 1140 - playerOneArea.width / 2
        playerOneArea.posY = 394 - playerOneArea.height / 2

        playerTwoArea.posX = 1550 - playerTwoArea.width / 2
        playerTwoArea.posY = 719 - playerTwoArea.height / 2

        playerThreeArea.posX = 1000 - playerThreeArea.width / 2
        playerThreeArea.posY = 844 - playerThreeArea.height / 2

        playerFourArea.posX = 590 - playerFourArea.width / 2
        playerFourArea.posY = 519 - playerFourArea.height / 2
    }

    private fun scaleArea() {
        val game = rootService.currentGame
        checkNotNull(game)
        val currentArea = listOf(playerOneArea,playerTwoArea,playerThreeArea,playerFourArea).elementAt(getPlayerId())

        if(currentArea.height > 134.0 || currentArea.width > 300.0) {
            val scaleX = 134.0 / currentArea.height
            val scaleY = 300.0 / currentArea.width
            val scaleFactor = min(scaleX, scaleY)
            currentArea.scale(scaleFactor)
        }
        adjustAreas()
    }

    private fun zoomOnNextPlayer() {
        val currentPlayerID = getPlayerId()
        val coordinates = getCameraCoordinates(currentPlayerID)

        if(animationsEnabled) {
            cameraPane.pan(x = 0, y = 0, zoom = 1.0, smooth = true)
            playAnimation(
                DelayAnimation(duration = (1000 / animationSpeed).toInt()).apply {
                    onFinished = {
                        cameraPane.pan(x = coordinates.first, y = coordinates.second, zoom = 4.32, smooth = true)
                    }
                }
            )
        } else {
            cameraPane.pan(x = coordinates.first, y = coordinates.second, zoom = 4.32, smooth = false)
        }
    }

    private fun showOtherPlayer(playerID: Int) {
        val coordinates = getCameraCoordinates(playerID)
        cameraPane.pan(x = coordinates.first, y = coordinates.second, zoom = 4.32, smooth = animationsEnabled)
        zoomFactor = 4.32
    }

    private fun zoomOnFirstPlayer() {
        val coordinates = getCameraCoordinates(0)
        cameraPane.pan(x = coordinates.first, y = coordinates.second, zoom = 4.32, smooth = animationsEnabled)
    }

    private fun changeZoom() {
        val currentPlayerID = getPlayerId()
        val coordinates = getCameraCoordinates(currentPlayerID)
        cameraPane.pan(x = coordinates.first+5, y = coordinates.second, zoom = zoomFactor, smooth = animationsEnabled)
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

    private fun openChat() {
        chatOpened = true
        listOf(chatBoxBackground, chatView, messageInput, sendButton).forEach { it.isVisible = true }
        chatButton.visual = ImageVisual("chat_icon.png")
    }

    private fun closeChat() {
        chatOpened = false
        listOf(chatBoxBackground, chatView, messageInput, sendButton).forEach { it.isVisible = false }
    }

    private fun chooseTile(index: Int) {
        if(index !in 0..3) throw IllegalArgumentException("Invalid index given: $index")

        if(selectTile == index) {
            selectTile = -1

        } else {
            selectTile = index
            scaleDownOtherTiles(selectTile)
        }

        adjustShopBasedOnCustomChoiceActive(customChoiceActive, tileShop[index], index)
        rootService.networkService.sendSelectHabitatTile(index)
    }

    private fun adjustShopBasedOnCustomChoiceActive(customChoiceActive: Boolean, tile: Label, index: Int) {
        if(!customChoiceActive) {
            scaleDownOtherAnimals(selectTile)
            scaleTile(selectTile != -1, tile)
            val animal = listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).elementAt(index)
            selectAnimal = selectTile
            scaleAnimal(selectAnimal != -1, animal)
        } else {
            scaleTile(selectTile != -1, tile)
        }

        val player = rootService.currentGame?.playerQueue?.peek()
        checkNotNull(player)

        if (player.type != PlayerType.NETWORK && (rootService.currentGame?.isLocal == false)) {
            rootService.networkService.sendSelectHabitatTile(index)
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

        val player = rootService.currentGame?.playerQueue?.peek()
        checkNotNull(player)

        if (player.type != PlayerType.NETWORK && (rootService.currentGame?.isLocal == false)) {
            rootService.networkService.sendSelectWildlife(index)
        }
    }

    private fun disableAllOnClicks() {
        disableAllTilesOnclick()
        disableShopOnclick()
        disableConfirmRotateButtons()
        disableShopButtons()
    }

    private fun disableAllForNetworkBotTurn() {
        disableAllTilesOnclick()
        disableShopOnclick()
        disableConfirmRotateButtons()
        disableShopButtons()
        disableGreyHexagonOnClicks()
        redo.isDisabled = true
        undo.isDisabled = true
        endTurn.isDisabled = true
        greyAllButtons()
    }

    private fun greyAllButtons() {
        listOf(redo, undo, endTurn, customChoiceButton, changeWildlifeButton, clearOverpopulationButton).forEach {
            it.visual = ColorVisual(0,0, 0, 127).apply { style.borderRadius = BorderRadius(10) }
            it.font = Font(size = 16, color = Color(255, 255, 255, 127))
        }
    }

    private fun makeButtonsBlack() {
        listOf(undo, endTurn, customChoiceButton, changeWildlifeButton).forEach {
            it.visual = ColorVisual(0,0, 0, 256).apply { style.borderRadius = BorderRadius(10) }
            it.font = Font(size = 16, color = Color(255, 255, 255, 256))
        }
    }

    private fun disableOnlineGameFeatures() {
        redo.isDisabled = true
        undo.isDisabled = true
        app.pauseMenu.saveAndExitButton.isDisabled = true
    }

    private fun disableAllTilesOnclick() {
        tileMap.entries.forEach {
            it.second.isDisabled = true
        }
    }

    private fun enableCurrentPlayerTilesOnClick() {
        val game = rootService.currentGame
        checkNotNull(game)
        game.playerQueue.peek().board.forEach { (_, tile) ->
            tileMap.forward(tile).isDisabled = false
        }
    }

    private fun disableShopOnclick() {
        val game = rootService.currentGame
        checkNotNull(game)
        tileShop.forEach { it.isDisabled = true }
        animalShop.forEach { it.isDisabled = true }
    }

    private fun enableShopOnclick() {
        val game = rootService.currentGame
        checkNotNull(game)
        tileShop.forEach { it.isDisabled = false }
        animalShop.forEach { it.isDisabled = false }
    }

    private fun disableConfirmRotateButtons() {
        listOf(confirm, rotateTileLeft, rotateTileRight).forEach { it.isDisabled = true }
    }

    private fun disableShopButtons() {
        listOf(customChoiceButton, changeWildlifeButton, clearOverpopulationButton).forEach { it.isDisabled = true }
    }

    private fun enableShopButtons() {
        listOf(customChoiceButton, changeWildlifeButton).forEach { it.isDisabled = false }
    }

    private fun disableGreyHexagonOnClicks() {
        changeGreyVisibility(false, getPlayerId())
    }

    private fun enableGreyHexagonOnClicks() {
        changeGreyVisibility(true, getPlayerId())
    }

    /**
     * scales down all shop tiles other than the one at the index [select]
     */
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

        if(animationsEnabled) {
            playAnimation(
                ScaleAnimation(
                    componentView = tileLabel,
                    fromScaleX = tileLabel.scaleX,
                    fromScaleY = tileLabel.scaleY,
                    toScaleX = targetScale,
                    toScaleY = targetScale,
                    duration = (300 / animationSpeed).toInt(),
                    persist = true
                )
            )
        } else {
            tileLabel.scale(targetScale)
        }
    }

    private fun scaleAnimal(sizeUp: Boolean, animalView: Label) {
        val targetScale = if(sizeUp) {100.0 / 81.0 } else { 1.0 }

        if(animationsEnabled) {
            playAnimation(
                ScaleAnimation(
                    componentView = animalView,
                    fromScaleX = animalView.scaleX,
                    fromScaleY = animalView.scaleY,
                    toScaleX = targetScale,
                    toScaleY = targetScale,
                    duration = (300 / animationSpeed).toInt(),
                    persist = true
                )
            )
        } else {
            animalView.scale(targetScale)
        }
    }

    private fun rotateInSelection(amount: Int, tile: HexagonViewExtended) {
        val rotation = amount.toDouble() * 60.0

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
    }

    private fun onClickForTiles(tile: HexagonViewExtended) {
        val game = rootService.currentGame
        checkNotNull(game)
        //wenn auf ein graues Randhexagon geklickt wird, wird select tile aufgerufen, sonst place wildlife
        if (tile.visual == ColorVisual(170, 170, 170, 127)) {
            selectedGridX = game.playerQueue.peek().board.entries.find {
                it.value == tileMap.backward(tile)}?.key?.third
            selectedGridY = game.playerQueue.peek().board.entries.find {
                it.value == tileMap.backward(tile)}?.key?.second

            if (customChoiceActive) rootService.playerActionService.freeSelection(selectTile, selectAnimal)
            else rootService.playerActionService.selectColumn(selectTile)

        } else if(game.gameState == GameState.PLAYED_TILE) {
            val tileObject = tileMap.backward(tile)
            if(game.choices.elementAt(game.selectedChoice.second).second in tileObject.possibles) {

                val currentArea =
                    listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(getPlayerId())
                val coordinates = currentArea.getCoordinateMap().entries.first { it.value == tile }.key
                val x = coordinates.first
                val y = coordinates.second
                val s = (x + y) * (-1)
                rootService.playerActionService.placeWildlife(Triple(s, y, x))
            }
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
        listOf(natureTokenCountOneSide, natureTokenCountTwoSide, natureTokenCountThreeSide, natureTokenCountFourSide).
        forEach { it.text = "0" }
        listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).forEach {
            it.clear()
            it.scale(1.0)
        }

        showScoreScene.isVisible = false
        zoomIn.isVisible = true
        zoomOut.isVisible = true
        chatOpened = false
        newMessage = false
        chatView.items.clear()
        listOf(chatBoxBackground, chatView, messageInput, sendButton).forEach { it.isVisible = false }
        messageInput.text = ""
        chatButton.visual = ImageVisual("chat_icon.png")
        selectAnimal = -1
        selectTile = -1
        animationSpeed = 1.0
        animationSpeedControl.selectedItem = animationSpeedControl.items[0]
        customChoiceActive = false
        changeWildlifeActive = false
        changeAnimalsArray = booleanArrayOf(false,false,false,false)
        player = 0
        botRotation = 0

        allButtonsAllowed = true
        animationsEnabled = false
        isPlayerHuman = true
        animationSpeed = 1.0
        fromScoringScene = false

        animalShop.forEach { animal -> animal.visual = Visual.EMPTY }
        tileShop.forEach { tile -> tile.visual = Visual.EMPTY }
        tileMap.clear()

        selectedGridX = null
        selectedGridY = null

        if(requireNotNull(rootService.currentGame).isLocal) {
            chatButton.isVisible = false
            closeChat()
        } else {
            chatButton.isVisible = true
        }
    }

    override fun refreshAfterStartGame() {
        val game = rootService.currentGame
        checkNotNull(game)

        playerListAtStart = game.playerQueue.toMutableList()

        showVisualForEndScreen(true)
        createGame()

        if(!isHuman()) { disableAllForNetworkBotTurn() }
        if(rootService.currentGame?.isLocal == false) { disableOnlineGameFeatures() }
    }

    private fun createGame() {
        val game = rootService.currentGame
        checkNotNull(game)

        redo.isDisabled = false
        undo.isDisabled = false
        endTurn.isDisabled = false
        app.pauseMenu.saveAndExitButton.isDisabled = false

        resetGame()

        makeButtonsBlack()
        createTileView()
        initializeShop()

        loadScoreCards(game.scoringCards)

        initializeCamerasOnSide()
        loadPlayerBoards()
        for(tile in game.playerQueue.peek().board){
            addGreyHexagon(tileMap.forward(tile.value))
        }
        setNames()
        updateRoundCounter()
        setNatureTokenCounts()
        adjustAreas()

        if(game.playerQueue.peek().type == PlayerType.HUMAN) changeGreyVisibility(true, 0)
        else changeGreyVisibility(false, 0)
        changeGreyVisibility(false, 1)
        changeGreyVisibility(false, 2)
        changeGreyVisibility(false, 3)

        zoomOnFirstPlayer()
        checkExterminateButton()
        disableAllOnClicks()

        enableShopButtons()
        enableShopOnclick()

        setPlayerNameAtBottom(null)
        showPlayerAreasAtStart()

        adjustAreas()

        if(!isHuman()) {
            disableAllForNetworkBotTurn()
            if(game.playerQueue.peek().type != PlayerType.NETWORK) {
                rootService.bot.makeTurn(game.playerQueue.peek().type)
            }
        } else {
           checkExterminateButton()
        }
    }

    private fun updateRoundCounter() {
        val game = rootService.currentGame
        checkNotNull(game)

        //+1, weil eigentlich -3 aber 4 Tiles sind im Shop
        val roundsLeft = ceil((game.tileStack.size + 1).toDouble() / game.playerQueue.size.toDouble()).toInt()
        roundCounterText.text = roundsLeft.toString()
    }

    private fun setNames() {
        val game = rootService.currentGame
        checkNotNull(game)

        for(i in game.playerQueue.indices) {
            listOf(nameOneSide, nameTwoSide, nameThreeSide, nameFourSide
            ).elementAt(i).text = game.playerQueue.elementAt(i).name
        }
    }

    private fun setNatureTokenCounts() {
        val game = rootService.currentGame
        checkNotNull(game)

        listOf(natureTokenCountOneSide, natureTokenCountTwoSide, natureTokenCountThreeSide, natureTokenCountFourSide
        ).forEachIndexed { index, tokenCount ->
            if(index < game.playerQueue.size)
                tokenCount.text = game.playerQueue.elementAt(index).natureTokens.toString()
        }
    }

    override fun refreshAfterSelectTile(tileIndex: Int) {
        chooseTile(tileIndex)
    }

    override fun refreshAfterSelectWildlife(wildlifeIndex: Int) {
        chooseAnimal(wildlifeIndex, animalShop.elementAt(wildlifeIndex))
    }

    override fun refreshAfterChatMessage(messageSender: String, message: String) {
        if(!chatOpened) chatButton.visual = ImageVisual("chat_icon_redDot.png")
        if(message.isNotBlank() && messageSender.isNotBlank()) {
            chatView.items.add(0, "$messageSender: $message")
        }
    }

    private fun loadPlayerBoards() {
        val game = rootService.currentGame
        checkNotNull(game)

        listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).forEachIndexed { index, grid ->
            if(index < game.playerQueue.size) {
                val playerBoard = game.playerQueue.elementAt(index).board
                playerBoard.entries.forEach { placedTile ->

                    val tileCoordinates: Triple<Int, Int, Int> = placedTile.key
                    val gridCoordinates = convertCoordinates(tileCoordinates)

                    val tile = playerBoard[tileCoordinates]
                    checkNotNull(tile)
                    val hexView = tileMap.forward(tile)

                    grid[gridCoordinates.first, gridCoordinates.second] = hexView

                    if(tile.occupant != null) {
                        hexView.visual = getTileWithAnimal(tile)
                    }

                }
            }
        }
    }

    private fun showPlayerAreasAtStart() {
        val game = rootService.currentGame
        checkNotNull(game)
        val playerCount = game.playerQueue.size

        //Erstmal für alle wieder freischalten (falls neues Spiel in selber Szene)
        if(this.components.none { it == cameraPaneThreeSide }) this.addComponents(cameraPaneThreeSide)
        if(this.components.none { it == cameraPaneFourSide }) this.addComponents(cameraPaneFourSide)
        if(this.components.none { it == lableAboveCamFour }) this.addComponents(lableAboveCamFour)
        if(this.components.none { it == lableAboveCamThree }) this.addComponents(lableAboveCamThree)
        listOf(cameraPaneThreeSide, cameraPaneFourSide, lableAboveCamFour, lableAboveCamThree, nameThreeSide,
            nameFourSide, natureTokenThreeSide, natureTokenFourSide, natureTokenCountThreeSide,
            natureTokenCountFourSide, barTwo, barThree).forEach { it.isVisible = true }
        viewPanel.height = 912.0

        //Danach die nicht gebrauchten ausblenden
        if(playerCount < 4) {
            this.removeComponents(cameraPaneFourSide)
            this.removeComponents(lableAboveCamFour)
            listOf(cameraPaneFourSide, nameFourSide, natureTokenFourSide, natureTokenCountFourSide, barThree).forEach{
                it.isVisible = false
            }
            viewPanel.height = 684.0
        }

        if(playerCount < 3) {
            this.removeComponents(cameraPaneThreeSide)
            this.removeComponents(lableAboveCamThree)
            listOf(cameraPaneThreeSide, nameThreeSide,natureTokenThreeSide,natureTokenCountThreeSide,barTwo).forEach{
                it.isVisible = false
            }
            viewPanel.height = 456.0
        }
    }

    //Wandelt Koordinaten der Serviceschicht um
    private fun convertCoordinates(serviceCoordinates: Triple<Int, Int, Int>): Pair<Int, Int> {
        val x = serviceCoordinates.third
        val y = serviceCoordinates.second
        return Pair(x, y)
    }

    /**
     * Die Methode blendet alle Sachen in der GameScene aus, außer die Boards der Spieler und die Minimap.
     * Die wird aufgerufen, wenn man von der ScoreScene nochmal in die GameScene möchte, um die Spielfelder zu sehen
     */
    fun showVisualForEndScreen(visible: Boolean) {
        fromScoringScene = true
        listOf(customChoiceButton, changeWildlifeButton, clearOverpopulationButton, pineCone1, pineCone2, grayBox,
            chatBoxBackground, chatView, chatButton, messageInput, sendButton,
            zoomIn, zoomOut, undo, redo, animationSpeedControl, endTurn, playerName, pause,
            roundCounterHex, roundCounterText, roundCounterLabel1, roundCounterLabel2).forEach{it.isVisible = visible}
        tileShop.forEach { it.isVisible = visible }
        animalShop.forEach { it.isVisible = visible }
        showScoreScene.isVisible = true
    }


    private fun placeChosenTile() {
        val game = rootService.currentGame
        checkNotNull(game)

        val playerID = getPlayerId()
        val currentArea = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(playerID)

        val chosenTile = tileShop[selectTile]
        scaleTile( false, chosenTile)

        val x = selectedGridX
        val y = selectedGridY
        checkNotNull(x)
        checkNotNull(y)
        val greyTile = currentArea[x,y]
        checkNotNull(greyTile)

        greyTile.visual = chosenTile.visual
        chosenTile.isVisible = false
        tileMap.add(game.choices[selectTile].first to greyTile)

        //deaktiviere nach dem Legen den Shop und alle Shop Buttons
        enableWildlifeInShop(false)
        customChoiceButton.isDisabled = true
        changeWildlifeButton.isDisabled = true
        clearOverpopulationButton.isDisabled = true

        activatesTileButtons()
        changeGreyVisibility(false, playerID, greyTile)

        greyTile.isDisabled = false
        greyTile.onMouseClicked = { onClickForTiles(greyTile) }
        greyTile.choiceHex = false

        confirm.isDisabled = false
    }

    /**
     * This function activates the confirmButton, rotateTileLeftButton and rotateTileRightButton
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
     * This function deactivates the confirmButton, rotateTileLeftButton and rotateTileRightButton
     */
    private fun deactivateTileButtons() {
        listOf(confirm, rotateTileLeft, rotateTileRight).forEach { it.isVisible = false }
    }

    private fun setPlayerNameAtBottom(nameIndex: Int?) {
        val game = rootService.currentGame
        checkNotNull(game)
        if(nameIndex == null) playerName.text = game.playerQueue.peek().name
        else playerName.text = listOf(nameOneSide, nameTwoSide, nameThreeSide, nameFourSide).elementAt(nameIndex).text
    }

    private fun resetCustomChoice() {
        customChoiceButton.font = Font(size = 16, color = Color(255, 255, 255, 255)).apply {
            customChoiceButton.visual = ColorVisual(0, 0, 0).apply {
                style.borderRadius = BorderRadius(10)
            }
        }
    }

    override fun refreshAfterBotTurn(coordinatesTile: Triple<Int, Int, Int>,
                                     coordinatesWildlife: Triple<Int?, Int?, Int?>) {
        val game = rootService.currentGame
        checkNotNull(game)
        val playerID = getPlayerId()
        val currentNatureTokenCount = listOf(natureTokenCountOneSide, natureTokenCountTwoSide,
            natureTokenCountThreeSide, natureTokenCountFourSide).elementAt(playerID)
        selectTile = game.selectedChoice.first
        selectAnimal = game.selectedChoice.second

        tileShop.elementAt(selectTile).isVisible = true
        animalShop.elementAt(selectAnimal).isVisible = true

        println("Start of Bot Turn '"+game.playerQueue.peek().name+"' Turn:")
        println("Selected Tile (ID): "+game.selectedChoice.first.toString())
        println("Selected Animal (ID): "+game.selectedChoice.second.toString())

        customChoiceActive = selectAnimal != selectTile
        if(customChoiceActive) {
            customChoiceButton.visual = ColorVisual(256, 181, 127).apply {
                style.borderRadius = BorderRadius(10)
            }
            customChoiceButton.font = Font(size = 16, color = Color(0, 0, 0, 127))
        }

        playAnimation(
            DelayAnimation(duration = (2000 / animationSpeed).toInt()).apply {
                onFinished = {
                    placeTileBot(coordinatesTile, tileShop.elementAt(game.selectedChoice.first))
                    playAnimation(
                        DelayAnimation(duration = (2000 / animationSpeed).toInt()).apply {
                            onFinished = {
                                if(coordinatesWildlife.third != null) {
                                    val coordinatesNotNull = Triple(requireNotNull(coordinatesWildlife.first),
                                        requireNotNull(coordinatesWildlife.second),
                                        requireNotNull(coordinatesWildlife.third))
                                    placeWildLifeBot(coordinatesNotNull)
                                    currentNatureTokenCount.text = game.playerQueue.peek().natureTokens.toString()
                                } else {
                                    println("Reject Wildlife")
                                }
                                playAnimation(
                                    DelayAnimation(duration = (2000 / animationSpeed).toInt()).apply {
                                        onFinished = {
                                            println("End Bot '"+game.playerQueue.peek().name+"' Turn")
                                            println("-----------------------------")
                                            rootService.gameService.changeTurn()
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }
        )
    }

    private fun placeTileBot(index: Triple<Int, Int, Int>, chosenTile: Label) {
        println("Place Tile at: "+index.third.toString()+", "+index.second.toString())

        val game = rootService.currentGame
        checkNotNull(game)
        val playerID = getPlayerId()
        val currentArea = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(playerID)

        val x = index.third
        val y = index.second
        val greyTile = currentArea[x,y]
        checkNotNull(greyTile)

        greyTile.visual = chosenTile.visual
        greyTile.rotation = 60.0* requireNotNull(game.playerQueue.peek().board[Triple(((x + y)*(-1)), y, x)]?.rotation)
        chosenTile.isVisible = false
        tileMap.add(game.choices[game.selectedChoice.first].first to greyTile)
        greyTile.choiceHex = false
        greyTile.isVisible = true
    }

    private fun placeWildLifeBot(index: Triple<Int, Int, Int>) {
        println("Place Wildlife at: "+index.third.toString()+", "+index.second.toString())
        placeTile(index)
    }

    private fun placeTile(index: Triple<Int, Int, Int>) {
        val game = rootService.currentGame
        checkNotNull(game)

        val currentArea = listOf(playerOneArea, playerTwoArea, playerThreeArea,playerFourArea).elementAt(getPlayerId())

        val tile = currentArea[index.third, index.second]
        checkNotNull(tile)
        val newVisual = getTileWithAnimal(tileMap.backward(tile))
        tile.visual = newVisual
        tile.isDisabled = true

        animalShop[game.selectedChoice.second].visual = Visual.EMPTY

        updateNatureTokenCount()
    }

    override fun refreshAfterChangeTurn(lastTurn: Boolean) {
        val game = rootService.currentGame
        checkNotNull(game)

        if(game.playerQueue.elementAt(game.playerQueue.size-1).type == PlayerType.NETWORK) {
            playAnimation(
                DelayAnimation(duration = (1000 / animationSpeed).toInt()).apply {
                    onFinished = { changeTurn() }
                }
            )
        } else {
            changeTurn()
        }
    }

    private fun changeTurn() {
        val game = rootService.currentGame
        checkNotNull(game)

        redo.isDisabled = false
        undo.isDisabled = false
        endTurn.isDisabled = false
        app.pauseMenu.saveAndExitButton.isDisabled = false
        makeButtonsBlack()

        botRotation = 0
        disableAllTilesOnclick()
        scaleArea()

        deactivateTileButtons()
        player = (player + 1)%game.playerQueue.size

        customChoiceActive = false
        resetCustomChoice()

        zoomOnNextPlayer()
        if(game.playerQueue.peek().type == PlayerType.HUMAN) changeGreyVisibility(true, player)

        customChoiceButton.isDisabled = game.playerQueue.peek().natureTokens == 0

        enableShopButtons()
        enableShopOnclick()
        checkRemoveWildlifeButton()
        checkExterminateButton()
        setPlayerNameAtBottom(null)

        if (selectTile != -1) {
            tileShop[selectTile].visual = getShopVisual(selectTile)
            moveLabelToShop(selectTile)

            animalShop[selectAnimal].visual = createAnimalView(game.choices[selectAnimal].second)
            scaleAnimal(false, animalShop[selectAnimal])
        } else {
            for (i in 0..3) {
                tileShop[i].visual = getShopVisual(i)
                moveLabelToShop(i)

                animalShop[i].visual = createAnimalView(game.choices[i].second)
                scaleAnimal(false, animalShop[i])
            }
        }

        selectTile = -1
        selectAnimal = -1
        for(tile in game.playerQueue.peek().board){
            addGreyHexagon(tileMap.forward(tile.value))
        }

        initializeCamerasOnSide()

        tileShop.forEach { scaleTile(false, it) }
        animalShop.forEach { scaleAnimal(false, it) }

        if(!isHuman()) {
            disableAllForNetworkBotTurn()
            if(game.playerQueue.peek().type != PlayerType.NETWORK) {
                rootService.bot.makeTurn(game.playerQueue.peek().type)
            }
        }

        updateRoundCounter()
        if(rootService.currentGame?.isLocal == false) { disableOnlineGameFeatures() }
    }

    private fun checkRedoButton() {
        if(rootService.history.undoneMoves.isNotEmpty()) {
            redo.isDisabled = false
            redo.visual = ColorVisual(0,0,0,256).apply { style.borderRadius = BorderRadius(10) }
            redo.font = Font(size = 16, color = Color(255, 255, 255, 256))
        } else {
            redo.isDisabled = true
            redo.visual = ColorVisual(0,0,0,127).apply { style.borderRadius = BorderRadius(10) }
            redo.font = Font(size = 16, color = Color(255, 255, 255, 127))
        }
    }

    /**
     * This function moves the Tile Label corresponding to the [shopIndex] back into its position in the shop and makes
     * it visible again
     */
    private fun moveLabelToShop(shopIndex: Int) {
        var label: Label
        var posX: Double
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

        if(animationsEnabled) {
            playAnimation(
                FadeAnimation(
                    label,
                    toOpacity = 1.0,
                    //duration = (1000/animationSpeed).toInt(),
                    duration = 1000
                ).apply {
                    onFinished = {
                        label.isVisible = true
                        //println("label was moved")
                    }
                }
            )
        } else {
            label.isVisible = true
        }
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

        for(i in 0..3) {
            val animal = game.choices.elementAt(i).second
            listOf(animalChoice1, animalChoice2, animalChoice3, animalChoice4).elementAt(i).visual =
                createAnimalView(animal)
        }
    }

    private fun getShopVisual(shopIndex: Int): ImageVisual {
        val game = rootService.currentGame
        checkNotNull(game)

        val tileID = game.choices[shopIndex].first.id
        return ImageVisual("tiles/choices/tile_$tileID.png")
    }

    private fun getTileWithAnimal(tile: Tile): ImageVisual {
        var path = "tilesWithWildlife/"

        path += getHabitatPath(tile.id)

        path += getPlacedWildlifePath(tile.occupant)

        val rotation = adjustForStartTileRotation(tile.id)

        return ImageVisual("$path.png", rotation = rotation)
    }

    private fun getHabitatPath(id: Int): String {
        return when(id) {
            0,1,2,3,4,5,420 -> "PF"
            6,7,8,9,10,16,120 -> "WF"
            22,23,24,25,26,27,510 -> "WP"
            39,40,41,42,43,44,320 -> "RW"
            45,46,47,48,49,50,520 -> "MR"
            55,58,59,60,61,62,220 -> "RF"
            56,57,64,65,66,67,110 -> "RP"
            33, 75,76,77,78,81,410 -> "WM"
            52,53,54,73,74,79,210 -> "PM"
            51,63, 80,82,83,84,310 -> "FM"
            11,12,13,14,15,500,17,18,19,20,21,200,28,29,30,31,32,300,34,35,36,37,38,400,68,69,70,71,72, 100 ->
                getSingleHabitatPath(id)

            else -> throw IllegalArgumentException("Invalid tile id: $id")
        }
    }

    private fun getSingleHabitatPath(id: Int): String {
        return when(id) {
            11,12,13,14,15,500 -> "F"
            17,18,19,20,21,200 -> "W"
            28,29,30,31,32,300 -> "P"
            34,35,36,37,38,400 -> "R"
            68,69,70,71,72,100 -> "M"
            else -> throw IllegalArgumentException("Invalid tile id: $id")
        }
    }

    private fun getPlacedWildlifePath(wildlife: WildlifeToken?): String {
        return if(wildlife != null) {
            when(wildlife) {
                WildlifeToken.ELK -> "_E"
                WildlifeToken.FOX -> "_F"
                WildlifeToken.BEAR -> "_B"
                WildlifeToken.HAWK -> "_H"
                WildlifeToken.SALMON -> "_S"
            }
        } else {
            throw IllegalArgumentException("Tile has no occupant!")
        }
    }

    private fun adjustForStartTileRotation (id: Int): Int {
        return when(id) {
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
    }

    private fun addGreyHexagon(tileView: HexagonViewExtended, playerIndex: Int = 0) {
        val game = rootService.currentGame
        checkNotNull(game)

        val currentArea = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(player)

        val neighborHexagons = listOf(Pair(1, -1), Pair(1, 0), Pair(0, 1), Pair(-1, 1), Pair(-1, 0), Pair(0, -1))

        val tile = tileMap.backward(tileView)
        val tilePos = game.playerQueue.elementAt(playerIndex).board.entries.find { it.value == tile }?.key
        checkNotNull(tilePos)
        val tileViewPos = Pair(tilePos.third, tilePos.second)

        neighborHexagons.forEach { neighborHexagon ->
            var neighborHexagonTileView =
                currentArea[tileViewPos.first + neighborHexagon.first, tileViewPos.second + neighborHexagon.second]
            if (neighborHexagonTileView == null) {
                neighborHexagonTileView = HexagonViewExtended(tileView.size, ColorVisual(170, 170, 170, 0.8))
                neighborHexagonTileView.apply {
                    onMouseClicked = {
                        greyHexagonOnClick(tileViewPos, neighborHexagon)

//                        if(selectTile != -1 && selectAnimal != -1) {
//                            selectedGridX = tileViewPos.first + neighborHexagon.first
//                            selectedGridY = tileViewPos.second + neighborHexagon.second
//
//                            if (customChoiceActive) rootService.playerActionService.freeSelection(
//                                selectTile,
//                                selectAnimal
//                            )
//                            else rootService.playerActionService.selectColumn(selectTile)
//                        }
                    }
                }

                neighborHexagonTileView.choiceHex = true
                currentArea[tileViewPos.first + neighborHexagon.first,
                    tileViewPos.second + neighborHexagon.second] = neighborHexagonTileView
            }
        }
    }

    private fun greyHexagonOnClick(tileViewPos: Pair<Int, Int>, neighborHexagon: Pair<Int,Int>) {
        if(selectTile != -1 && selectAnimal != -1) {
            selectedGridX = tileViewPos.first + neighborHexagon.first
            selectedGridY = tileViewPos.second + neighborHexagon.second

            if (customChoiceActive) rootService.playerActionService.freeSelection(
                selectTile,
                selectAnimal
            )
            else rootService.playerActionService.selectColumn(selectTile)
        }
    }

    private fun changeGreyVisibility(visible: Boolean, player: Int, keepVisibleException: HexagonViewExtended? = null){
        listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(player).components.filter {
            it.choiceHex }.forEach {
                if(keepVisibleException == null || it != keepVisibleException) {
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

        disableAllTilesOnclick()
        checkRemoveWildlifeButton()
        checkExterminateButton()
    }

    private fun checkRemoveWildlifeButton() {
        val game = rootService.currentGame
        checkNotNull(game)

        changeWildlifeButton.isDisabled = true

        if (game.gameState !in listOf(GameState.START_OF_TURN, GameState.HAS_EXTERMINATED)) return

        if (game.playerQueue.peek().natureTokens < 1) return

        changeButtonVisuals(changeWildlifeButton)
    }

    private fun checkExterminateButton() {
        val game = rootService.currentGame
        checkNotNull(game)

        clearOverpopulationButton.isDisabled = true

        if (game.gameState != GameState.START_OF_TURN) return

        val choices = game.choices.map {it.second}

        if (choices.distinct().size != 2) return

        if (choices.count {it == choices[0]} != 3 && choices.count {it == choices[1]} != 3) return

        changeButtonVisuals(clearOverpopulationButton)
    }

    private fun changeButtonVisuals(button: Button) {
        val game = rootService.currentGame
        checkNotNull(game)

        if(game.playerQueue.peek().type == PlayerType.HUMAN) {
            button.visual = ColorVisual(0, 0, 0, 255).apply {
                style.borderRadius = BorderRadius(10)
            }
            button.font = Font(size = 16, color = Color(255, 255, 255, 255))
            button.isDisabled = false
        }
    }

    override fun refreshAfterExterminate() {
        selectTile = -1
        selectAnimal = -1
        scaleDownOtherTiles(selectTile)
        scaleDownOtherAnimals(selectAnimal)

        refreshAfterChangeWildlife(listOf(0,1,2,3))
    }

    override fun refreshAfterUndo() {
        createGame()
        checkRedoButton()
    }

    override fun refreshAfterSelectColumn(index: Int) {
        if(!isHuman()) {
            selectTile = index
            selectAnimal = index
            disableGreyHexagonOnClicks()
            scaleAnimal(true, animalShop[index])
            scaleTile(true, tileShop[index])
        } else {
            placeChosenTile()
            disableShopOnclick()
            disableGreyHexagonOnClicks()
            disableShopButtons()
            disableAllTilesOnclick()
        }

    }

    override fun refreshAfterRedo() {
        refreshAfterStartGame()
        checkRedoButton()
    }

    override fun refreshAfterFreeSelection() {
        if (!isHuman()) {
            val game = rootService.currentGame
            checkNotNull(game)
            selectTile = game.selectedChoice.first
            selectAnimal = game.selectedChoice.second
            disableGreyHexagonOnClicks()
            scaleAnimal(true, animalShop[selectAnimal])
            scaleTile(true, tileShop[selectTile])
        } else {
            placeChosenTile()
            disableShopOnclick()
            disableGreyHexagonOnClicks()
            disableShopButtons()
            disableAllTilesOnclick()
        }
    }

    override fun refreshAfterLoadGame() {
        refreshAfterStartGame()
    }

    override fun refreshAfterRotate(amount: Int) {
        if(!isHuman()) {
            botRotation += amount
        } else {
            val game = rootService.currentGame
            checkNotNull(game)
            rotateInSelection(amount, tileMap.forward(game.choices.elementAt(game.selectedChoice.first).first))
        }
    }

    override fun refreshAfterPlaceTile(index: Triple<Int, Int, Int>) {
        val game = rootService.currentGame
        checkNotNull(game)

        if(game.playerQueue.peek().type == PlayerType.NETWORK) {
            println("Place Tile: " + index.third.toString() + ", " + index.second.toString())
            val game = rootService.currentGame
            checkNotNull(game)


            val chosenTile = tileShop[game.selectedChoice.first]
            val playerID = getPlayerId()
            val currentArea = listOf(playerOneArea, playerTwoArea, playerThreeArea, playerFourArea).elementAt(playerID)

            val hexagon = HexagonViewExtended(currentArea[0,0]?.size ?: 14.0, chosenTile.visual)
            hexagon.rotation = (game.playerQueue.peek().board[index]?.rotation ?: 0) * 60.0
            currentArea[index.third, index.second] = hexagon

            chosenTile.isVisible = false
            tileMap.add(game.choices[game.selectedChoice.first].first to hexagon)
            hexagon.choiceHex = false
        }

        if(isHuman()) {
            confirm.isVisible = false
            rotateTileRight.isVisible = false
            rotateTileLeft.isVisible = false
            enableCurrentPlayerTilesOnClick()
        }
    }


    override fun refreshAfterPlaceWildlife(index: Triple<Int, Int, Int>) {
        val game = rootService.currentGame
        checkNotNull(game)

        if(isHuman() || game.playerQueue.peek().type == PlayerType.NETWORK) {
            placeTile(index)
        }
    }

    private fun updateNatureTokenCount(removeManually: Int = 0) {
        val game = rootService.currentGame
        checkNotNull(game)
        val tokenCount = listOf(natureTokenCountOneSide, natureTokenCountTwoSide,
            natureTokenCountThreeSide, natureTokenCountFourSide).elementAt(player)

        val newCount = game.playerQueue.peek().natureTokens - removeManually
        tokenCount.text = "$newCount"
    }

    override fun refreshAfterUnlockSelection() {
        customChoiceButton.visual = ColorVisual(256, 181, 0,256).apply {
            style.borderRadius = BorderRadius(10)
        }
        customChoiceButton.font = Font(size = 16, color = Color(0, 0, 0, 256))
        customChoiceActive = true
        updateNatureTokenCount()
    }

    /**
     * Die Methode gibt den Index des aktuellen Spielers zurück, bezogen auf die Startreihenfolge.
     * Sollte ein Spieler das Spiel verlassen wird dieser nicht weiter berücksichtigt.
     * Es gibt eine Fehlermeldung, falls der Index nicht in der Range der Queue liegt.
     * @return Gibt den Index des Spielers zurück
     * */
    private fun getPlayerId(): Int {
        val game = rootService.currentGame
        checkNotNull(game)
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







//    private fun renderImage(tileName: String, tokenName: String, tileRotation: Int): String {
//        val background = ImageIO.read(File("tiles/clear/$tileName.png"))
//        val foreground = ImageIO.read(File("tokens/$tokenName.png"))
//
//        val result = BufferedImage(
//            background.width,
//            background.height,
//            BufferedImage.TYPE_INT_RGB
//        )
//
//        val g = result.createGraphics()
//
//        g.drawImage(background, 0, 0, background.width, background.height, null)
//
//        val angle = Math.toRadians(60.0 * tileRotation.toDouble())
//
//        val x = 50 // Abstand vom linken Rand des Hintergrundbildes
//        val y = 50 // Abstand vom oberen Rand des Hintergrundbildes
//
//        val centerX = x + foreground.width / 2.0
//        val centerY = y + foreground.height / 2.0
//
//        g.rotate(angle, centerX, centerY)
//        g.drawImage(foreground, x, y, foreground.width * 1, foreground.height * 1, null)
//
//        g.dispose()
//
//        val tmpFile = File.createTempFile("cascadia_renderedTile_", ".png")
//        ImageIO.write(result, "png", tmpFile)
//
//        return tmpFile.absolutePath
//    }

}