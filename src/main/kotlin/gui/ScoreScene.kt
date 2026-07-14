package gui

import service.Refreshable
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual
import tools.aqua.bgw.components.layoutviews.GridPane

/**
 * This scene displays the players' scores at the end of the game. The player can flip between seeing the total score
 * ranking, the habitat score table and the wildlife score table. The player can go back to the [MainMenuScene] to
 * start a new game or exit the application entirely from here.
 * @param app The [SopraApplication] of this game
 */
class ScoreScene(private val app: SopraApplication) : MenuScene(1920, 1080), Refreshable {

    private val sceneWidth = 1920
    private val sceneHeight = 1080
    private val paneWidth = 800
    private val paneHeight = 670
    private val tabHeight = 125
    private val tabWidth = 100
    private val borderThickness = 4
    private val paneX = (sceneWidth-paneWidth)/2
    private val paneY = (sceneHeight-paneHeight)/2
    private var playerNum = 4

    //
    // All panes and tabs
    //

    private val backgroundImage = Label(
        posX = 0,
        posY = 0,
        width = sceneWidth,
        height = sceneHeight,
        visual = ImageVisual("backgrounds/ScoreSceneBackground.png")
    )

    private val mainScorePane = Pane<StaticComponentView<*>>(
        posX = paneX,
        posY = paneY,
        width = paneWidth,
        height = paneHeight,
        visual = ColorVisual(64, 98, 70).apply{
            style.borderRadius = BorderRadius(35)
        }
    )

    private val wildlifeScorePane = Pane<StaticComponentView<*>>(
        posX = paneX,
        posY = paneY,
        width = paneWidth,
        height = paneHeight,
        visual = ColorVisual(236, 142, 14).apply{
            style.borderRadius = BorderRadius(35)
        }
    ).apply {
        isVisible = false
    }

    private val habitatScorePane = Pane<StaticComponentView<*>>(
        posX = paneX,
        posY = paneY,
        width = paneWidth,
        height = paneHeight,
        visual = ColorVisual(173, 208, 75).apply{
            style.borderRadius = BorderRadius(35)
        }
    ).apply {
        isVisible = false
    }

    private val paneBorder = Label(
        posX = paneX - borderThickness,
        posY = paneY - borderThickness,
        width = paneWidth + borderThickness*2,
        height = paneHeight + borderThickness*2,
        visual = ColorVisual(255, 255, 255).apply{
            style.borderRadius = BorderRadius(35)
        }
    )

    private val mainScoreTab = Label(
        posX = paneX + paneWidth - 30,
        posY = paneY,
        width = tabWidth,
        height = tabHeight,
        visual = ColorVisual(64, 98, 70).apply {
            style.borderRadius = BorderRadius(15.0)
        }
    ). apply {
        onMouseClicked = {
            wildlifeScorePane.isVisible = false
            wildlifeScoreTabOverlay.isVisible = false
            habitatScorePane.isVisible = false
            habitatScoreTabOverlay.isVisible = false
            mainScorePane.isVisible = true
            mainScoreTabOverlay.isVisible = true
        }
    }

    private val mainScoreTabOverlay = Label (
        posX = paneX + paneWidth - 30,
        posY = paneY,
        width = borderThickness + 30,
        height = tabHeight,
        visual = ColorVisual(64, 98, 70)
    )

    private val mainTabBorder = Label(
        posX = mainScoreTab.posX - borderThickness,
        posY = mainScoreTab.posY - borderThickness,
        width = tabWidth + borderThickness*2,
        height = tabHeight + borderThickness*2,
        visual = ColorVisual(255, 255, 255).apply{
            style.borderRadius = BorderRadius(15)
        }
    )

    private val wildlifeScoreTab = Label(
        posX = paneX + paneWidth - 30,
        posY = paneY + tabHeight + borderThickness*2,
        width = tabWidth,
        height = tabHeight,
        visual = ColorVisual(236, 142, 14).apply {
            style.borderRadius = BorderRadius(15.0)
        }
    ). apply {
        onMouseClicked = {
            wildlifeScorePane.isVisible = true
            wildlifeScoreTabOverlay.isVisible = true
            habitatScorePane.isVisible = false
            habitatScoreTabOverlay.isVisible = false
            mainScorePane.isVisible = false
            mainScoreTabOverlay.isVisible = false
        }
    }

    private val wildlifeScoreTabOverlay = Label (
        posX = wildlifeScorePane.posX + paneWidth,
        posY = paneY + tabHeight + borderThickness*2,
        width = borderThickness,
        height = tabHeight,
        visual = ColorVisual(236, 142, 14)
    ).apply {
        isVisible = false
    }

