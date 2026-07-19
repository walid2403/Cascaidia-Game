package service



import entity.GameState
import entity.PlayerType
import entity.WildlifeToken
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.*

/**
 * tests the startNewGame function of the GameService
 */
class StartNewGameTest {

    private lateinit var rootService: RootService
    private lateinit var gameService: GameService
    private var refreshWasCalled = false

    /**
     * Initialize service to set up the test environment. This function is executed before every test.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        gameService = rootService.gameService
        refreshWasCalled = false

        val refreshable = object : Refreshable {
            override fun refreshAfterStartGame(){
                refreshWasCalled = true
            }
        }
        rootService.addRefreshable(refreshable)
    }


    private fun getValidPlayers() = listOf(Pair("Mert", PlayerType.HUMAN), Pair("Noman", PlayerType.EASY_BOT))
    private fun getValidScoringCards() = listOf(true, false, true, false, true)

    /**
     * Tests if the game is initialized correctly.
     */
    @Test
    fun `start new game`() {
        gameService.startNewGame(
            getValidPlayers(),
            getValidScoringCards()
        )

        val game = rootService.currentGame
        assertNotNull(game)

        assertEquals(GameState.START_OF_TURN, game.gameState)
        assertEquals(4, game.choices.size,
            "There should be 4 wildlife tokens in the stack")
        assertEquals(39, game.tileStack.size,
            "There should be 39 habitat tiles tokens in the supply")

        assertTrue(game.wildlifeTokens.size == 96){
            "wildlife token sollten 96 sein, da 4 im Shop sind"
        }

        for(player in game.playerQueue){
            assertEquals(3,player.board.size,"Each player starts with 3 starting Tiles")

            assertTrue {
                player.board.containsKey(Triple(0,0,0))
            }
            assertTrue {
                player.board.containsKey(Triple(-1,1,0))
            }
            assertTrue {
                player.board.containsKey(Triple(0,1,-1))
            }
        }

        assertTrue(refreshWasCalled, "Refreshable sollte aufgerufen worden sein")
        assertEquals(1, rootService.history.prevMoves.size,
            "Historie sollte genau den Startzustand enthalten")
        assertTrue(rootService.history.undoneMoves.isEmpty(), "Redo-Historie muss leer sein")
    }

    /**
     * Tests if there are more than 4 players
     */
    @Test
    fun `test throws exception if to many players`(){
        assertFailsWith<IllegalArgumentException> {
            gameService.startNewGame(
                playerList = listOf(
                    Pair("Mert", PlayerType.HUMAN),
                    Pair("Noman", PlayerType.EASY_BOT),
                    Pair("Hans", PlayerType.EASY_BOT),
                    Pair("Klaus", PlayerType.EASY_BOT),
                    Pair("Lukas", PlayerType.EASY_BOT),
                ),
                getValidScoringCards()
            )
        }
    }

    /**
     * Tests if there are less than 2 players
     */
    @Test
    fun `test throws exception if to few players`(){
        assertFailsWith<IllegalArgumentException> {
            gameService.startNewGame(
                playerList = listOf(
                    Pair("Mert", PlayerType.HUMAN),
                ),
                getValidScoringCards()
            )
        }
    }

    /**
     * Tests if there are duplicate player names
     */
    @Test
    fun `test throws exception if duplicate player names`(){
        assertFailsWith<IllegalArgumentException> {
            gameService.startNewGame(
                playerList = listOf(
                    Pair("Mert", PlayerType.HUMAN),
                    Pair("Mert", PlayerType.EASY_BOT),
                ),
                getValidScoringCards()
            )
        }
    }

    /**
     * Tests if there are less than 5 scoring cards
     */
    @Test
    fun `test throws exception if to few scoring cards`(){
        assertFailsWith<IllegalArgumentException> {
            gameService.startNewGame(
                playerList = listOf(
                    Pair("Mert", PlayerType.HUMAN),
                    Pair("Noman", PlayerType.EASY_BOT),
                ),
                scoringCards = listOf(true, false, true),
            )
        }
    }

