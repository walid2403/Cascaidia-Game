package entity

import java.io.Serializable

/**
 * Speichert die sicheren, flachen Daten von genau EINEM Zustand.
 * Hier gibt es keine BGW-Stacks mehr, nur noch Listen.
 */
data class GameSnapshot(
    val tileStackList: List<Tile>,
    val natureTokens: Int,
    val choicesList: List<Pair<Tile, WildlifeToken>>,
    val selectedChoice: Pair<Int, Int>,
    val gameState: GameState,
    val playerList: List<Player>,
    val removedTokensList: List<WildlifeToken>,
    val wildlifeTokensList: List<WildlifeToken>,
    val scoringCards: List<Boolean>,
    val isLocal: Boolean
) : Serializable

/**
 * Beinhaltet das aktuelle Spiel UND die Historie
 */
data class SaveState(
    val currentGame: GameSnapshot,
    val prevMovesList: List<GameSnapshot>,
    val undoneMovesList: List<GameSnapshot>
) : Serializable