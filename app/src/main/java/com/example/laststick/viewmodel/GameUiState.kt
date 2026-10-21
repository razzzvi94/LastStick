package com.example.laststick.viewmodel

import com.example.laststick.game.GameState
import com.example.laststick.game.Player
import com.example.laststick.ui.board.StickBoard

enum class AiMode { PERFECT, LEARNING }

enum class Phase {
    /** No game running (before the first game, or after leaving to the menu). */
    IDLE,
    /** The coin is waiting for the player to tap it. */
    COIN_READY,
    /** The coin is spinning to decide who starts. */
    COIN_FLIP,
    /** The coin has landed; the starter is announced. */
    COIN_RESULT,
    HUMAN_TURN,
    HUMAN_REMOVING,
    AI_THINKING,
    AI_HIGHLIGHT,
    AI_REMOVING,
    GAME_OVER,
}

/** Transient appearance of a stick that is driven by the game flow (not by the user's swipe). */
enum class StickVisual { NORMAL, AI_TARGET, REMOVING }

/**
 * Everything the game screen renders. [game] is the authoritative rule state; [board] and
 * [visuals] are only the animated presentation of it and may lag behind while sticks animate out.
 */
data class GameUiState(
    val game: GameState = GameState.initial(),
    val board: StickBoard = StickBoard.fromCounts(GameState.INITIAL_ROWS),
    val visuals: Map<Int, StickVisual> = emptyMap(),
    val phase: Phase = Phase.IDLE,
    val aiMode: AiMode = AiMode.LEARNING,
) {
    val inputEnabled: Boolean get() = phase == Phase.HUMAN_TURN
    val inProgress: Boolean
        get() = phase != Phase.IDLE && phase != Phase.GAME_OVER &&
            phase != Phase.COIN_READY && phase != Phase.COIN_FLIP && phase != Phase.COIN_RESULT
    val movesMade: Boolean get() = game.totalSticks != GameState.INITIAL_ROWS.sum()
    val humanWon: Boolean get() = game.winner == Player.HUMAN
}