    private val wildlifeTabBorder = Label(
        posX = wildlifeScoreTab.posX - borderThickness,
        posY = wildlifeScoreTab.posY - borderThickness,
        width = tabWidth + borderThickness*2,
        height = tabHeight + borderThickness*2,
        visual = ColorVisual(255, 255, 255).apply{
            style.borderRadius = BorderRadius(15)
        }
    )

    private val habitatScoreTab = Label(
        posX = paneX + paneWidth - 30,
        posY = paneY + tabHeight * 2 + borderThickness * 4,
        width = tabWidth,
        height = tabHeight,
        visual = ColorVisual(173, 208, 75).apply {
            style.borderRadius = BorderRadius(15.0)
        }
    ). apply {
        onMouseClicked = {
            wildlifeScorePane.isVisible = false
            wildlifeScoreTabOverlay.isVisible = false
            habitatScorePane.isVisible = true
            habitatScoreTabOverlay.isVisible = true
            mainScorePane.isVisible = false
            mainScoreTabOverlay.isVisible = false
        }
    }

    private val habitatScoreTabOverlay = Label (
        posX = paneX + paneWidth,
        posY = paneY + tabHeight * 2 + borderThickness * 4,
        width = borderThickness,
        height = tabHeight,
        visual = ColorVisual(173, 208, 75)
    ).apply {
        isVisible = false
    }

    private val habitatTabBorder = Label(
        posX = habitatScoreTab.posX - borderThickness,
        posY = habitatScoreTab.posY - borderThickness,
        width = tabWidth + borderThickness*2,
        height = tabHeight + borderThickness*2,
        visual = ColorVisual(255, 255, 255).apply{
            style.borderRadius = BorderRadius(15)
        }
    )

    //
    // Components for the mainScorePane
    //

    private val firstPlaceName = Label(
        posX = 80,
        posY = 150,
        width = 540,
        height = 68,
        font = Font(size = 30),
        visual = ColorVisual(181, 181, 181).apply {
            style.borderRadius = BorderRadius(34)
        }
    )

    private val secondPlaceName = Label(
        posX = 80,
        posY = 240,
        width = 540,
        height = 68,
        font = Font(size = 30),
        visual = ColorVisual(181, 181, 181).apply {
            style.borderRadius = BorderRadius(34)
        }
    )

    private val thirdPlaceName = Label(
        posX = 80,
        posY = 330,
        width = 540,
        height = 68,
        font = Font(size = 30),
        visual = ColorVisual(181, 181, 181).apply {
            style.borderRadius = BorderRadius(34)
        }
    )

    private val fourthPlaceName = Label(
        posX = 80,
        posY = 420,
        width = 540,
        height = 68,
        font = Font(size = 30),
        visual = ColorVisual(181, 181, 181).apply {
            style.borderRadius = BorderRadius(34)
        }
    )

    private val firstPlaceScoreTotal = Label(
        posX = 660,
        posY = 150,
        width = 68,
        height = 68,
        font = Font(size = 30),
        visual = ColorVisual(181, 181, 181).apply {
            style.borderRadius = BorderRadius(34)
        }
    )

    private val secondPlaceScoreTotal = Label(
        posX = 660,
        posY = 240,
        width = 68,
        height = 68,
        font = Font(size = 30),
        visual = ColorVisual(181, 181, 181).apply {
            style.borderRadius = BorderRadius(34)
        }
    )

    private val thirdPlaceScoreTotal = Label(
        posX = 660,
        posY = 330,
        width = 68,
        height = 68,
        font = Font(size = 30),
        visual = ColorVisual(181, 181, 181).apply {
            style.borderRadius = BorderRadius(34)
        }
    )

    private val fourthPlaceScoreTotal = Label(
        posX = 660,
        posY = 420,
        width = 68,
        height = 68,
        font = Font(size = 30),
        visual = ColorVisual(181, 181, 181).apply {
            style.borderRadius = BorderRadius(34)
        }
    )

    val exitButton = Button(
        width = 280,
        height = 70,
        posX = 80,
        posY = 570,
        text = "Exit",
        font = Font(size = 30),
        visual = ColorVisual(181, 181, 181).apply {
            style.borderRadius = BorderRadius(10.0)
        }
    ).apply {
        onMouseClicked = {
            app.exit()
        }
    }

