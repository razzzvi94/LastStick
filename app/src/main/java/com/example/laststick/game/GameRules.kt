package com.example.laststick.game

/** Why a move was rejected. */
enum class MoveError {
    GAME_OVER,
    NO_SUCH_ROW,
    NOT_POSITIVE,
    TOO_MANY,
}

/** Pure rules of Misère Nim: whoever removes the last stick loses. */
object GameRules {

    fun validate(state: GameState, move: Move): MoveError? = when {
        state.isOver -> MoveError.GAME_OVER
        move.row !in state.rows.indices -> MoveError.NO_SUCH_ROW
        move.count < 1 -> MoveError.NOT_POSITIVE
        move.count > state.rows[move.row] -> MoveError.TOO_MANY
        else -> null
    }

    fun isLegal(state: GameState, move: Move): Boolean = validate(state, move) == null

    /**
     * Applies [move] and returns the new state, or `null` if the move is illegal.
     * Switches the turn exactly once; if no sticks remain the mover is recorded as the loser.
     */
    fun apply(state: GameState, move: Move): GameState? {
        if (!isLegal(state, move)) return null
        val newRows = state.rows.toMutableList().also { it[move.row] -= move.count }.toList()
        return if (newRows.all { it == 0 }) {
            state.copy(rows = newRows, loser = state.currentPlayer)
        } else {
            state.copy(rows = newRows, currentPlayer = state.currentPlayer.opponent)
        }
    }

    fun legalMoves(rows: List<Int>): List<Move> =
        rows.flatMapIndexed { row, size -> (1..size).map { Move(row, it) } }

    fun legalMoves(state: GameState): List<Move> =
        if (state.isOver) emptyList() else legalMoves(state.rows)
}
