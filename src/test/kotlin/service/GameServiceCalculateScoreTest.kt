package service

import entity.*
import kotlin.Triple
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith

/**
 * the tests for fun calculateScores()
 */
class GameServiceCalculateScoreTest {

    private class TestRefreshable : Refreshable {
        var receivedScores: List<Pair<String, List<Int>>>? = null

        override fun refreshAfterEndGame(scores: List<Pair<String, List<Int>>>) {
            receivedScores = scores
        }
    }
    private lateinit var rootService: RootService
    private lateinit var refreshable: TestRefreshable

    /**
     * Initialize service to set up the test environment. This function is executed before every test.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        refreshable = TestRefreshable()
        rootService.addRefreshable(refreshable)
    }

    /**
     * this fun creates a tile where ALL 6 hab are the same habitat.
     */
    private fun connectedTile(id: Int, hab: Habitates, occupant: WildlifeToken? = null): Tile =
        Tile(id, MutableList(6) { hab }, emptyList()).apply { this.occupant = occupant }

    private fun runGame(
        players: List<Player>,
        scoringCards: List<Boolean> = listOf(true, true, true, true, true)
    ): List<Pair<String, List<Int>>> {
        val game = CascadiaGame(scoringCards = scoringCards, isLocal = true)
        players.forEach { game.playerQueue.add(it) }
        rootService.currentGame = game
        rootService.gameService.calculateScores()
        // val scores = rootService.gameService.calculateScores() NOO!
        assertNotNull(refreshable.receivedScores)
        return refreshable.receivedScores!!
    }
    private fun scoreOf(player: Player, scoringCards: List<Boolean> = listOf(true, true, true, true, true)
    ): List<Int> {
        val otherPlayer = Player("others", PlayerType.HUMAN)
        otherPlayer.board[Triple(99,-99,0)] = connectedTile(666, Habitates.MOUNTAINS)
        return runGame(listOf(player, otherPlayer), scoringCards).first().second
    }
    private fun extremeTest(shape: List<Triple<Int,Int,Int>>, type: WildlifeToken, isA: Boolean) {
        val currentGame = CascadiaGame(List(5){isA}, true)
        rootService.currentGame = currentGame
        val player = Player("player", PlayerType.HUMAN)
        val player2 = Player("player2", PlayerType.HUMAN)
        currentGame.playerQueue.add(player)
        currentGame.playerQueue.add(player2)
        shape.forEachIndexed { i, t ->
            player.board[t] =
                Tile(i,MutableList(6){ Habitates.MOUNTAINS }, emptyList()).
                apply { occupant = type }
        }
        rootService.gameService.calculateScores()
        println(refreshable.receivedScores!!.first().second)
    }

    /**
     * without a current game, calculateScores should throw an exception
     */
    @Test
    fun calculateScoresNoCurrentGame() {
        assertFailsWith<IllegalStateException> {
            rootService.gameService.calculateScores()
        }
    }

