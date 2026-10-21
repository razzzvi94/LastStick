package com.example.laststick.ai

import com.example.laststick.game.GameRules
import com.example.laststick.game.GameState
import com.example.laststick.game.Move
import com.example.laststick.game.Player

/**
 * Exact solver for Misère Nim (the player who takes the last stick LOSES).
 *
 * Theory:
 *  * If every non-empty pile has size 1, the player to move loses iff the number of piles is odd.
 *  * Otherwise (some pile has ≥ 2 sticks) the position behaves like normal Nim: the player to move
 *    loses iff the XOR of all pile sizes is 0. The one wrinkle is when exactly one pile has ≥ 2
 *    sticks – there the winning move leaves an odd number of single-stick piles.
 *
 * "Winning" always means *for the player about to move*, with perfect play by both sides. The empty
 * position counts as a win for the player to move, because the opponent just took the last stick.
 */
object MisereNimSolver {

    fun isWinning(rows: List<Int>): Boolean {
        require(rows.all { it >= 0 }) { "Pile sizes must be non-negative" }
        val big = rows.count { it >= 2 }
        return if (big == 0) {
            rows.count { it == 1 } % 2 == 0
        } else {
            rows.fold(0) { acc, n -> acc xor n } != 0
        }
    }

    /** A winning move if one exists, otherwise any legal move. Requires at least one stick left. */
    fun bestMove(rows: List<Int>): Move {
        require(rows.any { it > 0 }) { "No sticks left to take" }
        return winningMove(rows) ?: fallbackMove(rows)
    }

    /** A move after which the opponent is in a losing position, or `null` if none exists. */
    fun winningMove(rows: List<Int>): Move? {
        if (rows.none { it > 0 }) return null
        val bigRows = rows.indices.filter { rows[it] >= 2 }
        val ones = rows.count { it == 1 }
        return when {
            bigRows.isEmpty() ->
                if (ones % 2 == 0) Move(rows.indexOfFirst { it == 1 }, 1) else null

            bigRows.size == 1 -> {
                val row = bigRows.single()
                // Leave an odd number of single-stick piles.
                val leave = if (ones % 2 == 0) 1 else 0
                Move(row, rows[row] - leave)
            }

            else -> {
                val x = rows.fold(0) { acc, n -> acc xor n }
                if (x == 0) {
                    null
                } else {
                    val row = rows.indices.first { (rows[it] xor x) < rows[it] }
                    Move(row, rows[row] - (rows[row] xor x))
                }
            }
        }
    }

    /** Used in lost positions: take a single stick from the largest pile to prolong the game. */
    private fun fallbackMove(rows: List<Int>): Move {
        val row = rows.indices.maxByOrNull { rows[it] }!!
        return Move(row, 1)
    }

    /** Returns true if [move] is legal in [rows] and leaves the opponent in a losing position. */
    fun isWinningMove(rows: List<Int>, move: Move): Boolean {
        if (!GameRules.isLegal(GameState(rows, Player.AI), move)) return false
        val after = rows.toMutableList().also { it[move.row] -= move.count }
        return !isWinning(after)
    }
}
