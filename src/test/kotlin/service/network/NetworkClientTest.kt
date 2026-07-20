package service.network

import entity.PlayerType
import service.RootService
import tools.aqua.bgw.net.common.notification.PlayerJoinedNotification
import tools.aqua.bgw.net.common.notification.PlayerLeftNotification
import tools.aqua.bgw.net.common.response.CreateGameResponse
import tools.aqua.bgw.net.common.response.CreateGameResponseStatus
import tools.aqua.bgw.net.common.response.Errors
import tools.aqua.bgw.net.common.response.GameActionResponse
import tools.aqua.bgw.net.common.response.GameActionResponseStatus
import tools.aqua.bgw.net.common.response.JoinGameResponse
import tools.aqua.bgw.net.common.response.JoinGameResponseStatus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFails

/**
 * A simple test class to demonstrate a basic unit test.
 */
class NetworkClientTest {

    /**
     * This service is initialized in the [setUp] function hence it is a late-initialized property.
     */
    private lateinit var rootService: RootService
    private lateinit var networkService: NetworkService
    private lateinit var networkClient: CascadiaNetworkClient

    /**
     * Initialize service to set up the test environment. This function is executed before every test.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        networkService = rootService.networkService
        networkClient = CascadiaNetworkClient("test", "test", "test", networkService)
    }

    /**
     * A simple test
     */
    @Test
    fun testOnCreateGameResponse() {
        networkService.updateConnectionState(ConnectionState.CONNECTED)

        assertFails { networkClient.onCreateGameResponse(CreateGameResponse(
            Pair(CreateGameResponseStatus.GAME_ID_DOES_NOT_EXIST, null))) }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnCreateGameResponseFails() {
        networkService.updateConnectionState(ConnectionState.WAITING_FOR_HOST_CONFIRMATION)

        assertFails { networkClient.onCreateGameResponse(CreateGameResponse(
            Pair(CreateGameResponseStatus.GAME_ID_DOES_NOT_EXIST, null))) }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnCreateGameResponseFails2() {
        networkService.updateConnectionState(ConnectionState.WAITING_FOR_HOST_CONFIRMATION)

        assertFails {
            networkClient.onCreateGameResponse(
                CreateGameResponse(
                    Pair(CreateGameResponseStatus.ALREADY_ASSOCIATED_WITH_GAME, null)
                )
            )
        }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnCreateGameResponseFails3() {
        networkService.updateConnectionState(ConnectionState.WAITING_FOR_HOST_CONFIRMATION)

        assertFails { networkClient.onCreateGameResponse(CreateGameResponse(
            Pair(CreateGameResponseStatus.SERVER_ERROR, null))) }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnCreateGameResponseFails4() {
        networkService.updateConnectionState(ConnectionState.WAITING_FOR_HOST_CONFIRMATION)

        assertFails { networkClient.onCreateGameResponse(CreateGameResponse(
            Pair(CreateGameResponseStatus.SESSION_WITH_ID_ALREADY_EXISTS, null))) }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnJoinGameResponse() {
        networkService.updateConnectionState(ConnectionState.CONNECTED)

        assertFails { networkClient.onJoinGameResponse(JoinGameResponse(JoinGameResponseStatus.SERVER_ERROR,
            "null", listOf("Peter"), "Peter")) }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnJoinGameResponseFails() {
        networkService.updateConnectionState(ConnectionState.CONNECTED)

        assertFails { networkClient.onJoinGameResponse(JoinGameResponse(
            JoinGameResponseStatus.PLAYER_NAME_ALREADY_TAKEN,
            "null", listOf("Peter"), "Peter")) }
        assertFails { networkClient.onJoinGameResponse(JoinGameResponse(
            JoinGameResponseStatus.ALREADY_ASSOCIATED_WITH_GAME,
            "null", listOf("Peter"), "Peter")) }
        assertFails { networkClient.onJoinGameResponse(JoinGameResponse(JoinGameResponseStatus.SERVER_ERROR,
            "null", listOf("Peter"), "Peter")) }
        assertFails { networkClient.onJoinGameResponse(JoinGameResponse(JoinGameResponseStatus.INVALID_SESSION_ID,
            "null", listOf("Peter"), "Peter")) }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnJoinGameResponseFails2() {
        networkService.updateConnectionState(ConnectionState.CONNECTED)

        assertFails { networkClient.onJoinGameResponse(JoinGameResponse(
            JoinGameResponseStatus.ALREADY_ASSOCIATED_WITH_GAME,
            "null", listOf("Peter"), "Peter")) }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnJoinGameResponseFails3() {
        networkService.updateConnectionState(ConnectionState.CONNECTED)

        assertFails { networkClient.onJoinGameResponse(JoinGameResponse(JoinGameResponseStatus.SERVER_ERROR,
            "null", listOf("Peter"), "Peter")) }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnJoinGameResponseFails4() {
        networkService.updateConnectionState(ConnectionState.CONNECTED)

        assertFails { networkClient.onJoinGameResponse(JoinGameResponse(JoinGameResponseStatus.INVALID_SESSION_ID,
            "null", listOf("Peter"), "Peter")) }
    }


    /**
     * A simple test
     */
    @Test
    fun testOnPlayerJoined() {
        networkService.updateConnectionState(ConnectionState.CONNECTED)

        assertFails { networkClient.onPlayerJoined(PlayerJoinedNotification("", "")) }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnGameActionResponse() {
        networkService.updateConnectionState(ConnectionState.CONNECTED)

        assertFails { networkClient.onGameActionResponse(GameActionResponse(GameActionResponseStatus.SUCCESS,
            Errors(emptyMap()))) }

        networkService.updateConnectionState(ConnectionState.WAITING_FOR_GUESTS)

        assertFails { networkClient.onGameActionResponse(GameActionResponse(
            GameActionResponseStatus.INVALID_JSON, Errors(emptyMap()))) }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnGameActionResponse2() {
        networkService.updateConnectionState(ConnectionState.CONNECTED)

        assertFails { networkClient.onGameActionResponse(GameActionResponse(GameActionResponseStatus.SUCCESS,
            Errors(emptyMap()))) }

        networkService.updateConnectionState(ConnectionState.WAITING_FOR_GUESTS)

        assertFails { networkClient.onGameActionResponse(GameActionResponse(
            GameActionResponseStatus.NO_ASSOCIATED_GAME, Errors(emptyMap()))) }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnGameActionResponse3() {
        networkService.updateConnectionState(ConnectionState.CONNECTED)

        assertFails { networkClient.onGameActionResponse(GameActionResponse(GameActionResponseStatus.SUCCESS,
            Errors(emptyMap()))) }

        networkService.updateConnectionState(ConnectionState.WAITING_FOR_GUESTS)

        assertFails { networkClient.onGameActionResponse(GameActionResponse(
            GameActionResponseStatus.SERVER_ERROR, Errors(emptyMap()))) }
    }

    /**
     * A simple test
     */
    @Test
    fun testOnGameActionResponse4() {
        networkService.updateConnectionState(ConnectionState.CONNECTED)

        assertFails { networkClient.onGameActionResponse(GameActionResponse(GameActionResponseStatus.SUCCESS,
            Errors(emptyMap()))) }

        networkService.updateConnectionState(ConnectionState.WAITING_FOR_GUESTS)

        assertFails { networkClient.onGameActionResponse(GameActionResponse(
            GameActionResponseStatus.SPECTATOR_ONLY, Errors(emptyMap()))) }
    }
}