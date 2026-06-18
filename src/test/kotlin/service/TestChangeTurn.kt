package service

import entity.*
import tools.aqua.bgw.util.Stack
import kotlin.test.*

/**
 * Eine Klasse um die Methode [GameService.changeTurn] zu testen.
 */
class TestChangeTurn {
    lateinit var rootService: RootService

    /**
     * Erstellt ein Cascadia Game und speichert es im rootService.
     * Initialisiert wird es standard mäßig mit folgenden Werten:
     * - scoringCards: Liste mit 5-mal true
     * - isLocal: true
     * - tileStack: 5 Tiles mit jeweils 6-mal Mountains als Habitat und keinen möglichen Tieren
     * - natureTokens: 10
     *
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        val currentGame = CascadiaGame(List(5) { true }, true)
        val tileStack = mutableListOf<Tile>()
        val habitats = MutableList<Habitates>(6) { Habitates.MOUNTAINS }
        for (i in 0 until 5) {
            tileStack.add(Tile(i,habitats,emptyList()))
        }
        currentGame.tileStack.pushAll(tileStack)
        currentGame.natureTokens = 10
        for (i in 0 until 4) {
            val pair = Pair(Tile(10+i,habitats,emptyList()),
                WildlifeToken.BEAR)
            currentGame.choices.add(pair)
        }

        rootService.currentGame = currentGame
    }

    @Test
    fun test() {
        val currentGame = rootService.currentGame
        assertNotNull(currentGame)
        val token = currentGame.choices[0].second
        for (p in currentGame.choices) {
            println(token == p.second)
        }
    }
}