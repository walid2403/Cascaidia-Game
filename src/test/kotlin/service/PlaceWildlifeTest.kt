package service

import entity.*
import kotlin.test.*

/**
 * This class tests the method [PlayerActionService.placeWildlife].
 */
class PlaceWildlifeTest {

    private lateinit var rootService: RootService
    private lateinit var playerActionService: PlayerActionService
    private var refreshWasCalled=false

    /**
     * Initializes the services before every test.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        playerActionService = rootService.playerActionService
        refreshWasCalled=false
        val refreshable= object : Refreshable {
            override fun refreshAfterPlaceWildlife(index: Triple<Int, Int, Int>) {
                refreshWasCalled=true
            }
        }
        rootService.addRefreshable(refreshable)
    }

    /**
     * Creates a simple tile with the given possible wildlife tokens.
     *
     * @param id The id of the tile.
     * @param possibles The wildlife tokens that are allowed on this tile.
     */
    private fun createTile(id: Int, possibles: List<WildlifeToken>): Tile {
        return Tile(
            id,
            MutableList(6) { Habitates.FORESTS },
            possibles
        )
    }

    /**
     * Creates a Cascadia game and stores it in the rootService.
     *
     * The game is initialized with:
     * - scoringCards: list with 5 times true
     * - isLocal: true
     * - gameState: PLAYED_TILE
     * - one HUMAN player
     * - selectedChoice: the wildlife token at index 0
     */
    private fun createGame(selectedWildlife: WildlifeToken): CascadiaGame {
        val game = CascadiaGame(List(5) { true }, true)

        game.gameState = GameState.PLAYED_TILE
        game.playerQueue.add(Player("Player 1", PlayerType.HUMAN))

        game.choices.add(Pair(createTile(100, listOf(selectedWildlife)), selectedWildlife))
        game.selectedChoice = Pair(0, 0)

        rootService.currentGame = game
        return game
    }

    /**
     * Tests if [PlayerActionService.placeWildlife] correctly places the selected
     * wildlife token on a valid empty tile.
     */
    @Test
    fun `placeWildlife places selected wildlife on valid tile`() {
        val game = createGame(WildlifeToken.FOX)
        val currentPlayer = game.playerQueue.first()

        val targetIndex = Triple(0, 0, 0)
        val targetTile = createTile(1, listOf(WildlifeToken.FOX, WildlifeToken.BEAR))

        currentPlayer.board[targetIndex] = targetTile

        playerActionService.placeWildlife(targetIndex)
        assertTrue(refreshWasCalled)

        assertEquals(WildlifeToken.FOX, targetTile.occupant,
            "Wildlife token was not placed on the tile")
        assertEquals(GameState.END_OF_TURN, game.gameState,
            "GameState was not changed to END_OF_TURN")
    }

    /**
     * Tests if [PlayerActionService.placeWildlife] throws an [IllegalStateException]
     * when the game is in a wrong state.
     */
    @Test
    fun `placeWildlife fails with wrong gameState`() {
        val game = createGame(WildlifeToken.FOX)
        val currentPlayer = game.playerQueue.first()

        val targetIndex = Triple(0, 0, 0)
        val targetTile = createTile(1, listOf(WildlifeToken.FOX))

        currentPlayer.board[targetIndex] = targetTile

        val wrongGameStates = listOf(
            GameState.START_OF_TURN,
            GameState.HAS_EXTERMINATED,
            GameState.MADE_CHOICE,
            GameState.END_OF_TURN
        )

        for (state in wrongGameStates) {
            game.gameState = state

            assertFailsWith<IllegalStateException>("Wrong GameState was allowed") {
                playerActionService.placeWildlife(targetIndex)
            }
            assertFalse(refreshWasCalled)

            assertNull(targetTile.occupant,
                "Wildlife should not be placed after an invalid call")
            assertEquals(state, game.gameState,
                "GameState should not change after an invalid call")
        }
    }

    /**
     * Tests if [PlayerActionService.placeWildlife] throws an [IllegalArgumentException]
     * when the target tile already has a wildlife token.
     */
    @Test
    fun `placeWildlife fails when tile already has wildlife`() {
        val game = createGame(WildlifeToken.FOX)
        val currentPlayer = game.playerQueue.first()

        val targetIndex = Triple(0, 0, 0)
        val targetTile = createTile(1, listOf(WildlifeToken.FOX, WildlifeToken.BEAR))
        targetTile.occupant = WildlifeToken.BEAR

        currentPlayer.board[targetIndex] = targetTile

        assertFailsWith<IllegalArgumentException>("Tile with wildlife was allowed") {
            playerActionService.placeWildlife(targetIndex)
        }
        assertFalse(refreshWasCalled)

        assertEquals(WildlifeToken.BEAR, targetTile.occupant,
            "Existing wildlife token should not be changed")
        assertEquals(GameState.PLAYED_TILE, game.gameState,
            "GameState should not change after an invalid placement")
    }

