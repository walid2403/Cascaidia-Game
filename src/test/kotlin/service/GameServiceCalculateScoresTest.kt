package service

import entity.*
import kotlin.collections.set
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class GameServiceCalculateScoresTest {
    private class TestRefreshable : Refreshable {

        var receivedScores: List<Pair<String, List<Int>>>? = null

        override fun refreshAfterEndGame(
            scores: List<Pair<String, List<Int>>>
        ) {
            receivedScores = scores
        }
    }
    private lateinit var rootService: RootService
    private lateinit var refreshable: TestRefreshable

    @BeforeTest
    fun setup() {
        rootService = RootService()

        refreshable = TestRefreshable()

        rootService.addRefreshable(refreshable)
    }
    @Test
    fun calculateScoresAllCardsA() {
        val game = CascadiaGame(
            scoringCards = listOf(true, true, true, true, true),
            isLocal = true
        )
        val player = Player("X", PlayerType.HUMAN)
        player.natureTokens = 3
        // Fox in the center
        player.board[Triple(0, 0, 0)] = Tile(
            id = 1,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.FOX)
        ).apply {
            occupant = WildlifeToken.FOX
        }
        // Bear pair
        player.board[Triple(1, -1, 0)] = Tile(
            id = 2,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        ).apply {
            occupant = WildlifeToken.BEAR
        }
        player.board[Triple(2, -1, -1)] = Tile(
            id = 3,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        ).apply {
            occupant = WildlifeToken.BEAR
        }
        // Single elk
        player.board[Triple(0, 1, -1)] = Tile(
            id = 4,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.ELK)
        ).apply {
            occupant = WildlifeToken.ELK
        }
        // Salmon run of 3
        player.board[Triple(-1, 0, 1)] = Tile(
            id = 5,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        ).apply {
            occupant = WildlifeToken.SALMON
        }

        player.board[Triple(-2, 0, 2)] = Tile(
            id = 6,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        ).apply {
            occupant = WildlifeToken.SALMON
        }

        player.board[Triple(0, -1, 1)] = Tile(
            id = 7,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        ).apply {
            occupant = WildlifeToken.SALMON
        }

        // Two isolated hawks
        player.board[Triple(-1, 1, 0)] = Tile(
            id = 8,
            habs = mutableListOf(Habitates.MOUNTAINS),
            possibles = listOf(WildlifeToken.HAWK)
        ).apply {
            occupant = WildlifeToken.HAWK
        }

        player.board[Triple(1, -2, 1)] = Tile(
            id = 9,
            habs = mutableListOf(Habitates.MOUNTAINS),
            possibles = listOf(WildlifeToken.HAWK)
        ).apply {
            occupant = WildlifeToken.HAWK
        }
        // Fill remaining tiles to satisfy the 20-tile requirement
        repeat(5){i->
            player.board[Triple(-1+i, 3-i, -2)] = Tile(
                id = 100+i,
                habs = mutableListOf(Habitates.MOUNTAINS),
                possibles = listOf(WildlifeToken.HAWK)
            )
        }
        repeat(4){i->
            player.board[Triple(0+i, 3-i, -3)] = Tile(
                id = 200+i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.HAWK)
            )
        }
        player.board[Triple(-2, 3, -1)] = Tile(
            id = 9,
            habs = mutableListOf(Habitates.MOUNTAINS),
            possibles = listOf(WildlifeToken.HAWK)
        )
        player.board[Triple(0, -1, -1)] = Tile(
            id = 10,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.HAWK)
        )


        game.playerQueue.add(player)
        rootService.currentGame = game
        val scores = refreshable.receivedScores!!

        assertNotNull(refreshable.receivedScores)
        assertEquals(1, scores.size)
        assertEquals("X", scores.first().first)

        val playerScore = scores.first().second

        assertEquals(4, playerScore[5])
        assertEquals(2, playerScore[6])
        assertEquals(8, playerScore[7])
        assertEquals(5, playerScore[8])
        assertEquals(4, playerScore[9])
        assertEquals(3, playerScore.last())

}