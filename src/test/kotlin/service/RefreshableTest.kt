package service

import entity.PlayerType
import kotlin.test.Test


/**
 * This class exists to cover the empty default method bodies
 * declared in the [Refreshable] interface.
 * No real assertions
 */
class RefreshableTest {
    private val refreshable = object: Refreshable{}

    @Test
    fun `refreshable test`() {
        refreshable.refreshAfterChangeTurn(lastTurn = false)
        refreshable.refreshAfterStartGame()
        refreshable.refreshAfterSelectColumn(index = 0)
        refreshable.refreshAfterChangeWildlife(indices = listOf(0))
        refreshable.refreshAfterExterminate()
        refreshable.refreshAfterLoadGame()
        refreshable.refreshAfterUndo()
        refreshable.refreshAfterRedo()
        refreshable.refreshAfterFreeSelection()
        refreshable.refreshAfterRotate(amount = 2)
        refreshable.refreshAfterPlaceTile(index = Triple(0, 0,0))
        refreshable.refreshAfterPlaceWildlife(index = Triple(0, 0,0))
        refreshable.refreshAfterEndGame(listOf())
        refreshable.refreshAfterSaveGame()
        refreshable.refreshAfterConnectionError(errorMessage = "")
        refreshable.refreshAfterGameConfigUpdate(playerList = listOf(), scoringCards = listOf())
        refreshable.refreshAfterHostGame(lobbyCode = "", playerName = "", playerType = PlayerType.HUMAN)
        refreshable.refreshAfterJoinGame(lobbyCode = "", playerName = "", playerType = PlayerType.HUMAN)
        refreshable.refreshAfterPlayerJoined(playerName = "")
        refreshable.refreshAfterChatMessage(messageSender = "", message = "")
        refreshable.refreshAfterUnlockSelection()
        refreshable.refreshAfterSelectWildlife(wildlifeIndex = 0)
        refreshable.refreshAfterSelectTile(tileIndex = 0)
        refreshable.refreshAfterBotTurn(
            coordinatesTile = Triple(0, 0, 0),
            coordinatesWildlife = Triple(0, 0, 0),
        )


    }
}