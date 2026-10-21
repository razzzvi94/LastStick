package com.example.laststick.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.laststick.R
import com.example.laststick.data.GameStats
import com.example.laststick.ui.components.ScreenHeader

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeaderboardScreen(stats: GameStats, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .align(Alignment.TopCenter)
            .widthIn(max = 600.dp)
            .fillMaxHeight()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(stringResource(R.string.leaderboard), onBack)
        Text(
            stringResource(R.string.leaderboard_note),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stat_games_played), stats.played.toString(), Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_win_rate), "${stats.winRatePercent}%", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stat_your_wins), stats.humanWins.toString(), Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_ai_wins), stats.aiWins.toString(), Modifier.weight(1f))
        }
        val streakText = when {
            stats.currentStreak > 0 -> stringResource(R.string.streak_win, stats.currentStreak)
            stats.currentStreak < 0 -> stringResource(R.string.streak_loss, -stats.currentStreak)
            else -> stringResource(R.string.streak_none)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stat_current_streak), streakText, Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_best_streak), stats.bestStreak.toString(), Modifier.weight(1f))
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.recent_games), style = MaterialTheme.typography.titleMedium)
            if (stats.recent.isEmpty()) {
                Text(
                    stringResource(R.string.no_games_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val win = stringResource(R.string.result_win)
                val loss = stringResource(R.string.result_loss)
                val description = stringResource(
                    R.string.recent_results_description,
                    stats.recent.joinToString(", ") { if (it) win else loss },
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                ) {
                    stats.recent.forEach { ResultChip(it) }
                }
            }
        }
    }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A win/loss chip that differs in letter and fill, not just colour. */
@Composable
private fun ResultChip(won: Boolean) {
    Row(
        Modifier
            .background(
                if (won) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(if (won) R.string.chip_win else R.string.chip_loss),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondary,
        )
    }
}
