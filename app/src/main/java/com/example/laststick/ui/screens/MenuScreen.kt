package com.example.laststick.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.laststick.R
import com.example.laststick.ui.components.PrimaryButton
import com.example.laststick.ui.components.SecondaryButton
import com.example.laststick.ui.tutorial.CoachMarkOverlay
import com.example.laststick.ui.tutorial.CoachStep
import com.example.laststick.ui.tutorial.coachTarget
import com.example.laststick.ui.tutorial.rememberCoachRegistry

private val MenuTutorial = listOf(
    CoachStep(null, R.string.tut_menu_welcome_title, R.string.tut_menu_welcome_body),
    CoachStep("play", R.string.tut_menu_play_title, R.string.tut_menu_play_body),
    CoachStep("perfect", R.string.tut_perfect_title, R.string.tut_perfect_body),
    CoachStep("leaderboard", R.string.tut_menu_leaderboard_title, R.string.tut_menu_leaderboard_body),
    CoachStep("settings", R.string.tut_menu_settings_title, R.string.tut_menu_settings_body),
)

@Composable
fun MenuScreen(
    perfectAi: Boolean,
    onPerfectAiChange: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onLeaderboard: () -> Unit,
    onSettings: () -> Unit,
    showTutorial: Boolean,
    onTutorialFinished: () -> Unit,
) {
    var rulesOpen by rememberSaveable { mutableStateOf(false) }
    val registry = rememberCoachRegistry()

    val header: @Composable () -> Unit = {
        LogoSticks(Modifier.size(136.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.supporting_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
    val actions: @Composable () -> Unit = {
        PrimaryButton(stringResource(R.string.play), onPlay, Modifier.coachTarget(registry, "play"))
        SecondaryButton(stringResource(R.string.leaderboard), onLeaderboard, Modifier.coachTarget(registry, "leaderboard"))
        SecondaryButton(stringResource(R.string.settings), onSettings, Modifier.coachTarget(registry, "settings"))

        PerfectAiRow(perfectAi, onPerfectAiChange, Modifier.coachTarget(registry, "perfect"))

        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .clickable { rulesOpen = !rulesOpen }
                .padding(16.dp),
        ) {
            Text(
                stringResource(if (rulesOpen) R.string.how_to_play_open else R.string.how_to_play_closed),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.heightIn(min = 32.dp),
            )
            if (rulesOpen) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.rules_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            // Landscape: identity on the left, actions on the right, each scrolling on its own if needed.
            Row(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                ) { header() }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) { actions() }
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 600.dp)
                    .fillMaxHeight()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                header()
                Spacer(Modifier.height(10.dp))
                actions()
            }
        }

        if (showTutorial) CoachMarkOverlay(MenuTutorial, registry, onTutorialFinished)
    }
}

@Composable
private fun PerfectAiRow(perfectAi: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val stateText = stringResource(if (perfectAi) R.string.state_on else R.string.state_off)
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .toggleable(value = perfectAi, role = Role.Switch, onValueChange = onChange)
            .semantics { stateDescription = stateText }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.perfect_ai), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (perfectAi) R.string.perfect_ai_on else R.string.perfect_ai_off),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = perfectAi, onCheckedChange = null)
    }
}

/**
 * The app logo ("swipe and survive"): a swipe has cleared the row and a single coral stick is left
 * standing. Drawn in a 190-unit design space (same as the launcher icon) and scaled to fit.
 */
@Composable
private fun LogoSticks(modifier: Modifier = Modifier) {
    val coral = Color(0xFFFF7B6B)
    val gold = Color(0xFFF2B84B)
    val mint = Color(0xFF5EEAD4)
    Canvas(modifier) {
        val unit = size.minDimension / 190f
        scale(unit, unit, pivot = Offset.Zero) {
            rotate(-14f, pivot = Offset(95f, 95f)) {
                drawCircle(coral.copy(alpha = 0.18f), radius = 46f, center = Offset(157f, 98f))
                val dash = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.5f, 4.5f)))
                listOf(20f, 48f, 76f, 104f).forEach { x ->
                    drawRoundRect(
                        color = gold.copy(alpha = 0.5f),
                        topLeft = Offset(x, 52f),
                        size = Size(14f, 86f),
                        cornerRadius = CornerRadius(7f),
                        style = dash,
                    )
                }
                drawRoundRect(coral, Offset(148f, 46f), Size(18f, 100f), CornerRadius(9f))
                drawRoundRect(
                    Color.White.copy(alpha = 0.55f),
                    Offset(152f, 54f),
                    Size(4f, 64f),
                    CornerRadius(2f),
                )
                drawLine(mint, Offset(10f, 148f), Offset(128f, 70f), strokeWidth = 8f, cap = StrokeCap.Round)
                drawCircle(Color.White, radius = 10.5f, center = Offset(133f, 66f))
                drawCircle(mint, radius = 10.5f, center = Offset(133f, 66f), style = Stroke(3.5f))
            }
        }
    }
}
