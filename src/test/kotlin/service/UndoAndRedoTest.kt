package service



import entity.CascadiaGame
import entity.GameState
import entity.Player
import entity.PlayerType
import kotlin.test.*

/**
 * This class tests the functionality of the undo and redo functions of the PlayerActionService.
 */
class UndoAndRedoTest {
    lateinit var rootService: RootService
    lateinit var gameService: GameService
    lateinit var playerActionService: PlayerActionService

    /**
     * Initialize service to set up the test environment. This function is executed before every test.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        gameService = rootService.gameService
        playerActionService = rootService.playerActionService
    }

    private fun getValidPlayers() = listOf(Pair("Mert", PlayerType.HUMAN), Pair("Noman", PlayerType.HUMAN))
    private fun getValidScoringCards() = listOf(true, false, true, false, true)

    /**
     * Tests if undo throws IllegalStateException when prevMoves is empty.
     */
    @Test
    fun `undo throws IllegalStateException when game is in beginning of first turn`() {
        gameService.startNewGame(getValidPlayers(), getValidScoringCards())

        assertFailsWith<IllegalStateException> {
            playerActionService.undo()
        }
    }

    /**
     * Tests if undo successfully reverts action and updates stacks.
     */
    @Test
    fun `undo resets current turn changes`() {
        gameService.startNewGame(getValidPlayers(), getValidScoringCards())

        val game = rootService.currentGame!!
        val player = game.playerQueue.first()
        val position = Triple(3, 3, 3)
        game.gameState= GameState.PLAYED_TILE

        player.natureTokens = 1
        player.board[position] = game.tileStack.pop()

        playerActionService.undo()

        val restoredGame = rootService.currentGame!!

        assertEquals(0, restoredGame.playerQueue.first().natureTokens)
        assertFalse {
            restoredGame.playerQueue.first().board.containsKey(position)
        }

    }
    /**
     * test if in beginning of turn undo reverts to previous turn
     */
    @Test
    fun`undo makes current turn into the last turn`() {
        gameService.startNewGame(getValidPlayers(), getValidScoringCards())

        rootService.playerActionService.selectColumn(1)
        rootService.playerActionService.placeTile(Triple(1,0,-1))
        gameService.changeTurn()

        assertEquals(2,rootService.history.prevMoves.size)

        playerActionService.undo()

        assertEquals(1,rootService.history.prevMoves.size)
        assertEquals(1,rootService.history.undoneMoves.size)
    }

    /**
     * Tests if redo throws IllegalStateException when undoneMoves is empty.
     */
    @Test
    fun `redo throws IllegalStateException when undoneMoves is empty`() {
        gameService.startNewGame(getValidPlayers(), getValidScoringCards())

        assertFailsWith<IllegalStateException> {
            playerActionService.redo()
        }
    }

    /**
     * Tests if redo successfully reverts action and updates stacks.
     */
    @Test
    fun `redo successfully reverts action and updates stacks`() {
        gameService.startNewGame(getValidPlayers(), getValidScoringCards())

        val undoneGame = CascadiaGame(getValidScoringCards(), true)
        undoneGame.playerQueue.add(Player("Player1", PlayerType.HUMAN))
        undoneGame.playerQueue.add(Player("Player2", PlayerType.HUMAN))

        rootService.history.undoneMoves.push(undoneGame)
        playerActionService.redo()

        assertTrue {
            rootService.history.undoneMoves.isEmpty()
        }
        assertEquals(2, rootService.history.prevMoves.size)

        val currentGame=rootService.currentGame!!
        assertEquals("Player1",currentGame.playerQueue.first().name)
        assertEquals(PlayerType.HUMAN,currentGame.playerQueue.last().type)
    }


    /**
     * Creates a minimal running game for the failure branch tests and stores it in the
     * [RootService].
     *
     * @param playerType the type of the single player in the queue
     * @param isLocal whether the game is local
     */
    private fun createHistoryGame(playerType: PlayerType, isLocal: Boolean): CascadiaGame {
        val game = CascadiaGame(getValidScoringCards(), isLocal)
        game.playerQueue.add(Player("P1", playerType))
        rootService.currentGame = game
        rootService.history.prevMoves.push(game)
        return game
    }

    /**
     * Tests that [PlayerActionService.redo] fails if no game is running.
     */
    @Test
    fun `redo throws IllegalStateException when no game is running`() {
        rootService.currentGame = null

        assertFailsWith<IllegalStateException> {
            playerActionService.redo()
        }
    }

    /**
     * Tests that [PlayerActionService.redo] fails if the current player is not human.
     */
    @Test
    fun `redo throws IllegalStateException for non human player`() {
        val game = createHistoryGame(PlayerType.EASY_BOT, isLocal = true)
        rootService.history.undoneMoves.push(CascadiaGame(game))

        assertFailsWith<IllegalStateException> {
            playerActionService.redo()
        }
    }

    /**
     * Tests that [PlayerActionService.redo] fails in a network game.
     */
    @Test
    fun `redo throws IllegalStateException in network game`() {
        val game = createHistoryGame(PlayerType.HUMAN, isLocal = false)
        rootService.history.undoneMoves.push(CascadiaGame(game))

        assertFailsWith<IllegalStateException> {
            playerActionService.redo()
        }
    }

    /**
     * Tests that [PlayerActionService.undo] fails if no game is running.
     */
    @Test
    fun `undo throws IllegalStateException when no game is running`() {
        rootService.currentGame = null

        assertFailsWith<IllegalStateException> {
            playerActionService.undo()
        }
    }

    /**
     * Tests that [PlayerActionService.undo] fails if the current player is not human.
     */
    @Test
    fun `undo throws IllegalStateException for non human player`() {
        createHistoryGame(PlayerType.EASY_BOT, isLocal = true)

        assertFailsWith<IllegalStateException> {
            playerActionService.undo()
        }
    }

    /**
     * Tests that [PlayerActionService.undo] fails in a network game.
     */
    @Test
    fun `undo throws IllegalStateException in network game`() {
        createHistoryGame(PlayerType.HUMAN, isLocal = false)

        assertFailsWith<IllegalStateException> {
            playerActionService.undo()
        }
    }

}