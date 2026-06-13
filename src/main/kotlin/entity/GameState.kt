package entity

/**
 * Enum Klasse um den Zustand des Spielzugs darzustellen
 */

enum class GameState {
    START_OF_TURN,
    HAS_EXTERMINATED,
    MADE_CHOICE,
    PLAYER_TILE,
    END_OF_TURN,
    ;
}