package service

import entity.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import service.bot.BotLocaleOptimum

/**
 * A class to test the [service.bot.BotLocaleOptimum]
 */
class BotLocaleOptimumTest {

    private lateinit var rootService: RootService

    /**
     * Initialize service to set up the test environment. This function is executed before every test.
     */
    @BeforeEach
    fun setUp() {
        println("setUp is running")
        rootService = RootService()

        println("Before startGame")

        rootService.gameService.startNewGame(
            listOf(
                "Bot" to PlayerType.HARD_BOT,
                "Opponent" to PlayerType.HUMAN
            ),
            List(5) { true }
        )
    }

    private fun currentGame() = rootService.currentGame!!

    /**
     * A test for the [BotLocaleOptimum]
     */
    @Test
    fun `bot makes a market selection without crashing`() {
        val bot = BotLocaleOptimum(rootService, rootService.bot)
        bot.makeTurn()
        assertEquals(GameState.MADE_CHOICE, currentGame().gameState)
    }

    }