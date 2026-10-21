package com.example.laststick.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.laststick.R
import com.example.laststick.data.AppLanguage
import com.example.laststick.data.AppSettings
import com.example.laststick.data.LearningInfo
import com.example.laststick.ui.components.ConfirmDialog
import com.example.laststick.ui.components.ScreenHeader
import com.example.laststick.ui.components.SecondaryButton
import com.example.laststick.ui.tutorial.CoachMarkOverlay
import com.example.laststick.ui.tutorial.CoachStep
import com.example.laststick.ui.tutorial.coachTarget
import com.example.laststick.ui.tutorial.rememberCoachRegistry

private const val TRAIN_GAMES = 2_000

private enum class PendingConfirm { NONE, STATS, LEARNING }

private val SettingsTutorial = listOf(
    CoachStep("perfect", R.string.tut_perfect_title, R.string.tut_perfect_body),
    CoachStep("coin", R.string.tut_settings_coin_title, R.string.tut_settings_coin_body),
    CoachStep("haptics", R.string.tut_settings_haptics_title, R.string.tut_settings_haptics_body),
    CoachStep("language", R.string.tut_settings_language_title, R.string.tut_settings_language_body),
    CoachStep("learning", R.string.tut_settings_learning_title, R.string.tut_settings_learning_body),
    CoachStep("stats", R.string.tut_settings_stats_title, R.string.tut_settings_stats_body),
    CoachStep("replay", R.string.tut_settings_replay_title, R.string.tut_settings_replay_body),
)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    learningInfo: LearningInfo?,
    training: Boolean,
    onPerfectAi: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onCoinFlip: (Boolean) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onTrain: (Int) -> Unit,
    onResetStats: () -> Unit,
    onResetLearning: () -> Unit,
    onReplayTutorial: () -> Unit,
    showTutorial: Boolean,
    onTutorialFinished: () -> Unit,
    onBack: () -> Unit,
) {
    var pending by rememberSaveable { mutableStateOf(PendingConfirm.NONE) }
    val registry = rememberCoachRegistry()

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
            ScreenHeader(stringResource(R.string.settings), onBack)

            SwitchRow(
                title = stringResource(R.string.perfect_ai),
                subtitle = stringResource(R.string.settings_perfect_ai_subtitle),
                checked = settings.perfectAi,
                onChange = onPerfectAi,
                modifier = Modifier.coachTarget(registry, "perfect"),
            )
            SwitchRow(
                title = stringResource(R.string.settings_coin_title),
                subtitle = stringResource(R.string.settings_coin_subtitle),
                checked = settings.coinFlip,
                onChange = onCoinFlip,
                modifier = Modifier.coachTarget(registry, "coin"),
            )
            SwitchRow(
                title = stringResource(R.string.settings_haptics_title),
                subtitle = stringResource(R.string.settings_haptics_subtitle),
                checked = settings.haptics,
                onChange = onHaptics,
                modifier = Modifier.coachTarget(registry, "haptics"),
            )

            LanguageCard(settings.language, onLanguage, Modifier.coachTarget(registry, "language"))

            Column(
                Modifier
                    .fillMaxWidth()
                    .coachTarget(registry, "learning")
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.settings_learning_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (learningInfo == null) {
                        stringResource(R.string.settings_learning_loading)
                    } else {
                        stringResource(R.string.settings_learning_info, learningInfo.games, learningInfo.explorationPercent)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SecondaryButton(
                    if (training) stringResource(R.string.settings_training) else stringResource(R.string.settings_train, TRAIN_GAMES),
                    onClick = { onTrain(TRAIN_GAMES) },
                    enabled = !training,
                )
                SecondaryButton(
                    stringResource(R.string.settings_reset_learning),
                    onClick = { pending = PendingConfirm.LEARNING },
                    enabled = !training,
                )
            }

            SecondaryButton(
                stringResource(R.string.settings_clear_stats),
                onClick = { pending = PendingConfirm.STATS },
                modifier = Modifier.coachTarget(registry, "stats"),
            )
            SecondaryButton(
                stringResource(R.string.settings_replay_tutorial),
                onClick = onReplayTutorial,
                modifier = Modifier.coachTarget(registry, "replay"),
            )
        }

        if (showTutorial) CoachMarkOverlay(SettingsTutorial, registry, onTutorialFinished)
    }

    when (pending) {
        PendingConfirm.STATS -> ConfirmDialog(
            title = stringResource(R.string.clear_stats_title),
            message = stringResource(R.string.clear_stats_message),
            confirmText = stringResource(R.string.clear),
            onConfirm = { onResetStats(); pending = PendingConfirm.NONE },
            onDismiss = { pending = PendingConfirm.NONE },
        )
        PendingConfirm.LEARNING -> ConfirmDialog(
            title = stringResource(R.string.reset_learning_title),
            message = stringResource(R.string.reset_learning_message),
            confirmText = stringResource(R.string.reset),
            onConfirm = { onResetLearning(); pending = PendingConfirm.NONE },
            onDismiss = { pending = PendingConfirm.NONE },
        )
        PendingConfirm.NONE -> Unit
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun LanguageCard(selected: AppLanguage, onSelect: (AppLanguage) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.settings_language_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.settings_language_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppLanguage.entries.forEach { language ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(selected = language == selected, role = Role.RadioButton, onClick = { onSelect(language) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(selected = language == selected, onClick = null)
                Text(
                    stringResource(
                        when (language) {
                            AppLanguage.SYSTEM -> R.string.language_system
                            AppLanguage.ENGLISH -> R.string.language_english
                            AppLanguage.ROMANIAN -> R.string.language_romanian
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
