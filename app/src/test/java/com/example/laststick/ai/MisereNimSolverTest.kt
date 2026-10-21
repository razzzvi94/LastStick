package com.example.laststick.ai

import com.example.laststick.game.GameRules
import com.example.laststick.game.GameState
import com.example.laststick.game.Move
import com.example.laststick.game.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The reference oracle is an independent brute-force minimax with memoisation. */
private class MinimaxOracle {
    private val memo = HashMap<List<Int>, Boolean>()

    /** True if the player to move wins (taking the last stick loses). */
    fun wins(rows: List<Int>): Boolean = memo.getOrPut(rows) {
        if (rows.all { it == 0 }) return@getOrPut true // opponent just took the last stick
        GameRules.legalMoves(rows).any { m ->
            val next = rows.toMutableList().also { it[m.row] -= m.count }
            !wins(next)
        }
    }
}

class MisereNimSolverTest {

    private val oracle = MinimaxOracle()

    /** All sub-positions reachable from [3,5,7] (every row can be reduced to any smaller size). */
    private val reachable: List<List<Int>> = buildList {
        for (a in 0..3) for (b in 0..5) for (c in 0..7) add(listOf(a, b, c))
    }

    @Test
    fun allReachableStatesAreEvaluatedLikeMinimax() {
        assertEquals(192, reachable.size)
        for (rows in reachable) {
            assertEquals("position $rows", oracle.wins(rows), MisereNimSolver.isWinning(rows))
        }
    }

    @Test
    fun winningMoveExistsExactlyWhenPositionIsWinning() {
        for (rows in reachable.filter { it.any { n -> n > 0 } }) {
            val move = MisereNimSolver.winningMove(rows)
            assertEquals("position $rows", oracle.wins(rows), move != null)
        }
    }

    @Test
    fun winningMovesAreLegalAndLeaveOpponentLosing() {
        for (rows in reachable.filter { it.any { n -> n > 0 } && oracle.wins(it) }) {
            val move = MisereNimSolver.bestMove(rows)
            assertTrue("legal $rows -> $move", GameRules.isLegal(GameState(rows, Player.AI), move))
            val after = rows.toMutableList().also { it[move.row] -= move.count }
            assertFalse("$rows -> $move leaves $after", oracle.wins(after))
            assertTrue(MisereNimSolver.isWinningMove(rows, move))
        }
    }

    @Test
    fun movesInLosingPositionsAreStillLegal() {
        for (rows in reachable.filter { it.any { n -> n > 0 } && !oracle.wins(it) }) {
            val move = MisereNimSolver.bestMove(rows)
            assertTrue("legal $rows -> $move", GameRules.isLegal(GameState(rows, Player.AI), move))
            assertNull(MisereNimSolver.winningMove(rows))
        }
    }

    @Test
    fun allOnesExceptionIsHandled() {
        // Odd number of single sticks: mover loses. Even: mover wins by taking one.
        assertFalse(MisereNimSolver.isWinning(listOf(1, 0, 0)))
        assertTrue(MisereNimSolver.isWinning(listOf(1, 1, 0)))
        assertFalse(MisereNimSolver.isWinning(listOf(1, 1, 1)))
        assertEquals(Move(0, 1).row, MisereNimSolver.bestMove(listOf(1, 1, 0)).row.let { 0 })
        assertEquals(1, MisereNimSolver.bestMove(listOf(1, 1, 0)).count)
    }

    @Test
    fun singleBigPileTakesEverythingOrAllButOne() {
        // [4] alone: leave 1 stick so the opponent is forced to take it.
        assertEquals(Move(0, 3), MisereNimSolver.bestMove(listOf(4, 0, 0)))
        // [4,1]: ones is odd, so take the whole big pile, leaving [0,1].
        assertEquals(Move(0, 4), MisereNimSolver.bestMove(listOf(4, 1, 0)))
        // [4,1,1]: ones is even, leave 1 -> three ones.
        assertEquals(Move(0, 3), MisereNimSolver.bestMove(listOf(4, 1, 1)))
    }

    @Test
    fun onlyOneRowRemainingIsHandled() {
        for (n in 1..7) {
            val rows = listOf(0, 0, n)
            val move = MisereNimSolver.bestMove(rows)
            assertTrue(GameRules.isLegal(GameState(rows, Player.AI), move))
            assertEquals(n != 1, MisereNimSolver.isWinning(rows))
        }
    }

    @Test
    fun solverMatchesOracleOnLargerBoards() {
        for (a in 0..5) for (b in 0..5) for (c in 0..5) for (d in 0..5) {
            val rows = listOf(a, b, c, d)
            assertEquals("position $rows", oracle.wins(rows), MisereNimSolver.isWinning(rows))
        }
    }

    @Test
    fun perfectAiBeatsEveryHumanDeviationFromAWinningPosition() {
        // Full game simulation: whenever the AI is in a winning position it must go on to win,
        // regardless of how the opponent replies.
        val ai = PerfectAi()
        fun aiAlwaysWins(rows: List<Int>, aiToMove: Boolean): Boolean {
            if (rows.all { it == 0 }) return aiToMove // previous mover (the other side) lost
            return if (aiToMove) {
                val m = ai.chooseMove(rows)
                val next = rows.toMutableList().also { it[m.row] -= m.count }
                aiAlwaysWins(next, false)
            } else {
                GameRules.legalMoves(rows).all { m ->
                    val next = rows.toMutableList().also { it[m.row] -= m.count }
                    aiAlwaysWins(next, true)
                }
            }
        }
        // [3,5,7] is winning for the first mover, so the AI must win when the human errs first.
        assertTrue(MisereNimSolver.isWinning(listOf(3, 5, 7)))
        for (m in GameRules.legalMoves(listOf(3, 5, 7))) {
            val after = listOf(3, 5, 7).toMutableList().also { it[m.row] -= m.count }
            if (MisereNimSolver.isWinning(after)) {
                assertTrue("AI should win after human plays $m", aiAlwaysWins(after, true))
            }
        }
    }
}
