package service.bot

import entity.PlayerType
import service.Refreshable
import service.RootService
import java.util.Scanner
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.random.Random

class BotComparison {
    private val participants = listOf(
        PlayerType.HEURISTIC_BOT,
        PlayerType.GREEDY_HEURISTIC_BOT,
        PlayerType.GREEDY_BOT,
        //PlayerType.MONTE_BOT,
        PlayerType.EASY_BOT,
        PlayerType.GREEDY_HEURISTIC_BOT,
    )

    private val addOns = MutableList(PlayerType.entries.size) { emptyList<List<Int>>() }

    private var firstWins = AtomicInteger(0)
    private var secondWins = AtomicInteger(0)
    private var draws = AtomicInteger(0)
    private var firstPoints = AtomicInteger(0)
    private var secondPoints = AtomicInteger(0)
    private var firstTime = AtomicLong(0)
    private var secondTime = AtomicLong(0)

    private val averages = MutableList(participants.size) { FloatArray(3) }

    private fun getBotName(index: Int): String {
        return when(index) {
            0 -> "Heuristic Bot"
            1 -> "Greedy Heuristic Bot"
            2 -> "Greedy Bot"
            3 -> "Monte Bot"
            4 -> "Random Bot"
            else -> "Unknown"
        }
    }

    fun ghBotTesten() {
        val threads = mutableListOf<Thread>()
        for (i in 0..499) {
            threads.add(thread(start = true) { playGame(1,4) })
        }
        for (t in threads) {
            t.join()
        }
        val avgPointsOld = firstPoints.get()/500f
        val avgPointsNew = secondPoints.get()/500f
        val winrate = (firstWins.get() / (draws.get() + firstWins.get() + secondWins.get().toFloat()))
        val avgTimeFirst = firstTime.get()/1000000f
        val avgTimeSecond = secondTime.get()/1000000f

        println("Old Bot got $avgPointsOld Points on Avg against new Bot, who got $avgPointsNew.\n" +
                "The winrate is $winrate. There where ${draws.get()} Draws\n" +
                "Time Old: $avgTimeFirst and Time New: $avgTimeSecond\n")

        firstWins.set(0)
        secondWins.set(0)
        firstPoints.set(0)
        secondPoints.set(0)
        draws.set(0)
        firstTime.set(0)
        secondTime.set(0)
    }

    /*private fun playGHTestGame() {
        var firstTimeL = 0L
        var secondTimeL = 0L
        val rootService = RootService()
        val scoringCards = List(5) { Random.nextBoolean() }
        val random = Random.nextBoolean()
        if (random) {
            rootService.gameService.startNewGame(
                listOf(
                    Pair("first", PlayerType.GREEDY_HEURISTIC_BOT),
                    Pair("second", PlayerType.HARD_BOT)
                ),
                scoringCards
            )
        } else {
            rootService.gameService.startNewGame(
                listOf(
                    Pair("second", PlayerType.HARD_BOT),
                    Pair("first", PlayerType.GREEDY_HEURISTIC_BOT)
                ),
                scoringCards
            )
        }

        var isFinished = false
        var result: List<Pair<String,List<Int>>> = emptyList()
        val refreshable = object : Refreshable {
            override fun refreshAfterEndGame(scores: List<Pair<String, List<Int>>>) {
                result = scores
                isFinished = true
            }
        }
        rootService.addRefreshable(refreshable)

        while (!isFinished) {
            rootService.bot.makeTurn(rootService.currentGame!!.playerQueue.peek().type)
            rootService.gameService.changeTurn()
        }

        val firstScore = result.single { it.first == "first" }.second.sum()
        val secondScore = result.single { it.first == "second" }.second.sum()

        firstPoints.getAndAdd(firstScore)
        secondPoints.getAndAdd(secondScore)

        if (firstScore > secondScore) {
            firstWins.getAndIncrement()
        } else if (firstScore < secondScore) {
            secondWins.getAndIncrement()
        } else {
            draws.getAndIncrement()
        }
    }*/

