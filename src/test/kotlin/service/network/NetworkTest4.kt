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
class NetworkTest4 {

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
     * A test for the network service
     */
    @Test
    fun receivePlaceTest() {
        setUp(lobbyCode = "DieZusammenarbeitBeiDerNTFHatJaEchtMegaGutGeklapptSoGanzOhneMissverständisse...")

        val game1 = rootService1.currentGame
        checkNotNull(game1) {"Game 1 should have been started"}
        val game2 = rootService2.currentGame
        checkNotNull(game2) {"Game 2 should have been started"}

        game1.selectedChoice = Pair(1, 1)

        rootService1.networkService.sendSelect(false)
        Thread.sleep(2000)

        rootService2.networkService.receivePlace(PlaceMessage(Pair(0, -1), null, 0))

        assertEquals(game1.wildlifeTokens.peekAll().map {it.name}, game2.wildlifeTokens.peekAll().map {it.name},)
    }
}