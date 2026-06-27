package service

import entity.*
import kotlin.test.*

/**
 * This class tests the method [GameService.exterminate].
 */
class ExterminateTest {

    private lateinit var rootService: RootService
    private var refreshWasCalled = false

    /**
     * Creates a Cascadia game and stores it in the rootService.
     *
     * The game is initialized with:
     * - scoringCards: list with 5 times true
     * - isLocal: true
     * - choices: 3 Salmon tokens and 1 Fox token
     * - gameState: START_OF_TURN
     * - wildlifeTokens: Bear, Elk and Hawk for refilling the choices
     *
     * A refreshable is also added to check if [Refreshable.refreshAfterExterminate]
     * is called.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        refreshWasCalled = false

        val currentGame = CascadiaGame(List(5) { true }, true)
        currentGame.gameState = GameState.START_OF_TURN

        setChoices(
            currentGame,
            listOf(
                WildlifeToken.SALMON,
                WildlifeToken.SALMON,
                WildlifeToken.SALMON,
                WildlifeToken.FOX
            )
        )

        fillWildlifeTokens(
            currentGame,
            listOf(
                WildlifeToken.BEAR,
                WildlifeToken.ELK,
                WildlifeToken.HAWK
            )
        )

        rootService.currentGame = currentGame

        val refreshable = object : Refreshable {
            override fun refreshAfterExterminate() {
                refreshWasCalled = true
            }
        }
        rootService.addRefreshable(refreshable)
    }

    /**
     * Creates a simple tile for the choice display.
     *
     * The habitats and possible wildlife tokens are not important here,
     * because [GameService.exterminate] only changes the wildlife tokens
     * in the choices.
     */
    private fun createTile(id: Int): Tile {
        return Tile(
            id,
            MutableList(6) { Habitates.FORESTS },
            listOf(WildlifeToken.BEAR)
        )
    }

    /**
     * Replaces the current choices of the game with new choices.
     *
     * Each wildlife token gets a simple tile.
     */
    private fun setChoices(game: CascadiaGame, tokens: List<WildlifeToken>) {
        game.choices.clear()

        for (i in tokens.indices) {
            game.choices.add(Pair(createTile(i), tokens[i]))
        }
    }

    /**
     * Fills the wildlife token stack.
     *
     * The first token in [tokens] will be the first token that is popped
     * during [GameService.exterminate].
     */
    private fun fillWildlifeTokens(game: CascadiaGame, tokens: List<WildlifeToken>) {
        game.wildlifeTokens.clear()

        for (token in tokens.reversed()) {
            game.wildlifeTokens.push(token)
        }
    }

    /**
     * Tests a correct player-triggered extermination.
     *
     * Three identical Salmon tokens should be replaced. The Fox token should
     * stay unchanged. The removed Salmon tokens should be stored in
     * [CascadiaGame.removedTokens].
     */
    @Test
    fun `player extermination replaces exactly three same wildlife tokens`() {
        val currentGame = rootService.currentGame
        assertNotNull(currentGame)

        rootService.gameService.exterminate(true)

        assertTrue(refreshWasCalled, "Refresh was not called")
        assertEquals(GameState.HAS_EXTERMINATED, currentGame.gameState,
            "GameState was not changed correctly")

        assertEquals(WildlifeToken.BEAR, currentGame.choices[0].second,
            "First Salmon token was not replaced correctly")
        assertEquals(WildlifeToken.ELK, currentGame.choices[1].second,
            "Second Salmon token was not replaced correctly")
        assertEquals(WildlifeToken.HAWK, currentGame.choices[2].second,
            "Third Salmon token was not replaced correctly")
        assertEquals(WildlifeToken.FOX, currentGame.choices[3].second,
            "The fourth token should not be replaced")

        assertEquals(3, currentGame.removedTokens.size,
            "Wrong number of removed tokens")
        assertTrue(currentGame.removedTokens.all { it == WildlifeToken.SALMON },
            "Removed tokens should all be Salmon")
    }

    /**
     * Tests a correct automatic extermination.
     *
     * If all four wildlife tokens are identical, [GameService.exterminate]
     * should replace all four tokens without changing the gameState to
     * [GameState.HAS_EXTERMINATED].
     */
    @Test
    fun `automatic extermination replaces four same wildlife tokens`() {
        val currentGame = rootService.currentGame
        assertNotNull(currentGame)

        setChoices(
            currentGame,
            listOf(
                WildlifeToken.BEAR,
                WildlifeToken.BEAR,
                WildlifeToken.BEAR,
                WildlifeToken.BEAR
            )
        )

        fillWildlifeTokens(
            currentGame,
            listOf(
                WildlifeToken.SALMON,
                WildlifeToken.ELK,
                WildlifeToken.HAWK,
                WildlifeToken.FOX
            )
        )

        rootService.gameService.exterminate(false)

        assertTrue(refreshWasCalled, "Refresh was not called")
        assertEquals(GameState.START_OF_TURN, currentGame.gameState,
            "GameState should stay START_OF_TURN")

        assertEquals(WildlifeToken.SALMON, currentGame.choices[0].second,
            "First Bear token was not replaced correctly")
        assertEquals(WildlifeToken.ELK, currentGame.choices[1].second,
            "Second Bear token was not replaced correctly")
        assertEquals(WildlifeToken.HAWK, currentGame.choices[2].second,
            "Third Bear token was not replaced correctly")
        assertEquals(WildlifeToken.FOX, currentGame.choices[3].second,
            "Fourth Bear token was not replaced correctly")

        assertEquals(4, currentGame.removedTokens.size,
            "Wrong number of removed tokens")
        assertTrue(currentGame.removedTokens.all { it == WildlifeToken.BEAR },
            "Removed tokens should all be Bear")
    }

    /**
     * Tests all wrong game states.
     *
     * [GameService.exterminate] should only work in [GameState.START_OF_TURN]
     * or [GameState.HAS_EXTERMINATED].
     */
    @Test
    fun `exterminate fails with wrong gameState`() {
        val currentGame = rootService.currentGame
        assertNotNull(currentGame)

        val wrongGameStates = listOf(
            GameState.MADE_CHOICE,
            GameState.PLAYED_TILE,
            GameState.END_OF_TURN
        )

        for (state in wrongGameStates) {
            currentGame.gameState = state

            assertFailsWith<IllegalStateException>("Wrong GameState was allowed") {
                rootService.gameService.exterminate(true)
            }

            assertFalse(refreshWasCalled,
                "Refresh should not be called after an invalid extermination")
            assertEquals(state, currentGame.gameState,
                "GameState should not be changed after an invalid extermination")
        }
    }

    /**
     * Tests the error case where there are not at least three identical
     * wildlife tokens in the choices.
     */
    @Test
    fun `exterminate fails when there are not three same wildlife tokens`() {
        val currentGame = rootService.currentGame
        assertNotNull(currentGame)

        setChoices(
            currentGame,
            listOf(
                WildlifeToken.BEAR,
                WildlifeToken.SALMON,
                WildlifeToken.ELK,
                WildlifeToken.FOX
            )
        )

        assertFailsWith<IllegalStateException>("Extermination without three same tokens was allowed") {
            rootService.gameService.exterminate(true)
        }

        assertFalse(refreshWasCalled,
            "Refresh should not be called after an invalid extermination")
        assertEquals(0, currentGame.removedTokens.size,
            "No token should be removed after an invalid extermination")
    }
}