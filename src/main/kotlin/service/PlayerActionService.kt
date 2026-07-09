package service

import entity.*
import java.io.File
import entity.SaveState
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule


/**
 * The player action service class of the Cascadia game. It includes all functions which rely heavily on player inputs.
 */

class PlayerActionService(private val rootService: RootService) : AbstractRefreshingService() {

    /**
     * A Jackson object mapper configured with a custom `SimpleModule` to handle
     * specific key deserialization needs for JSON Maps.
     */
    private val mapper = jacksonObjectMapper().apply {
        val module = SimpleModule()
        module.addKeyDeserializer(Triple::class.java, TripleKeyDeserializer())
        registerModule(module)
    }

    /**
     * Allows the active player to swap any number (0..4) of wildlife tokens in the market
     * by spending a nature token.
     *
     * According to the specified activity diagram, the process proceeds as follows:
     * 1. Checks if the current GameState is [GameState.START_OF_TURN] or [GameState.HAS_EXTERMINATED]
     * 2. Checks whether the player possesses a nature token.
     * 3. A nature token is deducted from the player's supply.
     * 4. The selected wildlife tokens (based on the given indices) are removed from the display
     * 5. The market is immediately refilled with new tokens from the "wildlifeTokens stack".
     * 6. The previously removed tokens are placed back into [CascadiaGame.wildlifeTokens].
     * 7. The token bag is shuffled
     *
     * @param indices A list of the positions (0 to 3) of the wildlife tokens in the market
     * that the player wishes to swap.
     * @throws IllegalStateException If the player attempts to perform this action without
     * possessing a nature token or if the gameState doesn't fit
     * @throws IllegalArgumentException If the provided indices are not between 0..3 or
     * if the number of indices is not in 0..4 or if not all indices are distinct
     */
    fun changeWildlife(indices: List<Int>) {
        val currentGame = checkNotNull(rootService.currentGame){"Es Wurde kein Spiel im RootService gefunden!"}
        val currentPlayer = currentGame.playerQueue.peek()

        //wirft automatisch ein IllegalStateException
        check( currentGame.gameState == GameState.START_OF_TURN ||
                currentGame.gameState == GameState.HAS_EXTERMINATED) {
            "Spieler darf Aktuell kein Combination auswählen"
        }

        // Naturzapfen prüfen (Muss GRÖSSER als 0 sein!)
        check(currentPlayer.natureTokens > 0) { "Spieler besitzt keinen NatureToken" }

        //throw IllegalArgumentException
        require(indices.size in 0..4) { "Man kann nur zwischen 0 und 4 Token tauschen!" }
        require(indices.all { it in 0..3 }) { "Die angegebenen Plätze müssen zwischen 0 und 3 liegen!" }
        require(indices.distinct().size == indices.size) {"Ein Index darf nicht doppelt in der Liste vorkommen"}
        //Sind genug Tiere zum Tauschen da?
        if(currentGame.wildlifeTokens.size < indices.size) {
            rootService.gameService.calculateScores()
            return
        }

        val alteTierToken: MutableList<WildlifeToken> = mutableListOf()

        for (index in indices) {
            val currentPair = currentGame.choices[index]
            alteTierToken.add(currentPair.second)
            val newToken = currentGame.wildlifeTokens.pop()
            currentGame.choices[index] = Pair(currentPair.first,newToken)
        }

        for (wildeLifeToken in alteTierToken) {
            currentGame.wildlifeTokens.push(wildeLifeToken)
        }

        currentGame.wildlifeTokens.shuffle()

        currentPlayer.natureTokens--
        onAllRefreshables { refreshAfterChangeWildlife(indices) }

    }

