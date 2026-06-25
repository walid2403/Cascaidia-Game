package entity

import tools.aqua.bgw.util.Stack
import java.io.Serializable
import java.util.Queue
import java.util.ArrayDeque

/**
 * Diese Entity-Klasse stellt ein Spiel des Spieles Cascadia dar.
 *
 * @param scoringCards Enthält die gewählten Scoring Cards als [List] von [Boolean]. Hierbei bedeutet true Scoring
 * Card A und false Scoring Card B. Die Reihenfolge ist Bear, Elk, Salmon, Hawk, Fox
 * @param isLocal Ein [Boolean] welcher angibt, ob das Spiel lokal läuft oder in einem Network
 *
 * @property tileStack Hält die Tiles die aktuell nicht ausliegen als [Tile] Objekte auf einem [Stack]
 * @property natureTokens Hält die Anzahl an nicht ausgegebenen Nature Tokens als [Int] fest
 * @property scoringCards Enthält die gewählten Scoring Cards als [List] von [Boolean]. Hierbei bedeutet true Scoring
 * Card A und false Scoring Card B. Die Reihenfolge ist Bear, Elk, Salmon, Hawk, Fox
 * @property choices Enthält die aktuell ausliegenden Kombinationen von [Tile] und [WildlifeToken] in einer
 * [MutableList] von [Pair] Objekten
 * @property selectedChoice Enthält ein [Pair] Objekt welches die Indices des gewählten [Tile] sowie des gewählten
 * [WildlifeToken] enthält
 * @property gameState Enthält den GameState des aktuellen Zuges als [GameState] Objekt
 * @property playerQueue Enthält die Spieler als [Player] Objekte in einer [Queue]
 * @property removedTokens Enthält die aktuell beiseite gelegten [WildlifeToken] in einer [MutableList]
 * @property wildlifeTokens Enthält die [WildlifeToken] welche sich momentan auf dem Nachziehstapel befinden in einem
 * [Stack]
 * @property isLocal Ein [Boolean] welcher angibt, ob das Spiel lokal läuft oder in einem Network
 */

class CascadiaGame(val scoringCards: List<Boolean>, val isLocal: Boolean) {

    val tileStack: Stack<Tile> = Stack()

    var natureTokens: Int = 0

    val choices: MutableList<Pair<Tile, WildlifeToken>> = mutableListOf()
    var selectedChoice: Pair<Int, Int> = Pair(-1, -1)

    var gameState: GameState = GameState.START_OF_TURN

    val playerQueue: Queue<Player> = ArrayDeque()

    val removedTokens: MutableList<WildlifeToken> = mutableListOf()
    val wildlifeTokens: Stack<WildlifeToken> = Stack()
}
