package service

import entity.*
import kotlin.test.*
import java.io.File

/**
 * Unit test class for verifying the save and load functionality of the game, including constraints
 * and recovery of game states and history. This test suite ensures that the save and load operations
 * behave as expected under various conditions and handle exceptions appropriately.
 */
class SaveLoadGameTest {

    private val testSaveName = "SopraTestSpielstand"

    /**
     * Creates a new instance of [RootService] with a manually initialized and running game.
     *
     * The created game instance includes:
     * - A set of scoring cards specified for the game.
     * - A local game configuration.
     * - Nature tokens initialized to 5.
     * - Game state set to the start of a turn.
     * - A predefined player queue with two players ("Alice" and "Bob"), both of type [PlayerType.HUMAN].
     *
     * The initialized game is set as the current active game in the returned [RootService] instance.
     *
     * @return A [RootService] instance with a preconfigured and running game attached.
     */
    private fun createRootServiceWithRunningGame(): RootService {
        val rootService = RootService()

        val dummyGame = CascadiaGame(
            scoringCards = listOf(true, false, true, false, true),
            isLocal = true
        )
        dummyGame.natureTokens = 5
        dummyGame.gameState = GameState.START_OF_TURN

        dummyGame.playerQueue.add(Player("Alice", PlayerType.HUMAN))
        dummyGame.playerQueue.add(Player("Bob", PlayerType.HUMAN))

        rootService.currentGame = dummyGame
        return rootService
    }

