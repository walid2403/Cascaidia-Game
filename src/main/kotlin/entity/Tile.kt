package entity

/**
 * Diese Entity-Klasse stellt ein Tile des Cascadia Spiels dar
 *
 * @param id Eine numerische ([Int]) ID um das Tile eindeutig identifizieren zu können
 * @param habs Eine [MutableList] welche die verschiedenen Habitate des Tiles als [Habitates] Objekte enthält.
 * Muss eine Länge von 1-3 haben
 * @param possibles Eine [List] an [WildlifeToken] die angibt welche Wildlife Tokens auf dem Tile platziert werden
 * dürfen
 *
 * @property id Eine numerische ([Int]) ID um das Tile eindeutig identifizieren zu können
 * @property habs Eine [MutableList] welche die verschiedenen Habitate des Tiles als [Habitates] Objekte enthält.
 * Muss eine Länge von 1-3 haben
 * @property possibles Eine [List] an [WildlifeToken] die angibt welche Wildlife Tokens auf dem Tile platziert werden
 *  * dürfen
 * @property rotation Enthält die Rotation des Tiles als [Int] Objekt. Die Rotation wird dabei rechtsrum gespeichert, in
 * Inkrementen von 60° (Also 0 = 0°, 1 = 60°, 2 = 120°, ...)
 * @property occupant Enthält den aktuellen Bewohner des Tiles als [WildlifeToken] Objekt. Da am Anfang jedes Tile
 * keinen Bewohner hat, ist der Parameter nullable
 */

class Tile(val id: Int, val habs: MutableList<Habitates>, val possibles: List<WildlifeToken>) {
    var rotation: Int = 0
    var occupant: WildlifeToken? = null
}