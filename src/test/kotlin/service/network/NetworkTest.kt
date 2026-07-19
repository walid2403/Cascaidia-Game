package service.network

import entity.PlayerType
import edu.udo.cs.sopra.ntf.*
import org.junit.jupiter.api.assertDoesNotThrow
import service.Refreshable
import service.RootService
import kotlin.test.*

/**
 * A class to test the [NetworkService]
 */
class NetworkTest {

    /**
     * This service is initialized in the [setUp] function hence it is a late-initialized property.
     */
    private lateinit var rootService1: RootService
    private lateinit var rootService2: RootService

    private var code = ""

    /**
     * Initialize service to set up the test environment. This function is executed before every test.
     */
    fun setUp(p1Type: PlayerType = PlayerType.HUMAN, p2Type: PlayerType = PlayerType.HUMAN) {
        rootService1 = RootService()
        rootService2 = RootService()

        val refreshable = object : Refreshable {
            override fun refreshAfterHostGame(lobbyCode: String, playerName: String, playerType: PlayerType) {
                code = lobbyCode
            }

            override fun refreshAfterConnectionError(errorMessage: String) {
                println("Error: $errorMessage")
            }
        }
        rootService1.addRefreshable(refreshable)

        rootService1.networkService.hostGame("test1", p1Type, "")
        Thread.sleep(2000)
        rootService2.networkService.joinGame("test2", p2Type, code)

        rootService1.networkService.sendGameConfig(listOf("test1", "test2"), List(5) { true })

        rootService1.networkService.startNewHostedGame()
        Thread.sleep(5000)
    }

    /**
     * A simple test to check if the [RootService] is initialized.
     */
    @Test
    fun testIfSetUpWorked() {
        setUp()

        val game1 = rootService1.currentGame
        checkNotNull(game1) {"Game 1 should have been started"}

        val game2 = rootService2.currentGame
        checkNotNull(game2) {"Game 2 should have been started"}

        assertEquals(game1.choices.map { Pair(it.first.id, it.second.name) },
            game2.choices.map { Pair(it.first.id, it.second.name) },
            "Both clients should have the same choices")
    }

    /**
     * A test to see if a game can be started with incorrect or missing things
     */
    @Test
    fun `test if game can be started with wrong configs`() {
        rootService1 = RootService()

        assertFailsWith<IllegalStateException>("This should fail because no players joined") {
            rootService1.networkService.startNewHostedGame()
        }

        rootService1.networkService.hostGame("test1", PlayerType.HUMAN, "")

        assertFailsWith<IllegalStateException>("This should fail because no scoring cards have been selected") {
            rootService1.networkService.startNewHostedGame()
        }

        rootService1.networkService.sendGameConfig(listOf("test1", "test2"), List(5) { true })

        assertFailsWith<IllegalStateException>("This should fail because only one palyer joined") {
            rootService1.networkService.startNewHostedGame()
        }
    }

    /**
     * A test to see if a turn plays out correctly
     */
    @Test
    fun fullTurn() {
        setUp()

        val game1 = rootService1.currentGame
        checkNotNull(game1) {"Game 1 should have been started"}
        val game2 = rootService2.currentGame
        checkNotNull(game2) {"Game 2 should have been started"}

        rootService1.playerActionService.selectColumn(1)

        Thread.sleep(1000)

        assertEquals(game1.selectedChoice, game2.selectedChoice,
            "Both clients should have the same choices")

        rootService1.playerActionService.placeTile(Triple(-1, 0, 1))
        rootService1.gameService.changeTurn()

        Thread.sleep(1000)

        assertEquals(game1.playerQueue.last().board[Triple(-1,0,1)]?.id,
            game2.playerQueue.last().board[Triple(-1,0,1)]?.id,
            "Both clients should have the same Tile placed")

        assertEquals(game1.wildlifeTokens.peekAll().map {it.name},
            game2.wildlifeTokens.peekAll().map {it.name},
            "Both clients should have the same wildlife bag")
    }

    /**
     * A test to confirm the currentGame == null cases
     */
    @Test
    fun nullTest() {
        val net = rootService1.networkService
        assertFailsWith<IllegalStateException> {net.startNewHostedGame()}
        assertFailsWith<IllegalStateException> {net.startNewHostedGame()}
        assertFailsWith<IllegalStateException> {net.sendSelect(true)}
        assertFailsWith<IllegalStateException> {net.receiveSelect(SelectMessage(0, 0, false))}
        assertFailsWith<IllegalStateException> {net.receivePlace(PlaceMessage(Pair(0, 0), null, 0))}
        assertFailsWith<IllegalStateException> {net.sendExterminate(listOf(), false)}
        assertFailsWith<IllegalStateException> {net.receiveExterminate(WipeWildlifeMessage(false, 0, listOf(), listOf()))}
        assertFailsWith<IllegalStateException> {net.sendRotation()}
    }

    /**
     * A test for the network service
     */
    @Test
    fun noConnectionTest() {
        setUp()

        val rootService3 = RootService()
        assertFails { rootService3.networkService.hostGame("Test", PlayerType.HUMAN, code) }
    }

    /**
     * A test for the network service
     */
    @Test
    fun test() {
        val rootService3 = RootService()
        assertDoesNotThrow { rootService3.networkService.hostGame("Test", PlayerType.HUMAN,
            "DetektSucksSoMuchAndIsVeryPoorlyImplementedInSoPra") }
    }
}