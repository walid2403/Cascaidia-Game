package service

import entity.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GameServiceTest {
    private lateinit var rootService: RootService
    @BeforeTest
    fun setUp() {
        rootService = RootService()
    }

    @Test
    fun calculateScoresWithoutCurrentGameThrowsException() {
        assertFailsWith<IllegalStateException> {
            rootService.gameService.calculateScores()
        }
    }
    @Test
    fun calculateScoresNumTilesWrong() {
        val game = CascadiaGame(
            scoringCards = listOf(true, true, true, true, true),
            isLocal = true
        )

        val player1 = Player("Alice", PlayerType.HUMAN)
        val player2 = Player("Bob", PlayerType.HUMAN)

        game.playerQueue.add(player1)
        game.playerQueue.add(player2)

        rootService.currentGame = game

        assertFailsWith<IllegalStateException> {
            rootService.gameService.calculateScores()
        }
    }

    /**
     * Tests that calculateScores throws an IllegalStateException
     *  when not every player has exactly 20 habitat tiles.
     */
    @Test
    fun calculateScoresWithMissingTiles() {
        val game = CascadiaGame(
            scoringCards = listOf(true, true, true, true, true),
            isLocal = true
        )
        val player1 = Player("X", PlayerType.HUMAN)
        val player2 = Player("Y", PlayerType.HUMAN)
        repeat(20) { i ->
            player1.board[Triple(i, 0, 0)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }
        repeat(19) { i ->
            player2.board[Triple(i, 1, 0)] = Tile(
                id = i + 100,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }
        game.playerQueue.add(player1)
        game.playerQueue.add(player2)
        rootService.currentGame = game
        assertFailsWith<IllegalStateException> { rootService.gameService.calculateScores() }
    }
    /**
     * Tests that calculateScores throws an IllegalStateException
     *  when a player has more than 20 habitat tiles.
     */
    @Test
    fun calculateScoresWithManyTiles() {
        val game = CascadiaGame(
            scoringCards = listOf(true, true, true, true, true),
            isLocal = true
        )
        val player1 = Player("X", PlayerType.HUMAN)
        val player2 = Player("Y", PlayerType.HUMAN)
        repeat(20) { i ->
            player1.board[Triple(i, 0, 0)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }
        repeat(21) { i ->
            player2.board[Triple(i, 1, 0)] = Tile(
                id = i + 100,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }
        game.playerQueue.add(player1)
        game.playerQueue.add(player2)
        rootService.currentGame = game
        assertFailsWith<IllegalStateException> { rootService.gameService.calculateScores() }
    }
    /**
     * Tests that calculateScores correctly computes all
     * wildlife scores for scoring card A.
     *
     * Expected:
     * Bear   = 4
     * Elk    = 2
     * Salmon = 8
     * Hawk   = 5
     * Fox    = 4
     * Nature = 3
     * Total  = 26
     */
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
        val scores = rootService.gameService.calculateScores()

        assertEquals(4, scores[player]!!.bearScore)
        assertEquals(2, scores[player]!!.elkScore)
        assertEquals(8, scores[player]!!.salmonScore)
        assertEquals(5, scores[player]!!.hawkScore)
        assertEquals(4, scores[player]!!.foxScore)
        assertEquals(3, scores[player]!!.natureTokenScore)
        assertEquals(26, scores[player]!!.wildlifeScore)
    }
    /**
     * Tests that calculateScores correctly determines the
     * largest habitat corridor for each habitat type.
     *
     * Expected:
     * Forest  = 4
     * River   = 3
     * Mountain= 2
     * Prairie = 1
     * Wetland = 1
     *
     * Habitat total = 11
     */
    @Test
    fun calculateScoresHabitatCorridors() {
        val game = CascadiaGame(
            scoringCards = listOf(true, true, true, true, true),
            isLocal = true
        )
        val player = Player("X", PlayerType.HUMAN)

        // forest corridor of size 4
        player.board[Triple(0, 0, 0)] = Tile(
            id = 1,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        )

        player.board[Triple(1, -1, 0)] = Tile(
            id = 2,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        )

        player.board[Triple(2, -1, -1)] = Tile(
            id = 3,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        )

        player.board[Triple(3, -1, -2)] = Tile(
            id = 4,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        )

        // Separate forest corridor of size 2
        player.board[Triple(0, -2, 2)] = Tile(
            id = 5,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        )

        player.board[Triple(0, -3, 3)] = Tile(
            id = 6,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        )

        // River corridor of size 3
        player.board[Triple(0, 1, -1)] = Tile(
            id = 7,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        )

        player.board[Triple(0, 2, -2)] = Tile(
            id = 8,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        )

        player.board[Triple(0, 3, -3)] = Tile(
            id = 9,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        )

        // Mountain corridor of size 2 ----------
        player.board[Triple(0, -1, 1)] = Tile(
            id = 10,
            habs = mutableListOf(Habitates.MOUNTAINS),
            possibles = listOf(WildlifeToken.HAWK)
        )

        player.board[Triple(1, -2, 1)] = Tile(
            id = 11,
            habs = mutableListOf(Habitates.MOUNTAINS),
            possibles = listOf(WildlifeToken.HAWK)
        )

        //Prairie corridor of size 1
        player.board[Triple(-1, 1, 0)] = Tile(
            id = 12,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.ELK)
        )

        //Wetland corridor of size 1
        player.board[Triple(-2, 2, 0)] = Tile(
            id = 13,
            habs = mutableListOf(Habitates.WETLANDS),
            possibles = listOf(WildlifeToken.FOX)
        )

        //Fill remaining tiles to reach 20
        player.board[Triple(-2, 3, -1)] = Tile(
            id = 30,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        )
        player.board[Triple(-3, 3, 0)] = Tile(
            id = 31,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.FOX)
        )
        player.board[Triple(-3, 2, 1)] = Tile(
            id = 32,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        )
        player.board[Triple(-3, 1, 2)] = Tile(
            id = 33,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.FOX)
        )
        player.board[Triple(-3, 0, 3)] = Tile(
            id = 34,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        )
        player.board[Triple(-2, -1, -3)] = Tile(
            id = 30,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.FOX)
        )
        player.board[Triple(-2, 0, 2)] = Tile(
            id = 30,
            habs = mutableListOf(Habitates.WETLANDS),
            possibles = listOf(WildlifeToken.SALMON)
        )

        game.playerQueue.add(player)

        rootService.currentGame = game

        val scores = rootService.gameService.calculateScores()

        assertEquals(4, scores[player]!!.forestScore)
        assertEquals(3, scores[player]!!.riverScore)
        assertEquals(2, scores[player]!!.mountainScore)
        assertEquals(1, scores[player]!!.prairieScore)
        assertEquals(1, scores[player]!!.wetlandScore)
        assertEquals(11, scores[player]!!.habitatScore)
    }
    /**
     * Tests that calculateScores correctly awards
     * habitat corridor majority bonus points in a
     * two-player game.
     *
     * Expected:
     * Player A forest corridor = 7
     * Player B forest corridor = 5
     *
     * Player A receives the forest majority bonus.
     * Player B receives no forest majority bonus.
     */
    @Test
    fun calculateScoresHabitatMajorityWinner() {
        val game = CascadiaGame(
            scoringCards = listOf(false, true, false, true, true),
            isLocal = true
        )
        val playerA = Player("X", PlayerType.HUMAN)
        val playerB = Player("Y", PlayerType.HUMAN)

        // Player A : forest corridor size 7
        repeat(7) { i ->
            playerA.board[Triple(-3+i, 3-i, 0)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }
        // Fill remaining tiles
        repeat(13) { i ->
            playerA.board[Triple(-2 + i,3-i, -1)] = Tile(
                id = 100 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        //  Player B : forest corridor size 5
        repeat(5) { i ->
            playerB.board[Triple(-3+i, 2-i, 1)] = Tile(
                id = 200 + i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }

        // Fill remaining tiles
        repeat(15) { i ->
            playerB.board[Triple(2, -3+i, 1-i)] = Tile(
                id = 300 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        game.playerQueue.add(playerA)
        game.playerQueue.add(playerB)

        rootService.currentGame = game

        val scores = rootService.gameService.calculateScores()
        assertEquals(2, scores[playerA]!!.forestMajorityBonus)
        assertEquals(0, scores[playerB]!!.forestMajorityBonus)
    }
    /**
     * Tests that calculateScores correctly handles
     * a tie for the largest habitat corridor in a
     * two-player game.
     *
     * Expected:
     * Player A forest corridor = 7
     * Player B forest corridor = 7
     *
     * Both players receive the forest majority bonus.
     */
    @Test
    fun calculateScoresHabitatMajorityTie(){
        val game = CascadiaGame(
            scoringCards = listOf(false, true, false, true, true),
            isLocal = true
        )
        val playerA = Player("X", PlayerType.HUMAN)
        val playerB = Player("Y", PlayerType.HUMAN)

        // Player A : forest corridor size 7
        repeat(7) { i ->
            playerA.board[Triple(-3+i, 3-i, 0)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }
        // Fill remaining tiles
        repeat(13) { i ->
            playerA.board[Triple(-2 + i,3-i, -1)] = Tile(
                id = 100 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }
        // Player B : forest corridor size 7
        repeat(7) { i ->
            playerB.board[Triple(-3+i, 3-i, 0)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }
        // Fill remaining tiles
        repeat(13) { i ->
            playerB.board[Triple(-2 + i,3-i, -1)] = Tile(
                id = 100 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }
        game.playerQueue.add(playerA)
        game.playerQueue.add(playerB)

        rootService.currentGame = game

        val scores = rootService.gameService.calculateScores()
        assertEquals(1, scores[playerA]!!.forestMajorityBonus)
        assertEquals(1, scores[playerB]!!.forestMajorityBonus)

    }
    /**
     * Tests that calculateScores correctly converts
     * unused nature tokens into points.
     *
     * Expected:
     * 5 nature tokens = 5 points.
     */
    @Test
    fun calculateScoresNatureTokens() {
        val game = CascadiaGame(
            scoringCards = listOf(true, true, true, true, true),
            isLocal = true
        )

        val player = Player("C", PlayerType.HUMAN)

        player.natureTokens = 5

        // Fill board with exactly 20 tiles
        repeat(20) { i ->
            player.board[Triple(-3+i, 0,3-i)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }

        game.playerQueue.add(player)
        rootService.currentGame = game
        val scores = rootService.gameService.calculateScores()
        assertEquals(5, scores[player]!!.natureTokenScore)
    }
    /**
     * Tests that calculateScores correctly computes all
     * wildlife scores for scoring card B.
     *
     * Expected:
     * Bear   = 10
     * Elk    = 2
     * Salmon = 9
     * Hawk   = 5
     * Fox    = 5
     * Nature = 2
     * Total  = 33
     */
    @Test
    fun calculateScoresAllCardsB() {
        val game = CascadiaGame(
            scoringCards = listOf(false, false, false, false, false),
            isLocal = true
        )
        val player = Player("X", PlayerType.HUMAN)
        player.natureTokens = 2

        //Fox in center
        player.board[Triple(0, 0, 0)] = Tile(
            id = 1,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.FOX)
        ).apply {
            occupant = WildlifeToken.FOX
        }

        // Bear pair around fox
        player.board[Triple(0, -1, 1)] = Tile(
            id = 2,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        ).apply {
            occupant = WildlifeToken.BEAR
        }

        player.board[Triple(-1, 0, 1)] = Tile(
            id = 3,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        ).apply {
            occupant = WildlifeToken.BEAR
        }

        // Third bear for Bear-B group of exactly 3
        player.board[Triple(1, -2, 1)] = Tile(
            id = 4,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        ).apply {
            occupant = WildlifeToken.BEAR
        }

        // Hawks
        player.board[Triple(-1, 1, 0)] = Tile(
            id = 5,
            habs = mutableListOf(Habitates.MOUNTAINS),
            possibles = listOf(WildlifeToken.HAWK)
        ).apply {
            occupant = WildlifeToken.HAWK
        }

        player.board[Triple(1, -1, 0)] = Tile(
            id = 6,
            habs = mutableListOf(Habitates.MOUNTAINS),
            possibles = listOf(WildlifeToken.HAWK)
        ).apply {
            occupant = WildlifeToken.HAWK
        }

        // Single Elk
        player.board[Triple(0, 1, -1)] = Tile(
            id = 7,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.ELK)
        ).apply {
            occupant = WildlifeToken.ELK
        }

        // Salmon run of 3
        player.board[Triple(2, -2, 0)] = Tile(
            id = 8,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        ).apply {
            occupant = WildlifeToken.SALMON
        }

        player.board[Triple(3, -3, 0)] = Tile(
            id = 9,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        ).apply {
            occupant = WildlifeToken.SALMON
        }

        player.board[Triple(2, -1, -1)] = Tile(
            id = 10,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        ).apply {
            occupant = WildlifeToken.SALMON
        }

        //Fill remaining tiles to reach 20
        repeat(10) { i ->
            player.board[Triple(-2, -1+i, 3-i)] = Tile(
                id = 100 + i,
                habs = mutableListOf(Habitates.WETLANDS),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        game.playerQueue.add(player)

        rootService.currentGame = game

        val scores = rootService.gameService.calculateScores()

        assertEquals(10, scores[player]!!.bearScore)
        assertEquals(2, scores[player]!!.elkScore)
        assertEquals(9, scores[player]!!.salmonScore)
        assertEquals(5, scores[player]!!.hawkScore)
        assertEquals(5, scores[player]!!.foxScore)
        assertEquals(2, scores[player]!!.natureTokenScore)
        assertEquals(33, scores[player]!!.wildlifeScore)
    }
    /**
     * Tests that calculateScores correctly awards
     * the solo habitat bonus for a habitat corridor
     * of size seven or greater.
     */
    @Test
    fun calculateScoresSoloHabitatBonus() {
        val game = CascadiaGame(
            scoringCards = listOf(true, true, true, true, true),
            isLocal = true
        )

        val player = Player("H", PlayerType.HUMAN)

        // Forest corridor of size 7
        repeat(7) { i ->
            player.board[Triple(-3+i, 3-i, 0)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }

        // Remaining 13 tiles
        repeat(13) { i ->
            player.board[Triple(-2 + i, 3-i, -1)] = Tile(
                id = 100 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        game.playerQueue.add(player)

        rootService.currentGame = game

        val scores = rootService.gameService.calculateScores()

        assertEquals(7, scores[player]!!.forestHabitatScore)
        assertEquals(2, scores[player]!!.forestBonus)
    }
    /**
     * Tests that calculateScores correctly awards
     * habitat majority bonuses in a three-player game.
     */
    @Test
    fun calculateScoresThreePlayerHabitatMajority() {
        val game = CascadiaGame(
            scoringCards = listOf(true, true, true, true, true),
            isLocal = true
        )

        val a = Player("A", PlayerType.HUMAN)
        val b = Player("B", PlayerType.HUMAN)
        val c = Player("C", PlayerType.HUMAN)

        // A: forest corridor size 8
        repeat(8) { i ->
            a.board[Triple(-3+i, 3-i, 0)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }

        // B: forest corridor size 6
        repeat(6) { i ->
            b.board[Triple(-3+i, 3-i, 0)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }

        // C: forest corridor size 4
        repeat(4) { i ->
            c.board[Triple(-3+i, 3-i, 0)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }

        // Fill each board to 20 tiles
        repeat(12) { i ->
            a.board[Triple(-2 + i, 3-i, -1)] = Tile(
                id = 100 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }
        repeat(14) { i ->
            b.board[Triple(-2 + i, 3-i, -1)] = Tile(
                id = 10 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }
        repeat(16) { i ->
            c.board[Triple(-2 + i, 3-i, -1)] = Tile(
                id = 1000 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        game.playerQueue.add(a)
        game.playerQueue.add(b)
        game.playerQueue.add(c)

        rootService.currentGame = game

        val scores = rootService.gameService.calculateScores()

        assertEquals(3, scores[a]!!.forestBonus)
        assertEquals(1, scores[b]!!.forestBonus)
        assertEquals(0, scores[c]!!.forestBonus)
    }
    /**
     * Tests that calculateScores correctly handles
     * a tie for the largest habitat corridor in a
     * three-player game.
     *
     * Expected:
     * A forest corridor = 8 -> bonus 2
     * B forest corridor = 8 -> bonus 2
     * C forest corridor = 6 -> bonus 0
     */
    @Test
    fun calculateScoresThreePlayerHabitatMajorityTie() {
        val game = CascadiaGame(
            scoringCards = listOf(true, true, true, true, true),
            isLocal = true
        )

        val a = Player("A", PlayerType.HUMAN)
        val b = Player("B", PlayerType.HUMAN)
        val c = Player("C", PlayerType.HUMAN)

        // A: forest corridor size 8
        repeat(8) { i ->
            a.board[Triple(-3 + i, 3 - i, 0)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }

        repeat(12) { i ->
            a.board[Triple(-2 + i, 3 - i, -1)] = Tile(
                id = 100 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        // B: forest corridor size 8
        repeat(8) { i ->
            b.board[Triple(-3 + i, 3 - i, 0)] = Tile(
                id = 200 + i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }

        repeat(12) { i ->
            b.board[Triple(-2+i, 3- i, -1)] = Tile(
                id = 300 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        // C: forest corridor size 6
        repeat(6) { i ->
            c.board[Triple(-3 + i, 3 - i, 0)] = Tile(
                id = 400 + i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }

        repeat(14) { i ->
            c.board[Triple(-2+i,3- i, -1)] = Tile(
                id = 500 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        game.playerQueue.add(a)
        game.playerQueue.add(b)
        game.playerQueue.add(c)

        rootService.currentGame = game

        val scores = rootService.gameService.calculateScores()

        assertEquals(2, scores[a]!!.forestMajorityBonus)
        assertEquals(2, scores[b]!!.forestMajorityBonus)
        assertEquals(0, scores[c]!!.forestMajorityBonus)
    }
    /**
     * Tests that calculateScores correctly handles
     * a three-way tie for the largest habitat corridor
     * in a three-player game.
     *
     * Expected:
     * A forest corridor = 8 -> bonus 1
     * B forest corridor = 8 -> bonus 1
     * C forest corridor = 8 -> bonus 1
     */
    @Test
    fun calculateScoresThreePlayerHabitatMajorityThreeWayTie() {
        val game = CascadiaGame(
            scoringCards = listOf(true, true, true, true, true),
            isLocal = true
        )

        val playerA = Player("A", PlayerType.HUMAN)
        val playerB = Player("B", PlayerType.HUMAN)
        val playerC = Player("C", PlayerType.HUMAN)

        // Player A: forest corridor size 8
        repeat(8) { i ->
            playerA.board[Triple(-3 + i, 3 - i, 0)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }

        repeat(12) { i ->
            playerA.board[Triple(-2 + i, 3 - i, -1)] = Tile(
                id = 100 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        // Player B: forest corridor size 8
        repeat(8) { i ->
            playerB.board[Triple(-3 + i, 3 - i, 0)] = Tile(
                id = 200 + i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }

        repeat(12) { i ->
            playerB.board[Triple(-2+i, 3-i, -1)] = Tile(
                id = 300 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        // Player C: forest corridor size 8
        repeat(8) { i ->
            playerC.board[Triple(-3 + i, 3 - i, 0)] = Tile(
                id = 400 + i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }

        repeat(12) { i ->
            playerC.board[Triple(-2+i, 3-i, -1)] = Tile(
                id = 500 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        game.playerQueue.add(playerA)
        game.playerQueue.add(playerB)
        game.playerQueue.add(playerC)

        rootService.currentGame = game

        val scores = rootService.gameService.calculateScores()

        assertEquals(1, scores[playerA]!!.forestMajorityBonus)
        assertEquals(1, scores[playerB]!!.forestMajorityBonus)
        assertEquals(1, scores[playerC]!!.forestMajorityBonus)
    }
    /**
     * Tests that calculateScores awards no points
     * for an invalid Bear B configuration.
     *
     * Expected:
     * Two adjacent bears score 0 points.
     */
    @Test
    fun calculateScoresInvalidBearGroupB() {
        val game = CascadiaGame(
            scoringCards = listOf(false, false, false, false, false),
            isLocal = true
        )
        val player = Player("A", PlayerType.HUMAN)
        // Only two adjacent bears
        player.board[Triple(0, 0, 0)] = Tile(
            id = 1,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        ).apply {
            occupant = WildlifeToken.BEAR
        }

        player.board[Triple(1, -1, 0)] = Tile(
            id = 2,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        ).apply {
            occupant = WildlifeToken.BEAR
        }

        // Fill remaining tiles to reach 20
        repeat(18) { i ->
            player.board[Triple(-2 + i, 3-i, -1)] = Tile(
                id = 100 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        game.playerQueue.add(player)

        rootService.currentGame = game

        val scores = rootService.gameService.calculateScores()

        assertEquals(0, scores[player]!!.bearScore)
    }
    /**
     * Tests that calculateScores does not award points
     * for invalid wildlife configurations.
     *
     * Expected:
     * Bear A   = 0
     * Elk B    = 0
     * Salmon A = 0
     * Hawk A   = 0
     */
    @Test
    fun calculateScoresInvalidWildlifeConfigurations() {
        val game = CascadiaGame(
            scoringCards = listOf(true, false, true, true, true),
            isLocal = true
        )

        val player = Player("A", PlayerType.HUMAN)
        // Invalid bear configuration:
        // Two bear pairs touching each other
        player.board[Triple(0, 0, 0)] = Tile(
            id = 1,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        ).apply { occupant = WildlifeToken.BEAR }

        player.board[Triple(1, -1, 0)] = Tile(
            id = 2,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        ).apply { occupant = WildlifeToken.BEAR }

        player.board[Triple(2, -2, 0)] = Tile(
            id = 3,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        ).apply { occupant = WildlifeToken.BEAR }

        player.board[Triple(3, -3, 0)] = Tile(
            id = 4,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        ).apply { occupant = WildlifeToken.BEAR }

        // Invalid elk configuration (Card B):
        // Three elk not matching any valid B shape

        player.board[Triple(0, 2, -2)] = Tile(
            id = 5,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.ELK)
        ).apply { occupant = WildlifeToken.ELK }

        player.board[Triple(1, 1, -2)] = Tile(
            id = 6,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.ELK)
        ).apply { occupant = WildlifeToken.ELK }

        player.board[Triple(2, 0, -2)] = Tile(
            id = 7,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.ELK)
        ).apply { occupant = WildlifeToken.ELK }


        // Invalid salmon configuration:
        // One salmon adjacent to three other salmon

        player.board[Triple(-2, 0, 2)] = Tile(
            id = 8,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        ).apply { occupant = WildlifeToken.SALMON }

        player.board[Triple(-1, 0, 1)] = Tile(
            id = 9,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        ).apply { occupant = WildlifeToken.SALMON }

        player.board[Triple(-3, 0, 3)] = Tile(
            id = 10,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        ).apply { occupant = WildlifeToken.SALMON }

        player.board[Triple(-2, -1, 3)] = Tile(
            id = 11,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        ).apply { occupant = WildlifeToken.SALMON }

        // Invalid hawk configuration:
        // Two adjacent hawks
        player.board[Triple(0, -2, 2)] = Tile(
            id = 12,
            habs = mutableListOf(Habitates.MOUNTAINS),
            possibles = listOf(WildlifeToken.HAWK)
        ).apply { occupant = WildlifeToken.HAWK }

        player.board[Triple(1, -3, 2)] = Tile(
            id = 13,
            habs = mutableListOf(Habitates.MOUNTAINS),
            possibles = listOf(WildlifeToken.HAWK)
        ).apply { occupant = WildlifeToken.HAWK }
        // Fill remaining tiles to reach 20 tiles

        repeat(7) { i ->
            player.board[Triple(10 + i, 0, 0)] = Tile(
                id = 100 + i,
                habs = mutableListOf(Habitates.WETLANDS),
                possibles = listOf(WildlifeToken.FOX)
            )
        }

        game.playerQueue.add(player)

        rootService.currentGame = game

        val scores = rootService.gameService.calculateScores()

        assertEquals(0, scores[player]!!.bearScore)
        assertEquals(0, scores[player]!!.elkScore)
        assertEquals(0, scores[player]!!.salmonScore)
        assertEquals(0, scores[player]!!.hawkScore)
    }
    /**
     * Tests that calculateScores only counts the
     * largest habitat corridor of a habitat type.
     * and give 0 for a missing habitat tile
     *
     * Expected:
     * Forest corridor 1 = 6
     * Forest corridor 2 = 4
     * Forest score = 6
     * Wetland score= 0
     */
    @Test
    fun calculateScoresLargestHabitatCorridorOnly() {
        val game = CascadiaGame(
            scoringCards = listOf(true, true, true, true, true),
            isLocal = true
        )

        val player = Player("A", PlayerType.HUMAN)
        // Forest corridor #1 (size 6)
        repeat(6) { i ->
            player.board[Triple(i, -i, 0)] = Tile(
                id = i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }
        // Forest corridor #2 (size 4)
        repeat(4) { i ->
            player.board[Triple(-1, i,1+i)] = Tile(
                id = 100 + i,
                habs = mutableListOf(Habitates.FORESTS),
                possibles = listOf(WildlifeToken.BEAR)
            )
        }
        // Fill remaining tiles to reach 20
        repeat(10) { i ->
            player.board[Triple(0, -i, i)] = Tile(
                id = 200 + i,
                habs = mutableListOf(Habitates.PRAIRIES),
                possibles = listOf(WildlifeToken.ELK)
            )
        }

        game.playerQueue.add(player)

        rootService.currentGame = game

        val scores = rootService.gameService.calculateScores()

        assertEquals(6, scores[player]!!.forestScore)
        assertEquals(0, scores[player]!!.wetlandScore)
    }
    /**
     * Tests that calculateScores correctly computes
     * the final total score from all score categories.
     *
     * Expected:
     * Wildlife = 26
     * Habitat  = 11
     * Nature   = 3
     * Total    = 40
     */
    @Test
    fun calculateScoresTotalScore() {
        val game = CascadiaGame(
            scoringCards = listOf(true, true, true, true, true),
            isLocal = true
        )
        val player = Player("A", PlayerType.HUMAN)
        player.natureTokens = 3

        //  Fox
        player.board[Triple(0, 0, 0)] = Tile(
            id = 1,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.FOX)
        ).apply {
            occupant = WildlifeToken.FOX
        }

        //  Bear pair
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

        //Elk
        player.board[Triple(0, 1, -1)] = Tile(
            id = 4,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.ELK)
        ).apply {
            occupant = WildlifeToken.ELK
        }

        //Salmon run of 3
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

        // Hawks
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

        //Habitat corridors
        player.board[Triple(3, -1, -2)] = Tile(
            id = 10,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        )

        player.board[Triple(4, -1, -3)] = Tile(
            id = 11,
            habs = mutableListOf(Habitates.FORESTS),
            possibles = listOf(WildlifeToken.BEAR)
        )

        player.board[Triple(0, 2, -2)] = Tile(
            id = 12,
            habs = mutableListOf(Habitates.RIVERS),
            possibles = listOf(WildlifeToken.SALMON)
        )

        player.board[Triple(0, -2, 2)] = Tile(
            id = 13,
            habs = mutableListOf(Habitates.MOUNTAINS),
            possibles = listOf(WildlifeToken.HAWK)
        )

        player.board[Triple(-2, 2, 0)] = Tile(
            id = 14,
            habs = mutableListOf(Habitates.WETLANDS),
            possibles = listOf(WildlifeToken.FOX)
        )

        player.board[Triple(-3, 2, 1)] = Tile(
            id = 15,
            habs = mutableListOf(Habitates.PRAIRIES),
            possibles = listOf(WildlifeToken.ELK)
        )

        // Fill remaining tiles to 20
        repeat(5) { i ->
            player.board[Triple(10 + i, 0, 0)] = Tile(
                id = 100 + i,
                habs = mutableListOf(Habitates.WETLANDS),
                possibles = listOf(WildlifeToken.FOX)
            )
        }

        game.playerQueue.add(player)

        rootService.currentGame = game

        val scores = rootService.gameService.calculateScores()

        assertEquals(26, scores[player]!!.wildlifeScore)
        assertEquals(11, scores[player]!!.habitatScore)
        assertEquals(3, scores[player]!!.natureTokenScore)
        assertEquals(40, scores[player]!!.totalScore)
    }


}