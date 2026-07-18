package service.network

import edu.udo.cs.sopra.ntf.*
import entity.PlayerType
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.net.client.BoardGameClient
import tools.aqua.bgw.net.client.NetworkLogging
import tools.aqua.bgw.net.common.annotations.GameActionReceiver
import tools.aqua.bgw.net.common.notification.PlayerJoinedNotification
import tools.aqua.bgw.net.common.notification.PlayerLeftNotification
import tools.aqua.bgw.net.common.response.*

/**
 * [BoardGameClient] implementation for network communication.
 *
 * @param playerName the name of the player using this client.
 * @param playerType The Typ of the player connected locally
 * @param host the host to connect to.
 * @param secret the secret to use for the connection.
 * @property networkService the [NetworkService] to potentially forward received messages to.
 */
class CascadiaNetworkClient(
    playerName: String,
    host: String,
    secret: String,
    var networkService: NetworkService,
) : BoardGameClient(playerName, host, secret, NetworkLogging.VERBOSE) {

    /** the identifier of this game session; can be null if no session started yet. */
    var sessionID: String? = null

    var playerType: PlayerType? = null
    var scoringCards = MutableList<Boolean?>(5) { null }

    var players = mutableListOf<Pair<String, PlayerType>>()

    var isHost = false

    var errorMessage = ""

    /**
     * Handle a [CreateGameResponse] sent by the server. Will await the guest player when its
     * status is [CreateGameResponseStatus.SUCCESS]. As recovery from network problems is not
     * implemented in NetWar, the method disconnects from the server and throws an
     * [IllegalStateException] otherwise.
     *
     * @throws IllegalStateException if status != success or currently not waiting for a game creation response.
     */
    override fun onCreateGameResponse(response: CreateGameResponse) {
        check(networkService.connectionState == ConnectionState.WAITING_FOR_HOST_CONFIRMATION) { "unexpected CreateGameResponse" }

        when (response.status) {
            CreateGameResponseStatus.SUCCESS -> {
                networkService.updateConnectionState(ConnectionState.WAITING_FOR_GUESTS)
                sessionID = response.sessionID

                players.add(Pair(playerName, playerType!!))

                networkService.triggerRefresh("createGame")
            }

            else -> {
                when (response.status) {
                    CreateGameResponseStatus.GAME_ID_DOES_NOT_EXIST -> {
                        errorMessage = "This game ID doesn't exist, no idea how you got to this point tbh"
                    }

                    CreateGameResponseStatus.ALREADY_ASSOCIATED_WITH_GAME -> {
                        errorMessage = "You are already in a game, leave it to start a new one"
                    }

                    CreateGameResponseStatus.SERVER_ERROR -> {
                        errorMessage = "Not your fault, the server broke, please come back later"
                    }

                    CreateGameResponseStatus.SESSION_WITH_ID_ALREADY_EXISTS -> {
                        errorMessage =
                            "This game ID is already in use, please use a different one or simply " + "leave the field blank"
                    }

                    else -> {

                    }
                }
                networkService.triggerRefresh("error")
                disconnectAndError(response.status)
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
        check(networkService.connectionState == ConnectionState.WAITING_FOR_JOIN_CONFIRMATION) { "unexpected JoinGameResponse" }

        checkNotNull(playerType) { "A playerType is required before joining a game" }

        when (response.status) {
            JoinGameResponseStatus.SUCCESS -> {
                players = response.opponents.map { Pair(it, PlayerType.NETWORK) }.toMutableList()
                players.add(Pair(playerName, playerType!!))
                sessionID = response.sessionID
                networkService.updateConnectionState(ConnectionState.WAITING_FOR_GUESTS)

                networkService.triggerRefresh("joinGame")
            }

            else -> {
                when (response.status) {
                    JoinGameResponseStatus.PLAYER_NAME_ALREADY_TAKEN -> {
                        errorMessage =
                            "A player with your name already exists in the lobby, please change it in " + "order to join this game"
                    }

                    JoinGameResponseStatus.ALREADY_ASSOCIATED_WITH_GAME -> {
                        errorMessage = "You are already in a game, leave it to start a new one"
                    }

                    JoinGameResponseStatus.SERVER_ERROR -> {
                        errorMessage = "Not your fault, the server broke, please come back later"
                    }

                    JoinGameResponseStatus.INVALID_SESSION_ID -> {
                        errorMessage =
                            "This session ID is invalid, if the host has already started the lobby ask " + "him for the correct ID"
                    }

                    else -> {

                    }
                }
                networkService.triggerRefresh("error")
                disconnectAndError(response.status)
            }
        }
    }

    /**
     * Handle a [PlayerJoinedNotification] sent by the server.
     *
     * @throws IllegalStateException if not currently expecting any guests to join.
     */
    override fun onPlayerJoined(notification: PlayerJoinedNotification) {
        check(networkService.connectionState == ConnectionState.WAITING_FOR_GUESTS) { "not awaiting any guests." }

        players.add(Pair(notification.sender, PlayerType.NETWORK))

        networkService.triggerRefresh("playerChanged")
        if (isHost) networkService.sendGameConfig(players.map {it.first}, scoringCards)

        if (players.size == 4) networkService.updateConnectionState(ConnectionState.WAITING_FOR_INIT)
    }

    /**
     * Handle a [PlayerLeftNotification] sent by the server.
     *
     * @throws IllegalStateException if not currently expecting any guests to join.
     */
    override fun onPlayerLeft(notification: PlayerLeftNotification) {
        players.removeAll { it.first == notification.sender }

        networkService.triggerRefresh("playerChanged")
    }

    /**
     * Handle a [GameActionResponse] sent by the server. Does nothing when its
     * status is [GameActionResponseStatus.SUCCESS]. As recovery from network problems is not
     * implemented in NetWar, the method disconnects from the server and throws an
     * [IllegalStateException] otherwise.
     */
    override fun onGameActionResponse(response: GameActionResponse) {
        val acceptableStates = listOf(
            ConnectionState.WAITING_FOR_GUESTS,
            ConnectionState.WAITING_FOR_INIT,
            ConnectionState.SELECTING,
            ConnectionState.PLACING,
            ConnectionState.WAITING_FOR_HOST_CONFIRMATION
        )

        check(networkService.connectionState in acceptableStates) { "not currently playing in a network game." }

        when (response.status) {
            GameActionResponseStatus.SUCCESS -> {} // do nothing in this case
            else -> {
                when (response.status) {
                    GameActionResponseStatus.INVALID_JSON -> {
                        errorMessage = "This request included invalid json"
                    }

                    GameActionResponseStatus.NO_ASSOCIATED_GAME -> {
                        errorMessage = "You are not in a game, join one or start a new one"
                    }

                    GameActionResponseStatus.SERVER_ERROR -> {
                        errorMessage = "Not your fault, the server broke, please come back later"
                    }

                    GameActionResponseStatus.SPECTATOR_ONLY -> {
                        errorMessage = "This action can only be performed by Spectators"
                    }

                    else -> {

                    }
                }
                networkService.triggerRefresh("error")
                disconnectAndError(response.status)
            }
        }
    }

    /**
     * handle a [GameInitMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onInitReceived(message: GameInitMessage, sender: String) {
        checkNotNull(playerType) { "A playerType is required before initiating a game" }

        networkService.startNewJoinedGame(
            message = message,
            playerName = playerName,
            playerType = playerType!!,
        )
    }

    /**
     * Handle a [GameConfigMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onGameConfigReceived(message: GameConfigMessage, sender: String) {
        check(players.size == message.players.size) { "The player count seems to have changed" }

        players.sortBy { message.players.indexOf(it.first) }

        networkService.receiveGameConfig(message)

    }

    /**
     * Handle a [SelectMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onSelectReceived(message: SelectMessage, sender: String) {
        networkService.receiveSelect(message)
    }

    /**
     * Handle a [PlaceMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onPlaceReceived(message: PlaceMessage, sender: String) {
        networkService.receivePlace(message)
    }

    /**
     * Handle a [WipeWildlifeMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onWipeWildlifeReceived(message: WipeWildlifeMessage, sender: String) {
        networkService.receiveExterminate(message)
    }

    /**
     * Handle a [UseNatureTokenMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onUseNatureTokenReceived(message: UseNatureTokenMessage, sender: String) {
        networkService.receiveUseNatureToken()
    }

    /**
     * Handle a [RotationMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onRotationReceived(message: RotationMessage, sender: String) {
        networkService.receiveRotation(message)
    }

    /**
     * Handle a [SelectWildlifeMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onSelectWildlifeReceived(message: SelectWildlifeMessage, sender: String) {
        networkService.receiveSelectWildlife(message)
    }

    /**
     * Handle a [SelectHabitatTileMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onSelectHabitatTileReceived(message: SelectHabitatTileMessage, sender: String) {
        networkService.receiveSelectHabitatTile(message)
    }

    /**
     * Handle a [ChatMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onChatReceived(message: ChatMessage, sender: String) {
        networkService.receiveChatMessage(message, sender)
    }

    /**
     * Handle a [NetPlayer] sent by the server (Dummy function for warning)
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onPlayerReceived(message: NetPlayer, sender: String) {
        println("For some reason $sender sent a NetPlayer object...")
    }

    private fun disconnectAndError(message: Any) {
        networkService.disconnect()
        error(message)
    }

}