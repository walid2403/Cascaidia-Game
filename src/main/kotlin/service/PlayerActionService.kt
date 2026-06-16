package service

/**
 * The player action service class of the Cascadia game. It includes all functions which rely heavily on player inputs.
 */

class PlayerActionService(private val rootService: RootService): AbstractRefreshingService() {

    /**
     * Allows the active player to swap any number (1..4) of wildlife tokens in the market
     * by spending a nature token.
     *
     * According to the specified activity diagram, the process proceeds as follows:
     * 1. The `exterminate` method is called by the GameService.
     * 2. Checks whether the player possesses a nature token.
     * 3. A nature token is deducted from the player's supply.
     * 4. The selected wildlife tokens (based on the given indices) are removed from the display
     * and stored in the "removedTokens list".
     * 5. The market is immediately refilled with new tokens from the "wildlifeTokens stack".
     * 6. The previously removed tokens are placed back into the "wildlifeTokens stack".
     *
     * @param indices A list of the positions (0 to 3) of the wildlife tokens in the market
     * that the player wishes to swap.
     * @throws IllegalStateException If the player attempts to perform this action without
     * possessing a nature token.
     * @throws IllegalArgumentException If the provided indices are invalid.
     */
    fun changeWildlife(indices: List<Int>) {

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
     * @param tileIndex The index (0 to 3) of the selected habitat tile in the market.
     * @param wildlifeIndex The index (0 to 3) of the selected wildlife token in the market.
     * @throws IllegalStateException If a free selection is made but the player no longer possesses
     * a nature token.
     * @throws IllegalArgumentException If the provided indices are invalid.
     */
    fun freeSelection(tileIndex: Int, wildlifeIndex: Int) {

    }

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
     *  If the player chooses not to place the wildlife token or no legal placement
     *  could be done, then the wildlife token is returned to the wildlife bag.
     *
     * @param index represents the position of the tile on the player's board represented
     *  by the three coordinates (x, y, z).
     *
     * @throws IllegalArgumentException if the placement is not valid.
     */
    fun placeWildlife(index: Triple<Int, Int, Int>) {

    }

    /**
     * this function allows the player to redo an action that has been undone
     *
     * if the following [entity.Player] is not a [entity.PlayerType.HUMAN],
     * the game is jumped back to the next turn of a human player
     *
     * @throws IllegalArgumentException if there are no undone moves
     */
    fun redo() {

    }

    fun rotateTile(right: Boolean) {

    }
    /**
     * Interrupts the current game and saves the game state under the specified name.
     *
     * The complete move history is saved along with the game.
     * When the game is loaded at a later time (even after restarting the application),
     * the undo and redo functions will work exactly as they did before the interruption.
     *
     * The save and load feature is disabled for network games.
     *
     * @param name The file name or identifier under which the game should be saved.
     */
    fun saveGame(name: String) {

    }
    /**
     * Selects a given combination of a habitat tile and a wildlife token from the offered selection.
     *
     * This method executes the standard turn where the player does not spend a nature token
     * to decouple the selection. Based on the provided index, the system automatically selects
     * the habitat tile and the corresponding wildlife token from the exact same column of the current selection.
     *
     * @param index The index of the selected column
     * (corresponds to the position of the selected tile and token combination)
     */
    fun selectColumn(index: Int) {

    }


    /**
     * this function reverts the last action
     * it allows the current player to go back to their previous action
     * or to the end of the previous players turn
     *
     * if the previous player is not a [entity.PlayerType.HUMAN],
     * the game is reverted until it reaches the end of the most recent human players turn
     *
     * stores current [entity.CascadiaGame] in [entity.CascadiaGame.undoneMoves]
     * takes the previous Game from [entity.CascadiaGame.prevMoves]
     * @throws IllegalArgumentException if [entity.CascadiaGame.prevMoves] is empty
     * (this would occur in the first Action of the first turn by a human)
     */
    fun undo() {

    }
}