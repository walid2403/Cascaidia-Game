package service

import entity.GameState
import entity.PlayerType
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.*

/**
 * testen der Methode [PlayerActionService.placeTile]
 */
class PlaceTileTest {
    private lateinit var rootService: RootService
    private lateinit var playerActionService: PlayerActionService
    private lateinit var gameService: GameService

    /**
     * setUp
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        playerActionService = rootService.playerActionService
        gameService = rootService.gameService

    }

    /**
     * damit Spielerliste für [GameService.startNewGame] direkt erstellt wird
     */
    private fun setUpPlayers():List<Pair<String, PlayerType>> {
        return listOf(Pair("Mert",PlayerType.EASY_BOT),Pair("Lotfi", PlayerType.HUMAN))
    }
    /**
     * damit Valueliste für [GameService.startNewGame] direkt erstellt wird
     */
    private fun setUpCards():List<Boolean> {
        return listOf(true,true,true,true,true)
    }

    /**
     * versichern dass Methode nur in madechoice funktioniert
     */
    @Test
    fun `fails with false gameState`(){
        gameService.startNewGame(setUpPlayers(),setUpCards())
        val game=rootService.currentGame!!

        val gameStates= listOf<GameState>(
            GameState.PLAYED_TILE,
            GameState.START_OF_TURN,
            GameState.END_OF_TURN,
            GameState.HAS_EXTERMINATED
        )
        for(gameState in gameStates){
            game.gameState=gameState
            assertFailsWith<IllegalStateException> {
                playerActionService.placeTile(Triple(0,0,0))
            }
        }

    }

    /**
     * testen ob bei gültiger cubekoordinate problemlos die tile an das board kommt
     */
    @Test
    fun `placing Tile correctly (connected to atleast one edge of a tile)`(){
        gameService.startNewGame(setUpPlayers(),setUpCards())
        val game=rootService.currentGame!!

        game.gameState= GameState.MADE_CHOICE
        assertDoesNotThrow {
            playerActionService.placeTile(Triple(1,0,-1))
        }

        assertTrue(game.playerQueue.first().board.containsKey(Triple(1,0,-1)))

    }

    /**
     * teste den Fehlerfall dass man auf belegte Koordinaten legt
     */
    @Test
    fun `placing Tile on a coordinate which already belongs to Tile of the board`(){
        gameService.startNewGame(setUpPlayers(),setUpCards())
        val game=rootService.currentGame!!

        game.gameState= GameState.MADE_CHOICE
        assertFailsWith<IllegalArgumentException> {
            playerActionService.placeTile(Triple(0,0,0))
        }
    }

    /**
     * teste den Fehlerfall dass man nicht an das board dranlegt
     */

    @Test
    fun `placing Tile with no edge connecting to the board`(){
        gameService.startNewGame(setUpPlayers(),setUpCards())
        val game=rootService.currentGame!!

        game.gameState= GameState.MADE_CHOICE
        assertFailsWith<IllegalArgumentException> {
            playerActionService.placeTile(Triple(15,-15,0))
        }
    }
}