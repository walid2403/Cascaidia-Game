package service

import kotlin.test.*
import entity.*


/**
 * A simple test class to demonstrate a basic unit test.
 */
class ChangeWildlifeTest {

    /**
     * This service is initialized in the [setUp] function hence it is a late-initialized property.
     */
    private lateinit var rootService: RootService
    var refreshWasCalled = false
    var refreshIndices = emptyList<Int>()

    /**
     * Initialize service to set up the test environment. This function is executed before every test.
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
        currentGame.selectedChoice = Pair(-1,-1)
        currentGame.gameState = GameState.START_OF_TURN
        val players = mutableListOf<Player>()
        for (i in 0 until 3) {
            val player = Player("player$i", PlayerType.HUMAN)
            player.natureTokens = 3
            players.add(player)
        }
        currentGame.playerQueue.addAll(players)
        val wildlifeTokens = List(30) { WildlifeToken.SALMON }
        currentGame.wildlifeTokens.pushAll(wildlifeTokens)

        rootService.currentGame = currentGame
        rootService.history.prevMoves.push(currentGame)

        val refreshable = object : Refreshable {
            override fun refreshAfterChangeWildlife(indices: List<Int>) {
                refreshWasCalled = true
                refreshIndices = indices
            }
        }
        rootService.addRefreshable(refreshable)
    }

    /**
     * A simple test to check if the [RootService] is initialized.
     */
    @Test
    fun `valider Spielzug 1`() {
        val game = rootService.currentGame
        assertNotNull(game)

        val selection = game.choices

        rootService.playerActionService.changeWildlife(listOf(0, 1, 2))

        assertTrue(refreshWasCalled, "Der Refresh sollte getriggert haben")
        assertEquals(listOf(0, 1, 2), refreshIndices)

        assertEquals(WildlifeToken.SALMON, selection.elementAt(0).second)
        assertEquals(WildlifeToken.SALMON, selection.elementAt(1).second)
        assertEquals(WildlifeToken.SALMON, selection.elementAt(2).second)
        assertEquals(WildlifeToken.BEAR, selection.elementAt(3).second)

        assertEquals(2, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte um eins reduziert worden sein")

        var bearCount = 0
        val bearPositions = mutableListOf<Int>()

        val wildlifeTokenCount = game.wildlifeTokens.size

        for (i in 0 until wildlifeTokenCount) {
            val token = game.wildlifeTokens.pop()
            if (token == WildlifeToken.BEAR) {
                bearCount++
                bearPositions.add(i)
            }
        }

        assertEquals(3, bearCount, "Es sind Bären verloren gegangen :(")
        assertFalse(bearPositions.contains(wildlifeTokenCount - 3) && bearPositions.contains(wildlifeTokenCount - 2)
                && bearPositions.contains(wildlifeTokenCount - 1),
            "Der Wildlife Token Stack wurde scheinbar nicht gemischt")
    }

    /**
     * A simple test to check if the [RootService] is initialized.
     */
    @Test
    fun `valider Spielzug 2`() {
        val game = rootService.currentGame
        assertNotNull(game)

        val selection = game.choices

        game.gameState = GameState.HAS_EXTERMINATED

        // Markt diversifizieren, damit exterminate() keine automatische
        // Ueberbevoelkerungs-Bereinigung (4 identische Token) ausloest
        game.choices[1] = Pair(game.choices[1].first, WildlifeToken.ELK)
        game.choices[2] = Pair(game.choices[2].first, WildlifeToken.HAWK)

        rootService.playerActionService.changeWildlife(emptyList())

        assertTrue(refreshWasCalled, "Der Refresh sollte getriggert haben")
        assertEquals(listOf(), refreshIndices)

        assertEquals(WildlifeToken.BEAR, selection.elementAt(0).second)
        assertEquals(WildlifeToken.ELK, selection.elementAt(1).second)
        assertEquals(WildlifeToken.HAWK, selection.elementAt(2).second)
        assertEquals(WildlifeToken.BEAR, selection.elementAt(3).second)

        assertEquals(2, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte um eins reduziert worden sein")

        var bearCount = 0
        val bearPositions = mutableListOf<Int>()

        val wildlifeTokenCount = game.wildlifeTokens.size

        for (i in 0 until wildlifeTokenCount) {
            val token = game.wildlifeTokens.pop()
            if (token == WildlifeToken.BEAR) {
                bearCount++
                bearPositions.add(i)
            }
        }

        assertEquals(0, bearCount, "Es sind Bären enstanden :(")
        assertFalse(bearPositions.contains(wildlifeTokenCount - 3) && bearPositions.contains(wildlifeTokenCount - 2)
                && bearPositions.contains(wildlifeTokenCount - 1),
            "Der Wildlife Token Stack wurde scheinbar nicht gemischt")
    }

    /**
     * A simple test to check if the [RootService] is initialized.
     */
    @Test
    fun `invalider Spielzug durch keine Nature Tokens`() {
        val game = rootService.currentGame
        assertNotNull(game)

        val selection = game.choices

        game.playerQueue.peek().natureTokens = 0

        assertFailsWith<IllegalStateException> { rootService.playerActionService.changeWildlife(listOf(0, 1, 2)) }

        assertFalse(refreshWasCalled)

        assertEquals(WildlifeToken.BEAR, selection.elementAt(0).second)
        assertEquals(WildlifeToken.BEAR, selection.elementAt(1).second)
        assertEquals(WildlifeToken.BEAR, selection.elementAt(2).second)
        assertEquals(WildlifeToken.BEAR, selection.elementAt(3).second)

        assertEquals(0, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte nicht verändert worden sein worden sein")
    }

    /**
     * A simple test to check if the [RootService] is initialized.
     */
    @Test
    fun `invalider Spielzug durch falschen GameState`() {
        val game = rootService.currentGame
        assertNotNull(game)

        val selection = game.choices

        game.gameState = GameState.MADE_CHOICE

        assertFailsWith<IllegalStateException> { rootService.playerActionService.changeWildlife(listOf(0, 1, 2)) }

        assertFalse(refreshWasCalled)

        assertEquals(WildlifeToken.BEAR, selection.elementAt(0).second)
        assertEquals(WildlifeToken.BEAR, selection.elementAt(1).second)
        assertEquals(WildlifeToken.BEAR, selection.elementAt(2).second)
        assertEquals(WildlifeToken.BEAR, selection.elementAt(3).second)

        assertEquals(GameState.MADE_CHOICE, game.gameState,
            "Der GameState darf nicht angepasst worden sein")

        assertEquals(3, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte nicht verändert worden sein worden sein")
    }

    /**
     * A simple test to check if the [RootService] is initialized.
     */
    @Test
    fun `invalider Spielzug durch falsche Indizes`() {
        val game = rootService.currentGame
        assertNotNull(game)

        val selection = game.choices

        assertFailsWith<IllegalArgumentException> { rootService.playerActionService.changeWildlife(listOf(0, -1, 2)) }
        assertFailsWith<IllegalArgumentException> { rootService.playerActionService.changeWildlife(listOf(0, 1, 4)) }

        assertFalse(refreshWasCalled)

        assertEquals(GameState.START_OF_TURN, game.gameState,
            "Der GameState darf nicht angepasst worden sein")

        assertEquals(WildlifeToken.BEAR, selection.elementAt(0).second)
        assertEquals(WildlifeToken.BEAR, selection.elementAt(1).second)
        assertEquals(WildlifeToken.BEAR, selection.elementAt(2).second)
        assertEquals(WildlifeToken.BEAR, selection.elementAt(3).second)

        assertEquals(3, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte nicht verändert worden sein worden sein")
    }

    /**
     * Testet, dass [PlayerActionService.changeWildlife] fehlschlägt, wenn kein Spiel läuft.
     */
    @Test
    fun `invalider Spielzug ohne laufendes Spiel`() {
        rootService.currentGame = null

        assertFailsWith<IllegalStateException> {
            rootService.playerActionService.changeWildlife(listOf(0))
        }
        assertFalse(refreshWasCalled)
    }

    /**
     * Testet, dass [PlayerActionService.changeWildlife] mehr als vier Indizes ablehnt.
     */
    @Test
    fun `invalider Spielzug durch mehr als vier Indizes`() {
        val game = rootService.currentGame
        assertNotNull(game)

        assertFailsWith<IllegalArgumentException> {
            rootService.playerActionService.changeWildlife(listOf(0, 1, 2, 3, 0))
        }

        assertFalse(refreshWasCalled)
        assertEquals(3, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte nicht verändert worden sein")
    }

    /**
     * Testet, dass [PlayerActionService.changeWildlife] doppelte Indizes ablehnt.
     */
    @Test
    fun `invalider Spielzug durch doppelte Indizes`() {
        val game = rootService.currentGame
        assertNotNull(game)

        assertFailsWith<IllegalArgumentException> {
            rootService.playerActionService.changeWildlife(listOf(0, 0))
        }

        assertFalse(refreshWasCalled)
        assertEquals(3, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte nicht verändert worden sein")
    }

    /**
     * Testet, dass [PlayerActionService.changeWildlife] die Endwertung auslöst und früh
     * zurückkehrt, wenn der Beutel weniger Token enthält als getauscht werden sollen.
     * Der Markt und die Nature Tokens des Spielers dürfen sich dabei nicht verändern.
     */
    @Test
    fun `Wertung wird ausgeloest wenn der Beutel zu klein ist`() {
        val game = rootService.currentGame
        assertNotNull(game)

        while (game.wildlifeTokens.size > 2) {
            game.wildlifeTokens.pop()
        }

        rootService.playerActionService.changeWildlife(listOf(0, 1, 2))

        assertFalse(refreshWasCalled, "Der Tausch-Refresh darf bei leerem Beutel nicht feuern")
        assertEquals(WildlifeToken.BEAR, game.choices[0].second, "Der Markt darf sich nicht verändern")
        assertEquals(3, game.playerQueue.peek().natureTokens,
            "Es darf kein Nature Token ausgegeben werden")
    }

    /**
     * Testet, dass [PlayerActionService.changeWildlife] auch in einem Netzwerkspiel mit einem
     * menschlichen Spieler funktioniert. Dies führt den Netzwerk-Sende-Zweig aus, der ohne
     * verbundenen Client eine sichere No-Op ist.
     */
    @Test
    fun `valider Spielzug im Netzwerkspiel`() {
        val game = CascadiaGame(List(5) { true }, false)
        val habitats = MutableList(6) { Habitates.MOUNTAINS }
        for (i in 0 until 5) {
            game.tileStack.push(Tile(i, habitats, emptyList()))
        }
        game.natureTokens = 10
        for (i in 0 until 4) {
            game.choices.add(Pair(Tile(10 + i, habitats, emptyList()), WildlifeToken.BEAR))
        }
        game.selectedChoice = Pair(-1, -1)
        game.gameState = GameState.START_OF_TURN
        for (i in 0 until 3) {
            val player = Player("player$i", PlayerType.HUMAN)
            player.natureTokens = 3
            game.playerQueue.add(player)
        }
        game.wildlifeTokens.pushAll(List(30) { WildlifeToken.SALMON })
        rootService.currentGame = game

        rootService.playerActionService.changeWildlife(listOf(0, 1))

        assertTrue(refreshWasCalled, "Der Refresh sollte getriggert haben")
        assertEquals(WildlifeToken.SALMON, game.choices[0].second)
        assertEquals(WildlifeToken.SALMON, game.choices[1].second)
        assertEquals(2, game.playerQueue.peek().natureTokens,
            "Die Anzahl der Nature Tokens des Spielers sollte um eins reduziert worden sein")
    }

}