    /**
     * Processes the selection of a combination of a habitat tile and a wildlife token
     * from the market.
     *
     * According to the "TakeCombination" activity diagram, this method is used exclusively
     * when the player selects a tile and an animal from different columns. In this case,
     * it is checked whether the player possesses a nature token. If this is the case,
     * a token is deducted from their supply.
     *
     * After successful verification, the selection is confirmed and stored for the current
     * turn (in the `selectedChoice` variable).
     *
     * Checks if the GameState is [GameState.START_OF_TURN] or [GameState.HAS_EXTERMINATED]
     *
     * @param tileIndex The index (0 to 3) of the selected habitat tile in the market.
     * @param wildlifeIndex The index (0 to 3) of the selected wildlife token in the market.
     * @throws IllegalStateException If a free selection is made but the player no longer possesses
     * a nature token or if the gameState is not correct
     * @throws IllegalArgumentException If the provided indices are not in 0..3
     */
    fun freeSelection(tileIndex: Int, wildlifeIndex: Int) {
        val currentGame = checkNotNull(rootService.currentGame){"Es Wurde kein Spiel im RootService gefunden!"}
        val currentPlayer = currentGame.playerQueue.peek()

        //wirft automatisch ein IllegalStateException
        check( currentGame.gameState == GameState.START_OF_TURN ||
                currentGame.gameState == GameState.HAS_EXTERMINATED) {
            "Spieler darf Aktuell kein Combination auswählen"
        }
        //wirft automatisch ein IllegalArgumentException
        require (tileIndex in 0..3){
            "Zug ungültig: tileIndex $tileIndex außerhalb des Markts"
        }
        //wirft automatisch ein IllegalArgumentException
        require (wildlifeIndex in 0..3){
            "Zug ungültig: wildlifeIndex $wildlifeIndex außerhalb des Markts"
        }

        //wirft automatisch ein IllegalStateException.
        // NUR WENN es eine echte freie Auswahl ist, muss er einen Token haben.
        check(currentPlayer.natureTokens > 0) {
            "Spieler besitzt Kein NatureToken, um ungleiche Paare zu wählen"
        }


        currentPlayer.natureTokens--
        currentGame.selectedChoice = Pair(tileIndex,wildlifeIndex)
        currentGame.gameState = GameState.MADE_CHOICE


        onAllRefreshables { refreshAfterFreeSelection() }

    }

    /**
     * Selects a given combination of a habitat tile and a wildlife token from the offered selection.
     *
     * This method executes the standard turn where the player does not spend a nature token
     * to decouple the selection. Based on the provided index, the system automatically selects
     * the habitat tile and the corresponding wildlife token from the exact same column of the current selection.
     *
     * @param index The index of the selected column
     *
     * @throws IllegalStateException If the gameState is not [GameState.START_OF_TURN] or [GameState.HAS_EXTERMINATED]
     */
    fun selectColumn(index: Int) {
        val currentGame = checkNotNull(rootService.currentGame){"Es Wurde kein Spiel im RootService gefunden!"}

        //wirft automatisch ein IllegalStateException
        check( currentGame.gameState == GameState.START_OF_TURN ||
                currentGame.gameState == GameState.HAS_EXTERMINATED) {
            "Spieler darf Aktuell kein Combination auswählen"
        }
        //wirft automatisch ein IllegalArgumentException
        require (index in 0..3){ //war require

            "Zug ungültig: Index $index außerhalb des Markts"

        }

        currentGame.selectedChoice = Pair(index,index)
        currentGame.gameState = GameState.MADE_CHOICE

        onAllRefreshables { refreshAfterSelectColumn(index) }

    }

    /**
     * Allows the player to rotate their selected [Tile] by 60°
     * This changes which edges of the tile face which neighboring tiles; the order of the
     * tile's habitat list is changed to reflect this
     *
     * @param right the direction of rotation: `true` clockwise, `false` counterclockwise.
     *
     * @throws IllegalStateException if the [GameState] is not `MADE_CHOICE`.
     */
    fun rotateTile(right: Boolean?, targetRotation: Int? = null) {
        val game = rootService.currentGame ?: error("No current game")

        check(game.gameState == GameState.MADE_CHOICE) {
            "Tile can only be rotated after a choice was made."
        }

        val tileIndex = game.selectedChoice.first

        require(tileIndex in game.choices.indices) {
            "No valid tile was selected."
        }

        val selectedTile = game.choices[tileIndex].first

        if (right != null) {
            if (right) {
                selectedTile.rotation = (selectedTile.rotation + 1) % 6

                if (selectedTile.habs.isNotEmpty()) {
                    val lastHabitat = selectedTile.habs.removeAt(selectedTile.habs.lastIndex)
                    selectedTile.habs.add(0, lastHabitat)
                }

                onAllRefreshables { refreshAfterRotate(1) }
            } else {
                selectedTile.rotation = (selectedTile.rotation + 5) % 6

                if (selectedTile.habs.isNotEmpty()) {
                    val firstHabitat = selectedTile.habs.removeAt(0)
                    selectedTile.habs.add(firstHabitat)
                }

                onAllRefreshables { refreshAfterRotate(-1) }
            }
        } else if (targetRotation != null) {
            val rightTimes = targetRotation - selectedTile.rotation
            val leftTimes = selectedTile.rotation - targetRotation

            selectedTile.rotation = targetRotation

            var amount = rightTimes

            if (leftTimes < rightTimes) {
                amount = leftTimes * (-1)
            }

            onAllRefreshables { refreshAfterRotate(amount) }
        }
        onAllRefreshables { refreshAfterRotate(right) }
    }

