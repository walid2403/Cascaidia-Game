package service

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 *  test for checking  [RootService.addRefreshable] registers every refreshable passed in.
 *
 */
class RootServiceTest {
    @Test
    fun addRefreshableTest() {
        val rootService = RootService()
        var firstCalled=false
        var secondCalled=false
        val firstRefreshable = object:Refreshable {
            override fun refreshAfterStartGame() {
                firstCalled=true
            }
        }
        val secondRefreshable = object:Refreshable {
            override fun refreshAfterStartGame() {
                secondCalled=true
            }
        }
        rootService.addRefreshables(firstRefreshable,secondRefreshable)
        rootService.gameService.onAllRefreshables { refreshAfterStartGame() }

        assertTrue(firstCalled, message = "First refreshable was not registered")
        assertTrue(secondCalled, message = "Second refreshable was not registered")
    }
}