package service

import entity.*
import entity.SaveState
import java.io.*

/**
 * The game service class of the Cascadia Game. It includes all functions which work mostly on the system-logic side
 */

class GameService(private val rootService: RootService): AbstractRefreshingService() {

    /**
     * A function to start a new game from scratch
     *
     * @param playerList A list of the players to be entered into the game. The list must not be longer then four
     * elements or shorter than two elements. The [List] object contains a [Pair] object for every player with the
     * players name as a [String] and the players Type as a [PlayerType]
     * @param scoringCards The scoring cards to be used in the game, as a [List] of [Boolean] objects,
     * see [CascadiaGame.scoringCards]
     *
     * @throws IllegalArgumentException If the list-size is not 2 - 4, if there are duplicate names or if there are
     * not exactly five scoringCards
     * @throws IllegalStateException If there is currently a game running
     */
    fun startNewGame(playerList: List<Pair<String, PlayerType>>, scoringCards: List<Boolean>) {

    }

    /**
     * Helper function to reconstruct a fully playable [CascadiaGame] from a [GameSnapshot].
     * * Reconstructs the non-serializable BGW stacks from safe Kotlin lists and restores all other state variables.
     * * @param snapshot The saved state of a single game.
     * @return A live [CascadiaGame] object ready to be played.
     */
    private fun restoreSnapshot(snapshot: GameSnapshot): CascadiaGame {

        val game = CascadiaGame(snapshot.scoringCards, snapshot.isLocal)

        game.natureTokens = snapshot.natureTokens
        game.gameState = snapshot.gameState
        game.selectedChoice = snapshot.selectedChoice

        game.choices.clear()
        game.choices.addAll(snapshot.choicesList)

        game.removedTokens.clear()
        game.removedTokens.addAll(snapshot.removedTokensList)

        snapshot.tileStackList.reversed().forEach { game.tileStack.push(it) }
        snapshot.wildlifeTokensList.reversed().forEach { game.wildlifeTokens.push(it) }
        snapshot.playerList.forEach { game.playerQueue.add(it) }

        return game
    }

    /**
     * A function to load a previously saved game. The saved game is identified by the name Parameter.
     *
     * @param name The name of the previously saved game
     *
     * @throws IllegalArgumentException If the name is empty or if there isn't a saved game with the entered name
     * @throws IllegalStateException If there is currently a game running
     */
    fun loadGame(name: String) {

        if (rootService.currentGame != null) {
            throw IllegalStateException("A game is already running.")
        }
        if (name.isBlank()) {
            throw IllegalArgumentException("The save name cannot be empty.")
        }

        val file = File(RootService.SAVE_DIRECTORY, "$name${RootService.SAVE_EXTENSION}")

        if (!file.exists()) {
            throw IllegalArgumentException("Save game '$name' does not exist.")
        }

        val loadedState = ObjectInputStream(FileInputStream(file)).use { stream ->
            stream.readObject() as SaveState
        }

        rootService.currentGame = restoreSnapshot(loadedState.currentGame)

        rootService.history.prevMoves.clear()

        loadedState.prevMovesList.reversed().forEach { snapshot ->
            rootService.history.prevMoves.push(restoreSnapshot(snapshot))
        }

        rootService.history.undoneMoves.clear()
        loadedState.undoneMovesList.reversed().forEach { snapshot ->
            rootService.history.undoneMoves.push(restoreSnapshot(snapshot))
        }

        onAllRefreshables { refreshAfterLoadGame() }
    }

    /**
     * this functions resolves the overpopulation of the wildlife tokens
     *
     * If all four wildlife tokens are identical, they are automatically
     * removed and replaced. If only three identical wildlife tokens are present,
     * the current player may choose whether to remove and replace them if the gameState is [GameState.START_OF_TURN].
     *
     * Removed wildlife tokens are temporarily set aside
     *
     * The game ends if there are not enough wildlifeTokens available
     *
     * recursively calls itself if there are 4 tokens of the same type at the end of the function
     *
     * @param playerTrigger indicates whether the extermination is initiated by the player (true)
     * or automatically by the game (false)
     *
     * @throws IllegalStateException If the gameState is not [GameState.START_OF_TURN] or [GameState.HAS_EXTERMINATED]
     * or if there are not at least 3 tokens of the same type or
     * if there are 3 and the current gameState is not [GameState.START_OF_TURN]
     */
    fun exterminate( playerTrigger: Boolean) {

    }

    /**
     * the function calculates the score for every player. The score consist of points in the following categories:
     * - For each wildlife scoring card
     * - For each habitat corridor
     * - For each habitat corridor majority
     * - Nature tokens
     *
     * the resulting score is parsed directly to the GUI
     *
     * @throws IllegalStateException if there is no current game or
     *                               if not every player has 20 habitat tiles
     */
    fun calculateScores() {

    }

    /**
     * the function changes the current Player by rotating the [CascadiaGame.playerQueue]
     * and setting the [CascadiaGame.gameState] to [GameState.START_OF_TURN]
     *
     * it also checks if the [CascadiaGame.tileStack] is empty
     * if the condition is true, [calculateScores] is executed
     *
     *@throws IllegalStateException if Game is not in [GameState.END_OF_TURN]
     *@throws IllegalArgumentException if the [CascadiaGame.playerQueue] is empty
     */
    fun changeTurn() {

    }
}