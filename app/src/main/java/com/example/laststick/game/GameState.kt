package com.example.laststick.game

/**
 * Immutable, authoritative Misère Nim position.
 *
 * [rows] holds the number of remaining sticks per row. When the game is over, [loser] is the
 * player who removed the final stick and [currentPlayer] is the player who would have moved next.
 */
data class GameState(
    val rows: List<Int>,
    val currentPlayer: Player,
    val loser: Player? = null,
) {
    val totalSticks: Int get() = rows.sum()
    val isOver: Boolean get() = loser != null
    val winner: Player? get() = loser?.opponent

    companion object {
        val INITIAL_ROWS: List<Int> = listOf(3, 5, 7)

        fun initial(starter: Player = Player.HUMAN): GameState = GameState(INITIAL_ROWS, starter)
    }
}
