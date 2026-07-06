package service



import entity.GameState
import entity.PlayerType
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.*

/**
 * tests the startNewGame function of the GameService
 */
class StartNewGameTest {

    private lateinit var rootService: RootService
    private lateinit var gameService: GameService

    /**
     * Initialize service to set up the test environment. This function is executed before every test.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        gameService = rootService.gameService
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
            "There should be 39 habitattiles tokens in the supply")

        assertTrue(game.wildlifeTokens.size == 96){
            "wildlifetoken sollten 96 sein, da 4 im Shop sind"
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
     * Tests if there is currently a game running
     */
    @Test
    fun `test throws exception if there is currently a game running`(){
        gameService.startNewGame(
            playerList = listOf(
                Pair("Mert", PlayerType.HUMAN),
                Pair("Noman", PlayerType.EASY_BOT),
            ),
            getValidScoringCards()
        )
        assertFailsWith<IllegalStateException> {
            gameService.startNewGame(
                playerList = listOf(
                    Pair("Mert", PlayerType.HUMAN),
                    Pair("Noman", PlayerType.EASY_BOT),
                ),
                getValidScoringCards()
            )
        }
    }
}