    /**
     * Allows the player to insert the previously selected [Tile] (stored in
     * `selectedChoice`) into their own [Player.board] at the position specified by
     * [index]. [index] must contain exactly one element, the target coordinate (x, y, z) on
     * the player's board. The target must be adjacent to at least one tile already on the
     * board (or to the starter tile) and must not already be occupied.
     *
     * @param index represents the position of the tile on the player's board represented
     *  by the three coordinates (x, y, z).
     *
     * @throws IllegalStateException if the [GameState] is not `MADE_CHOICE`.
     * @throws IllegalArgumentException if the target coordinate is already occupied or if
     * it is not adjacent to any existing tile.
     */
    fun placeTile(index: Triple<Int, Int, Int>) {
        val game = rootService.currentGame ?: error("No current game")

        check(game.gameState == GameState.MADE_CHOICE) {
            "Tile can only be placed after a choice was made."
        }

        val currentPlayer = game.playerQueue.peek()
            ?: throw IllegalStateException("No current player found.")

        val tileIndex = game.selectedChoice.first

        require(tileIndex in game.choices.indices) {
            "No valid tile was selected."
        }

        require(index.first + index.second + index.third == 0) {
            "The coordinate must be a valid cube coordinate."
        }

        require(!currentPlayer.board.containsKey(index)) {
            "There is already a tile at this coordinate."
        }

        val x = index.first
        val y = index.second
        val z = index.third

        val neighbours = listOf(
            Triple(x + 1, y - 1, z),
            Triple(x + 1, y, z - 1),
            Triple(x, y + 1, z - 1),
            Triple(x - 1, y + 1, z),
            Triple(x - 1, y, z + 1),
            Triple(x, y - 1, z + 1)
        )

        require(neighbours.any { currentPlayer.board.containsKey(it) }) {
            "The tile must be placed next to another tile."
        }

        val selectedTile = game.choices[tileIndex].first
        currentPlayer.board[index] = selectedTile

        game.gameState = GameState.PLAYED_TILE

        onAllRefreshables { refreshAfterPlaceTile(Triple(x, y, z)) }
    }

    /**
     * this function is about placing the currently selected wild life token on a tile
     * of the current player board
     *
     * this function methods makes sure that:
     *    the tile does not already contain another wild life token.
     *    the wildlife token is allowed on the selected tile.
     *    When a wildlife token is placed on a keystone tile, the player receives
     *   a nature token.
     *
     * @param index represents the position of the tile on the player's board represented
     *  by the three coordinates (x, y, z).
     *
     * @throws IllegalStateException if the game is not in gameState [GameState.PLAYED_TILE]
     * @throws IllegalArgumentException if the placement is not valid, meaning the position is either not empty or
     * not adjacent to another position
     */
    fun placeWildlife(index: Triple<Int, Int, Int>) {
        val game = rootService.currentGame ?: error("No current game")
        val currentPlayer = game.playerQueue.peek()
        check(game.gameState == GameState.PLAYED_TILE) {
            "Wildlife can only be placed in PLAYED_TILE state"
        }
        val tile = currentPlayer.board[index]
            ?: throw IllegalArgumentException(
                "There is no tile at the selected position"
            )
        require(tile.occupant == null) {
            "This tile already contains a wildlife token"
        }
        require(game.selectedChoice.second in game.choices.indices) {
            "No wildlife token has been selected"
        }
        val selectedWildlife = game.choices[game.selectedChoice.second].second
        require(selectedWildlife in tile.possibles) {
            "This wildlife token is not allowed on the selected tile"
        }
        tile.occupant = selectedWildlife
        if (tile.possibles.size == 1 && game.natureTokens > 0) {
            currentPlayer.natureTokens++
            game.natureTokens--
        }
        game.gameState = GameState.END_OF_TURN
        onAllRefreshables {
            refreshAfterPlaceWildlife(index)
        }
    }

