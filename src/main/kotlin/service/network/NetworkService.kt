package service.network

import edu.udo.cs.sopra.ntf.*
import entity.Player
import entity.PlayerType
import entity.WildlifeToken
import service.AbstractRefreshingService
import service.RootService
import tools.aqua.bgw.examples.war.service.CascadiaNetworkClient
import kotlin.collections.get

class NetworkService(private val rootService: RootService) : AbstractRefreshingService() {

    companion object {
        /** URL of the BGW net server hosted for SoPra participants */
        const val SERVER_ADDRESS = "sopra.cs.tu-dortmund.de:80/bgw-net/connect"

        /** Name of the game as registered with the server */
        const val GAME_ID = "Cascadia"
    }

    /** Network client. Nullable for offline games. */
    var client: CascadiaNetworkClient? = null
        private set

    /**
     * current state of the connection in a network game.
     */
    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set

    /**
     * Connects to server and creates a new game session.
     *
     * @param secret Server secret.
     * @param name Player name.
     * @param sessionID identifier of the hosted session (to be used by guest on join)
     *
     * @throws IllegalStateException if already connected to another game or connection attempt fails
     */
    fun hostGame(secret: String, name: String, sessionID: String?) {
        if (!connect(secret, name)) {
            error("Connection failed")
        }
        updateConnectionState(ConnectionState.CONNECTED)

        if (sessionID.isNullOrBlank()) {
            client?.createGame(NetworkService.Companion.GAME_ID, "Welcome!")
        } else {
            client?.createGame(NetworkService.Companion.GAME_ID, sessionID, "Welcome!")
        }
        updateConnectionState(ConnectionState.WAITING_FOR_HOST_CONFIRMATION)
    }

    /**
     * Disconnects the [client] from the server, nulls it and updates the
     * [connectionState] to [ConnectionState.DISCONNECTED]. Can safely be called
     * even if no connection is currently active.
     */
    fun disconnect() {
        client?.apply {
            if (sessionID != null) leaveGame("Goodbye!")
            if (isOpen) disconnect()
        }
        client = null
        updateConnectionState(ConnectionState.DISCONNECTED)
    }

    /**
     * Connects to server and joins a game session as guest player.
     *
     * @param secret Server secret.
     * @param name Player name.
     * @param sessionID identifier of the joined session (as defined by host on create)
     *
     * @throws IllegalStateException if already connected to another game or connection attempt fails
     */
    fun joinGame(name: String, sessionID: String, secret: String = "wildlife") {
        if (!connect(secret, name)) {
            error("Connection failed")
        }
        updateConnectionState(ConnectionState.CONNECTED)

        client?.joinGame(sessionID, "Hello!")

        updateConnectionState(ConnectionState.WAITING_FOR_JOIN_CONFIRMATION)
    }

    /**
     * Connects to server, sets the [NetworkService.client] if successful and returns `true` on success.
     *
     * @param secret Network secret. Must not be blank (i.e. empty or only whitespaces)
     * @param name Player name. Must not be blank
     *
     * @throws IllegalArgumentException if secret or name is blank
     * @throws IllegalStateException if already connected to another game
     */
    private fun connect(name: String, secret: String = "wildlife"): Boolean {
        require(connectionState == ConnectionState.DISCONNECTED && client == null)
        { "already connected to another game" }

        require(secret.isNotBlank()) { "server secret must be given" }
        require(name.isNotBlank()) { "player name must be given" }

        val newClient =
            CascadiaNetworkClient(
                playerName = name,
                host = SERVER_ADDRESS,
                secret = secret,
                networkService = this
            )

        return if (newClient.connect()) {
            this.client = newClient
            true
        } else {
            false
        }
    }

    /**
     * set up the game using [GameService.startNewGame] and send the game init message
     * to the guest player. [connectionState] needs to be [ConnectionState.WAITING_FOR_GUEST].
     * This method should be called from the [WarNetworkClient] when the guest joined notification
     * arrived. See [WarNetworkClient.onPlayerJoined].
     *
     * @param hostPlayerName player name of the host player
     * @param guestPlayerName player name of the guest player
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.WAITING_FOR_GUEST]
     */
    fun startNewHostedGame(playerNames : List<Pair<String, PlayerType>>, scoringCards : List<Boolean>) {
        check(connectionState == ConnectionState.WAITING_FOR_GUESTS && playerNames.size in 2..4)
        { "currently not prepared to start a new hosted game." }

        rootService.gameService.startNewGame(playerNames, scoringCards)
        val game = rootService.currentGame
        checkNotNull(game) { "game should not be null right after starting it." }

        val tileList = game.choices.map {it.first.id}.reversed().toMutableList()
        tileList.addAll(game.tileStack.peekAll().map { it.id }.reversed())

        val reorderList = listOf(0, 1, 4, 3, 2)

        val wildlifeList = game.choices.map { NetWildlife.valueOf(it.second.name) }.reversed().toMutableList()
        wildlifeList.addAll(game.wildlifeTokens.peekAll().map { NetWildlife.valueOf(it.name) }.reversed())

        val message = GameInitMessage(
            tileList, reorderList.map { scoringCards[it] },
            game.playerQueue.map { NetPlayer(it.name, it.board[Triple(0,0,0)]?.id ?: 0) }, wildlifeList
        )

        if (game.playerQueue.peek().type == PlayerType.NETWORK) updateConnectionState(ConnectionState.WAITING_FOR_PLAYER_TURN)
        else updateConnectionState(ConnectionState.PLACING)
        client?.sendGameActionMessage(message)
    }

