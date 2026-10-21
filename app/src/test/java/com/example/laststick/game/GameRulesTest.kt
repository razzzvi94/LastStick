package com.example.laststick.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {

    @Test
    fun initialStateIsThreeFiveSeven() {
        val s = GameState.initial()
        assertEquals(listOf(3, 5, 7), s.rows)
        assertEquals(15, s.totalSticks)
        assertEquals(Player.HUMAN, s.currentPlayer)
        assertFalse(s.isOver)
    }

    @Test
    fun legalMoveRemovesFromExactlyOneRow() {
        val next = GameRules.apply(GameState.initial(), Move(1, 3))!!
        assertEquals(listOf(3, 2, 7), next.rows)
    }

    @Test
    fun removingWholeRowIsLegal() {
        val next = GameRules.apply(GameState.initial(), Move(2, 7))!!
        assertEquals(listOf(3, 5, 0), next.rows)
    }

    @Test
    fun illegalMovesAreRejectedAndStateUnchanged() {
        val s = GameState.initial()
        assertEquals(MoveError.NOT_POSITIVE, GameRules.validate(s, Move(0, 0)))
        assertEquals(MoveError.NOT_POSITIVE, GameRules.validate(s, Move(0, -2)))
        assertEquals(MoveError.TOO_MANY, GameRules.validate(s, Move(0, 4)))
        assertEquals(MoveError.NO_SUCH_ROW, GameRules.validate(s, Move(3, 1)))
        assertEquals(MoveError.NO_SUCH_ROW, GameRules.validate(s, Move(-1, 1)))
        assertNull(GameRules.apply(s, Move(0, 4)))
        assertEquals(listOf(3, 5, 7), s.rows)
    }

    @Test
    fun cannotTakeFromEmptyRow() {
        val s = GameState(listOf(0, 2, 0), Player.HUMAN)
        assertEquals(MoveError.TOO_MANY, GameRules.validate(s, Move(0, 1)))
    }

    @Test
    fun turnChangesExactlyOncePerMove() {
        var s = GameState.initial()
        s = GameRules.apply(s, Move(0, 1))!!
        assertEquals(Player.AI, s.currentPlayer)
        s = GameRules.apply(s, Move(1, 1))!!
        assertEquals(Player.HUMAN, s.currentPlayer)
        assertNull(GameRules.apply(s, Move(0, 99)))
        assertEquals(Player.HUMAN, s.currentPlayer)
    }

    @Test
    fun gameDoesNotEndWhileSticksRemain() {
        val s = GameState(listOf(0, 0, 2), Player.HUMAN)
        val next = GameRules.apply(s, Move(2, 1))!!
        assertFalse(next.isOver)
        assertNull(next.loser)
        assertEquals(Player.AI, next.currentPlayer)
    }

    @Test
    fun takingLastStickLoses() {
        val s = GameState(listOf(0, 1, 0), Player.HUMAN)
        val end = GameRules.apply(s, Move(1, 1))!!
        assertTrue(end.isOver)
        assertEquals(Player.HUMAN, end.loser)
        assertEquals(Player.AI, end.winner)

        val end2 = GameRules.apply(GameState(listOf(0, 0, 4), Player.AI), Move(2, 4))!!
        assertEquals(Player.AI, end2.loser)
        assertEquals(Player.HUMAN, end2.winner)
    }

    @Test
    fun noMovesAfterGameOver() {
        val end = GameRules.apply(GameState(listOf(1, 0, 0), Player.HUMAN), Move(0, 1))!!
        assertEquals(MoveError.GAME_OVER, GameRules.validate(end, Move(0, 1)))
        assertTrue(GameRules.legalMoves(end).isEmpty())
    }

    @Test
    fun legalMoveGenerationCoversAllRowsAndCounts() {
        val moves = GameRules.legalMoves(listOf(3, 0, 2))
        assertEquals(5, moves.size)
        assertEquals(setOf(Move(0, 1), Move(0, 2), Move(0, 3), Move(2, 1), Move(2, 2)), moves.toSet())
    }
}
