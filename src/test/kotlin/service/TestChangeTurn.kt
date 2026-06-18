package service

import entity.*
import kotlin.test.*

/**
 * Eine Klasse um die Methode [GameService.changeTurn] zu testen.
 */
class TestChangeTurn {
    lateinit var rootService: RootService
    lateinit var refreshable: Refreshable
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
        this.refreshable = refreshable
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
        assertNotEquals(WildlifeToken.BEAR, currentGame.choices[1].second,
            "Token nicht richtig ausgetauscht")
        assertEquals(4, currentGame.choices[0].first.id,
            "Tile nicht richtig ausgetauscht")
        assertEquals(13, currentGame.wildlifeTokens.size,
            "Falsche Anzahl an WildlifeToken")
        assertEquals(4, currentGame.tileStack.size,
            "Falsche Anzahl Tiles")
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
    }

    /**
     * Überprüft einen richtigen Aufruf von [GameService.changeTurn], bei dem alles richtig läuft und
     * der nächste Spieler sich in der letzten Runde befindet.
     */
    @Test
    fun `korrekter Fall letzte Runde`() {
        val currentGame = rootService.currentGame
        assertNotNull(currentGame)
        repeat(2) {
            currentGame.tileStack.pop()
        }

        rootService.gameService.changeTurn()

        assertTrue(refreshWasCalled,
            "Refresh nicht aufgerufen")
        assertTrue(lastRound,
            "Der nächste Spieler ist schon in seinem letzten Zug")
        assertTrue(currentGame.removedTokens.isEmpty(),
            "Temporär entfernte Tokens sind nicht vollständig zurückgelegt")
        assertEquals(GameState.START_OF_TURN, currentGame.gameState,
            "GameState wurde nicht passend geändert")
        assertNotEquals(WildlifeToken.BEAR, currentGame.choices[1].second,
            "Token nicht richtig ausgetauscht")
        assertEquals(4, currentGame.choices[0].first.id,
            "Tile nicht richtig ausgetauscht")
        assertEquals(13, currentGame.wildlifeTokens.size,
            "Falsche Anzahl an WildlifeToken")
        assertEquals(2, currentGame.tileStack.size,
            "Falsche Anzahl Tiles")
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
    }

    /**
     * Überprüft den Aufruf nach dem letzten Zug des Spiels.
     */
    @Test
    fun `nach letztem Zug`() {
        val currentGame = rootService.currentGame
        assertNotNull(currentGame)
        repeat(5) {
            currentGame.tileStack.pop()
        }

        rootService.gameService.changeTurn()

        assertFalse(refreshWasCalled,
            "Refresh wurde aufgerufen")
        assertEquals(3, currentGame.playerQueue.size,
            "Spieler verloren gegangen")


        currentGame.tileStack.push(Tile(100,mutableListOf(), emptyList()))
        currentGame.wildlifeTokens.clear()
        currentGame.removedTokens.clear()

        rootService.gameService.changeTurn()

        assertFalse(refreshWasCalled,
            "Refresh wurde aufgerufen")
        assertEquals(3, currentGame.playerQueue.size,
            "Spieler verloren gegangen")

        currentGame.wildlifeTokens.pushAll(List(12) { WildlifeToken.BEAR })

        rootService.gameService.changeTurn()

        assertFalse(refreshWasCalled,
            "Refresh wurde aufgerufen")
        assertEquals(3, currentGame.playerQueue.size,
            "Spieler verloren gegangen")
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
        }
    }
}