    val showGameScene = Button(
        width = 280,
        height = 70,
        posX = 40,
        posY = 40,
        text = "Show Boards",
        font = Font(size = 30),
        visual = ColorVisual(181, 181, 181).apply {
            style.borderRadius = BorderRadius(10.0)
        }
    ).apply {
        onMouseClicked = {
            app.gameScene.showVisualForEndScreen(false)
            app.hideMenuScene()
        }
    }

    val newGameButton = Button(
        width = 280,
        height = 70,
        posX = paneWidth-280-80,
        posY = 570,
        text = "New Game",
        font = Font(size = 30),
        visual = ColorVisual(181, 181, 181).apply {
            style.borderRadius = BorderRadius(10.0)
        }
    ).apply {
        onMouseClicked = {
            app.showMenuScene(app.mainMenuScene)
        }
    }

    //
    // Components for the wildlife and habitatScorePane
    //

    private val spacing = 10
    private val entryWidth = 85
    private val entryHeight = 60

    private val wildlifeTableTopHalf = GridPane<Label>(
        posX = paneWidth/2 - (5*entryWidth + 4*spacing)/2,
        posY = paneHeight/2 - (7*entryHeight + 6*spacing)/2,
        rows = 5,
        columns = 5,
        spacing = spacing,
        layoutFromCenter = false
    )

    private val wildlifeTableBottomHalf = GridPane<Label>(
        posX = paneWidth/2 - (5*entryWidth + 4*spacing)/2,
        posY = wildlifeTableTopHalf.posY + 5*entryHeight + 5*spacing,
        rows = 2,
        columns = 5,
        spacing = spacing,
        layoutFromCenter = false
    )

    private val habitatTableTopHalf = GridPane<Label>(
        posX = paneWidth/2 - (5*entryWidth + 4*spacing)/2,
        posY = paneHeight/2 - (8*entryHeight + 7*spacing)/2,
        rows = 5,
        columns = 5,
        spacing = spacing,
        layoutFromCenter = false
    )

    private val habitatTableBottomHalf = GridPane<Label>(
        posX = paneWidth/2 - (5*entryWidth + 4*spacing)/2,
        posY = habitatTableTopHalf.posY + 5*entryHeight + 5*spacing,
        rows = 3,
        columns = 5,
        spacing = spacing,
        layoutFromCenter = false
    )

    init {
        addComponents(
            backgroundImage,
            mainTabBorder,
            wildlifeTabBorder,
            habitatTabBorder,
            wildlifeScoreTab,
            habitatScoreTab,
            mainScoreTab,
            paneBorder,
            mainScorePane,
            wildlifeScorePane,
            habitatScorePane,
            mainScoreTabOverlay,
            wildlifeScoreTabOverlay,
            habitatScoreTabOverlay,
            showGameScene
        )
        mainScorePane.addAll(
            firstPlaceName,
            secondPlaceName,
            thirdPlaceName,
            fourthPlaceName,
            firstPlaceScoreTotal,
            secondPlaceScoreTotal,
            thirdPlaceScoreTotal,
            fourthPlaceScoreTotal,
            newGameButton,
            exitButton,
        )
        wildlifeScorePane.addAll(
            wildlifeTableTopHalf,
            wildlifeTableBottomHalf,
        )
        habitatScorePane.addAll(
            habitatTableTopHalf,
            habitatTableBottomHalf
        )
    }

    override fun refreshAfterStartGame() {
        //clear all information that may still be saved from a previous round.
        // Name and Score labes will be made visible and overwritten as needed
        habitatTableTopHalf.clear()
        habitatTableBottomHalf.clear()
        wildlifeTableTopHalf.clear()
        wildlifeTableBottomHalf.clear()
        firstPlaceName.isVisible = false
        secondPlaceName.isVisible = false
        thirdPlaceName.isVisible = false
        fourthPlaceName.isVisible = false
        firstPlaceScoreTotal.isVisible = false
        secondPlaceScoreTotal.isVisible = false
        thirdPlaceScoreTotal.isVisible = false
        fourthPlaceScoreTotal.isVisible = false
    }