    /**
     * Initializes the entity structure with the data given by the [NetWarGameInitMessage] sent by the host.
     * [connectionState] needs to be [ConnectionState.WAITING_FOR_INIT].
     * This method should be called from the [WarNetworkClient] when the host sends the init message.
     * See [WarNetworkClient.onInitReceived].
     *
     * @throws IllegalStateException if not currently waiting for an init message
     */
    fun startNewJoinedGame(message: GameInitMessage, playerName: String, playerType: PlayerType) {
        check(connectionState == ConnectionState.WAITING_FOR_INIT)
        { "not waiting for game init message. " }

        val reorderList = listOf(0, 1, 4, 3, 2)

        rootService.gameService.startNewGame(
            message.players.map { Pair(it.name, if (it.name == playerName) playerType else PlayerType.NETWORK) },
            reorderList.map { message.scoringCards[it] },
            message.players.map { it.startingTileID },
            message.tileStack,
            message.wildlifeBag.map { WildlifeToken.valueOf(it.name)}
        )

        val game = rootService.currentGame
        checkNotNull(game) { "Game should not be null right after starting it." }

        if (game.playerQueue.peek().type == PlayerType.NETWORK) updateConnectionState(ConnectionState.WAITING_FOR_PLAYER_TURN)
        else updateConnectionState(ConnectionState.PLACING)
    }

    fun sendSelect(unlockedChoices: Boolean) {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game found" }

        val message = SelectMessage(
            game.selectedChoice.second,
            game.selectedChoice.first,
            unlockedChoices
        )

        client?.sendGameActionMessage(message)
    }

    fun receiveSelect(message: SelectMessage) {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game found" }

        if (message.usedNatureToken)
            rootService.playerActionService.freeSelection(message.habitatShopIndex, message.wildlifeShopIndex)
        else {
            check(message.habitatShopIndex == message.wildlifeShopIndex) {
                "Both indices have to be the same if you don't spend a nature token"
            }

            rootService.playerActionService.selectColumn(message.habitatShopIndex)
        }
    }

    fun sendPlace(habCoords: Triple<Int, Int, Int>, tokenCoords: Triple<Int, Int, Int>?, habRotation: Int) {
        val wildlifeCoords = if (tokenCoords != null) {
            Pair(tokenCoords.first, tokenCoords.second)
        } else {
            null
        }

        val message: PlaceMessage = PlaceMessage(
            Pair(habCoords.first, habCoords.second), wildlifeCoords, habRotation
        )

        client?.sendGameActionMessage(message)
    }

    fun receivePlace(message: PlaceMessage) {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game found" }

        val habCoords: Triple<Int, Int, Int> = Triple(message.habitatCoordinates.first, message.habitatCoordinates.second,
            -(message.habitatCoordinates.first + message.habitatCoordinates.second))

        rootService.playerActionService.placeTile(habCoords)
        game.playerQueue.peek().board[habCoords]?.rotation = message.habitatRotation

        if (message.wildlifeCoordinates != null) {
            val tokenCoords: Triple<Int, Int, Int> = Triple(
                message.wildlifeCoordinates!!.first, message.wildlifeCoordinates!!.second,
                -(message.wildlifeCoordinates!!.first + message.wildlifeCoordinates!!.second))

            rootService.playerActionService.placeWildlife(tokenCoords)
        }
    }

    fun sendExterminate(indices: List<Int>, natureToken: Boolean) {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game found" }

        val wildlifeList = game.choices.map { NetWildlife.valueOf(it.second.name) }.reversed().toMutableList()
        wildlifeList.addAll(game.wildlifeTokens.peekAll().map { NetWildlife.valueOf(it.name) }.reversed())

        val message = WipeWildlifeMessage(
            natureToken, game.playerQueue.peek().natureTokens, indices, wildlifeList
        )
    }

    fun receiveExterminate(message: WipeWildlifeMessage) {

    }

    fun sendUseNatureToken() {
        client?.sendGameActionMessage(UseNatureTokenMessage())
    }

    fun receiveUseNatureToken() {
        onAllRefreshables { refreshAfterUseNatureToken() }
    }

    fun sendGameConfig() {

    }

    fun receiveGameConfig(message: GameConfigMessage) {

    }

    fun sendSelectWildlife() {

    }

    fun receiveSelectWildlife(message: SelectWildlifeMessage) {

    }

    fun sendRotation() {

    }

    fun receiveRotation(message: RotationMessage) {

    }

    fun sendSelectHabitatTile() {

    }

    fun receiveSelectHabitatTile(message: SelectHabitatTileMessage) {

    }

    fun sendChatMessage(message: String) {
        val message = ChatMessage(message)
        client?.sendGameActionMessage(message)
    }

    fun receiveChatMessage(message: ChatMessage, messageSender: String) {
        onAllRefreshables { refreshAfterChatMessage(messageSender, message.message) }
    }

    /**
     * Updates the [connectionState] to [newState] and notifies
     * all refreshables via [Refreshable.refreshConnectionState]
     */
    fun updateConnectionState(newState: ConnectionState) {
        this.connectionState = newState
    }
}