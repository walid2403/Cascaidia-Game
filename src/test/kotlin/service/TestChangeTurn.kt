package service

import entity.*
import kotlin.test.*

/**
 * Eine Klasse um die Methode [GameService.changeTurn] zu testen.
 */
class TestChangeTurn {
    lateinit var rootService: RootService
    var refreshWasCalled = false
    var lastRound = false

    /**
     * Erstellt ein Cascadia Game und speichert es im rootService.
     * Initialisiert wird es standard mäßig mit folgenden Werten:
     * - scoringCards: Liste mit 5-mal true
     * - isLocal: true
     * - tileStack: 5 Tiles mit jeweils 6-mal Mountains als Habitat und keinen möglichen Tieren. Die id's sind 0 bis 4
     * - natureTokens: 10
     * - choices: 4-mal ein Tile mit jeweils 6-mal Mountains und als id 10 bis 13 und dazu immer ein Bear
     * - selectedChoice: Tile 0 und WildlifeToken 1
     * - gameState: END_OF_TURN
     * - players: 3 HUMAN Player mit Namen player0 bis player2
     * - removedTokens: 8-mal Hawk
     * - wildlifeTokens: 6-mal Salmon
     * zusätzlich wird der Startzustand im rootService unter history gespeichert.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        val currentGame = CascadiaGame(List(5) { true }, true)
        val tileStack = mutableListOf<Tile>()
        val habitats = MutableList(6) { Habitates.MOUNTAINS }
        for (i in 0 until 5) {
            tileStack.add(Tile(i,habitats,emptyList()))
        }
        currentGame.tileStack.pushAll(tileStack)
        currentGame.natureTokens = 10
        val choices = mutableListOf<Pair<Tile, WildlifeToken>>()
        for (i in 0 until 4) {
            val pair = Pair(Tile(10+i,habitats,emptyList()),
                WildlifeToken.BEAR)
            choices.add(pair)
        }
        currentGame.choices += choices
        currentGame.selectedChoice = Pair(0,1)
        currentGame.gameState = GameState.END_OF_TURN
        val players = mutableListOf<Player>()
        for (i in 0 until 3) {
            val player = Player("player$i", PlayerType.HUMAN)
            for (j in 0..20) {
                player.board[Triple(0,0,j)] = Tile("$i$j".toInt(), habitats, emptyList())
            }
            players.add(player)
        }
        currentGame.playerQueue.addAll(players)
        val removedTokens = MutableList(8) { WildlifeToken.HAWK }
        currentGame.removedTokens.addAll(removedTokens)
        val wildlifeTokens = List(6) { WildlifeToken.SALMON }
        currentGame.wildlifeTokens.pushAll(wildlifeTokens)

        rootService.currentGame = currentGame
        rootService.history.prevMoves.push(currentGame)

        val refreshable = object : Refreshable {
            override fun refreshAfterChangeTurn(lastTurn: Boolean) {
                refreshWasCalled = true
                lastRound = lastTurn
            }
        }
        rootService.addRefreshable(refreshable)
    }

    /**
     * Überprüft einen Aufruf von [GameService.changeTurn], bei dem alles richtig läuft.
     */
    @Test
    fun `korrekter Fall`() {
        val currentGame = rootService.currentGame
        assertNotNull(currentGame)

        rootService.gameService.changeTurn()

        assertTrue(refreshWasCalled,
            "Refresh nicht aufgerufen")
        assertFalse(lastRound,
            "Der nächste Spieler hat noch nicht seinen letzten Zug")
        assertTrue(currentGame.removedTokens.isEmpty(),
            "Temporär entfernte Tokens sind nicht vollständig zurückgelegt")
        assertEquals(GameState.START_OF_TURN, currentGame.gameState,
            "GameState wurde nicht passend geändert")
        /*assertNotEquals(WildlifeToken.BEAR, currentGame.choices[1].second,
            "Token nicht richtig ausgetauscht")
        assertEquals(4, currentGame.choices[0].first.id,
            "Tile nicht richtig ausgetauscht")
        assertEquals(13, currentGame.wildlifeTokens.size,
            "Falsche Anzahl an WildlifeToken")
        assertEquals(4, currentGame.tileStack.size,
            "Falsche Anzahl Tiles")*/
        assertTrue(currentGame.scoringCards.fold(true) {acc, bool -> acc && bool},
            "Die scoringCards wurden verändert")
        assertEquals(10, currentGame.natureTokens,
            "Naturetokens wurden verändert")
        assertEquals(4, currentGame.choices.size,
            "Falsche Anzahl an Möglichkeiten")
        assertEquals("player1", currentGame.playerQueue.peek().name,
            "Falscher Spieler an der Reihe")
        assertEquals(3, currentGame.playerQueue.size,
            "Spieler verloren gegangen")
        assertEquals(0, rootService.history.undoneMoves.size,
            "Rückgängig gemachte Züge nicht gelöscht")
        assertEquals(2, rootService.history.prevMoves.size,
            "Game nicht auf dem Stack abgelegt")
    }

