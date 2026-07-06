package service

/**
 * This interface provides a mechanism for the service layer classes to communicate
 * (usually to the GUI classes) that certain changes have been made to the entity
 * layer, so that the user interface can be updated accordingly.
 *
 * Default (empty) implementations are provided for all methods, so that implementing
 * GUI classes only need to react to events relevant to them.
 *
 * @see AbstractRefreshingService
 */
interface Refreshable {

    /**
     * Perform refreshes necessary after a new player started their turn
     *
     * @param lastTurn Indicates wether the game is in the final turn or not ([Boolean])
     */
    fun refreshAfterChangeTurn(lastTurn: Boolean) {}

    /**
     * Perform refreshes necessary after the game has been started
     */
    fun refreshAfterStartGame() {}

    /**
     * Perform refreshes necessary after a column has been selected
     *
     * @param index The index of the selected Column ([Int])
     */
    fun refreshAfterSelectColumn(index: Int) {}

    /**
     * Perform refreshes necessary after the wildlife tokens have been changed
     *
     * @param indices A [List] with the positions of the now changed wildlife tokens
     */
    fun refreshAfterChangeWildlife(indices: List<Int>) {}

    /**
     * Perform refreshes necessary after the wildlife tokens have been exterminated
     */
    fun refreshAfterExterminate() {}

    /**
     * Perform refreshes necessary after a game has been loaded
     */
    fun refreshAfterLoadGame() {}

    /**
     * Perform refreshes necessary after undo has been used
     */
    fun refreshAfterUndo() {}

    /**
     * Perform refreshes necessary after redo has been used
     */
    fun refreshAfterRedo() {}

    /**
     * Perform refreshes necessary after a free selection has been executed
     */
    fun refreshAfterFreeSelection() {}

    /**
     * Perform refreshes necessary after a tile has been rotated
     *
     * @param right Has the tile been rotated in the right direction? ([Boolean])
     */
    fun refreshAfterRotate(right: Boolean) {}

    /**
     * Perform refreshes necessary after a tile has been placed
     *
     * @param index A [Triple] containing the coordinates of the placement as [Int]
     */
    fun refreshAfterPlaceTile(index: Triple<Int, Int, Int>) {}

    /**
     * Perform refreshes necessary after a wildlife token has been placed
     *
     * @param index A [Triple] containing the coordinates of the placement as [Int]
     */
    fun refreshAfterPlaceWildlife(index: Triple<Int, Int, Int>) {}

    /**
     * Perform refreshes necessary after the game has ended
     */
    fun refreshAfterEndGame(scores: List<Pair<String,List<Int>>>) {}

    // Network specific refreshes:
    /**
     * Perform refreshes necessary after a player couldn't join a game
     */
    fun refreshAfterConnectionError(errorMessage: String) {}

    /**
     * Perform refreshes necessary after the game config has been updated
     */
    fun refreshAfterGameConfigUpdate(playerList: List<String>, scoringCards: List<Boolean?>) {}

    /**
     * Perform refreshes necessary after a network game has been started
     */
    fun refreshAfterHostGame(lobbyCode: String) {}

    /**
     * Perform refreshes necessary after a chat message has been received
     */
    fun refreshAfterChatMessage(messageSender: String, message: String) {}

    /**
     * Perform refreshes necessary after a nature token has been used
     */
    fun refreshAfterUseNatureToken() {}
}