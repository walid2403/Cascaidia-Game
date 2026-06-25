package entity

import tools.aqua.bgw.util.Stack
import java.io.Serializable

/**
 * Diese Entity-Klasse speichert mehrere [CascadiaGame] Instanzen
 *
 * @property prevMoves Hält die vergangenen Spielstände als [CascadiaGame] Objekte auf einem [Stack]
 * @property undoneMoves Hält die rückgängig gemachten Spielstände als [CascadiaGame] Objekte auf einem [Stack]
 */

class CascadiaGames {
    val prevMoves: Stack<CascadiaGame> = Stack()
    val undoneMoves: Stack<CascadiaGame> = Stack()
}