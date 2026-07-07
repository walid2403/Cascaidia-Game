package service

import kotlin.test.*
import entity.*


/**
 * A simple test class to demonstrate a basic unit test.
 */
class FreeSelectionTest {

    /**
     * This service is initialized in the [setUp] function hence it is a late-initialized property.
     */
    private lateinit var rootService: RootService
    var refreshWasCalled = false

    /**
     * Initialize service to set up the test environment. This function is executed before every test.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        val currentGame = CascadiaGame(List(5) { true }, true)
        val tileStack = mutableListOf<Tile>()
        val habitats = MutableList(6) { Habitates.MOUNTAINS }
        for (i in 0 until 5) {
            tileStack.add(Tile(i,habitats,emptyList()))
        }
        currentGame.tileStack.pushAll(tileStack)
        currentGame.natureTokens = 10
        val choices = mutableListOf<Pair<Tile, WildlifeToken>>()
        for (i in 0 until 4) {
            val pair = Pair(Tile(10+i,habitats,emptyList()),
                WildlifeToken.BEAR)
            choices.add(pair)
        }
        currentGame.choices += choices
        currentGame.selectedChoice = Pair(-1,-1)
        currentGame.gameState = GameState.START_OF_TURN
        val players = mutableListOf<Player>()
        for (i in 0 until 3) {
            val player = Player("player$i", PlayerType.HUMAN)
            player.natureTokens = 3
            players.add(player)
        }
        currentGame.playerQueue.addAll(players)
        val wildlifeTokens = List(6) { WildlifeToken.SALMON }
        currentGame.wildlifeTokens.pushAll(wildlifeTokens)

        rootService.currentGame = currentGame
        rootService.history.prevMoves.push(currentGame)

        val refreshable = object : Refreshable {
            override fun refreshAfterFreeSelection() {
                refreshWasCalled = true
            }
        }
        rootService.addRefreshable(refreshable)
    }

    /**
     * A simple test to check if the [RootService] is initialized.
     */
    @Test
    fun `valider Spielzug 1`() {
        val game = rootService.currentGame
        assertNotNull(game)

        val selection = game.choices

        rootService.playerActionService.freeSelection(0, 0)

        assertTrue(refreshWasCalled, "Der Refresh sollte getriggert haben")

        assertEquals(selection, game.choices, "Die angebotene Selection darf sich zu diesem Zeitpunkt nicht verändert haben")

        assertEquals(GameState.MADE_CHOICE, game.gameState,
            "Der GameState muss angepasst worden sein")

        assertEquals(0, game.selectedChoice.first,
            "Das gewählte Tile muss im Game abgespeichert werden")
        assertEquals(0, game.selectedChoice.second,
            "Das gewählte Token muss im Game abgespeichert werden")

        assertEquals(2, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte um eins reduziert worden sein")
    }

    /**
     * A simple test to check if the [RootService] is initialized.
     */
    @Test
    fun `valider Spielzug 2`() {
        val game = rootService.currentGame
        assertNotNull(game)

        val selection = game.choices

        rootService.playerActionService.freeSelection(1, 3)

        assertTrue(refreshWasCalled, "Der Refresh sollte getriggert haben")

        assertEquals(selection, game.choices, "Die angebotene Selection darf sich zu diesem Zeitpunkt nicht verändert haben")

        assertEquals(GameState.MADE_CHOICE, game.gameState,
            "Der GameState muss angepasst worden sein")

        assertEquals(1, game.selectedChoice.first,
            "Das gewählte Tile muss im Game abgespeichert werden")
        assertEquals(3, game.selectedChoice.second,
            "Das gewählte Token muss im Game abgespeichert werden")

        assertEquals(2, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte um eins reduziert worden sein")
    }

    /**
     * A simple test to check if the [RootService] is initialized.
     */
    @Test
    fun `invalider Spielzug durch keine Nature Tokens`() {
        val game = rootService.currentGame
        assertNotNull(game)

        game.playerQueue.peek().natureTokens = 0

        assertFailsWith<IllegalStateException> { rootService.playerActionService.freeSelection(0, 0) }

        assertFalse(refreshWasCalled)

        assertEquals(GameState.START_OF_TURN, game.gameState,
            "Der GameState darf nicht angepasst worden sein")

        assertEquals(-1, game.selectedChoice.first,
            "Das gewählte Tile darf nicht im Game abgespeichert werden")
        assertEquals(-1, game.selectedChoice.second,
            "Das gewählte Token darf nicht im Game abgespeichert werden")

        assertEquals(0, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte nicht verändert worden sein worden sein")
    }

    /**
     * A simple test to check if the [RootService] is initialized.
     */
    @Test
    fun `invalider Spielzug durch falschen GameState`() {
        val game = rootService.currentGame
        assertNotNull(game)

        game.gameState = GameState.MADE_CHOICE

        assertFailsWith<IllegalStateException> { rootService.playerActionService.freeSelection(0, 0) }

        assertFalse(refreshWasCalled)

        assertEquals(GameState.MADE_CHOICE, game.gameState,
            "Der GameState darf nicht angepasst worden sein")

        assertEquals(-1, game.selectedChoice.first,
            "Das gewählte Tile darf nicht im Game abgespeichert werden")
        assertEquals(-1, game.selectedChoice.second,
            "Das gewählte Token darf nicht im Game abgespeichert werden")

        assertEquals(3, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte nicht verändert worden sein worden sein")
    }

    /**
     * A simple test to check if the [RootService] is initialized.
     */
    @Test
    fun `invalider Spielzug durch falsche Indizes`() {
        val game = rootService.currentGame
        assertNotNull(game)

        assertFailsWith<IllegalArgumentException> { rootService.playerActionService.freeSelection(-1, 0) }
        assertFailsWith<IllegalArgumentException> { rootService.playerActionService.freeSelection(0, -1) }
        assertFailsWith<IllegalArgumentException> { rootService.playerActionService.freeSelection(4, 0) }
        assertFailsWith<IllegalArgumentException> { rootService.playerActionService.freeSelection(0, 4) }

        assertFalse(refreshWasCalled)

        assertEquals(GameState.START_OF_TURN, game.gameState,
            "Der GameState darf nicht angepasst worden sein")

        assertEquals(-1, game.selectedChoice.first,
            "Das gewählte Tile darf nicht im Game abgespeichert werden")
        assertEquals(-1, game.selectedChoice.second,
            "Das gewählte Token darf nicht im Game abgespeichert werden")

        assertEquals(3, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte nicht verändert worden sein worden sein")
    }
}