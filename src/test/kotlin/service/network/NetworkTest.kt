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

    /**
     * Initialize service to set up the test environment. This function is executed before every test.
     */
    fun setUp(p1Type: PlayerType = PlayerType.HUMAN, p2Type: PlayerType = PlayerType.HUMAN, lobbyCode: String = "") {
        rootService1 = RootService()
        rootService2 = RootService()

        var code = lobbyCode

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
     * A test to confirm the currentGame == null cases
     */
    @Test
    fun nullTest() {
        rootService1 = RootService()
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
    fun test() {
        val rootService3 = RootService()
        assertDoesNotThrow { rootService3.networkService.hostGame("Test", PlayerType.HUMAN,
            "DetektSucksSoMuchAndIsVeryPoorlyImplementedInSoPra") }
    }
}