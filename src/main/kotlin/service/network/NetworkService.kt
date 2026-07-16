package service.network

import edu.udo.cs.sopra.ntf.*
import entity.GameState
import entity.Player
import entity.PlayerType
import entity.WildlifeToken
import service.AbstractRefreshingService
import service.RootService

class NetworkService(private val rootService: RootService) : AbstractRefreshingService() {

    /** URL of the BGW net server hosted for SoPra participants */
    private val serverAddress = "sopra.cs.tu-dortmund.de:80/bgw-net-test/connect"

    /** Name of the game as registered with the server */
    private val gameID = "Cascadia"

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
    fun hostGame(name: String, playerType: PlayerType, sessionID: String, secret: String = "wildlife") {
        if (!connect(name, secret)) {
            error("Connection failed")
        }

        client?.playerType = playerType

        updateConnectionState(ConnectionState.CONNECTED)

        if (sessionID.isNullOrBlank()) {
            client?.createGame(gameID, "Welcome!")
        } else {
            client?.createGame(gameID, sessionID, "Welcome!")
        }
        updateConnectionState(ConnectionState.WAITING_FOR_HOST_CONFIRMATION)
    }

    fun triggerRefresh(refresh: String) {
        when (refresh) {
            "createGame" -> {
                val sessionID = client?.sessionID
                checkNotNull(sessionID)

                val playerName = client?.playerName
                checkNotNull(playerName)

                val playerType = client?.playerType
                checkNotNull(playerType)

                onAllRefreshables { refreshAfterHostGame(sessionID, playerName, playerType) }
            }
            "joinGame" -> {
                val sessionID = client?.sessionID
                checkNotNull(sessionID)

                val playerName = client?.playerName
                checkNotNull(playerName)

                val playerType = client?.playerType
                checkNotNull(playerType)

                onAllRefreshables { refreshAfterJoinGame(sessionID, playerName, playerType) }

                val playerNames = client?.players?.map {it.first}
                checkNotNull(playerNames) { "After joining a game the names should not be empty" }

                val scoringCards = client?.scoringCards?.toList()
                checkNotNull(scoringCards)

                onAllRefreshables { refreshAfterGameConfigUpdate(playerNames, scoringCards) }
            }
            "playerChanged" -> {
                val playerNames = client?.players?.map {it.first}
                checkNotNull(playerNames) { "After joining a game the names should not be empty" }

                val scoringCards = client?.scoringCards?.toList()
                checkNotNull(scoringCards)

                onAllRefreshables { refreshAfterGameConfigUpdate(playerNames, scoringCards) }
            }
            "error" -> {
                println(client?.errorMessage)
                onAllRefreshables { refreshAfterConnectionError(client?.errorMessage ?: "") }
            }
        }
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
    fun joinGame(name: String, playerType: PlayerType, sessionID: String, secret: String = "wildlife") {
        if (!connect(name, secret)) {
            error("Connection failed")
        }

        client?.playerType = playerType

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
                host = serverAddress,
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
    fun startNewHostedGame() {
        val playerNames = client?.players?.toList()
        checkNotNull(playerNames) { "Player names must be entered" }

        check(client?.scoringCards?.all {it != null} ?: false)
        val scoringCards = client?.scoringCards as List<Boolean>

        check(connectionState == ConnectionState.WAITING_FOR_GUESTS && playerNames.size in 2..4)
        { "currently not prepared to start a new hosted game." }

        rootService.gameService.startNewGame(playerNames, scoringCards)
        val game = rootService.currentGame
        checkNotNull(game) { "game should not be null right after starting it." }

        val tileList = game.tileStack.peekAll().map { it.id }.reversed().toMutableList()
        tileList.addAll(game.choices.map {it.first.id}.reversed())

        val reorderList = listOf(0, 1, 4, 3, 2)

        val wildlifeList = game.wildlifeTokens.peekAll().map { NetWildlife.valueOf(it.name) }.reversed().toMutableList()
        wildlifeList.addAll(game.choices.map { NetWildlife.valueOf(it.second.name) }.reversed())

        val message = GameInitMessage(
            tileList, reorderList.map { scoringCards[it] },
            game.playerQueue.map { NetPlayer(it.name, (it.board[Triple(0,0,0)]?.id ?: 0) / 10) }, wildlifeList
        )

        if (game.playerQueue.peek().type == PlayerType.NETWORK) updateConnectionState(ConnectionState.WAITING_FOR_PLAYER_TURN)
        else updateConnectionState(ConnectionState.PLACING)
        client?.sendGameActionMessage(message)

        onAllRefreshables { refreshAfterStartGame() }
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

        onAllRefreshables { refreshAfterStartGame() }
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
            Pair(tokenCoords.third, tokenCoords.second)
        } else {
            null
        }

        val message = PlaceMessage(
            Pair(habCoords.third, habCoords.second), wildlifeCoords, habRotation
        )

        client?.sendGameActionMessage(message)
    }

    fun receivePlace(message: PlaceMessage) {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game found" }

        val habCoords: Triple<Int, Int, Int> = Triple(
            (message.habitatCoordinates.first + message.habitatCoordinates.second) * (-1),
            message.habitatCoordinates.second, message.habitatCoordinates.first)

        rootService.playerActionService.placeTile(habCoords)
        rootService.playerActionService.rotateTile(null, message.habitatRotation, habCoords)
//        game.playerQueue.peek().board[habCoords]?.rotation = message.habitatRotation
//
//        for (i in 1..message.habitatRotation) {
//            val hab = game.playerQueue.peek().board[habCoords]?.habs?.removeLast()
//            checkNotNull(hab)
//            game.playerQueue.peek().board[habCoords]?.habs?.add(1, hab)
//        }

        onAllRefreshables { refreshAfterPlaceTile(habCoords) }

        if (message.wildlifeCoordinates != null) {
            val tokenCoords: Triple<Int, Int, Int> = Triple(
                (message.wildlifeCoordinates!!.first + message.wildlifeCoordinates!!.second) * (-1),
                message.wildlifeCoordinates!!.second, message.wildlifeCoordinates!!.first)

            rootService.playerActionService.placeWildlife(tokenCoords)

            rootService.gameService.changeTurn()
        }
    }

    fun sendExterminate(indices: List<Int>, natureToken: Boolean) {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game found" }

        val wildlifeList = game.wildlifeTokens.peekAll().map { NetWildlife.valueOf(it.name) }.reversed().toMutableList()

        val message = WipeWildlifeMessage(
            natureToken, game.playerQueue.peek().natureTokens, indices, wildlifeList
        )

        client?.sendGameActionMessage(message)
    }

    fun receiveExterminate(message: WipeWildlifeMessage) {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game found" }

        if (message.usedNatureToken) game.playerQueue.peek().natureTokens--

//        check(game.playerQueue.peek().natureTokens == message.natureTokenAmount) { "Difference in Nature Tokens detected" }

        val wildlifeBag = message.wildlifeBag.toMutableList()

        message.wipedWildlifeIndices.sorted().forEach {
            game.choices[it] = Pair(game.choices[it].first, game.wildlifeTokens.pop())
        }

        game.wildlifeTokens.clear()
        game.wildlifeTokens.pushAll(wildlifeBag.map {WildlifeToken.valueOf(it.name) })

        onAllRefreshables { refreshAfterChangeWildlife(message.wipedWildlifeIndices) }

        if (message.wipedWildlifeIndices.isEmpty() && game.gameState == GameState.PLAYED_TILE) {
            rootService.gameService.changeTurn()
        }
    }

    fun sendUseNatureToken() {
        client?.sendGameActionMessage(UseNatureTokenMessage("Very important message"))
    }

    fun receiveUseNatureToken() {
        onAllRefreshables { refreshAfterUnlockSelection() }
    }

    fun sendGameConfig(playerList: List<String>, scoringCards: List<Boolean?>) {
        client?.players?.sortBy { playerList.indexOf(it.first) }
        client?.scoringCards = scoringCards.toMutableList()
        client?.sendGameActionMessage(GameConfigMessage(playerList, scoringCards))
    }

    fun receiveGameConfig(message: GameConfigMessage) {
        onAllRefreshables { refreshAfterGameConfigUpdate(message.players, message.scoringCards) }
    }

    fun sendSelectWildlife(wildlifeIndex: Int) {
        client?.sendGameActionMessage(SelectWildlifeMessage(wildlifeIndex))
    }

    fun receiveSelectWildlife(message: SelectWildlifeMessage) {
        onAllRefreshables { refreshAfterSelectWildlife(message.wildlifeIndex) }
    }

    fun sendRotation() {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game found" }

        client?.sendGameActionMessage(RotationMessage(game.choices[game.selectedChoice.first].first.rotation))
    }

    fun receiveRotation(message: RotationMessage) {
        rootService.playerActionService.rotateTile(null, message.habitatRotation)
    }

    fun sendSelectHabitatTile(tileIndex: Int) {
        client?.sendGameActionMessage(SelectHabitatTileMessage(tileIndex))
    }

    fun receiveSelectHabitatTile(message: SelectHabitatTileMessage) {
        onAllRefreshables { refreshAfterSelectTile(message.habitatIndex) }
    }

    fun sendChatMessage(message: String) {
        val message = ChatMessage(message)
        client?.sendGameActionMessage(message)
    }

    fun receiveChatMessage(message: ChatMessage, messageSender: String) {
        onAllRefreshables { refreshAfterChatMessage(messageSender, message.message) }
    }

    /**
     * Updates the [connectionState] to [newState]
     */
    fun updateConnectionState(newState: ConnectionState) {
        this.connectionState = newState
    }
}