    /**
     * Tests if there are more than 5 scoring cards
     */
    @Test
    fun `test throws exception if to many scoring cards`(){
        assertFailsWith<IllegalArgumentException> {
            gameService.startNewGame(
                getValidPlayers(),
                scoringCards = listOf(true, false, true, false, true, false),
            )
        }
    }

    /**
     * Testet, dass eine Exception geworfen wird, wenn ein Spielername leer ist
     * oder nur aus Leerzeichen besteht.
     */
    @Test
    fun `test throws exception if player name is blank`() {
        assertFailsWith<IllegalArgumentException>("Sollte bei leerem Namen fehlschlagen") {
            gameService.startNewGame(
                listOf(Pair("", PlayerType.HUMAN), Pair("Noman", PlayerType.EASY_BOT)),
                getValidScoringCards()
            )
        }

        assertFailsWith<IllegalArgumentException>("Sollte bei Namen nur aus Leerzeichen fehlschlagen") {
            gameService.startNewGame(
                listOf(Pair("   ", PlayerType.HUMAN), Pair("Noman", PlayerType.EASY_BOT)),
                getValidScoringCards()
            )
        }
    }

    /**
     * Tests if the game is initialized correctly when a network player is present.
     */
    @Test
    fun `start network game sets isLocal to false`() {
        gameService.startNewGame(
            listOf(Pair("Mert", PlayerType.HUMAN), Pair("NetPlayer", PlayerType.NETWORK)),
            getValidScoringCards()
        )

        val game = rootService.currentGame
        assertNotNull(game)
        assertFalse(game.isLocal, "Das Spiel sollte als Netzwerkspiel (isLocal = false) markiert sein")
    }

    /**
     * Tests if the game is initialized correctly with optional parameters.
     */
    @Test
    fun `start game with optional parameters`() {
        val customWildlifeBag = listOf(
            WildlifeToken.HAWK,
            WildlifeToken.BEAR,
            WildlifeToken.ELK,
            WildlifeToken.SALMON,
            WildlifeToken.FOX,
            WildlifeToken.HAWK,
            WildlifeToken.BEAR,
            WildlifeToken.ELK,
            WildlifeToken.SALMON,
            WildlifeToken.FOX
        )
        val customTileIDs = List(10) { 1 } // Nutzt die Zeile mit Index 1 aus tiles.csv

        // 10 führt zum Start-Board Index 0, 20 führt zum Start-Board Index 1.
        val customStartingTiles = listOf(10, 20)

        gameService.startNewGame(
            getValidPlayers(),
            getValidScoringCards(),
            customStartingTiles,
            customTileIDs,
            customWildlifeBag
        )

        val game = rootService.currentGame
        assertNotNull(game)

        // sollten genau 6 von den 10 übergebenen übrig bleiben.
        assertEquals(6, game.wildlifeTokens.size,
            "Der übergebene Tierbeutel wurde überschrieben (vermutlich Überpopulation!)")

        assertEquals(6, game.tileStack.size,
            "Die übergebenen TileIDs wurden nicht korrekt verwendet")
    }

    /**
     * Tests that the market creation resolves an overpopulated wildlife bag: if the top
     * four tokens of the provided bag are identical, the bag is rebuilt before the four
     * market pairs are drawn.
     */
    @Test
    fun `startNewGame resolves overpopulated wildlife bag`() {
        val bag = List(20) { WildlifeToken.SALMON } + List(4) { WildlifeToken.BEAR }

        gameService.startNewGame(
            getValidPlayers(),
            getValidScoringCards(),
            wildlifeBag = bag
        )

        val game = rootService.currentGame
        assertNotNull(game)
        assertEquals(4, game.choices.size, "The market must contain four pairs")
        assertTrue(game.choices.map { it.second }.distinct().size > 1) {
            "The market must not start with four identical tokens"
        }
    }

}