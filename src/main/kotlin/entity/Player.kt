package entity

/**
 * Diese Entity-Klasse stellt einen Spieler des Spiels Cascadia dar.
 *
 * @param name Der Name des Spielers als [String]
 * @param type Die Art des Spielers als [PlayerType] Objekt
 *
 * @property name Der Name des Spielers als [String]
 * @property type Die Art des Spielers als [PlayerType] Objekt
 * @property natureTokens Die Anzahl an Nature Tokens die der Spieler besitzt als [Int]
 * @property board Das Spiel-Board des Spielers, als [Map] eines [Triple] Objekts mit [Int] Objekten,
 * und einem [Tile] Objekt
 */

class Player(val name: String, val type: PlayerType) {
    var natureTokens: Int = 0

    val board: MutableMap<Triple<Int, Int, Int>, Tile> = mutableMapOf()
}