    /**
     * Überprüft einen richtigen Aufruf von [GameService.changeTurn], bei dem alles richtig läuft und
     * der nächste Spieler sich in der letzten Runde befindet.
     */
    @Test
    fun `korrekter Fall letzte Runde`() {
        val currentGame = rootService.currentGame
        assertNotNull(currentGame)
        currentGame.playerQueue.forEach { it.board[Triple(0,0,-1)] = Tile(-1,
            MutableList(6) { Habitates.MOUNTAINS }, emptyList()) }

        rootService.gameService.changeTurn()

        assertTrue(refreshWasCalled,
            "Refresh nicht aufgerufen")
        assertTrue(lastRound,
            "Der nächste Spieler ist schon in seinem letzten Zug")
        assertTrue(currentGame.removedTokens.isEmpty(),
            "Temporär entfernte Tokens sind nicht vollständig zurückgelegt")
        assertEquals(GameState.START_OF_TURN, currentGame.gameState,
            "GameState wurde nicht passend geändert")
        /*assertNotEquals(WildlifeToken.BEAR, currentGame.choices[1].second,
            "Token nicht richtig ausgetauscht")
        assertEquals(4, currentGame.choices[0].first.id,
            "Tile nicht richtig ausgetauscht")
        assertEquals(13, currentGame.wildlifeTokens.size,
            "Falsche Anzahl an WildlifeToken")
        assertEquals(2, currentGame.tileStack.size,
            "Falsche Anzahl Tiles")*/
        assertTrue(currentGame.scoringCards.fold(true) {acc, bool -> acc && bool},
            "Die scoringCards wurden verändert")
        assertEquals(10, currentGame.natureTokens,
            "Naturetokens wurden verändert")
        assertEquals(4, currentGame.choices.size,
            "Falsche Anzahl an Möglichkeiten")
        assertEquals("player1", currentGame.playerQueue.peek().name,
            "Falscher Spieler an der Reihe")
        assertEquals(3, currentGame.playerQueue.size,
            "Spieler verloren gegangen")
        assertEquals(0, rootService.history.undoneMoves.size,
            "Rückgängig gemachte Züge nicht gelöscht")
        assertEquals(2, rootService.history.prevMoves.size,
            "Game nicht auf dem Stack abgelegt")
    }

