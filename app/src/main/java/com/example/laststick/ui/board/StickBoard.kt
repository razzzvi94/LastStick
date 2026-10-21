package com.example.laststick.ui.board

/**
 * Visual bookkeeping of which individual sticks (by stable id) are still on the table.
 * It mirrors the authoritative [com.example.laststick.game.GameState] row counts but adds
 * identity, so the UI can animate and hit-test specific sticks.
 */
data class StickBoard(val rows: List<List<Int>>) {

    val counts: List<Int> get() = rows.map { it.size }

    fun rowOf(id: Int): Int = rows.indexOfFirst { id in it }

    /** Ids removed from the board; unknown ids are ignored. */
    fun remove(ids: Set<Int>): StickBoard = StickBoard(rows.map { row -> row.filterNot { it in ids } })

    /** The [count] right-most sticks of [row] (what the AI "takes"). */
    fun lastIds(row: Int, count: Int): List<Int> = rows[row].takeLast(count)

    companion object {
        /** Builds a board with unique sequential ids matching [counts]. */
        fun fromCounts(counts: List<Int>): StickBoard {
            var next = 0
            return StickBoard(counts.map { n -> List(n) { next++ } })
        }
    }
}