    fun testing() {
        val threads = mutableListOf<Thread>()
        for (i in participants.indices) {
            for (j in participants.indices) {
                repeat(20) {
                    repeat(5) {
                        threads.add(thread(start = true) { playGame(i, j) })
                    }
                    for (t in threads) {
                        t.join()
                    }
                    threads.clear()
                }
                val firstBot = getBotName(i)
                val secondBot = getBotName(j)
                val avgPointsFirst = firstPoints.get()/100f
                val avgPointsSecond = secondPoints.get()/100f
                val avgTimeFirst = firstTime.get()/100000f
                val avgTimeSecond = secondTime.get()/100000f
                val winrate = (firstWins.get() / (draws.get() + firstWins.get() + secondWins.get().toFloat()))

                averages[i][0] += winrate / (2*participants.size)
                averages[i][1] += avgPointsFirst / (2*participants.size)
                averages[i][2] += avgTimeFirst / (2*participants.size)

                averages[j][0] += (secondWins.get() / (draws.get() + firstWins.get() + secondWins.get().toFloat())) / (2*participants.size)
                averages[j][1] += avgPointsSecond / (2*participants.size)
                averages[j][2] += avgTimeSecond / (2*participants.size)

                println("$firstBot got $avgPointsFirst Points on Avg against $secondBot, who got $avgPointsSecond.\n" +
                        "The winrate is $winrate. There where ${draws.get()} Draws\n" +
                        "$firstBot took ${avgTimeFirst}s on Avg per Game. $secondBot took ${avgTimeSecond}s.\n")

                firstWins.set(0)
                secondWins.set(0)
                firstPoints.set(0)
                secondPoints.set(0)
                draws.set(0)
                firstTime.set(0)
                secondTime.set(0)
            }
        }


    }

    private fun playGame(player1: Int, player2: Int) {
        var firstTimeL = 0L
        var secondTimeL = 0L
        val rootService = RootService()
        val scoringCards = List(5) { Random.nextBoolean() }

        var player1Type = participants[player1]
        var player2Type = participants[player2]

        if (player1Type == player2Type && player1 != player2) {
            if (player1 > player2) {
                player1Type = PlayerType.HARD_BOT
            } else {
                player2Type = PlayerType.HARD_BOT
            }
        }

        rootService.gameService.startNewGame(
            listOf(
                Pair("first", player1Type),
                Pair("second", player2Type)
            ),
            scoringCards
        )

        var isFinished = false
        var result: List<Pair<String,List<Int>>> = emptyList()
        val refreshable = object : Refreshable {
            override fun refreshAfterEndGame(scores: List<Pair<String, List<Int>>>) {
                result = scores
                isFinished = true
            }
        }
        rootService.addRefreshable(refreshable)

        while (!isFinished) {
            var startTime = System.currentTimeMillis()
            rootService.bot.makeTurn(player1Type)
            rootService.gameService.changeTurn()
            firstTimeL += System.currentTimeMillis() - startTime
            startTime = System.currentTimeMillis()
            rootService.bot.makeTurn(player2Type)
            rootService.gameService.changeTurn()
            secondTimeL += System.currentTimeMillis() - startTime
        }

        val firstScore = result.single { it.first == "first" }.second.sum()
        val secondScore = result.single { it.first == "second" }.second.sum()

        firstPoints.getAndAdd(firstScore)
        secondPoints.getAndAdd(secondScore)
        firstTime.getAndAdd(firstTimeL)
        secondTime.getAndAdd(secondTimeL)

        if (firstScore > secondScore) {
            firstWins.getAndIncrement()
        } else if (firstScore < secondScore) {
            secondWins.getAndIncrement()
        } else {
            draws.getAndIncrement()
        }

    }

    fun printAverages() {
        for (i in participants.indices) {
            val name = getBotName(i)
            println("$name won ${averages[i][0]}% off his games\n" +
                    "The average Score across all games was ${averages[i][1]}\n" +
                    "The average Time per Move was ${averages[i][2]/20}\n")
        }
    }
}

fun main() {
    val scanner = Scanner(System.`in`)
    val compare = BotComparison()
    //compare.testing()
    compare.ghBotTesten()

    /*
    Runtime.getRuntime().addShutdownHook(Thread {
        compare.printAverages()
    })*/
}