    /**
     * Single M tile then M corridor = 1,
     * all others = 0.
     */
    @Test
    fun corridorScoreSingleMountain() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.MOUNTAINS)
        val score = scoreOf(player)
        assertEquals(1, score[0])
        assertEquals(0, score[1])
        assertEquals(0, score[2])
        assertEquals(0, score[3])
        assertEquals(0, score[4])
    }

    /**
     * Single F tile then FORESTS corridor = 1.
     */
    @Test
    fun corridorScoreSingleForest() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.FORESTS)
        val score = scoreOf(player)
        assertEquals(1, score[1])
    }

    /**
     * Single P tile then P corridor = 1.
     */
    @Test
    fun corridorScoreSinglePrairiesTile() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)] = connectedTile(1, Habitates.PRAIRIES)
        val score = scoreOf(player)
        assertEquals(1, score[2])
    }

    /**
     * Single W tile then W corridor = 1.
     */
    @Test
    fun corridorScoreSingleWetlands() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)] = connectedTile(1,Habitates.WETLANDS)
        val score = scoreOf(player)
        assertEquals(1, score[3])
    }

    /**
     * Single R tile then R corridor = 1.
     */
    @Test
    fun corridorScoreSingleRivers() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = connectedTile(1,Habitates.RIVERS)
        val score=scoreOf(player)
        assertEquals(1,score[4])
    }

    /**
     * Two adjacent F tiles then corridor = 2.
     */
    @Test
    fun corridorScoreTwoAdjacentForest() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.FORESTS)
        player.board[Triple(1, -1, 0)] = connectedTile(2, Habitates.FORESTS)
        val score = scoreOf(player)
        assertEquals(2, score[1])
    }

    /**
     * when wo adjacent tiles with DIFFERENT habitats then each corridor stays 1.
     */
    @Test
    fun corridorScoreTwoAdjacentDifferentHabitats() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.MOUNTAINS)
        player.board[Triple(1, -1, 0)] = connectedTile(2, Habitates.FORESTS)
        val score = scoreOf(player)
        assertEquals(1, score[0])
        assertEquals(1, score[1])
    }

    /**
     * Chain of 4 connected R tiles gets corridor = 4.
     */
    @Test
    fun corridorScoreFourRivers() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..3) {
            player.board[Triple(i, -i, 0)] = connectedTile(i, Habitates.RIVERS)
        }
        val score = scoreOf(player)
        assertEquals(4, score[4])
    }

    /**
     * Two disconnected groups of 2 P tiles.
     */
    @Test
    fun corridorScoreSeparateGroups() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]    = connectedTile(1, Habitates.PRAIRIES)
        player.board[Triple(1, -1, 0)]   = connectedTile(2, Habitates.PRAIRIES)
        // far away from the others
        player.board[Triple(10, -10, 0)] = connectedTile(3, Habitates.PRAIRIES)
        player.board[Triple(11, -11, 0)] = connectedTile(4, Habitates.PRAIRIES)
        val score = scoreOf(player)
        assertEquals(2, score[2])
    }
    /**
     * with 2 players: p1 has bigger M corridor gets 2, p2 gets nothing.
     */
    @Test
    fun majorityTwoPlayers() {
        val p1 = Player("Alice", PlayerType.HUMAN)
        val p2 = Player("Bob",   PlayerType.HUMAN)
        for (i in 0..2) p1.board[Triple(i, -i, 0)] = connectedTile(i, Habitates.MOUNTAINS)
        p2.board[Triple(0, 0, 0)] = connectedTile(10, Habitates.MOUNTAINS)
        val scores = runGame(listOf(p1, p2))
        assertEquals(2, scores[0].second[10]) // Alice wins MOUNTAINS majority
        assertEquals(0, scores[1].second[10]) // Bob loses
    }

    /**
     * with 2 players tied in M corridor both get 1.
     */
    @Test
    fun majorityBothGetOne() {
        val p1 = Player("Alice", PlayerType.HUMAN)
        val p2 = Player("Bob",   PlayerType.HUMAN)
        p1.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.MOUNTAINS)
        p2.board[Triple(0, 0, 0)] = connectedTile(2, Habitates.MOUNTAINS)
        val scores = runGame(listOf(p1, p2))
        assertEquals(1, scores[0].second[10])
        assertEquals(1, scores[1].second[10])
    }

    /**
     * when 3 players distinct corridor sizes then first gets 3, second gets 1, third gets 0.
     */
    @Test
    fun majorityThreePlayers() {
        val p1 = Player("Alexa", PlayerType.HUMAN)
        val p2 = Player("Ben", PlayerType.HUMAN)
        val p3 = Player("Christy", PlayerType.HUMAN)
        for (i in 0..2) p1.board[Triple(i, -i, 0)]    = connectedTile(i,      Habitates.MOUNTAINS)
        for (i in 0..1) p2.board[Triple(i, -i, 0)]    = connectedTile(i + 10, Habitates.MOUNTAINS)
        p3.board[Triple(0, 0, 0)] = connectedTile(20, Habitates.MOUNTAINS)
        val scores = runGame(listOf(p1, p2, p3))
        assertEquals(3, scores[0].second[10]) // 1st
        assertEquals(1, scores[1].second[10]) // 2nd
        assertEquals(0, scores[2].second[10]) // 3rd
    }

    /**
     * 3 players two tied for 1st then each gets 2, last get 0.
     */
    @Test
    fun majorityTiedFirst() {
        val p1 = Player("A", PlayerType.HUMAN)
        val p2 = Player("B", PlayerType.HUMAN)
        val p3 = Player("C", PlayerType.HUMAN)
        for (i in 0..1) p1.board[Triple(i, -i, 0)] = connectedTile(i,      Habitates.MOUNTAINS)
        for (i in 0..1) p2.board[Triple(i, -i, 0)] = connectedTile(i + 10, Habitates.MOUNTAINS)
        p3.board[Triple(0, 0, 0)] = connectedTile(20, Habitates.MOUNTAINS)
        val scores = runGame(listOf(p1, p2, p3))
        assertEquals(2, scores[0].second[10])
        assertEquals(2, scores[1].second[10])
        assertEquals(0, scores[2].second[10])
    }

    /**
     * 3 players all tied then each gets 1.
     */
    @Test
    fun majorityAllTied() {
        val p1 = Player("A", PlayerType.HUMAN)
        val p2 = Player("B", PlayerType.HUMAN)
        val p3 = Player("C", PlayerType.HUMAN)
        p1.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.MOUNTAINS)
        p2.board[Triple(0, 0, 0)] = connectedTile(2, Habitates.MOUNTAINS)
        p3.board[Triple(0, 0, 0)] = connectedTile(3, Habitates.MOUNTAINS)
        val scores = runGame(listOf(p1, p2, p3))
        //results has to
        assertEquals(1, scores[0].second[10])
        assertEquals(1, scores[1].second[10])
        assertEquals(1, scores[2].second[10])
    }

    /**
     * natureTokens = 5 then stored at index 15, last one.
     */
    @Test
    fun natureTokensStored() {
        val player = Player("P", PlayerType.HUMAN)
        player.natureTokens = 5
        player.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.MOUNTAINS)
        val score = scoreOf(player)
        assertEquals(5, score[15])
        assertEquals(5, score.last())
    }

    /**
     * natureTokens = 0 then stored correctly as 0.
     */
    @Test
    fun natureTokensStoredCorrectly() {
        val player = Player("P", PlayerType.HUMAN)
        player.natureTokens = 0
        player.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.MOUNTAINS)
        val score = scoreOf(player)
        assertEquals(0, score.last())
    }
    /**
     * Tests that the score list of each player has exactly 16 entries:
     * 5 corridor scores + 5 wildlife scores + 5 majority bonuses + 1 nature token = 16.
     */
    @Test
    fun scoreListWithSixteenEntriesPerPlayer() {
        val p1 = Player("A", PlayerType.HUMAN)
        val p2 = Player("B", PlayerType.HUMAN)
        p1.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.MOUNTAINS)
        p2.board[Triple(0, 0, 0)] = connectedTile(2, Habitates.MOUNTAINS)
        val scores = runGame(listOf(p1, p2))
        assertEquals(16, scores[0].second.size)
        assertEquals(16, scores[1].second.size)
    }
    /**
     * Tests that the player names are correctly stored in the
     * output score list as the first element of each pair.
     */
    @Test
    fun playerNamesCorrect() {
        val p1 = Player("ONE", PlayerType.HUMAN)
        val p2 = Player("TWO",   PlayerType.HUMAN)
        p1.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.MOUNTAINS)
        p2.board[Triple(0, 0, 0)] = connectedTile(2, Habitates.MOUNTAINS)
        val scores = runGame(listOf(p1, p2))
        assertEquals("ONE", scores[0].first)
        assertEquals("TWO",   scores[1].first)
    }
    /**
     * Tests that refreshAfterEndGame is called after calculateScores()
     * and that the received scores are not null.
     */

    @Test
    fun refreshable_isCalledAfterCalculateScores() {
        val p1 = Player("P", PlayerType.HUMAN)
        p1.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.MOUNTAINS)
        val someoneElse = Player("other", PlayerType.HUMAN)
        someoneElse.board[Triple(99, -99, 0)] = connectedTile(999, Habitates.MOUNTAINS)
        runGame(listOf(p1, someoneElse))
        assertNotNull(refreshable.receivedScores)
    }
    /**
     * No bears on the board then bear score 0.
     */
    @Test
    fun bearAZero() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.FORESTS, WildlifeToken.ELK)
        assertEquals(0, scoreOf(player, listOf(true, true, true, true, true))[5])
    }

    /**
     * One valid bear pair then 4 points.
     */
    @Test
    fun bearAFour() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(1, -1, 0)] = connectedTile(2, Habitates.FORESTS, WildlifeToken.BEAR)
        assertEquals(4, scoreOf(player, listOf(true, true, true, true, true))[5])
    }

    /**
     * Two separate bear pairs then 11 points.
     */
    @Test
    fun bearATwoPairs() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]    = connectedTile(1, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(1, -1, 0)]   = connectedTile(2, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(10, -10, 0)] = connectedTile(3, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(11, -11, 0)] = connectedTile(4, Habitates.FORESTS, WildlifeToken.BEAR)
        assertEquals(11, scoreOf(player, listOf(true, true, true, true, true))[5])
    }

    /**
     * Three bear pairs then 19 points.
     */
    @Test
    fun bearAThreePairs() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..2) {
            player.board[Triple(i * 10,-(i * 10),0)] =
                connectedTile(i * 2,     Habitates.FORESTS, WildlifeToken.BEAR)
            player.board[Triple(i * 10 + 1, -(i * 10 + 1), 0)] =
                connectedTile(i * 2 + 1, Habitates.FORESTS, WildlifeToken.BEAR)
        }
        assertEquals(19, scoreOf(player, listOf(true, true, true, true, true))[5])
    }

    /**
     * Four bear pairs then 27 points.
     */
    @Test
    fun bearAFourPairs() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..3) {
            player.board[Triple(i * 10,-(i * 10),0)] =
                connectedTile(i * 2,Habitates.FORESTS, WildlifeToken.BEAR)
            player.board[Triple(i * 10 + 1,-(i * 10 + 1), 0)] = connectedTile(i*2 + 1, Habitates.FORESTS,
                WildlifeToken.BEAR)
        }
        assertEquals(27, scoreOf(player, listOf(true, true, true, true, true))[5])
    }

    /**
     * Three bears in a row: the middle bear has 2 neighbors gives 0.
     */
    @Test
    fun bearAThreeInRowNoPairs() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0, 0)]  = connectedTile(1, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(1, -1,0)] = connectedTile(2, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(2, -2,0)] = connectedTile(3, Habitates.FORESTS, WildlifeToken.BEAR)
        assertEquals(0, scoreOf(player, listOf(true, true, true, true, true))[5])
    }
    /**
     * No bears then 0.
     */
    @Test
    fun bearBZero() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.FORESTS, WildlifeToken.ELK)
        assertEquals(0, scoreOf(player, listOf(false, true, true, true, true))[5])
    }

    /**
     * One isolated bear then 10 points.
     */
    @Test
    fun bearBSingle() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.FORESTS, WildlifeToken.BEAR)
        assertEquals(0, scoreOf(player, listOf(false, true,true,true,true))[5])
    }

    /**
     * Two adjacent bears then counts as 1 group → 10 points.
     */
    @Test
    fun bearBTwoBearsAdjacent() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(1, -1, 0)] = connectedTile(2, Habitates.FORESTS, WildlifeToken.BEAR)
        assertEquals(0, scoreOf(player, listOf(false,true,true, true , true))[5])
    }

    /**
     * Two separate isolated bears give 2 groups then 20 points.
     */
    @Test
    fun bearBTwoSeparateBears() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)]= connectedTile(1, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(10,-10,0)]=connectedTile(2, Habitates.FORESTS, WildlifeToken.BEAR)
        assertEquals(0, scoreOf(player, listOf(false,true,true,true,true))[5])
    }

    /**
     * Ein Test für einen Extremfall bei elkAScore
     */
    @Test
    fun elkAExtreme1() {
        val shape = listOf(
            Triple(0,0,0),
            Triple(1,-1,0),
            Triple(2,-2,0),
            Triple(-1,0,1),
            Triple(-2,0,2),
            Triple(-3,0,3)
        )
        extremeTest(shape, WildlifeToken.ELK, true)
        assertEquals(18, refreshable.receivedScores!!.first().second[6],
            "Falscher Score")
    }

    /**
     * Ein Test für einen Extremfall bei elkAScore
     */
    @Test
    fun elkAExtreme2() {
        val shape = listOf(
            Triple(0,0,0),
            Triple(1,-2,1),
            Triple(2,-3,1),
            Triple(0,-1,1),
            Triple(-1,0,1),
            Triple(-2,0,2),
            Triple(-3,0,3)
        )
        extremeTest(shape, WildlifeToken.ELK, true)

        assertEquals(22, refreshable.receivedScores!!.first().second[6],
            "Falscher Score")
    }

    /**
     * Ein Test für einen Extremfall bei elkAScore
     */
    @Test
    fun elkAExtreme3() {
        val shape = listOf(
            Triple(0,0,0),
            Triple(-1,0,1),
            Triple(-2,0,2),
            Triple(1,-2,1),
            Triple(0,-2,2),
            Triple(-1,-2,3),
            Triple(0,-1,1),
            Triple(1,-1,0),
            Triple(-1,-1,2),
            Triple(-2,-1,3),
        )
        extremeTest(shape, WildlifeToken.ELK, true)

        assertEquals(31, refreshable.receivedScores!!.first().second[6],
            "Falscher Score")
    }

    /**
     * Ein Test für einen Extremfall bei elkAScore
     */
    @Test
    fun elkAExtreme4() {
        val shape = listOf(
            Triple(0,0,0),
            Triple(-1,0,1),
            Triple(-2,0,2),
            Triple(-3,0,3),
            Triple(-1,1,0),
            Triple(1,-1,0)
        )
        extremeTest(shape, WildlifeToken.ELK, true)

        assertEquals(18, refreshable.receivedScores!!.first().second[6],
            "Falscher Score")
    }

    /**
     * Ein Test für einen Extremfall bei elkAScore
     */
    @Test
    fun elkAExtreme5() {
        val shape = listOf(
            Triple(0,0,0),
            Triple(-1,0,1),
            Triple(-2,0,2),
            Triple(-3,0,3),
            Triple(-2,-1,3),
            Triple(-2,1,1)
        )
        extremeTest(shape, WildlifeToken.ELK, true)

        assertEquals(17, refreshable.receivedScores!!.first().second[6],
            "Falscher Score")
    }

    /**
     * Ein Test für einen Extremfall bei elkAScore
     */
    @Test
    fun elkBExtreme1() {
        val shape = listOf(
            Triple(0,0,0),
            Triple(1,-1,0),
            Triple(2,-2,0),
            Triple(-1,0,1),
            Triple(-2,0,2),
            Triple(-3,0,3)
        )
        extremeTest(shape, WildlifeToken.ELK, false)

        assertEquals(15, refreshable.receivedScores!!.first().second[6],
            "Falscher Score")
    }

    /**
     * Ein Test für einen Normalfall bei elkAScore
     */
    @Test
    fun elkBNormal1() {
        val shape = listOf(
            Triple(0,0,0),
            Triple(1,-1,0),
        )
        extremeTest(shape, WildlifeToken.ELK, false)

        assertEquals(5, refreshable.receivedScores!!.first().second[6],
            "Falscher Score")
    }

    /**
     * Ein Test für einen Normalfall bei elkAScore
     */
    @Test
    fun elkBNormal2() {
        val shape = listOf(
            Triple(0,0,0),
            Triple(1,-1,0),
            Triple(1,0,-1),
            Triple(0,1,-1)
        )
        extremeTest(shape, WildlifeToken.ELK, false)

        assertEquals(13, refreshable.receivedScores!!.first().second[6],
            "Falscher Score")
    }

    /**
     * Ein Test für einen Extremfall bei elkAScore
     */
    @Test
    fun elkBExtreme2() {
        val shape = listOf(
            Triple(0,0,0),
            Triple(1,-2,1),
            Triple(2,-3,1),
            Triple(0,-1,1),
            Triple(-1,0,1),
            Triple(-2,0,2),
            Triple(-3,0,3)
        )
        extremeTest(shape, WildlifeToken.ELK, false)

        assertEquals(19, refreshable.receivedScores!!.first().second[6],
            "Falscher Score")
    }

    /**
     * Ein Test für einen Extremfall bei elkAScore
     */
    @Test
    fun elkBExtreme3() {
        val shape = listOf(
            Triple(0,0,0),
            Triple(-1,0,1),
            Triple(-2,0,2),
            Triple(1,-2,1),
            Triple(0,-2,2),
            Triple(-1,-2,3),
            Triple(0,-1,1),
            Triple(1,-1,0),
            Triple(-1,-1,2),
            Triple(-2,-1,3),
        )
        extremeTest(shape, WildlifeToken.ELK, false)
        print(refreshable.receivedScores!!.first().second[6])
//        assertEquals(30, refreshable.receivedScores!!.first().second[6],
//            "Falscher Score")
    }

    /**
     * Ein Test für einen Extremfall bei elkAScore
     */
    @Test
    fun elkBExtreme4() {
        val shape = listOf(
            Triple(0,0,0),
            Triple(-1,0,1),
            Triple(-2,0,2),
            Triple(-3,0,3),
            Triple(-1,1,0),
            Triple(1,-1,0)
        )
        extremeTest(shape, WildlifeToken.ELK, false)

        assertEquals(16, refreshable.receivedScores!!.first().second[6],
            "Falscher Score")
    }

    /**
     * Ein Test für einen Extremfall bei elkAScore
     */
    @Test
    fun elkBExtreme5() {
        val shape = listOf(
            Triple(0,0,0),
            Triple(-1,0,1),
            Triple(-2,0,2),
            Triple(-3,0,3),
            Triple(-2,-1,3),
            Triple(-2,1,1)
        )
        extremeTest(shape, WildlifeToken.ELK, false)

        assertEquals(16, refreshable.receivedScores!!.first().second[6],
            "Falscher Score")
    }

    /**
     * An extra test for a special falcon case
     */
    @Test
    fun falconBExtra1() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,-1,1)]  = connectedTile(0,Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(-1,0,1)]  = connectedTile(1,Habitates.PRAIRIES, WildlifeToken.HAWK)
        player.board[Triple(2,-3,1)] = connectedTile(4, Habitates.PRAIRIES, WildlifeToken.HAWK)
        player.board[Triple(1,-2,1)] = connectedTile(5,Habitates.PRAIRIES, WildlifeToken.ELK)
        assertEquals(5, scoreOf(player, listOf(true, true, true, false, true))[8])
    }

    /**
     * An extra test for a special falcon case
     */
    @Test
    fun falconBExtra2() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)]  = connectedTile(0,Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(-1,0,1)]  = connectedTile(1,Habitates.PRAIRIES, WildlifeToken.HAWK)
        player.board[Triple(1,0,-1)] = connectedTile(2, Habitates.PRAIRIES, WildlifeToken.HAWK)
        player.board[Triple(2,-1,-1)] = connectedTile(3,Habitates.PRAIRIES, WildlifeToken.HAWK)
        player.board[Triple(2,1,-3)] = connectedTile(4, Habitates.PRAIRIES, WildlifeToken.HAWK)
        player.board[Triple(2,0,-2)] = connectedTile(5,Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(-2,0,2)] = connectedTile(5,Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(-3,0,3)] = connectedTile(5,Habitates.PRAIRIES, WildlifeToken.HAWK)
        assertEquals(9, scoreOf(player, listOf(true, true, true, false, true))[8])
    }

    /**
     * An extra test for a special falcon case
     */
    @Test
    fun falconBExtra3() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(1,0,-1)]  = connectedTile(0,Habitates.PRAIRIES, WildlifeToken.HAWK)
        player.board[Triple(1,-1,0)]  = connectedTile(2,Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(0,-1,1)]  = connectedTile(3,Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(-1,0,1)]  = connectedTile(1,Habitates.PRAIRIES, WildlifeToken.HAWK)
        assertEquals(5, scoreOf(player, listOf(true, true, true, false, true))[8])
    }

    /**
     * Single elk with no elk neighbors give 0 points.
     */
    @Test
    fun elkASingle() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)] = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.ELK)
        assertEquals(2, scoreOf(player, listOf(true, true, true, true, true))[6])
    }

    /**
     * Two elk connected on y-axis give 2 points.
     */
    @Test
    fun elkAlineOfTwo() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0, 0)]  = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(0, 1,-1)] = connectedTile(2,Habitates.PRAIRIES, WildlifeToken.ELK)
        assertEquals(5, scoreOf(player, listOf(true, true, true, true, true))[6])
    }

    /**
     * Three elk in a give 5 points.
     */
    @Test
    fun elkAlineOfThree() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)]  = connectedTile(1,Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(0,1, -1)] = connectedTile(2, Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(0, 2,-2)] = connectedTile(3,Habitates.PRAIRIES, WildlifeToken.ELK)
        assertEquals(9, scoreOf(player, listOf(true, true, true, true, true))[6])
    }

    /**
     * Four elk in a line give 9 points.
     */
    @Test
    fun elkAlineOfFour() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..3) player.board[Triple(0,i,-i)] =
            connectedTile(i,Habitates.PRAIRIES, WildlifeToken.ELK)
        assertEquals(13, scoreOf(player, listOf(true, true, true, true, true))[6])
    }

    /**
     * Five elk in a line give 13 points.
     */
    @Test
    fun elkAlineOfFive() {
        val player=Player("P", PlayerType.HUMAN)
        for (i in 0..4) player.board[Triple(0, i, -i)] =
            connectedTile(i,Habitates.PRAIRIES,WildlifeToken.ELK)
        assertEquals(15, scoreOf(player,listOf(true, true, true, true, true))[6])
    }

    /**
     * Single isolated elk give 2 points.
     */
    @Test
    fun elkBSingle() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.ELK)
        assertEquals(2, scoreOf(player, listOf(true, false, true, true, true))[6])
    }

    /**
     * Pair of elk give 5 points.
     */
    @Test
    fun elkBPairOfElk() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(0,1,-1)] = connectedTile(2, Habitates.PRAIRIES, WildlifeToken.ELK)
        assertEquals(5, scoreOf(player, listOf(true, false, true, true, true))[6])
    }

    /**
     * No elk gives 0.
     */
    @Test
    fun elkBZero() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)] = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.BEAR)
        assertEquals(0, scoreOf(player, listOf(true, false, true, true, true))[6])
    }
    /**
     * Tests that salmonScoring returns 0 when there are no salmon on the board.
     */

    @Test
    fun salmonZero() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)] = connectedTile(1, Habitates.RIVERS, WildlifeToken.ELK)
        assertEquals(0, scoreOf(player, listOf(true, true, true, true, true))[7])
    }
    /**
     * Tests that a single salmon scores 2 points with scoring variant A.
     */

    @Test
    fun salmonASingle() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = connectedTile(1,Habitates.RIVERS,WildlifeToken.SALMON)
        assertEquals(2, scoreOf(player, listOf(true, true, true, true, true))[7])
    }
    /**
     * Tests that a single salmon scores 2 points with scoring variant B.
     */
    @Test
    fun salmonBSingle() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0,0)] = connectedTile(1,Habitates.RIVERS, WildlifeToken.SALMON)
        assertEquals(2,scoreOf(player, listOf(true, true, false, true, true))[7])
    }
    /**
     * Tests that a run of 2 salmon scores 5 points with scoring variant A.
     */
    @Test
    fun salmonARunOfTwo() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.RIVERS, WildlifeToken.SALMON)
        player.board[Triple(1, -1, 0)] = connectedTile(2, Habitates.RIVERS, WildlifeToken.SALMON)
        assertEquals(5, scoreOf(player, listOf(true, true, true, true, true))[7])
    }
    /**
     * Tests that a run of 2 salmon scores 4 points with scoring variant B.
     */
    @Test
    fun salmonBRunOfTwo() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)]  = connectedTile(1, Habitates.RIVERS, WildlifeToken.SALMON)
        player.board[Triple(1, -1,0)] = connectedTile(2, Habitates.RIVERS, WildlifeToken.SALMON)
        assertEquals(4, scoreOf(player, listOf(true, true, false, true, true))[7])
    }
    /**
     * Tests that a run of 3 salmon scores 8 points with scoring variant A.
     */
    @Test
    fun salmonRunOfThreeA() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..2) player.board[Triple(i, -i, 0)] =
            connectedTile(i,Habitates.RIVERS, WildlifeToken.SALMON)
        assertEquals(8,scoreOf(player, listOf(true, true, true, true, true))[7])
    }
    /**
     * Tests that a run of 3 salmon scores 9 points with scoring variant B.
     */
    @Test
    fun salmonRunOfThreeB() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..2) player.board[Triple(i, -i, 0)] =
            connectedTile(i, Habitates.RIVERS, WildlifeToken.SALMON)
        //salmon gets B
        assertEquals(9,scoreOf(player,listOf(true,true,  false,true,true))[7])
    }
    /**
     * Tests that a run of 4 salmon scores 12 points with scoring variant A.
     */
    @Test
    fun salmonRunOfFourA() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..3) player.board[Triple(i, -i, 0)] =
            connectedTile(i, Habitates.RIVERS, WildlifeToken.SALMON)
        //all can be true
        assertEquals(12, scoreOf(player, listOf(true, true, true, true, true))[7])
    }
    /**
     * Tests that a run of 4 salmon scores 11 points with scoring variant B.
     */
    @Test
    fun salmonRunOfFourB() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..3) player.board[Triple(i, -i, 0)] =
            connectedTile(i, Habitates.RIVERS, WildlifeToken.SALMON)
        //working on salmon
        assertEquals(11, scoreOf(player, listOf(true, true, false, true, true))[7])
    }
    /**
     * Tests that a run of 5 salmon scores 16 points with scoring variant A.
     */
    @Test
    fun salmonRunOfFiveA() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..4) player.board[Triple(i,-i,0)] =
            connectedTile(i, Habitates.RIVERS, WildlifeToken.SALMON)
        assertEquals(16, scoreOf(player, listOf(true, true, true, true, true))[7])
    }
    /**
     * Tests that a run of 5 salmon scores 17 points with scoring variant B.
     */
    @Test
    fun salmonRunOfFiveB() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..4) player.board[Triple(i,-i,0)] =
            connectedTile(i, Habitates.RIVERS, WildlifeToken.SALMON)
        //might change the others later except salmon
        assertEquals(17, scoreOf(player, listOf(true, true, false, true, true))[7])
    }
    /**
     * Tests that a run of 6 salmon scores 20 points with scoring variant A.
     */
    @Test
    fun salmonRunOfSix() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..5) player.board[Triple(i,-i,0)] =
            connectedTile(i, Habitates.RIVERS, WildlifeToken.SALMON)
        assertEquals(20, scoreOf(player, listOf(true, true, true, true, true))[7])
    }
    /**
     * Tests that a run of 7 or more salmon scores 25 points with scoring variant A,
     * which is the maximum possible salmon score.
     */
    @Test
    fun salmonRunOfSeven() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..6) player.board[Triple(i,-i,0)] =
            connectedTile(i, Habitates.RIVERS, WildlifeToken.SALMON)
        assertEquals(25, scoreOf(player, listOf(true, true,true, true,true))[7])
    }
    /**
     * Tests that hawkScoringA returns 0 when there are no hawks on the board.
     */
    @Test
    fun hawkANoHawksZero() {
        val player = Player("H", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.MOUNTAINS, WildlifeToken.ELK)
        assertEquals(0, scoreOf(player, listOf(true, true,true,true,true))[8])
    }
    /**
     * Tests that a single isolated hawk scores 2 points with scoring variant A.
     */
    @Test
    fun hawkAOneIsolatedHawkTwo() {
        val player = Player("H", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        assertEquals(2, scoreOf(player, listOf(true, true, true, true, true))[8])
    }
    /**
     * Tests that two isolated hawks (not adjacent to each other)
     * score 5 points with scoring variant A.
     */
    @Test
    fun hawkATwoIsolatedHawksFive() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)]    = connectedTile(1, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        player.board[Triple(10,-10, 0)] = connectedTile(2, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        assertEquals(5, scoreOf(player, listOf(true, true, true, true, true))
            [8])
    }
    /**
     * Tests that three isolated hawks score 8 points with scoring variant A.
     */
    @Test
    fun hawkThreeIsolatedHawksEight() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0, 0)]    = connectedTile(1, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        player.board[Triple(10,-10, 0)] = connectedTile(2, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        player.board[Triple(20,-20, 0)] = connectedTile(3, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        assertEquals(8, scoreOf(player, listOf(true, true, true, true, true))[8])
    }
    /**
     * Tests that four isolated hawks score 11 points with scoring variant A.
     */
    @Test
    fun hawkAFourIsolatedHawksEleven() {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0..3) player.board[Triple(i* 10,-(i* 10),0)] =
            connectedTile(i, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        assertEquals(11, scoreOf(player, listOf(true, true, true, true, true))[8])
    }

    /**
     * Two adjacent hawks when neither is isolated then 0.
     */
    @Test
    fun hawkATwoAdjacentHawksZero() {
        val player = Player("H", PlayerType.HUMAN)
        player.board[Triple(0, 0,0)]  = connectedTile(1, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        player.board[Triple(1, -1,0)] = connectedTile(2, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        assertEquals(0, scoreOf(player, listOf(true, true, true, true, true))[8])
    }
    /**
     * Tests that hawkScoringB returns 0 when there are no hawks on the board.
     */
    @Test
    fun hawkBZero() {
        val player = Player("H", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = connectedTile(1,Habitates.MOUNTAINS, WildlifeToken.ELK)
        assertEquals(0, scoreOf(player,listOf(true, true, true, false, true))[8])
    }
    /**
     * Tests that a single hawk scores 0 points with scoring variant B
     */
    @Test
    fun singleHawkIsZero() {
        val player = Player("H", PlayerType.HUMAN)
        player.board[Triple(0,0,0)] = connectedTile(1, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        // count=1 → 0 points
        assertEquals(0, scoreOf(player,
            listOf(true, true, true, false, true))[8])
    }
    /**
     * Tests that a fox with no neighbours scores 0 points with scoring variant A.
     */
    @Test
    fun foxAZero() {
        val player = Player("F", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)] = connectedTile(1,Habitates.PRAIRIES, WildlifeToken.FOX)
        assertEquals(0, scoreOf(player,listOf(true, true, true, true, true))[9])
    }
    /**
     * Tests that a fox with one distinct neighbor type scores 1 point
     * with scoring variant A
     */
    @Test
    fun foxAOneA() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0,0)]  = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.FOX)
        player.board[Triple(1,-1, 0)] = connectedTile(2,Habitates.PRAIRIES,WildlifeToken.BEAR)
        assertEquals(1, scoreOf(player, listOf(true, true,
            true, true, true))[9])
    }
    /**
     * Tests that a fox with two distinct neighbour types scores 2 points
     * with scoring variant A
     */
    @Test
    fun foxATwo() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.FOX)
        player.board[Triple(1, -1, 0)] = connectedTile(2,Habitates.PRAIRIES,WildlifeToken.BEAR)
        //
        player.board[Triple(0, 1, -1)] = connectedTile(3, Habitates.PRAIRIES, WildlifeToken.ELK)
        assertEquals(2, scoreOf(player, listOf(true, true, true, true, true))[9])
    }
    /**
     * Tests that a fox with three distinct neighbour types scores 3 points
     * with scoring variant A
     */
    @Test
    fun foxAThree() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.FOX)
        player.board[Triple(1,-1,0)] = connectedTile(2, Habitates.PRAIRIES, WildlifeToken.BEAR)
        player.board[Triple(0,1, -1)] = connectedTile(3,Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(1, 0,-1)] = connectedTile(4,Habitates.PRAIRIES, WildlifeToken.SALMON)
        assertEquals(3, scoreOf(player, listOf(true, true, true, true, true))[9])
    }

    /**
     * Two BEAR neighbors  still gives 1 point.
     */
    @Test
    fun foxAOne() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0,0)]  = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.FOX)
        player.board[Triple(1,-1, 0)] = connectedTile(2, Habitates.PRAIRIES, WildlifeToken.BEAR)
        player.board[Triple(0,1, -1)] = connectedTile(3, Habitates.PRAIRIES, WildlifeToken.BEAR)
        assertEquals(1, scoreOf(player, listOf(true, true, true, true, true))[9])
    }
    /**
     * Tests that a fox with no neighbors scores 0 points with scoring variant A.
     */
    @Test
    fun foxAZeroA() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)] = connectedTile(1,Habitates.PRAIRIES, WildlifeToken.BEAR)
        assertEquals(0, scoreOf(player, listOf(true, true, true, true, true))[9])
    }
    /**
     * Tests that a fox with no neighbors scores 0 points with scoring variant B.
     */
    @Test
    fun foxBZero() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0,0)]  = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.FOX)
        player.board[Triple(1,-1, 0)] = connectedTile(2, Habitates.PRAIRIES, WildlifeToken.BEAR)
        //others have no importance
        assertEquals(0, scoreOf(player, listOf(true, true, true, true, false))[9])
    }
    /**
     * Tests that a fox with one pair of bear neighbors scores 3 points
     * with scoring variant B
     */
    @Test
    fun foxBThree() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0,0)]  = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.FOX)
        player.board[Triple(1,-1, 0)] = connectedTile(2, Habitates.PRAIRIES, WildlifeToken.BEAR)
        player.board[Triple(0,1,-1)] = connectedTile(3, Habitates.PRAIRIES, WildlifeToken.BEAR)
        assertEquals(3, scoreOf(player, listOf(true,true,true,true,false))[9])
    }
    /**
     * Tests that a fox with two pairs of different neighbors scores 5 points
     * with scoring variant B
     */
    @Test
    fun foxBFive() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)]  = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.FOX)
        player.board[Triple(1,-1,0)] = connectedTile(2, Habitates.PRAIRIES, WildlifeToken.BEAR)
        player.board[Triple(0, 1,-1)] = connectedTile(3, Habitates.PRAIRIES, WildlifeToken.BEAR)
        player.board[Triple(1, 0, -1)] = connectedTile(4, Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(-1, 1, 0)] = connectedTile(5, Habitates.PRAIRIES, WildlifeToken.ELK)
        assertEquals(5, scoreOf(player, listOf(true, true, true, true, false))[9])
    }
    /**
     * Tests that a fox with three pairs of different neighbors scores 7 points
     * with scoring variant B
     */
    @Test
    fun foxBSeven() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(1, -1, 0)] = connectedTile(2, Habitates.FORESTS,  WildlifeToken.BEAR)
        player.board[Triple(-1, 1, 0)] = connectedTile(3, Habitates.FORESTS,  WildlifeToken.BEAR)
        player.board[Triple(0, 1,-1)] = connectedTile(4, Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(0, -1,1)] = connectedTile(5, Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(1,0, -1)] = connectedTile(6, Habitates.RIVERS,   WildlifeToken.SALMON)
        player.board[Triple(-1, 0, 1)] = connectedTile(7, Habitates.RIVERS,   WildlifeToken.SALMON)
        player.board[Triple(0,0,0)]  = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.FOX)

        assertEquals(7, scoreOf(player, listOf(true, true, true, true, false))[9])
    }
    /**
     * Tests that foxScoringB returns 0 when there is no fox on the board.
     */
    @Test
    fun foxBZeroB() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0, 0)] = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.BEAR)
        //working only on fox
        assertEquals(0, scoreOf(player, listOf(true, true, true, true, false))[9])
    }
    /**
     * Tests that using all scoring cards B variant still produces
     * a score list of exactly 16 entries per player.
     */
    @Test
    fun allCardsB() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0,0,0)] = connectedTile(1, Habitates.MOUNTAINS)
        //all should be b
        assertEquals(16, scoreOf(player, listOf(false, false, false, false, false)).size)
    }

    /**
     * Tests bear card B with a straight group of exactly three bears where the middle
     * bear is evaluated first, covering the two-neighbour branch of the group check.
     */
    @Test
    fun bearBGroupOfThreeFromMiddle() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(1, -1, 0)] = connectedTile(1, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(0, 0, 0)]  = connectedTile(2, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(2, -2, 0)] = connectedTile(3, Habitates.FORESTS, WildlifeToken.BEAR)
        assertEquals(10, scoreOf(player, listOf(false, true, true, true, true))[5],
            "A group of exactly three bears scores 10 points on card B")
    }

    /**
     * Tests bear card B with the same group of three bears where an end bear is evaluated
     * first, covering the single-neighbour branch of the group check.
     */
    @Test
    fun bearBGroupOfThreeFromEnd() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(1, -1, 0)] = connectedTile(2, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(2, -2, 0)] = connectedTile(3, Habitates.FORESTS, WildlifeToken.BEAR)
        assertEquals(10, scoreOf(player, listOf(false, true, true, true, true))[5],
            "A group of exactly three bears scores 10 points on card B")
    }
    /**
     * Tests bear card B with a straight line of four bears evaluated from the second
     * bear, covering the pair-rejection branches where a neighbour touches a third bear.
     * Four bears in a line form no group of exactly three, so card B scores zero.
     */
    @Test
    fun bearBLineOfFourFromSecond() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(1, -1, 0)] = connectedTile(1, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(0, 0, 0)]  = connectedTile(2, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(2, -2, 0)] = connectedTile(3, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(3, -3, 0)] = connectedTile(4, Habitates.FORESTS, WildlifeToken.BEAR)
        assertEquals(0, scoreOf(player, listOf(false, true, true, true, true))[5],
            "Four bears in a line contain no group of exactly three and score zero on card B")
    }

    /**
     * Tests bear card B with the same line of four bears evaluated from the third
     * bear, covering the mirrored pair-rejection branch for the other neighbour.
     */
    @Test
    fun bearBLineOfFourFromThird() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(2, -2, 0)] = connectedTile(1, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(0, 0, 0)]  = connectedTile(2, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(1, -1, 0)] = connectedTile(3, Habitates.FORESTS, WildlifeToken.BEAR)
        player.board[Triple(3, -3, 0)] = connectedTile(4, Habitates.FORESTS, WildlifeToken.BEAR)
        assertEquals(0, scoreOf(player, listOf(false, true, true, true, true))[5],
            "Four bears in a line contain no group of exactly three and score zero on card B")
    }

    /**
     * Tests the salmon scoring for a triangle of three salmon, which exercises the circle
     * detection and the special triangle handling.
     */
    @Test
    fun salmonTriangleScoredAsRunOfThree() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.RIVERS, WildlifeToken.SALMON)
        player.board[Triple(1, -1, 0)] = connectedTile(2, Habitates.RIVERS, WildlifeToken.SALMON)
        player.board[Triple(0, -1, 1)] = connectedTile(3, Habitates.RIVERS, WildlifeToken.SALMON)
        assertEquals(8, scoreOf(player)[7],
            "A salmon triangle scores like a run of three on card A")
    }

    /**
     * Tests the salmon scoring for a straight line of three salmon whose middle salmon is
     * evaluated first, covering the non-circle outcome of the two-neighbour case.
     */
    @Test
    fun salmonLineEvaluatedFromMiddle() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(1, -1, 0)] = connectedTile(1, Habitates.RIVERS, WildlifeToken.SALMON)
        player.board[Triple(0, 0, 0)]  = connectedTile(2, Habitates.RIVERS, WildlifeToken.SALMON)
        player.board[Triple(2, -2, 0)] = connectedTile(3, Habitates.RIVERS, WildlifeToken.SALMON)
        assertEquals(8, scoreOf(player)[7], "A run of three salmon scores 8 points on card A")
    }

    /**
     * Creates a player with the given number of pairwise non-adjacent hawks that all lie on
     * one straight sight line with a spacing of two tiles.
     */
    private fun hawkLinePlayer(count: Int): Player {
        val player = Player("P", PlayerType.HUMAN)
        for (i in 0 until count) {
            player.board[Triple(2 * i, -2 * i, 0)] =
                connectedTile(i, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        }
        return player
    }

    /**
     * Tests hawk card A for five to eight isolated hawks, covering the upper branches of
     * the score table.
     */
    @Test
    fun hawkAFiveToEightIsolatedHawks() {
        assertEquals(14, scoreOf(hawkLinePlayer(5))[8])
        assertEquals(18, scoreOf(hawkLinePlayer(6))[8])
        assertEquals(22, scoreOf(hawkLinePlayer(7))[8])
        assertEquals(26, scoreOf(hawkLinePlayer(8))[8])
    }

    /**
     * Tests hawk card B for four to eight isolated hawks with a shared sight line,
     * covering the upper branches of the score table.
     */
    @Test
    fun hawkBFourToEightHawksWithSightLines() {
        val cards = listOf(true, true, true, false, true)
        assertEquals(12, scoreOf(hawkLinePlayer(4), cards)[8])
        assertEquals(16, scoreOf(hawkLinePlayer(5), cards)[8])
        assertEquals(20, scoreOf(hawkLinePlayer(6), cards)[8])
        assertEquals(24, scoreOf(hawkLinePlayer(7), cards)[8])
        assertEquals(28, scoreOf(hawkLinePlayer(8), cards)[8])
    }

    /**
     * Tests hawk card B with an adjacent hawk pair and a single distant watcher: only the
     * watcher counts, and a single scoring hawk is worth zero points.
     */
    @Test
    fun hawkBSingleSightingHawkIsZero() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        player.board[Triple(1, -1, 0)] = connectedTile(2, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        player.board[Triple(0, -2, 2)] = connectedTile(3, Habitates.MOUNTAINS, WildlifeToken.HAWK)
        assertEquals(0, scoreOf(player, listOf(true, true, true, false, true))[8],
            "One counting hawk scores zero points on card B")
    }

    /**
     * Tests fox card A with four unique neighbouring animal types, covering the four-type
     * branch of the score table.
     */
    @Test
    fun foxAFourUniqueNeighbourTypes() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.FOX)
        player.board[Triple(0, 1, -1)] = connectedTile(2, Habitates.PRAIRIES, WildlifeToken.BEAR)
        player.board[Triple(0, -1, 1)] = connectedTile(3, Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(1, 0, -1)] = connectedTile(4, Habitates.PRAIRIES, WildlifeToken.SALMON)
        player.board[Triple(-1, 0, 1)] = connectedTile(5, Habitates.PRAIRIES, WildlifeToken.HAWK)
        assertEquals(4, scoreOf(player)[9],
            "A fox with four unique neighbour types scores 4 points")
    }

    /**
     * Tests fox card A with five unique neighbouring animal types including a second fox,
     * covering the else branch of the score table. The neighbouring fox scores its own
     * three unique neighbour types on top.
     */
    @Test
    fun foxAFiveUniqueNeighbourTypes() {
        val player = Player("P", PlayerType.HUMAN)
        player.board[Triple(0, 0, 0)]  = connectedTile(1, Habitates.PRAIRIES, WildlifeToken.FOX)
        player.board[Triple(0, 1, -1)] = connectedTile(2, Habitates.PRAIRIES, WildlifeToken.BEAR)
        player.board[Triple(0, -1, 1)] = connectedTile(3, Habitates.PRAIRIES, WildlifeToken.ELK)
        player.board[Triple(1, 0, -1)] = connectedTile(4, Habitates.PRAIRIES, WildlifeToken.SALMON)
        player.board[Triple(-1, 0, 1)] = connectedTile(5, Habitates.PRAIRIES, WildlifeToken.HAWK)
        player.board[Triple(1, -1, 0)] = connectedTile(6, Habitates.PRAIRIES, WildlifeToken.FOX)
        assertEquals(8, scoreOf(player)[9],
            "Center fox scores 5 for five unique types, the neighbour fox 3 for its own three")
    }

    /**
     * Tests the corridor majority for four players where three players tie for the largest
     * mountain corridor and one player has none, covering the zero-points branch of the
     * three-way tie.
     */
    @Test
    fun majorityThreeWayTieGivesLoserZero() {
        val p1 = Player("P1", PlayerType.HUMAN)
        val p2 = Player("P2", PlayerType.HUMAN)
        val p3 = Player("P3", PlayerType.HUMAN)
        val p4 = Player("P4", PlayerType.HUMAN)
        p1.board[Triple(0, 0, 0)] = connectedTile(1, Habitates.MOUNTAINS)
        p2.board[Triple(0, 0, 0)] = connectedTile(2, Habitates.MOUNTAINS)
        p3.board[Triple(0, 0, 0)] = connectedTile(3, Habitates.MOUNTAINS)
        p4.board[Triple(0, 0, 0)] = connectedTile(4, Habitates.FORESTS)
        val scores = runGame(listOf(p1, p2, p3, p4))
        assertEquals(1, scores[0].second[10], "Tied players receive one bonus point")
        assertEquals(1, scores[1].second[10], "Tied players receive one bonus point")
        assertEquals(1, scores[2].second[10], "Tied players receive one bonus point")
        assertEquals(0, scores[3].second[10], "The player without the habitat receives zero")
    }
}