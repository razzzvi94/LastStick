package com.example.laststick.ui.board

/** An immutable snapshot of what a swipe has selected so far: sticks of a single [row]. */
data class Selection(val row: Int, val ids: Set<Int>) {
    val count: Int get() = ids.size
}

/**
 * Pure swipe-selection logic (no Android or Compose types), driven by the gesture handler.
 *
 * Behaviour:
 *  * A swipe must **start on or near** a stick; otherwise it stays inactive.
 *  * The row of the first stick locks the selection. Sticks in other rows are **ignored** – the
 *    swipe is never cancelled by crossing rows, and never selects from them.
 *  * Every segment between consecutive pointer samples is tested against each stick's (slightly
 *    inflated) bounds, so fast swipes cannot skip sticks. A stick is selected at most once.
 *  * The selection behaves like a stack in the order sticks were crossed: moving the finger back
 *    over an already selected stick **deselects** every stick selected after it. Sliding all the
 *    way back onto the first stick deselects that one too (the selection becomes empty, the row
 *    stays locked and the swipe can start over). Merely resting on or jittering around a lone
 *    first stick never deselects it.
 */
class SwipeSelection(
    sticks: List<PlacedStick>,
    slopX: Float,
    slopY: Float,
) {
    private val hitAreas = sticks.map { it to it.bounds.inflate(slopX, slopY) }
    private val selected = mutableListOf<Int>()
    private var lockedRow: Int? = null
    private var last: Pt? = null

    val isActive: Boolean get() = lockedRow != null

    fun current(): Selection? = lockedRow?.let { Selection(it, selected.toSet()) }

    /** Starts a swipe at [p]. Returns the initial selection, or `null` if nothing is near. */
    fun begin(p: Pt): Selection? {
        reset()
        val hit = hitAreas
            .filter { (_, area) -> area.contains(p) }
            .minByOrNull { (stick, _) -> distSq(p, stick.bounds) }
            ?.first ?: return null
        lockedRow = hit.row
        selected += hit.id
        last = p
        return current()
    }

    /** Extends the swipe to [p]. Returns the updated selection or `null` if the swipe is inactive. */
    fun moveTo(p: Pt): Selection? {
        val row = lockedRow ?: return null
        val from = last ?: p
        val crossings = hitAreas
            // Only sticks the finger *enters*: the one it is already standing on is not a new crossing.
            .filter { (stick, area) -> stick.row == row && !area.contains(from) }
            .mapNotNull { (stick, area) -> area.segmentEntry(from, p)?.let { stick.id to it } }
            .sortedBy { it.second } // in the order the finger meets them
        for ((id, _) in crossings) {
            val position = selected.indexOf(id)
            when {
                position < 0 -> selected += id
                // Back on the very first stick after reaching others: deselect everything.
                position == 0 && selected.size > 1 -> selected.clear()
                // Revisiting a later stick: drop everything selected after it.
                position in 1 until selected.lastIndex ->
                    while (selected.lastIndex > position) selected.removeAt(selected.lastIndex)
            }
        }
        last = p
        return current()
    }

    /** Ends the swipe and returns the final selection (or `null` if empty/inactive). */
    fun finish(): Selection? {
        val result = current()?.takeIf { it.ids.isNotEmpty() }
        reset()
        return result
    }

    fun cancel() = reset()

    private fun reset() {
        selected.clear()
        lockedRow = null
        last = null
    }

    private fun distSq(p: Pt, b: Bounds): Float {
        val dx = p.x - b.centerX
        val dy = p.y - b.centerY
        return dx * dx + dy * dy
    }
}
