package com.example.laststick.ai

import com.example.laststick.game.Move

/** Everything the learning agent needs to resume after an app restart. */
data class LearningSnapshot(
    val table: QTable,
    val epsilon: Double,
    val episodes: Int,
) {
    /**
     * Text format (versioned, line based):
     * ```
     * LASTSTICK_Q1
     * epsilon=0.25
     * episodes=12
     * 3,5,7|0:1=0.5|1:2=-0.25
     * ```
     */
    fun serialize(): String = buildString {
        appendLine(HEADER)
        appendLine("epsilon=$epsilon")
        appendLine("episodes=$episodes")
        table.entries()
            .groupBy({ it.first }, { it.second to it.third })
            .toSortedMap(compareBy({ it[0] }, { it[1] }, { it[2] }))
            .forEach { (rows, moves) ->
                append(rows.joinToString(","))
                moves.sortedWith(compareBy({ it.first.row }, { it.first.count })).forEach { (m, v) ->
                    append('|').append(m.row).append(':').append(m.count).append('=').append(v)
                }
                appendLine()
            }
    }

    companion object {
        private const val HEADER = "LASTSTICK_Q1"

        /** Returns `null` for any missing, truncated or otherwise invalid data. */
        fun parse(text: String): LearningSnapshot? {
            return try {
                parseOrThrow(text)
            } catch (e: RuntimeException) {
                null
            }
        }

        private fun parseOrThrow(text: String): LearningSnapshot? {
            val lines = text.lines().filter { it.isNotBlank() }
            if (lines.size < 3 || lines[0] != HEADER) return null
            if (!lines[1].startsWith("epsilon=") || !lines[2].startsWith("episodes=")) return null
            val epsilon = lines[1].removePrefix("epsilon=").toDouble()
            val episodes = lines[2].removePrefix("episodes=").toInt()
            if (!epsilon.isFinite() || epsilon !in 0.0..1.0 || episodes < 0) return null

            val table = QTable()
            for (line in lines.drop(3)) {
                val parts = line.split('|')
                val rows = parts[0].split(',').map { it.toInt() }
                if (rows.size != 3 || rows.any { it < 0 }) return null
                for (entry in parts.drop(1)) {
                    val (moveText, valueText) = entry.split('=')
                    val (rowText, countText) = moveText.split(':')
                    val move = Move(rowText.toInt(), countText.toInt())
                    val value = valueText.toDouble()
                    val legal = move.row in rows.indices && move.count in 1..rows[move.row]
                    if (!legal || !value.isFinite()) return null
                    table[rows, move] = value
                }
            }
            return LearningSnapshot(table, epsilon, episodes)
        }
    }
}
