package service

import entity.*

/**
 * The player action service class of the Cascadia game. It includes all functions which rely heavily on player inputs.
 */

class PlayerActionService(private val rootService: RootService) : AbstractRefreshingService() {

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
        require(currentGame.wildlifeTokens.size >= indices.size) { "Nicht genug Token im Beutel zum Tauschen!" }

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
        check (index in 0..3){ //war require

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
    fun rotateTile(right: Boolean) {

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
     * @throws IllegalArgumentException if there is no .cascadia file with the given name in the SavedGames Folder
     */
    fun saveGame(name: String) {

    }

    /**
     * this function allows the player to redo an action that has been undone
     *
     * @throws IllegalStateException if there are no undone moves
     */
    fun redo() {

    }

    /**
     * this function reverts the last action
     * it allows the current player to go back to their previous action
     * or to the end of the previous players turn
     *
     * stores current [CascadiaGame] in [CascadiaGames.undoneMoves]
     * takes the previous Game from [entity.CascadiaGames.prevMoves]
     * @throws IllegalStateException if [CascadiaGames.prevMoves] is empty
     * (this would occur in the first Action of the first turn by a human)
     */
    fun undo() {

    }
}