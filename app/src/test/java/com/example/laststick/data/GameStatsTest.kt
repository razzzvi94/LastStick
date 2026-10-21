package com.example.laststick.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GameStatsTest {
    @Test
    fun recordsWinsLossesAndWinRate() {
        val s = GameStats().record(true).record(false).record(true).record(true)
        assertEquals(4, s.played)
        assertEquals(3, s.humanWins)
        assertEquals(1, s.aiWins)
        assertEquals(75, s.winRatePercent)
    }

    @Test
    fun streaksTrackConsecutiveResults() {
        var s = GameStats()
        s = s.record(true).record(true).record(true)
        assertEquals(3, s.currentStreak)
        assertEquals(3, s.bestStreak)
        s = s.record(false)
        assertEquals(-1, s.currentStreak)
        assertEquals(3, s.bestStreak)
    }

    @Test
    fun recentResultsAreCappedAndRoundTrip() {
        var s = GameStats()
        repeat(15) { s = s.record(it % 2 == 0) }
        assertEquals(GameStats.MAX_RECENT, s.recent.size)
        assertEquals(s.recent, GameStats.decodeRecent(GameStats.encodeRecent(s.recent)))
        assertEquals(emptyList<Boolean>(), GameStats.decodeRecent("xyz"))
    }

    @Test
    fun emptyStatsHaveZeroWinRate() {
        assertEquals(0, GameStats().winRatePercent)
    }
}
