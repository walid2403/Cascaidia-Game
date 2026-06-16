package service

/**
 * The player action service class of the Cascadia game. It includes all functions which rely heavily on player inputs.
 */

class PlayerActionService(private val rootService: RootService): AbstractRefreshingService() {

    fun changeWildlife(indices: List<Int>) {

    }

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
     * @throws IllegalArgumentException if prevMoves is empty
     * (this would occur in the first Action of the first turn by a human)
     */
    fun undo() {

    }
}