    /**
     * Creates a snapshot of the current game state for persistence or undo/redo functionality.
     *
     * @param game The current instance of the game [CascadiaGame] whose state is to be captured.
     * @return A [GameSnapshot] object representing the current game state, including the tile stack,
     * nature tokens, player choices, selected choice, game state, player queue, removed tokens,
     * wildlife tokens, scoring cards, and local game information.
     */
    private fun createSnapshot(game: CascadiaGame): GameSnapshot {
        return GameSnapshot(
            tileStackList = game.tileStack.peekAll(),
            natureTokens = game.natureTokens,
            choicesList = game.choices.toList(),
            selectedChoice = game.selectedChoice,
            gameState = game.gameState,
            playerQueue = java.util.ArrayDeque(game.playerQueue),
            removedTokensList = game.removedTokens.toList(),
            wildlifeTokensList = game.wildlifeTokens.peekAll(),
            scoringCards = game.scoringCards,
            isLocal = game.isLocal
        )
    }
    /**
     * Interrupts the current game and saves it under the specified name.
     *
     * The complete move history is saved along with the game.
     * When the game is loaded at a later time (even after restarting the application),
     * the undo and redo functions will work exactly as they did before the interruption.
     *
     * The save and load feature is disabled for network games.
     *
     * @param name The file name or identifier under which the game should be saved.
     *
     * @throws IllegalArgumentException if the name is empty, or if the game is not in a state where it can be saved.
     */
    fun saveGame(name: String) {
        val game = rootService.currentGame ?: throw IllegalStateException("Kein aktives Spiel zum Speichern vorhanden.")
        if (name.isEmpty()) throw IllegalArgumentException("Der Name darf nicht leer sein.")
        if (!game.isLocal) {
            throw IllegalArgumentException("Netzwerkspiele können nicht gespeichert werden.")
        }

        val folder = File(RootService.SAVE_DIRECTORY)
        if (!folder.exists()) folder.mkdirs()
        val file = File(folder, "$name${RootService.SAVE_EXTENSION}")

        val state = SaveState(
            currentGame = createSnapshot(game),
            prevMovesList = rootService.history.prevMoves.peekAll().map { createSnapshot(it) },
            undoneMovesList = rootService.history.undoneMoves.peekAll().map { createSnapshot(it) }
        )
        mapper.writeValue(file, state)

        onAllRefreshables { refreshAfterSaveGame() }
    }

    /**
     * this function allows the player to redo an action that has been undone
     *
     * @throws IllegalStateException if there are no undone moves and current player is not human and current game
     * is not local
     */
    fun redo() {
        val game=rootService.currentGame
        checkNotNull(game){"Spiel nicht initialisiert"}

        val games=rootService.history

        check(games.undoneMoves.isNotEmpty()){"keine zurückgenommenen Züge existieren "}
        check(game.playerQueue.first().type== PlayerType.HUMAN)
        {"Bots und Netzwerkspieler dürfen nicht redoen"}
        check(game.isLocal){"im Netzwerkmodus ist die Funktion nicht erlaubt"}


        val nextGame=games.undoneMoves.pop()

        games.prevMoves.push(CascadiaGame(nextGame))

        rootService.currentGame=nextGame

        onAllRefreshables { refreshAfterRedo() }




    }

    /**
     * this function allows the player to redo an action that has been undone
     *
     * @throws IllegalStateException if there are no undone moves and current player is not human and current game
     * is not local
     */
    fun undo() {
        val  game=rootService.currentGame
        checkNotNull(game)

        val games=rootService.history

        check(game.playerQueue.first().type== PlayerType.HUMAN){"nur Menschen dürfen zurückgehen"}
        check(game.isLocal){"Funktion nur im lokalen Modus gestattet"}
        if(game.gameState == GameState.START_OF_TURN) {
            check(games.prevMoves.size > 1) {
                "Am Anfang der ersten Runde gibt es keine vorherigen Züge"
            }
        }
        if(game.gameState==GameState.START_OF_TURN) {
            //wenn am Anfang der Runde: Spiel in undoneMoves speichern
            val currentGame = games.prevMoves.pop()
            games.undoneMoves.push(CascadiaGame(currentGame))
        }
        //letztes gespeichertes Spiel "laden"
        val prevGame=games.prevMoves.peek()
        rootService.currentGame= CascadiaGame(prevGame)



        onAllRefreshables { refreshAfterUndo() }
    }
}