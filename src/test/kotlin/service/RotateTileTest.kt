package service

import entity.*
import kotlin.test.*

/**
 * Tests the method [PlayerActionService.rotateTile].
 */
class RotateTileTest {

    private lateinit var rootService: RootService
    private lateinit var playerActionService: PlayerActionService
    private var refreshRotateAmount: Int? = null

    /**
     * Initializes the services and a refreshable that records the reported rotation amount.
     * This function is executed before every test.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        playerActionService = rootService.playerActionService
        refreshRotateAmount = null

        val refreshable = object : Refreshable {
            override fun refreshAfterRotate(amount: Int) {
                refreshRotateAmount = amount
            }
        }
        rootService.addRefreshable(refreshable)
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
    private fun createGame(isLocal: Boolean = true): CascadiaGame {
        val game = CascadiaGame(List(5) { true }, isLocal)

        game.choices.add(Pair(createTile(), WildlifeToken.FOX))
        game.selectedChoice = Pair(0, 0)
        game.gameState = GameState.MADE_CHOICE
        game.playerQueue.add(Player("P1", PlayerType.HUMAN))

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

    /**
     * Tests that [PlayerActionService.rotateTile] can rotate a tile that already lies on the
     * player's board when a target tile position is provided.
     */
    @Test
    fun `rotation with targetTilePos rotates a board tile`() {
        val game = createGame()
        val boardTile = createTile()
        val position = Triple(0, 0, 0)
        game.playerQueue.peek().board[position] = boardTile

        playerActionService.rotateTile(right = true, targetTilePos = position)

        assertEquals(1, boardTile.rotation, "The board tile should be rotated once to the right.")
        assertEquals(Habitates.FORESTS, boardTile.habs[0], "The habitat list should be shifted.")
        assertEquals(1, game.tileRotation)
    }

    /**
     * Tests that [PlayerActionService.rotateTile] fails when the given target tile position
     * does not contain a tile.
     */
    @Test
    fun `rotation fails for unknown targetTilePos`() {
        createGame()

        assertFailsWith<IllegalStateException> {
            playerActionService.rotateTile(right = true, targetTilePos = Triple(5, -5, 0))
        }
    }

    /**
     * Tests the targetRotation branch with a forward rotation in a non-local game, which
     * additionally executes the refresh and network-send branches inside that path. The
     * network send is a safe no-op because no client is connected.
     */
    @Test
    fun `rotation with targetRotation forward in network game`() {
        val game = createGame(isLocal = false)
        val tile = game.choices[0].first

        playerActionService.rotateTile(right = null, targetRotation = 2)

        assertEquals(2, tile.rotation, "The rotation should be set to the target value.")
        assertEquals(2, game.tileRotation)

        val expectedHabs = mutableListOf(
            Habitates.FORESTS,
            Habitates.FORESTS,
            Habitates.PRAIRIES,
            Habitates.PRAIRIES,
            Habitates.PRAIRIES,
            Habitates.FORESTS
        )
        assertEquals(expectedHabs, tile.habs, "The habitat list should be shifted twice to the right.")
        assertEquals(2, refreshRotateAmount, "The refresh should report the rotation amount.")
    }

    /**
     * Tests the targetRotation branch with a backward rotation in a local game, covering the
     * opposite outcomes of the amount comparison and of the local-game condition.
     */
    @Test
    fun `rotation with targetRotation backward in local game`() {
        val game = createGame()
        val tile = game.choices[0].first
        tile.rotation = 3

        playerActionService.rotateTile(right = null, targetRotation = 1)

        assertEquals(1, tile.rotation)
        assertEquals(1, game.tileRotation)
        assertNull(refreshRotateAmount, "No refresh may fire in a local game on this path.")
    }

    /**
     * Tests that the targetRotation branch also works for a tile without habitats,
     * covering the empty-habitat branch.
     */
    @Test
    fun `rotation with targetRotation on tile without habitats`() {
        val game = createGame()
        game.choices[0] = Pair(Tile(7, mutableListOf(), emptyList()), WildlifeToken.FOX)

        playerActionService.rotateTile(right = null, targetRotation = 1)

        assertEquals(1, game.choices[0].first.rotation)
        assertEquals(1, game.tileRotation)
    }

    /**
     * Tests that [PlayerActionService.rotateTile] handles tiles without habitats for both
     * manual rotation directions.
     */
    @Test
    fun `rotation right and left with empty habitat list`() {
        val game = createGame()
        game.choices[0] = Pair(Tile(8, mutableListOf(), emptyList()), WildlifeToken.FOX)

        playerActionService.rotateTile(right = true)
        assertEquals(1, game.choices[0].first.rotation)

        playerActionService.rotateTile(right = false)
        assertEquals(0, game.choices[0].first.rotation)
    }

    /**
     * Tests that [PlayerActionService.rotateTile] without a direction and without a target
     * rotation only synchronizes the stored tile rotation of the game.
     */
    @Test
    fun `rotation without direction only syncs rotation`() {
        val game = createGame()
        game.choices[0].first.rotation = 4

        playerActionService.rotateTile(right = null)

        assertEquals(4, game.tileRotation)
        assertEquals(4, game.choices[0].first.rotation, "The tile itself must remain unchanged.")
    }

    /**
     * Tests that [PlayerActionService.rotateTile] executes the final network-send branch in a
     * non-local game as a safe no-op because no network client is connected.
     */
    @Test
    fun `rotation sends rotation in network game`() {
        val game = createGame(isLocal = false)

        playerActionService.rotateTile(right = true)

        assertEquals(1, game.choices[0].first.rotation)
        assertEquals(1, game.tileRotation)
    }

}