    override fun refreshAfterEndGame(scores: List<Pair<String, List<Int>>>) {

        playerNum = scores.size
        habitatTableBottomHalf.posX = (paneWidth/2 - ((playerNum+1)*entryWidth + playerNum*spacing)/2) + 0.0
        wildlifeTableBottomHalf.posX = (paneWidth/2 - ((playerNum+1)*entryWidth + playerNum*spacing)/2) + 0.0
        wildlifeTableTopHalf.posX = (paneWidth/2 - ((playerNum+1)*entryWidth + playerNum*spacing)/2) + 0.0
        habitatTableTopHalf.posX = (paneWidth/2 - ((playerNum+1)*entryWidth + playerNum*spacing)/2) + 0.0

        setUpScoreTables()

        val playersTotalScore: MutableList<Pair<String, Int>> = mutableListOf()

        for (i in 0..3) {

            var totalScore = 0
            val wildlifeSum = fillWildlifeTable(i, scores)
            val habitatSum = fillHabitatTable(i, scores)

            totalScore += wildlifeSum

            totalScore += habitatSum

            if (i in scores.indices) {
                totalScore += scores[i].second[15]
                playersTotalScore.add(Pair(scores[i].first, totalScore))
            }
        }
        setMainScores(playersTotalScore)
    }

    private fun fillWildlifeTable(index: Int, scores: List<Pair<String, List<Int>>>): Int {
        wildlifeTableTopHalf[index+1, 0] = Label (
            width = entryWidth,
            height = entryHeight,
            visual = ColorVisual(153, 172, 255)
        ).apply {
            if (index in scores.indices) {
                text = scores[index].first
            } else {
                isVisible = false
            }
        }

        var wildlifeSum = 0

        for (j in 5..9) {

            val score = Label(
                width = entryWidth,
                height = entryHeight,
                visual = ColorVisual(181, 181, 181)
            ).apply {
                if (index in scores.indices) {
                    text = "" + scores[index].second[j]
                } else {
                    isVisible = false
                }
            }
            when (j) {
                9 -> wildlifeTableBottomHalf[index+1, 0] = score
                else -> wildlifeTableTopHalf[index+1, j+1-5] = score
            }
            wildlifeSum +=  if (index in scores.indices) {
                scores[index].second[j]
            } else {
                0
            }
        }

        wildlifeTableBottomHalf[index+1, 1] = Label(
            width = entryWidth,
            height = entryHeight,
            visual = ColorVisual(181, 181, 181)
        ).apply {
            if (index in scores.indices) {
                text = "" + wildlifeSum
            } else {
                isVisible = false
            }
        }

        return wildlifeSum
    }

    private fun fillHabitatTable(index: Int, scores: List<Pair<String, List<Int>>>): Int {
        habitatTableTopHalf[index+1, 0] = Label (
            width = entryWidth,
            height = entryHeight,
            visual = ColorVisual(153, 172, 255)
        ).apply {
            if (index in scores.indices) {
                text = scores[index].first
            } else {
                isVisible = false
            }
        }

        var habitatSum = 0

        for (j in 0..4) {
            val score = Label(
                width = entryWidth,
                height = entryHeight,
                visual = ColorVisual(181, 181, 181)
            ).apply {
                if (index in scores.indices) {
                    text = "" + scores[index].second[j] + "  |  " + scores[index].second[j+10]
                } else {
                    isVisible = false
                }
            }
            when(j) {
                4 -> habitatTableBottomHalf[index+1, 0] = score
                else -> habitatTableTopHalf[index+1, j+1] = score
            }
            if (index in scores.indices) {
                habitatSum += scores[index].second[j] + scores[index].second[j+10]
            }

            habitatTableBottomHalf[index+1, 1] = Label(
                width = entryWidth,
                height = entryHeight,
                visual = ColorVisual(181, 181, 181)
            ).apply {
                if (index in scores.indices) {
                    text = "" + habitatSum
                } else {
                    isVisible = false
                }
            }

            habitatTableBottomHalf[index+1, 2] = Label(
                width = entryWidth,
                height = entryHeight,
                visual = ColorVisual(181, 181, 181)
            ).apply {
                if (index in scores.indices) {
                    text = "" + scores[index].second[15]
                } else {
                    isVisible = false
                }
            }
        }
        return habitatSum
    }

    private fun setMainScores(namesAndScores: MutableList<Pair<String, Int>>) {

        val playersAndScores = namesAndScores.sortedByDescending{it.second}

        setNameAndTotalScore(playersAndScores[0].first, playersAndScores[0].second,
            firstPlaceName, firstPlaceScoreTotal, true)

        setNameAndTotalScore(playersAndScores[1].first, playersAndScores[1].second, secondPlaceName,
            secondPlaceScoreTotal, playersAndScores[1].second == playersAndScores[0].second)


        if(playersAndScores.size > 2) {
            setNameAndTotalScore(playersAndScores[2].first, playersAndScores[2].second, thirdPlaceName,
                thirdPlaceScoreTotal, playersAndScores[2].second == playersAndScores[0].second)
        }

        if(playersAndScores.size > 3) {
            setNameAndTotalScore(playersAndScores[3].first, playersAndScores[3].second, fourthPlaceName,
                fourthPlaceScoreTotal, playersAndScores[3].second == playersAndScores[0].second)
        }
    }

