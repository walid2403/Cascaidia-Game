package service.bot

import entity.*
import service.RootService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Klasse um den Greedy Bot zu testen.
 */
class BotLocaleOptimumTest {

    private lateinit var rootService: RootService

    /**
     * Methode die einen RootService für die anderen Tests initialisiert und ein Spiel startet
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
     * Testet, ob der Bot eine Market Selection machen kann
     */
    @Test
    fun `bot makes a market selection without crashing`() {
        val bot = BotLocaleOptimum(rootService, rootService.bot)
        bot.makeTurn()
        assertEquals(GameState.MADE_CHOICE, currentGame().gameState)
    }
}