    /**
     * Cleans up the test environment by removing any saved game file created during testing.
     *
     * This method ensures that each test begins with a clean slate by deleting the test save file
     * if it exists in the designated save directory. It is annotated with `@AfterTest` to execute
     * after each test in the `SaveLoadGameTest` class.
     *
     * Behavior:
     * - Checks if a file with the test save name and save extension exists in the save directory.
     * - Deletes the file if it exists.
     */
    @AfterTest
    fun tearDown() {
        val file = File(RootService.SAVE_DIRECTORY, "$testSaveName${RootService.SAVE_EXTENSION}")
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * Tests the process of saving and loading a game along with reconstructing its history.
     *
     * This method covers the following functionality:
     * - Initializes a simulated game environment with a running game and manually places a move in its history.
     * - Asserts that the history contains moves before the save operation.
     * - Simulates saving the game to a file system and verifies the presence of the saved file.
     * - Deletes the game and history in memory to simulate application restart.
     * - Reloads the previously saved game and verifies:
     *    - The game object is successfully restored.
     *    - The correct number of players exists in the restored game queue.
     *    - The undo history of the game is reconstructed successfully.
     *
     * Validation steps include assertions to ensure logical and physical consistency during save/load operations.
     */
    @Test
    fun `save and load game successfully and reconstruct history`() {
        val rootService = createRootServiceWithRunningGame()
        val gameService = rootService.gameService
        val playerActionService = rootService.playerActionService

        val oldMove = CascadiaGame(listOf(true, false, true, false, true), true)
        oldMove.playerQueue.add(Player("Alice", PlayerType.HUMAN))
        rootService.history.prevMoves.push(oldMove)

        assertFalse(rootService.history.prevMoves.isEmpty(),
            "Die Historie sollte vor dem Speichern Züge enthalten.")

        playerActionService.saveGame(testSaveName)

        val saveFile = File(RootService.SAVE_DIRECTORY, "$testSaveName${RootService.SAVE_EXTENSION}")
        assertTrue(saveFile.exists(),
            "Die Speicherdatei wurde nicht physisch auf der Festplatte erstellt!")

        rootService.currentGame = null
        rootService.history.prevMoves.clear()

        gameService.loadGame(testSaveName)

        val loadedGame = rootService.currentGame
        assertNotNull(loadedGame, "Das Spiel sollte nach dem Laden nicht null sein!")
        assertEquals(2, loadedGame.playerQueue.size,
            "Es sollten wieder genau 2 Spieler in der Queue sein!")
        assertFalse(rootService.history.prevMoves.isEmpty(),
            "Die Undo-Historie wurde beim Laden nicht rekonstruiert!")
    }

    /**
     * Tests that the `saveGame` method in the `RootService` throws an `IllegalStateException`
     * when no game is currently running.
     *
     * This test ensures that attempting to save a game without an active game context
     * is correctly identified as an invalid operation, preventing unforeseen behavior
     * during the save process.
     *
     * @throws IllegalStateException if there is no active game when attempting to save.
     */
    @Test
    fun `save game throws IllegalStateException if no game is currently running`() {
        val rootService = RootService()

        assertFailsWith<IllegalStateException>("Speichern ohne aktives Spiel muss fehlschlagen!") {
            rootService.playerActionService.saveGame(testSaveName)
        }
    }

    /**
     * Tests that the `saveGame` method in the `RootService` throws an `IllegalArgumentException`
     * when the provided name is empty.
     *
     * This test verifies that the game saving functionality enforces a validation rule
     * prohibiting empty names to ensure saved games are properly identifiable and manageable.
     *
     * @throws IllegalArgumentException if the provided name is empty.
     */
    @Test
    fun `save game throws IllegalArgumentException if the provided name is empty`() {
        val rootService = createRootServiceWithRunningGame()

        assertFailsWith<IllegalArgumentException>("Speichern mit leerem Namen sollte nicht erlaubt sein!") {
            rootService.playerActionService.saveGame("")
        }
    }

    /**
     * Validates that the `saveGame` method in the `RootService` throws an `IllegalArgumentException`
     * when attempting to save a network-based game.
     *
     * A network game, represented by a `CascadiaGame` instance with `isLocal` set to `false`
     * and the presence of a player with `PlayerType.NETWORK`, cannot be saved due to restrictions.
     * This test ensures the application enforces this rule by throwing an appropriate exception.
     *
     * @throws IllegalArgumentException if the game being saved is identified as a network game.
     */
    @Test
    fun `save game throws IllegalArgumentException if it is a network game`() {
        val rootService = RootService()

        val networkGame = CascadiaGame(listOf(true, false, true, false, true), true)
        networkGame.playerQueue.add(Player("LocalPlayer", PlayerType.HUMAN))
        networkGame.playerQueue.add(Player("NetPlayer", PlayerType.NETWORK))
        rootService.currentGame = networkGame

        assertFailsWith<IllegalArgumentException>("Speichern von Netzwerkspielen sollte verboten sein!") {
            rootService.playerActionService.saveGame(testSaveName)
        }
    }

    /**
     * Tests that attempting to load a game while another game is already active in memory
     * correctly results in an `IllegalStateException` being thrown.
     *
     * This method validates that the `loadGame` functionality enforces the constraint
     * preventing a new game from being loaded when a game session is already running.
     *
     * Test setup involves creating a simulated environment with an active game loaded in memory.
     *
     * @throws IllegalStateException if a game is already running while attempting to load another.
     */
    @Test
    fun `load game throws IllegalStateException if another game is already active in RAM`() {
        val rootService = createRootServiceWithRunningGame()

        assertFailsWith<IllegalStateException>("Laden, während ein Spiel läuft, sollte fehlschlagen!") {
            rootService.gameService.loadGame(testSaveName)
        }
    }

    /**
     * Validates that the `loadGame` function in the `RootService` class throws an appropriate exception
     * if the provided file name is invalid. Specifically, the method is expected to throw an `IllegalArgumentException`
     * in the following cases:
     *
     * 1. When the provided name is blank.
     * 2. When there is no file matching the provided name in the save directory.
     *
     * This test ensures that the `loadGame` method enforces preconditions to prevent invalid or non-existent
     * game loads from proceeding, thus maintaining state integrity.
     *
     * @throws IllegalArgumentException if the name is blank, or if a file corresponding to the name does not exist.
     */
    @Test
    fun `load game throws IllegalArgumentException if file does not exist or name is blank`() {
        val rootService = RootService()

        assertFailsWith<IllegalArgumentException>("Laden mit leerem Namen darf nicht erlaubt sein!") {
            rootService.gameService.loadGame("")
        }

        assertFailsWith<IllegalArgumentException>(
            "Laden einer nicht existierenden Datei muss fehlschlagen!") {
            rootService.gameService.loadGame("datei_die_es_niemals_gibt")
        }
    }
}