    private fun setNameAndTotalScore(name: String, score: Int, nameLabel: Label, scoreLabel: Label, winner: Boolean) {
        nameLabel.apply {
            text = name
            isVisible = true
            if(winner) {
                visual = ColorVisual(255, 207, 0).apply {
                    style.borderRadius = BorderRadius(34)
                }
            }
        }
        scoreLabel.apply {
            text = "" + score
            isVisible = true
            if(winner) {
                visual = ColorVisual(255, 207, 0).apply {
                    style.borderRadius = BorderRadius(34)
                }
            }
        }
    }

    private fun setUpScoreTables() {
        for (i in 0..4) {
            wildlifeTableTopHalf[0,i] = Label(
                width = entryWidth,
                height = entryHeight,
            ).apply {
                if(i in 1..4) {
                    visual = fillInScoreTableImages(wildlifeTableTopHalf, i)
                } else {
                    visual = ColorVisual(153, 172, 255)
                    text = "👤"
                }
            }
            habitatTableTopHalf[0,i] = Label(
                width = entryWidth,
                height = entryHeight,
                ).apply {
                if(i in 1..4) {
                    visual = fillInScoreTableImages(habitatTableTopHalf, i)
                } else {
                    visual = ColorVisual(153, 172, 255)
                    text = "👤"
                }
            }
            if(i <= 1){
                wildlifeTableBottomHalf[0,i] = Label(
                    width = entryWidth,
                    height = entryHeight,
                ).apply {
                    if(i == 0) {
                        visual = fillInScoreTableImages(wildlifeTableBottomHalf, i)
                    } else {
                        visual = ColorVisual(153, 172, 255)
                        text = "W"
                    }
                }
            }
            if (i <=2) {
                habitatTableBottomHalf[0,i] = Label(
                    width = entryWidth,
                    height = entryHeight,
                ).apply {
                    if(i == 1) {
                        visual = ColorVisual(153, 172, 255)
                        text = "H"
                    } else {
                        visual = fillInScoreTableImages(habitatTableBottomHalf, i)
                    }
                }
            }
        }
    }

    private fun fillInScoreTableImages(table: GridPane<Label>, index: Int): ImageVisual {
        return when(table) {
            wildlifeTableTopHalf -> {
                wildlifeTableTopHalfImage(index)
            }
            wildlifeTableBottomHalf -> {
               wildlifeTableBottomHalfImage(index)
            }
            habitatTableTopHalf -> {
                habitatTableTopHalfImage(index)
            }
            habitatTableBottomHalf -> {
                habitatTableBottomHalfImage(index)
            }
            else -> throw IllegalArgumentException("invalid table given: ${table.name}" )
        }
    }

    private fun wildlifeTableTopHalfImage(index: Int): ImageVisual {
        return when(index) {
            1 -> ImageVisual("tokens/bear.png")
            2 -> ImageVisual("tokens/elk.png")
            3 -> ImageVisual("tokens/salmon.png")
            4 -> ImageVisual("tokens/hawk.png")
            else -> throw IllegalArgumentException("index $index in wildlifeTableTopHalf " +
                    "does not contain an image" )
        }
    }

    private fun wildlifeTableBottomHalfImage(index: Int): ImageVisual {
        return when(index) {
            0 -> ImageVisual("tokens/fox.png")
            else -> throw IllegalArgumentException("index $index in wildlifeTableBottomHalf " +
                    "does not contain an image" )
        }
    }

    private fun habitatTableTopHalfImage(index: Int): ImageVisual {
        return when(index) {
            1 -> ImageVisual("tiles/scoreSceneTiles/mountain.png")
            2 -> ImageVisual("tiles/scoreSceneTiles/forest.png")
            3 -> ImageVisual("tiles/scoreSceneTiles/desert.png")
            4 -> ImageVisual("tiles/scoreSceneTiles/swamp.png")
            else -> throw IllegalArgumentException("index $index in habitatTableTopHalf " +
                    "does not contain an image" )
        }
    }

    private fun habitatTableBottomHalfImage(index: Int): ImageVisual {
        return when(index) {
            0 -> ImageVisual("tiles/scoreSceneTiles/lake.png")
            2 -> ImageVisual("tokens/pinecone.png")
            else -> throw IllegalArgumentException("index $index in habitatTableBottomHalf " +
                    "does not contain an image" )
        }
    }
}