    /**
     * Überprüft den Aufruf nach dem letzten Zug des Spiels.
     */
    @Test
    fun `nach letztem Zug`() {
        val currentGame = rootService.currentGame
        assertNotNull(currentGame)
        currentGame.playerQueue.forEach { it.board[Triple(0,0,-1)] = Tile(-1,
            MutableList(6) { Habitates.MOUNTAINS }, emptyList())
            it.board[Triple(0,0,-2)] = Tile(-2,
                MutableList(6) { Habitates.MOUNTAINS }, emptyList()) }
        var calledCalculateScores = false
        val testRefresh = object : Refreshable {
            override fun refreshAfterEndGame() {
                calledCalculateScores = true
            }
        }
        rootService.addRefreshable(testRefresh)

        rootService.gameService.changeTurn()

        assertFalse(refreshWasCalled,
            "Refresh wurde aufgerufen")
        assertTrue(calledCalculateScores,
            "CalculateScores wurde nicht aufgerufen")
        assertEquals(3, currentGame.playerQueue.size,
            "Spieler verloren gegangen")
        /*
        currentGame.gameState = GameState.END_OF_TURN
        currentGame.tileStack.push(Tile(100,mutableListOf(), emptyList()))
        currentGame.wildlifeTokens.clear()
        currentGame.removedTokens.clear()

        rootService.gameService.changeTurn()

        assertFalse(refreshWasCalled,
            "Refresh wurde aufgerufen")
        assertTrue(calledCalculateScores,
            "CalculateScores wurde nicht aufgerufen")
        calledCalculateScores = false
        assertEquals(3, currentGame.playerQueue.size,
            "Spieler verloren gegangen")

        currentGame.gameState = GameState.END_OF_TURN
        currentGame.wildlifeTokens.pushAll(List(12) { WildlifeToken.BEAR })

        rootService.gameService.changeTurn()

        assertFalse(refreshWasCalled,
            "Refresh wurde aufgerufen")
        assertTrue(calledCalculateScores,
            "CalculateScores wurde nicht aufgerufen")
        assertEquals(3, currentGame.playerQueue.size,
            "Spieler verloren gegangen")*/
    }

    /**
     * Überprüft alle GameState Fehler
     */
    @Test
    fun `Fehler GameState`() {
        val currentGame = rootService.currentGame
        assertNotNull(currentGame)

        for (state in GameState.entries) {
            currentGame.gameState = state
            if (state in listOf(GameState.END_OF_TURN, GameState.PLAYED_TILE)) {
                continue
            }
            assertFailsWith<IllegalStateException>("Falscher GameState zugelassen")
            { rootService.gameService.changeTurn() }
            assertEquals(8, currentGame.removedTokens.size,
                "Removed Tokens trotz Fehler entfernt")
            assertEquals(3, currentGame.playerQueue.size,
                "Spieler verloren gegangen")
            assertEquals(state,currentGame.gameState,
                "GameState geändert")
        }
    }

    /**
     * Überprüft Fälle für Zeilen Abdeckung
     */
    @Test
    fun `line coverage Tests`() {
        val currentGame = rootService.currentGame
        assertNotNull(currentGame)
        rootService.currentGame = null
        assertFailsWith<IllegalStateException>("Ohne Spiel ausgeführt")
        { rootService.gameService.changeTurn() }


        rootService.currentGame = currentGame
        currentGame.playerQueue.clear()
        currentGame.playerQueue.add(Player("test", PlayerType.EASY_BOT))
        rootService.gameService.changeTurn()
        assertEquals(1,rootService.history.prevMoves.size,
            "Bot Zug auf Stack gespeichert")

        val newGame = CascadiaGame(List(5) { true }, false)
        val tileStack = mutableListOf<Tile>()
        val habitats = MutableList(6) { Habitates.MOUNTAINS }
        for (i in 0 until 5) {
            tileStack.add(Tile(i,habitats,emptyList()))
        }
        newGame.tileStack.pushAll(tileStack)
        newGame.natureTokens = 10
        val choices = mutableListOf<Pair<Tile, WildlifeToken>>()
        for (i in 0 until 4) {
            val pair = Pair(Tile(10+i,habitats,emptyList()),
                WildlifeToken.BEAR)
            choices.add(pair)
        }
        newGame.choices += choices
        newGame.selectedChoice = Pair(0,1)
        newGame.gameState = GameState.END_OF_TURN
        val players = mutableListOf<Player>()
        for (i in 0 until 3) {
            val player = Player("player$i", PlayerType.HUMAN)
            players.add(player)
        }
        newGame.playerQueue.addAll(players)
        val removedTokens = MutableList(8) { WildlifeToken.HAWK }
        newGame.removedTokens.addAll(removedTokens)
        val wildlifeTokens = List(6) { WildlifeToken.SALMON }
        newGame.wildlifeTokens.pushAll(wildlifeTokens)

        rootService.currentGame = newGame
        rootService.history.prevMoves.clear()
        rootService.history.prevMoves.push(currentGame)
        rootService.gameService.changeTurn()
        assertEquals(1,rootService.history.prevMoves.size,
            "Bot Zug auf Stack gespeichert")
    }
}