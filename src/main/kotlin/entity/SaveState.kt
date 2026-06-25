package entity

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer

/**
 * Repräsentiert das "Foto" (Snapshot) eines einzelnen Spielzustands.
 * Verhindert, dass Jackson versucht, komplexe BGW-Klassen direkt zu serialisieren.
 */
data class GameSnapshot(
    val tileStackList: List<Tile>,
    val natureTokens: Int,
    val choicesList: List<Pair<Tile, WildlifeToken>>,
    val selectedChoice: Pair<Int, Int>,
    val gameState: GameState,
    val playerQueue: java.util.ArrayDeque<Player>,
    val removedTokensList: List<WildlifeToken>,
    val wildlifeTokensList: List<WildlifeToken>,
    val scoringCards: List<Boolean>,
    val isLocal: Boolean
)

/**
 * Der Hauptkarton für die Festplatte, welcher das aktuelle Spiel
 * und die komplette Undo/Redo-Historie bündelt.
 */
data class SaveState(
    val currentGame: GameSnapshot,
    val prevMovesList: List<GameSnapshot>,
    val undoneMovesList: List<GameSnapshot>
)

/**
 * Bringt Jackson bei, wie er JSON-Map-Schlüssel im Format "(1, -1, 0)"
 * wieder in ein echtes Kotlin-Triple<Int, Int, Int> umwandelt.
 */
class TripleKeyDeserializer : KeyDeserializer() {
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any {
        val clean = key.replace("(", "").replace(")", "")
        val parts = clean.split(",").map { it.trim().toInt() }
        return Triple(parts[0], parts[1], parts[2])
    }
}