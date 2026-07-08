package service

import entity.*
import kotlin.test.*

/**
 * Tests the method [PlayerActionService.rotateTile].
 */
class RotateTileTest {

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
     * Creates a tile with a fixed habitat order.
     *
     * Original order:
     * PRAIRIES, PRAIRIES, PRAIRIES, FORESTS, FORESTS, FORESTS
     */
    private fun createTile(): Tile {
        val habs = mutableListOf(
            Habitates.PRAIRIES,
            Habitates.PRAIRIES,
            Habitates.PRAIRIES,
            Habitates.FORESTS,
            Habitates.FORESTS,
            Habitates.FORESTS
        )

        val possibles = listOf(
            WildlifeToken.ELK,
            WildlifeToken.BEAR
        )

        return Tile(0, habs, possibles)
    }

    /**
     * Creates a minimal game for testing [PlayerActionService.rotateTile].
     *
     * The game contains:
     * - one selected tile in choices at index 0
     * - selectedChoice = Pair(0, 0)
     * - gameState = MADE_CHOICE
     */
    private fun createGame(): CascadiaGame {
        val game = CascadiaGame(List(5) { true }, true)

        game.choices.add(Pair(createTile(), WildlifeToken.FOX))
        game.selectedChoice = Pair(0, 0)
        game.gameState = GameState.MADE_CHOICE

        rootService.currentGame = game
        return game
    }

    /**
     * Tests that [PlayerActionService.rotateTile] only works in [GameState.MADE_CHOICE].
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
                playerActionService.rotateTile(right = true)
            }
        }
    }

    /**
     * Tests that a rotation to the right changes the habitat order correctly.
     */
    @Test
    fun `rotation to the right correctly`() {
        val game = createGame()

        playerActionService.rotateTile(right = true)

        val expectedRightRotation = mutableListOf(
            Habitates.FORESTS,
            Habitates.PRAIRIES,
            Habitates.PRAIRIES,
            Habitates.PRAIRIES,
            Habitates.FORESTS,
            Habitates.FORESTS
        )

        assertEquals(
            expectedRightRotation,
            game.choices[0].first.habs,
            "The tile was not rotated correctly to the right."
        )

        assertEquals(
            1,
            game.choices[0].first.rotation,
            "The rotation value should be increased to 1."
        )
    }

    /**
     * Tests that a rotation to the left changes the habitat order correctly.
     */
    @Test
    fun `rotation to the left correctly`() {
        val game = createGame()

        playerActionService.rotateTile(right = false)

        val expectedLeftRotation = mutableListOf(
            Habitates.PRAIRIES,
            Habitates.PRAIRIES,
            Habitates.FORESTS,
            Habitates.FORESTS,
            Habitates.FORESTS,
            Habitates.PRAIRIES
        )

        assertEquals(
            expectedLeftRotation,
            game.choices[0].first.habs,
            "The tile was not rotated correctly to the left."
        )

        assertEquals(
            5,
            game.choices[0].first.rotation,
            "The rotation value should become 5 after rotating left."
        )
    }

    /**
     * Tests that [PlayerActionService.rotateTile] fails if there is no current game.
     */
    @Test
    fun `rotation fails if there is no current game`() {
        rootService.currentGame = null

        assertFailsWith<IllegalStateException> {
            playerActionService.rotateTile(right = true)
        }
    }

    /**
     * Tests that [PlayerActionService.rotateTile] fails if no valid tile was selected.
     */
    @Test
    fun `rotation fails if selected tile index is invalid`() {
        val game = createGame()

        game.selectedChoice = Pair(-1, 0)

        assertFailsWith<IllegalArgumentException> {
            playerActionService.rotateTile(right = true)
        }
    }
}