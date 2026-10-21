package com.example.laststick.ai

import com.example.laststick.game.Move

/**
 * Sparse Q-table: Q(rows, move) -> expected outcome for the player who is about to make [move]
 * (+1 = certain win, -1 = certain loss). Unvisited entries are 0.0.
 */
class QTable {
    private val values = HashMap<List<Int>, HashMap<Move, Double>>()

    operator fun get(rows: List<Int>, move: Move): Double = values[rows]?.get(move) ?: 0.0

    operator fun set(rows: List<Int>, move: Move, value: Double) {
        require(value.isFinite()) { "Q-values must be finite" }
        values.getOrPut(rows.toList()) { HashMap() }[move] = value
    }

    /** Best Q over [moves] (must be non-empty). */
    fun maxValue(rows: List<Int>, moves: List<Move>): Double = moves.maxOf { get(rows, it) }

    fun entries(): List<Triple<List<Int>, Move, Double>> =
        values.flatMap { (rows, byMove) -> byMove.map { (m, v) -> Triple(rows, m, v) } }

    fun copy(): QTable = QTable().also { t -> entries().forEach { (r, m, v) -> t[r, m] = v } }

    override fun equals(other: Any?): Boolean =
        other is QTable && entries().toSet() == other.entries().toSet()

    override fun hashCode(): Int = entries().toSet().hashCode()
}
