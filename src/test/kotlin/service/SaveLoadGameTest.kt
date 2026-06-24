package service

import entity.*
import kotlin.test.*
import java.io.File
import org.junit.jupiter.api.assertThrows

class SaveLoadGameTest {


    //hilfsmethode, um für die Tests eine saubere Umgebung aufzubauen
    private fun createRootServiceWithRunningGame() : RootService{
        val rootService = RootService()

        //Ein neues Spiel mit fiktiven Spielern starten
        val players = listOf(Pair("X", PlayerType.HUMAN), Pair("Y", PlayerType.HUMAN))
        val scoringCards = listOf(true, false, true, false, true)

        rootService.gameService.startNewGame(players, scoringCards)
        return rootService
    }

    @Test
    fun testSaveAndLoadGameSuccess() {
        val rootService = createRootServiceWithRunningGame()
        val gameService = rootService.gameService
        val playerActionService = rootService.playerActionService

        //1.einen Zug simulieren.
        playerActionService.selectColumn(0)

        //koordinate für das Plättchen, später muss angepasst werden
        val targetHex = Triple(1,-1,0)
        playerActionService.placeTile(targetHex)
        playerActionService.placeWildlife(targetHex)

        gameService.changeTurn()

        //2.Speichern Ausführen
        val saveName = "test_cascadia_save"
        playerActionService.saveGame(saveName)

        //3.datei speichern
        val saveFile = File("SavedGames/$saveName.cascadia")

        //try-finally stellt sicher, dass die Datei immer gelöscht wird, auch wenn ein assert fehlschlägt
        try{
            //4.Simulation neustart.
            rootService.currentGame = null

            //5.laden ausführen
            gameService.loadGame(saveName)

            //6.Überprüfung
            val loadedGame = rootService.currentGame
            assertNotNull(loadedGame, "Das Spiel sollte nach dem Laden nicht null sein!")
            assertEquals(2, loadedGame.playerQueue.size,
                "Es sollten wieder 2 Spieler in der Queue sein! ")
            assertFalse(rootService.history.prevMoves.isEmpty(),
                "Die Undo-Historie wurde nicht mitgeladen!")


        }
        //egal was passiert dieser Code wird ausgeführt
        finally{
            //testdatei nach dem Testen wieder löschen, damit die Festplatte sauber bleibt.
            if(saveFile.exists()) saveFile.delete()
        }

    }

    @Test
    fun testLoadGameThrowsIfGameIsAlreadyRunning() {
        val rootService = createRootServiceWithRunningGame()

        //@throws IllegalStateException If there is currently a game running
        //Es läuft bereits ein Spiel (rootService.currentGame != null)
        //Wenn wir jetzt versuchen ein Spiel zu laden, MUSS eine IllegalStateException fliegen
        assertThrows <IllegalStateException>("Laden ohne laufendes Spiel sollte fehlschlagen")
        { rootService.gameService.loadGame("test-Spiel") }

    }
    @Test
    fun testSaveGameThrowsIfNetworkGame(){
        val rootService = RootService()
        //Spiel mit einem Netzwerk-Spieler erstellen
        val players = listOf(Pair("LocalPlayer", PlayerType.HUMAN), Pair("NetPLayer", PlayerType.NETWORK))
        val scoringCards = listOf(true, false, true, false, true)

        rootService.gameService.startNewGame(players, scoringCards)

        assertThrows<IllegalArgumentException>("Speichern von Netzwerkspielen sollte verboten sein!"){
            rootService.playerActionService.saveGame("network_save")
        }
    }

    @Test
    fun testLoadGameThrowsIfFileDoesNotExistOrNameIsEmpty(){
        val rootService = RootService() //leeres RootService initialisieren

        //@throws IllegalArgumentException If the name is empty or if there isn't a saved game

        // Test 1: Leerer Name : IllegalArgumentException
        assertThrows<IllegalArgumentException> {
            rootService.gameService.loadGame("")
        }

        // Test 2: Datei existiert nicht : IllegalArgumentException
        assertThrows<IllegalArgumentException> {
            rootService.gameService.loadGame("datei_die_es_niemals_gibt")
        }
    }

    @Test
    fun testSaveGameThrowsIfNameIsEmpty(){

        val rootService = createRootServiceWithRunningGame()

        assertThrows<IllegalArgumentException> ("Speichern mit leerem Namen sollte nicht erlaubt sein!"){
            rootService.playerActionService.saveGame("")
        }
    }


}