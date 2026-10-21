package com.example.laststick.ui.board

import kotlin.math.max
import kotlin.math.min

data class Pt(val x: Float, val y: Float)

data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun inflate(dx: Float, dy: Float) = Bounds(left - dx, top - dy, right + dx, bottom + dy)

    fun contains(p: Pt) = p.x in left..right && p.y in top..bottom

    /**
     * Where the segment [a]–[b] first touches this rectangle, as a fraction 0..1 along the segment,
     * or `null` if it never does (Liang–Barsky clipping).
     */
    fun segmentEntry(a: Pt, b: Pt): Float? {
        var t0 = 0f
        var t1 = 1f
        val dx = b.x - a.x
        val dy = b.y - a.y
        fun clip(p: Float, q: Float): Boolean {
            if (p == 0f) return q >= 0f
            val r = q / p
            if (p < 0f) {
                if (r > t1) return false
                t0 = max(t0, r)
            } else {
                if (r < t0) return false
                t1 = min(t1, r)
            }
            return true
        }
        val hit = clip(-dx, a.x - left) && clip(dx, right - a.x) &&
            clip(-dy, a.y - top) && clip(dy, bottom - a.y)
        return if (hit) t0 else null
    }
}

/** A stick with its row, its index within the row and its on-screen bounds. */
data class PlacedStick(val id: Int, val row: Int, val index: Int, val bounds: Bounds)

/** Pure layout of the board for a given pixel size; shared by drawing and hit-testing. */
object BoardLayout {
    const val MAX_PER_ROW = 7
    private const val STICK_WIDTH_RATIO = 0.42f
    private const val STICK_HEIGHT_PER_WIDTH = 6.5f

    /**
     * Distance between stick centres. [gutter] reserves space at the left edge (for the row labels):
     * the widest row of [MAX_PER_ROW] sticks is shrunk until its first stick starts after the gutter.
     */
    fun pitch(width: Float, gutter: Float = 0f): Float {
        val halfSpan = (MAX_PER_ROW - 1) / 2f + STICK_WIDTH_RATIO / 2f // centre-to-outer-edge, in pitches
        return min(width / (MAX_PER_ROW + 1), (width / 2f - gutter) / halfSpan).coerceAtLeast(1f)
    }

    fun place(board: StickBoard, width: Float, height: Float, gutter: Float = 0f): List<PlacedStick> {
        if (board.rows.isEmpty() || width <= 0f || height <= 0f) return emptyList()
        val rowHeight = height / board.rows.size
        val pitch = pitch(width, gutter)
        val stickWidth = pitch * STICK_WIDTH_RATIO
        val stickHeight = min(rowHeight * 0.8f, stickWidth * STICK_HEIGHT_PER_WIDTH)
        return board.rows.flatMapIndexed { row, ids ->
            val centerY = (row + 0.5f) * rowHeight
            ids.mapIndexed { index, id ->
                val cx = width / 2f + (index - (ids.size - 1) / 2f) * pitch
                PlacedStick(
                    id = id,
                    row = row,
                    index = index,
                    bounds = Bounds(
                        cx - stickWidth / 2f,
                        centerY - stickHeight / 2f,
                        cx + stickWidth / 2f,
                        centerY + stickHeight / 2f,
                    ),
                )
            }
        }
    }

    /** Extra hit area around each stick, small enough that neighbouring areas never overlap. */
    fun slopX(width: Float, gutter: Float = 0f): Float = pitch(width, gutter) * 0.2f

    fun slopY(height: Float, rows: Int): Float = if (rows == 0) 0f else height / rows * 0.08f
}
