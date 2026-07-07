package service

import entity.*
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.*

/**
 * Tests the method [PlayerActionService.placeTile].
 */
class PlaceTileTest {

    private lateinit var rootService: RootService
    private lateinit var playerActionService: PlayerActionService

    /**
     * Initializes the services before every test.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        playerActionService = rootService.playerActionService
    }

    /**
     * Creates a simple tile for the test board or choices.
     */
    private fun createTile(id: Int): Tile {
        return Tile(
            id,
            MutableList(6) { Habitates.FORESTS },
            listOf(WildlifeToken.BEAR)
        )
    }

    /**
     * Creates a minimal game for testing [PlayerActionService.placeTile].
     *
     * the  game contains:
     * - one player
     * - one existing tile at coordinate (0, 0, 0)
     * - one selected tile in choices at index 0
     * - selectedChoice = Pair(0, 0)
     * - gameState = MADE_CHOICE
     */
    private fun createGame(): CascadiaGame {
        val game = CascadiaGame(List(5) { true }, true)

        val player = Player("Lotfi", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = createTile(0)

        game.playerQueue.add(player)
        game.choices.add(Pair(createTile(1), WildlifeToken.BEAR))
        game.selectedChoice = Pair(0, 0)
        game.gameState = GameState.MADE_CHOICE

        rootService.currentGame = game
        return game
    }

    /**
     * Tests that [PlayerActionService.placeTile] only works in [GameState.MADE_CHOICE].
     */
    @Test
    fun `fails with false gameState`() {
        val game = createGame()

        val gameStates = listOf(
            GameState.PLAYED_TILE,
            GameState.START_OF_TURN,
            GameState.END_OF_TURN,
            GameState.HAS_EXTERMINATED
        )

        for (gameState in gameStates) {
            game.gameState = gameState

            assertFailsWith<IllegalStateException> {
                playerActionService.placeTile(Triple(1, 0, -1))
            }
        }
    }

    /**
     * Tests that a tile is placed correctly on a valid coordinate
     * next to an already existing tile.
     */
    @Test
    fun `placing Tile correctly connected to at least one edge of a tile`() {
        val game = createGame()

        assertDoesNotThrow {
            playerActionService.placeTile(Triple(1, 0, -1))
        }

        assertTrue(
            game.playerQueue.first().board.containsKey(Triple(1, 0, -1)),
            "The tile should be placed on the board."
        )
    }

    /**
     * Tests that placing a tile on an already occupied coordinate fails.
     */
    @Test
    fun `placing Tile on a coordinate which already belongs to Tile of the board`() {
        createGame()

        assertFailsWith<IllegalArgumentException> {
            playerActionService.placeTile(Triple(0, 0, 0))
        }
    }

    /**
     * Tests that placing a tile without connection to the board fails.
     */
    @Test
    fun `placing Tile with no edge connecting to the board`() {
        createGame()

        assertFailsWith<IllegalArgumentException> {
            playerActionService.placeTile(Triple(15, -15, 0))
        }
    }

    /**
     * Tests that placing a tile on an invalid cube coordinate fails.
     */
    @Test
    fun `placing Tile with invalid cube coordinate fails`() {
        createGame()

        assertFailsWith<IllegalArgumentException> {
            playerActionService.placeTile(Triple(1, 1, 1))
        }
    }

    /**
     * Tests that [PlayerActionService.placeTile] fails if there is no current game.
     */
    @Test
    fun `placing Tile fails if there is no current game`() {
        rootService.currentGame = null

        assertFailsWith<IllegalStateException> {
            playerActionService.placeTile(Triple(1, 0, -1))
        }
    }

    /**
     * Tests that [PlayerActionService.placeTile] fails if there is no current player.
     */
    @Test
    fun `placing Tile fails if there is no current player`() {
        val game = CascadiaGame(List(5) { true }, true)

        game.choices.add(Pair(createTile(1), WildlifeToken.BEAR))
        game.selectedChoice = Pair(0, 0)
        game.gameState = GameState.MADE_CHOICE

        rootService.currentGame = game

        assertFailsWith<IllegalStateException> {
            playerActionService.placeTile(Triple(1, 0, -1))
        }
    }

    /**
     * Tests that [PlayerActionService.placeTile] fails if no valid tile was selected.
     */
    @Test
    fun `placing Tile fails if selected tile index is invalid`() {
        val game = createGame()

        game.selectedChoice = Pair(-1, 0)

        assertFailsWith<IllegalArgumentException> {
            playerActionService.placeTile(Triple(1, 0, -1))
        }
    }
}