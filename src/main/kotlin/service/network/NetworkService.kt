package service.network

import edu.udo.cs.sopra.ntf.*
import entity.*
import service.AbstractRefreshingService
import service.RootService

/**
 * The NetworkService Class, it implements all necessary functions to play online, in combination with the
 * [CascadiaNetworkClient]
 * @param rootService Link to the [RootService] class
 * @property rootService Link to the [RootService] class
 */
class NetworkService(private val rootService: RootService) : AbstractRefreshingService() {

    /** URL of the BGW net server hosted for SoPra participants */
    private val serverAddress = "sopra.cs.tu-dortmund.de:80/bgw-net/connect"

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
        client?.isHost = true

        updateConnectionState(ConnectionState.CONNECTED)

        if (sessionID.isBlank()) {
            client?.createGame(gameID, "Welcome!")
        } else {
            client?.createGame(gameID, sessionID, "Welcome!")
        }
        updateConnectionState(ConnectionState.WAITING_FOR_HOST_CONFIRMATION)
    }

    /**
     * Helper function to trigger refreshes from the [CascadiaNetworkClient]
     *
     * @param refresh A [String] to manage which refresh should be triggered
     */
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
        println("SessionID: ${client?.sessionID}, isOpen: ${client?.isOpen}")
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
     * set up the game using [service.GameService.startNewGame] and send the game init message
     * to the guest player. [connectionState] needs to be [ConnectionState.WAITING_FOR_GUESTS].
     * This method should be called from the [CascadiaNetworkClient] when the guest joined notification
     * arrived. See [CascadiaNetworkClient.onPlayerJoined].
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.WAITING_FOR_GUESTS]
     */
    fun startNewHostedGame() {
        val playerNames = client?.players?.toList()
        checkNotNull(playerNames) { "Player names must be entered" }

        check(client?.scoringCards?.all {it != null} ?: false)
        val scoringCards = client?.scoringCards as List<Boolean>

        check(connectionState == ConnectionState.WAITING_FOR_GUESTS && playerNames.size in 2..4)
        { "currently not prepared to start a new hosted game." }

        val reorderList = listOf(0, 1, 4, 3, 2)

        rootService.gameService.startNewGame(playerNames, reorderList.map { scoringCards[it] })
        val game = rootService.currentGame
        checkNotNull(game) { "game should not be null right after starting it." }

        val tileList = game.tileStack.peekAll().map { it.id }.reversed().toMutableList()
        tileList.addAll(game.choices.map {it.first.id}.reversed())

//        game.wildlifeTokens.popAll(21)
//        for (i in 0..20) game.wildlifeTokens.push(WildlifeToken.ELK)

        val wildlifeList = game.wildlifeTokens.peekAll().map {
            NetWildlife.valueOf(it.name)
        }.reversed().toMutableList()
        wildlifeList.addAll(game.choices.map { NetWildlife.valueOf(it.second.name) }.reversed())

        val message = GameInitMessage(
            tileList, scoringCards,
            game.playerQueue.map { NetPlayer(it.name, (it.board[Triple(0,0,0)]?.id ?: 0) / 10) }, wildlifeList
        )

        if (game.playerQueue.peek().type == PlayerType.NETWORK)
            updateConnectionState(ConnectionState.WAITING_FOR_PLAYER_TURN)
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
        check(connectionState in listOf(ConnectionState.WAITING_FOR_INIT, ConnectionState.WAITING_FOR_GUESTS))
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

        if (game.playerQueue.peek().type == PlayerType.NETWORK)
            updateConnectionState(ConnectionState.WAITING_FOR_PLAYER_TURN)
        else updateConnectionState(ConnectionState.PLACING)

        onAllRefreshables { refreshAfterStartGame() }
    }

    /**
     * A function to send the selected choices to the network via a [SelectMessage]
     *
     * @param unlockedChoices A [Boolean] to note if the player spend a nature token to unlock the choices
     *
     * @throws IllegalStateException If no game is running at the moment
     */
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

    /**
     * A function to handle a received [SelectMessage]
     *
     * @param message The received [SelectMessage]
     *
     * @throws IllegalStateException If no game is running at the moment
     */
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

    /**
     * A function to send the positions of the placed [Tile] and [WildlifeToken] as well as the rotation of the [Tile]
     * via a [PlaceMessage]
     *
     * @param habCoords The coordinates at which the [Tile] has been placed, as a [Triple] of [Int]s
     * @param tokenCoords The coordinates at which the [WildlifeToken] has been placed, as a [Triple] of [Int]s,
     * nullable, to enable the discarding of the [WildlifeToken]
     * @param habRotation The rotation of the [Tile] as [Int] clockwise in 60° Intervalls
     */
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

    /**
     * A function to handle a received [PlaceMessage]
     *
     * @param message The received [PlaceMessage]
     *
     * @throws IllegalStateException If no game is running at the moment
     */
    fun receivePlace(message: PlaceMessage) {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game found" }

        println("Network-Player '"+game.playerQueue.peek().name+"' makes Turn:")
        println("Selected Tile (ID): "+game.selectedChoice.first.toString())
        println("Selected Animal (ID): "+game.selectedChoice.second.toString())
        
        val habCoords: Triple<Int, Int, Int> = Triple(
            (message.habitatCoordinates.first + message.habitatCoordinates.second) * (-1),
            message.habitatCoordinates.second, message.habitatCoordinates.first)

        println("Place Tile at: "+habCoords.third.toString()+", "+habCoords.second.toString())
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
            val cords = checkNotNull(message.wildlifeCoordinates)
            val tokenCoords: Triple<Int, Int, Int> = Triple(
                (cords.first + cords.second) * (-1),
                cords.second, cords.first)

            println("Place Wildlife at: "+tokenCoords.third.toString()+", "+tokenCoords.second.toString())
            rootService.playerActionService.placeWildlife(tokenCoords)

            rootService.gameService.changeTurn()
        } else {
            println("Rejected Wildlife")
        }

        println("End Network-Player '"+game.playerQueue.peek().name+"' Turn")
        println("-----------------------------")
    }

    /**
     * A function to send a [WipeWildlifeMessage]
     *
     * @param indices A list of [Int], denoting the shop positions which have been changed
     * @param natureToken A [Boolean], saying if a nature token has been spent for the action
     *
     * @throws IllegalStateException If no game is running at the moment
     */
    fun sendExterminate(indices: List<Int>, natureToken: Boolean) {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game found" }

        val wildlifeList = game.wildlifeTokens.peekAll().map { NetWildlife.valueOf(it.name) }.reversed().toMutableList()

        val message = WipeWildlifeMessage(
            natureToken, game.playerQueue.peek().natureTokens, indices, wildlifeList
        )

        client?.sendGameActionMessage(message)
    }

    /**
     * A function to handle a received [WipeWildlifeMessage]
     *
     * @param message The received [WipeWildlifeMessage]
     *
     * @throws IllegalStateException If no game is running at the moment
     */
    fun receiveExterminate(message: WipeWildlifeMessage) {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game found" }

        if (message.usedNatureToken) game.playerQueue.peek().natureTokens--

//        check(game.playerQueue.peek().natureTokens == message.natureTokenAmount) {
//            "Difference in Nature Tokens detected"
//        }

        val wildlifeBag = message.wildlifeBag.toMutableList()

        val numChanges = message.wipedWildlifeIndices.size
        if (numChanges == 4 && !message.usedNatureToken) {
            rootService.gameService.exterminate(playerTrigger = false, networkOverride = true)
        } else if (numChanges == 3 && !message.usedNatureToken) {
            rootService.gameService.exterminate(playerTrigger = true, networkOverride = true)
        } else {
            message.wipedWildlifeIndices.sorted().forEach {
                game.choices[it] = Pair(game.choices[it].first, game.wildlifeTokens.pop())
            }
        }



        game.wildlifeTokens.clear()
        game.wildlifeTokens.pushAll(wildlifeBag.map {WildlifeToken.valueOf(it.name) })

        onAllRefreshables { refreshAfterChangeWildlife(message.wipedWildlifeIndices) }

        if (message.wipedWildlifeIndices.isEmpty() && game.gameState == GameState.PLAYED_TILE) {
            rootService.gameService.changeTurn()
        }
    }

    /**
     * A function to send a [UseNatureTokenMessage]
     */
    fun sendUseNatureToken() {
        client?.sendGameActionMessage(UseNatureTokenMessage("Very important message"))
    }

    /**
     * A function to receive a [UseNatureTokenMessage]
     */
    fun receiveUseNatureToken() {
        onAllRefreshables { refreshAfterUnlockSelection() }
    }

    /**
     * A function to send a [GameConfigMessage]
     *
     * @param playerList A [List] containing the player names as [String]s
     * @param scoringCards A [List] containing a nullable [Boolean] for every scoring card to denote if variant A or B
     * has been selected
     */
    fun sendGameConfig(playerList: List<String>, scoringCards: List<Boolean?>) {
        client?.players?.sortBy { playerList.indexOf(it.first) }
        client?.scoringCards = scoringCards.toMutableList()
        client?.sendGameActionMessage(GameConfigMessage(playerList, scoringCards))
    }

    /**
     * A function to handle a received [GameConfigMessage]
     *
     * @param message The received [GameConfigMessage]
     */
    fun receiveGameConfig(message: GameConfigMessage) {
        onAllRefreshables { refreshAfterGameConfigUpdate(message.players, message.scoringCards) }
    }

    /**
     * A function to send a [SelectWildlifeMessage]
     *
     * @param wildlifeIndex The index of the selected [WildlifeToken] as [Int]
     */
    fun sendSelectWildlife(wildlifeIndex: Int) {
        client?.sendGameActionMessage(SelectWildlifeMessage(wildlifeIndex))
    }

    /**
     * A function to handle a received [SelectWildlifeMessage]
     *
     * @param message The received [SelectWildlifeMessage]
     */
    fun receiveSelectWildlife(message: SelectWildlifeMessage) {
        onAllRefreshables { refreshAfterSelectWildlife(message.wildlifeIndex) }
    }

    /**
     * A function to send a [RotationMessage]
     */
    fun sendRotation() {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game found" }

        client?.sendGameActionMessage(
            RotationMessage(game.choices[game.selectedChoice.first].first.rotation))
    }

    /**
     * A function to handle a received [RotationMessage]
     *
     * @param message The received [RotationMessage]
     */
    fun receiveRotation(message: RotationMessage) {
        rootService.playerActionService.rotateTile(null, message.habitatRotation)
    }

    /**
     * A function to send a [SelectHabitatTileMessage]
     *
     * @param tileIndex The index of the selected [Tile] as [Int]
     */
    fun sendSelectHabitatTile(tileIndex: Int) {
        client?.sendGameActionMessage(SelectHabitatTileMessage(tileIndex))
    }

    /**
     * A function to handle a received [SelectHabitatTileMessage]
     *
     * @param message The received [SelectHabitatTileMessage]
     */
    fun receiveSelectHabitatTile(message: SelectHabitatTileMessage) {
        onAllRefreshables { refreshAfterSelectTile(message.habitatIndex) }
    }

    /**
     * A function to send a [ChatMessage]
     *
     * @param message The chat message to be sent (as [String])
     */
    fun sendChatMessage(message: String) {
        val message = ChatMessage(message)
        client?.sendGameActionMessage(message)
    }

    /**
     * A function to handle a received [ChatMessage]
     *
     * @param message The received [ChatMessage]
     */
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