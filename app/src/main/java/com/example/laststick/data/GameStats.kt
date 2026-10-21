package com.example.laststick.data

/**
 * Local game statistics. [currentStreak] is positive for consecutive human wins and negative for
 * consecutive human losses; [recent] lists the latest results, newest last (true = human won).
 */
data class GameStats(
    val played: Int = 0,
    val humanWins: Int = 0,
    val aiWins: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val recent: List<Boolean> = emptyList(),
) {
    val winRatePercent: Int
        get() = if (played == 0) 0 else Math.round(humanWins * 100f / played)

    fun record(humanWon: Boolean): GameStats {
        val streak = when {
            humanWon -> if (currentStreak > 0) currentStreak + 1 else 1
            else -> if (currentStreak < 0) currentStreak - 1 else -1
        }
        return copy(
            played = played + 1,
            humanWins = humanWins + if (humanWon) 1 else 0,
            aiWins = aiWins + if (humanWon) 0 else 1,
            currentStreak = streak,
            bestStreak = maxOf(bestStreak, streak),
            recent = (recent + humanWon).takeLast(MAX_RECENT),
        )
    }

    companion object {
        const val MAX_RECENT = 10

        fun encodeRecent(recent: List<Boolean>): String =
            recent.joinToString("") { if (it) "W" else "L" }

        fun decodeRecent(text: String): List<Boolean> =
            text.filter { it == 'W' || it == 'L' }.map { it == 'W' }.takeLast(MAX_RECENT)
    }
}