    /**
     * Tests if [PlayerActionService.placeWildlife] throws an [IllegalArgumentException]
     * when the selected wildlife token is not allowed on the target tile.
     */
    @Test
    fun `placeWildlife fails when wildlife is not allowed on tile`() {
        val game = createGame(WildlifeToken.FOX)
        val currentPlayer = game.playerQueue.first()

        val targetIndex = Triple(0, 0, 0)
        val targetTile = createTile(1, listOf(WildlifeToken.BEAR))

        currentPlayer.board[targetIndex] = targetTile

        assertFailsWith<IllegalArgumentException>("Wrong wildlife token was allowed") {
            playerActionService.placeWildlife(targetIndex)
        }
        assertFalse(refreshWasCalled)

        assertNull(targetTile.occupant,
            "Wildlife should not be placed on a wrong tile")
        assertEquals(GameState.PLAYED_TILE, game.gameState,
            "GameState should not change after an invalid placement")
    }

    /**
     * Tests if [PlayerActionService.placeWildlife] throws an [IllegalArgumentException]
     * when there is no tile at the selected board coordinate.
     */
    @Test
    fun `placeWildlife fails when coordinate has no tile`() {
        val game = createGame(WildlifeToken.FOX)

        val emptyIndex = Triple(5, -5, 0)
        val currentPlayer = game.playerQueue.first()
        val natTokenBefore= currentPlayer.natureTokens

        assertFailsWith<IllegalArgumentException>("Empty coordinate was allowed") {
            playerActionService.placeWildlife(emptyIndex)
        }
        assertFalse(refreshWasCalled)

        assertEquals(GameState.PLAYED_TILE, game.gameState,
            "GameState should not change after an invalid placement")
        assertEquals(natTokenBefore, currentPlayer.natureTokens,"Player should not recieve natureToken after an invalid placement")
        assertEquals(WildlifeToken.FOX, game.choices[0].second)
    }
    /**
     * testing that placing a wildlife token on a keystone
     * awards one nature token
     */
    @Test
    fun placeWildlifeAwardsNatureToken() {
        val game = createGame(WildlifeToken.FOX)
        val currentPlayer = game.playerQueue.first()
        game.natureTokens=5
        currentPlayer.natureTokens=0
        val index=Triple(0, 0, 0)
        val tile = createTile(1, listOf(WildlifeToken.FOX))
        currentPlayer.board[index] = tile

        playerActionService.placeWildlife(index)
        assertTrue(refreshWasCalled)
        assertEquals(1,currentPlayer.natureTokens)
        assertEquals(WildlifeToken.FOX, tile.occupant)
        assertEquals(4, game.natureTokens)
        assertEquals(GameState.END_OF_TURN, game.gameState)
    }
    /**
     * tests that no nature token is awarded if none
     * is available
     */
    @Test
    fun placeWildlifeWithoutNatureToken() {
        val game = createGame(WildlifeToken.FOX)
        val currentPlayer = game.playerQueue.first()
        game.natureTokens=0
        currentPlayer.natureTokens=0
        val index=Triple(0, 0, 0)
        currentPlayer.board[index]=createTile(1, listOf(WildlifeToken.FOX))

        playerActionService.placeWildlife(index)
        assertTrue(refreshWasCalled)
        assertEquals(0,currentPlayer.natureTokens)
        assertEquals(0,game.natureTokens)
        assertEquals(WildlifeToken.FOX, currentPlayer.board[index]!!.occupant)
        assertEquals(GameState.END_OF_TURN, game.gameState)
    }
    /**
     * this test for checking that placing wildlife fails
     * when no wildlife token has been selected
     */
    @Test
    fun placeWildlifeWithoutSelection() {
        val game = createGame(WildlifeToken.FOX)
        val currentPlayer = game.playerQueue.first()
        game.selectedChoice= Pair(-1, -1)
        val index=Triple(0, 0, 0)
        currentPlayer.board[index]=createTile(1, listOf(WildlifeToken.FOX))

        assertFailsWith<IllegalArgumentException> {
            playerActionService.placeWildlife(index)
        }
        assertFalse(refreshWasCalled)
        assertNull(currentPlayer.board[index]?.occupant)
        assertEquals(GameState.PLAYED_TILE, game.gameState)
    }
}