package com.example.laststick.ui

import com.example.laststick.game.GameRules
import com.example.laststick.game.GameState
import com.example.laststick.game.Move
import com.example.laststick.game.Player
import com.example.laststick.ui.board.BoardLayout
import com.example.laststick.ui.board.PlacedStick
import com.example.laststick.ui.board.Pt
import com.example.laststick.ui.board.StickBoard
import com.example.laststick.ui.board.SwipeSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeSelectionTest {
    private val w = 800f
    private val h = 900f
    private val board = StickBoard.fromCounts(listOf(3, 5, 7))
    private val sticks = BoardLayout.place(board, w, h)

    private fun selection(sticks: List<PlacedStick> = this.sticks) =
        SwipeSelection(sticks, BoardLayout.slopX(w), BoardLayout.slopY(h, 3))

    private fun center(row: Int, index: Int): Pt {
        val b = sticks.first { it.row == row && it.index == index }.bounds
        return Pt(b.centerX, b.centerY)
    }

    private fun idOf(row: Int, index: Int) = sticks.first { it.row == row && it.index == index }.id

    @Test
    fun boardStartsWithThreeFiveSevenUniqueSticks() {
        assertEquals(listOf(3, 5, 7), board.counts)
        assertEquals(15, sticks.map { it.id }.toSet().size)
    }

    @Test
    fun layoutKeepsSticksInsideBoundsWithoutOverlap() {
        for (s in sticks) {
            assertTrue(s.bounds.left >= 0f && s.bounds.right <= w)
            assertTrue(s.bounds.top >= 0f && s.bounds.bottom <= h)
        }
        for (row in 0..2) {
            val r = sticks.filter { it.row == row }.sortedBy { it.index }
            r.zipWithNext().forEach { (a, b) -> assertTrue(a.bounds.right < b.bounds.left) }
        }
    }

    @Test
    fun swipeAcrossRowSelectsEverySwipedStickOnce() {
        val sel = selection()
        sel.begin(center(2, 1))
        sel.moveTo(center(2, 3))
        sel.moveTo(center(2, 4))
        val result = sel.finish()!!
        assertEquals(2, result.row)
        assertEquals(setOf(idOf(2, 1), idOf(2, 2), idOf(2, 3), idOf(2, 4)), result.ids)
    }

    @Test
    fun aStickIsNeverSelectedTwiceEvenIfCrossedRepeatedly() {
        val sel = selection()
        sel.begin(center(1, 2))
        repeat(5) {
            sel.moveTo(center(1, 3))
            // Hovering on the last selected stick again must not duplicate or drop anything.
            val now = sel.moveTo(center(1, 3))!!
            assertEquals(2, now.count)
        }
        assertEquals(setOf(idOf(1, 2), idOf(1, 3)), sel.finish()!!.ids)
    }

    @Test
    fun movingBackDeselectsTheSticksAfterThePointer() {
        val sel = selection()
        sel.begin(center(2, 1))
        assertEquals(4, sel.moveTo(center(2, 4))!!.count) // 1,2,3,4
        val back = sel.moveTo(center(2, 2))!! // retrace to stick 2
        assertEquals(setOf(idOf(2, 1), idOf(2, 2)), back.ids)
        assertEquals(setOf(idOf(2, 1), idOf(2, 2)), sel.finish()!!.ids)
    }

    @Test
    fun aFastBackwardJumpDeselectsEverythingPastTheTarget() {
        val sel = selection()
        sel.begin(center(2, 0))
        sel.moveTo(center(2, 5))
        val back = sel.moveTo(center(2, 2))!! // one big jump back over 5,4,3
        assertEquals(setOf(idOf(2, 0), idOf(2, 1), idOf(2, 2)), back.ids)
    }

    @Test
    fun slidingBackToTheFirstStickDeselectsItToo() {
        val sel = selection()
        sel.begin(center(2, 3))
        sel.moveTo(center(2, 5))
        val back = sel.moveTo(center(2, 3))!!
        assertTrue(back.ids.isEmpty())
        assertNull(sel.finish()) // empty selection: nothing is removed
    }

    @Test
    fun afterClearingTheSwipeCanContinueAndTheFirstStickStaysOut() {
        val sel = selection()
        sel.begin(center(2, 3))
        sel.moveTo(center(2, 1)) // 3,2,1
        assertEquals(3, sel.current()!!.count)
        assertTrue(sel.moveTo(center(2, 3))!!.ids.isEmpty())
        val other = sel.moveTo(center(2, 5))!!
        assertEquals(setOf(idOf(2, 4), idOf(2, 5)), other.ids)
    }

    @Test
    fun jitteringOnALoneFirstStickDoesNotDeselectIt() {
        val sel = selection()
        val b = sticks.first { it.row == 2 && it.index == 3 }.bounds
        sel.begin(center(2, 3))
        repeat(5) {
            sel.moveTo(Pt(b.right + 2f, b.centerY))
            sel.moveTo(Pt(b.left - 2f, b.centerY))
            sel.moveTo(Pt(b.centerX, b.centerY))
        }
        assertEquals(setOf(idOf(2, 3)), sel.current()!!.ids)
    }

    @Test
    fun fastSwipeDoesNotSkipSticksBetweenSamples() {
        val sel = selection()
        val left = center(2, 0)
        val right = center(2, 6)
        sel.begin(left)
        sel.moveTo(right) // one huge jump
        assertEquals(7, sel.finish()!!.count)
    }

    @Test
    fun otherRowsAreIgnoredAndRowStaysLocked() {
        val sel = selection()
        sel.begin(center(1, 0))
        // Drag diagonally through rows 0 and 2.
        sel.moveTo(center(0, 1))
        sel.moveTo(center(2, 3))
        sel.moveTo(center(1, 2))
        val result = sel.finish()!!
        assertEquals(1, result.row)
        assertTrue(result.ids.all { id -> sticks.first { it.id == id }.row == 1 })
        assertTrue(idOf(1, 0) in result.ids && idOf(1, 2) in result.ids)
    }

    @Test
    fun swipeStartingInEmptySpaceStaysInactive() {
        val sel = selection()
        assertNull(sel.begin(Pt(1f, 1f)))
        assertFalse(sel.isActive)
        assertNull(sel.moveTo(center(2, 3)))
        assertNull(sel.finish())
    }

    @Test
    fun cancelledGestureLeavesNothingSelected() {
        val sel = selection()
        sel.begin(center(0, 0))
        sel.moveTo(center(0, 1))
        sel.cancel()
        assertFalse(sel.isActive)
        assertNull(sel.finish())
    }

    @Test
    fun removedSticksCannotBeSelected() {
        val removed = board.remove(setOf(idOf(2, 2), idOf(2, 3)))
        val placed = BoardLayout.place(removed, w, h)
        val sel = selection(placed)
        val rowSticks = placed.filter { it.row == 2 }
        sel.begin(Pt(rowSticks.first().bounds.centerX, rowSticks.first().bounds.centerY))
        sel.moveTo(Pt(rowSticks.last().bounds.centerX, rowSticks.last().bounds.centerY))
        val result = sel.finish()!!
        assertEquals(5, result.count)
        assertFalse(idOf(2, 2) in result.ids)
        assertFalse(idOf(2, 3) in result.ids)
    }

    @Test
    fun committedSelectionBecomesOneLegalMove() {
        val sel = selection()
        sel.begin(center(1, 0))
        sel.moveTo(center(1, 2))
        val s = sel.finish()!!
        val state = GameState.initial()
        val next = GameRules.apply(state, Move(s.row, s.count))!!
        assertEquals(listOf(3, 2, 7), next.rows)
        assertEquals(Player.AI, next.currentPlayer)
        val newBoard = board.remove(s.ids)
        assertEquals(next.rows, newBoard.counts)
    }

    @Test
    fun emptySelectionNeverChangesState() {
        val sel = selection()
        sel.begin(Pt(-50f, -50f))
        assertNull(sel.finish())
    }

    @Test
    fun aiTakesRightMostSticks() {
        val ids = board.lastIds(2, 3)
        assertEquals(3, ids.size)
        assertEquals(listOf(4, 5, 6).map { it + 8 }, ids)
        assertEquals(listOf(3, 5, 4), board.remove(ids.toSet()).counts)
    }
}
