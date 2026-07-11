package service.network

import edu.udo.cs.sopra.ntf.*
import entity.PlayerType
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.net.client.BoardGameClient
import tools.aqua.bgw.net.client.NetworkLogging
import tools.aqua.bgw.net.common.annotations.GameActionReceiver
import tools.aqua.bgw.net.common.notification.PlayerJoinedNotification
import tools.aqua.bgw.net.common.response.*

/**
 * [BoardGameClient] implementation for network communication.
 *
 * @param playerName the name of the player using this client.
 * @param host the host to connect to.
 * @param secret the secret to use for the connection.
 * @property networkService the [NetworkService] to potentially forward received messages to.
 */
class CascadiaNetworkClient(
    playerName: String,
    host: String,
    secret: String,
    var networkService: NetworkService,
): BoardGameClient(playerName, host, secret, NetworkLogging.VERBOSE) {

    /** the identifier of this game session; can be null if no session started yet. */
    var sessionID: String? = null

    var playerType: PlayerType? = null

    var players = mutableListOf<Pair<String, PlayerType>>()

    /**
     * Handle a [CreateGameResponse] sent by the server. Will await the guest player when its
     * status is [CreateGameResponseStatus.SUCCESS]. As recovery from network problems is not
     * implemented in NetWar, the method disconnects from the server and throws an
     * [IllegalStateException] otherwise.
     *
     * @throws IllegalStateException if status != success or currently not waiting for a game creation response.
     */
    override fun onCreateGameResponse(response: CreateGameResponse) {
        BoardGameApplication.runOnGUIThread {
            check(networkService.connectionState == ConnectionState.WAITING_FOR_HOST_CONFIRMATION)
            { "unexpected CreateGameResponse" }

            when (response.status) {
                CreateGameResponseStatus.SUCCESS -> {
                    networkService.updateConnectionState(ConnectionState.WAITING_FOR_GUESTS)
                    sessionID = response.sessionID

                    networkService.triggerRefresh("createGame")
                }
                else -> disconnectAndError(response.status)
            }
        }
    }

    /**
     * Handle a [JoinGameResponse] sent by the server. Will await the init message when its
     * status is [JoinGameResponseStatus.SUCCESS]. As recovery from network problems is not
     * implemented in NetWar, the method disconnects from the server and throws an
     * [IllegalStateException] otherwise.
     *
     * @throws IllegalStateException if status != success or currently not waiting for a join game response.
     */
    override fun onJoinGameResponse(response: JoinGameResponse) {
        BoardGameApplication.runOnGUIThread {
            check(networkService.connectionState == ConnectionState.WAITING_FOR_JOIN_CONFIRMATION)
            { "unexpected JoinGameResponse" }

            checkNotNull(playerType) { "A playerType is required before joining a game" }

            when (response.status) {
                JoinGameResponseStatus.SUCCESS -> {
                    players = response.opponents.map { Pair(it, PlayerType.NETWORK) }.toMutableList()
                    players.add(Pair(playerName, playerType!!))
                    sessionID = response.sessionID
                    networkService.updateConnectionState(ConnectionState.WAITING_FOR_INIT)

                    networkService.triggerRefresh("joinGame")
                }
                else -> disconnectAndError(response.status)
            }
        }
    }

    /**
     * Handle a [PlayerJoinedNotification] sent by the server. As War only supports two players,
     * this will immediately start the hosted game (and send the init message to the opponent).
     *
     * @throws IllegalStateException if not currently expecting any guests to join.
     */
    override fun onPlayerJoined(notification: PlayerJoinedNotification) {
        BoardGameApplication.runOnGUIThread {
            check(networkService.connectionState == ConnectionState.WAITING_FOR_GUESTS )
            { "not awaiting any guests."}

            players.add(Pair(notification.sender, PlayerType.NETWORK))
        }
    }

    /**
     * Handle a [GameActionResponse] sent by the server. Does nothing when its
     * status is [GameActionResponseStatus.SUCCESS]. As recovery from network problems is not
     * implemented in NetWar, the method disconnects from the server and throws an
     * [IllegalStateException] otherwise.
     */
    override fun onGameActionResponse(response: GameActionResponse) {
        BoardGameApplication.runOnGUIThread {
            val acceptableStates = listOf(ConnectionState.WAITING_FOR_GUESTS, ConnectionState.WAITING_FOR_INIT,
                ConnectionState.SELECTING, ConnectionState.PLACING, ConnectionState.WAITING_FOR_HOST_CONFIRMATION)

            check(networkService.connectionState in acceptableStates)
            { "not currently playing in a network game."}

            when (response.status) {
                GameActionResponseStatus.SUCCESS -> {} // do nothing in this case
                else -> disconnectAndError(response.status)
            }
        }
    }

    /**
     * handle a [GameInitMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onInitReceived(message: GameInitMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            checkNotNull(playerType) { "A playerType is required before initiating a game" }

            networkService.startNewJoinedGame(
                message = message,
                playerName = playerName,
                playerType = playerType!!,
            )
        }
    }

    /**
     * Handle a [GameConfigMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onGameConfigReceived(message: GameConfigMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            check(players.size == message.players.size) { "The player count seems to have changed" }

            players.sortBy {message.players.indexOf(it.first)}

            networkService.receiveGameConfig(message)
        }
    }

    /**
     * Handle a [SelectMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onSelectReceived(message: SelectMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {

        }
    }

    /**
     * Handle a [PlaceMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onPlaceReceived(message: PlaceMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {

        }
    }

    /**
     * Handle a [WipeWildlifeMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onWipeWildlifeReceived(message: WipeWildlifeMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {

        }
    }

    /**
     * Handle a [UseNatureTokenMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onUseNatureTokenReceived(message: UseNatureTokenMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {

        }
    }

    /**
     * Handle a [RotationMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onRotationReceived(message: RotationMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {

        }
    }

    /**
     * Handle a [SelectWildlifeMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onSelectWildlifeReceived(message: SelectWildlifeMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {

        }
    }

    /**
     * Handle a [SelectHabitatTileMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onSelectHabitatTileReceived(message: SelectHabitatTileMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {

        }
    }

    /**
     * Handle a [ChatMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onChatReceived(message: ChatMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {

        }
    }


    private fun disconnectAndError(message: Any) {
        networkService.disconnect()
        error(message)
    }

}