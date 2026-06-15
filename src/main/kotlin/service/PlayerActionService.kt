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

    fun saveGame(name: String) {

    }

    fun selectColumn(index: Int) {

    }

    fun undo() {

    }
}