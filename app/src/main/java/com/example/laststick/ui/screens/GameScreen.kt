package com.example.laststick.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.laststick.R
import com.example.laststick.game.Player
import com.example.laststick.ui.board.Selection
import com.example.laststick.ui.rememberHaptics
import com.example.laststick.ui.components.CoinFlipOverlay
import com.example.laststick.ui.components.ConfirmDialog
import com.example.laststick.ui.components.IconLabel
import com.example.laststick.ui.components.PrimaryButton
import com.example.laststick.ui.components.SecondaryButton
import com.example.laststick.ui.components.StickBoardView
import com.example.laststick.ui.tutorial.CoachMarkOverlay
import com.example.laststick.ui.tutorial.CoachStep
import com.example.laststick.ui.tutorial.coachTarget
import com.example.laststick.ui.tutorial.rememberCoachRegistry
import com.example.laststick.viewmodel.AiMode
import com.example.laststick.viewmodel.GameTimings
import com.example.laststick.viewmodel.GameUiState
import com.example.laststick.viewmodel.GameViewModel
import com.example.laststick.viewmodel.Phase
import com.example.laststick.viewmodel.StickVisual

private enum class GameDialog { NONE, LEAVE, RESTART }

private val GameTutorial = listOf(
    CoachStep("board", R.string.tut_game_board_title, R.string.tut_game_board_body, swipeDemo = true),
    CoachStep("status", R.string.tut_game_status_title, R.string.tut_game_status_body),
    CoachStep("top", R.string.tut_game_top_title, R.string.tut_game_top_body),
    CoachStep(null, R.string.tut_game_goal_title, R.string.tut_game_goal_body),
)

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    state: GameUiState,
    haptics: Boolean,
    showTutorial: Boolean,
    onTutorialFinished: () -> Unit,
    onExitToMenu: () -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf(GameDialog.NONE) }
    var selection by remember { mutableStateOf<Selection?>(null) }
    val haptic = rememberHaptics()
    val registry = rememberCoachRegistry()

    LaunchedEffect(Unit) { viewModel.ensureGame() }
    // Never leave a half-drawn selection around once input is closed.
    LaunchedEffect(state.inputEnabled) { if (!state.inputEnabled) selection = null }

    fun requestLeave() {
        if (state.inProgress) {
            dialog = GameDialog.LEAVE
        } else {
            viewModel.leaveGame()
            onExitToMenu()
        }
    }

    fun requestRestart() {
        if (state.inProgress && state.movesMade) dialog = GameDialog.RESTART else viewModel.startNewGame()
    }

    BackHandler { requestLeave() }

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            val landscape = maxWidth > maxHeight
            val topBar: @Composable () -> Unit = {
                TopBar(
                    state.aiMode,
                    onMenu = ::requestLeave,
                    onRestart = ::requestRestart,
                    modifier = Modifier.coachTarget(registry, "top"),
                )
            }
            val statusCard: @Composable () -> Unit = {
                StatusCard(state, selection, Modifier.coachTarget(registry, "status"))
            }
            val board: @Composable (Modifier) -> Unit = { boardModifier ->
                StickBoardView(
                    board = state.board,
                    visuals = state.visuals,
                    selection = selection,
                    inputEnabled = state.inputEnabled,
                    removeAnimationMs = GameTimings().removeMs.toInt(),
                    onSelectionChange = { new ->
                        if (haptics && new != null && new.count > (selection?.count ?: 0)) {
                            haptic.tick()
                        }
                        selection = new
                    },
                    onCommit = { sel ->
                        if (viewModel.onHumanMove(sel.row, sel.ids) && haptics) {
                            haptic.confirm()
                        }
                    },
                    onAccessibleMove = { row, count ->
                        viewModel.onHumanMove(row, state.board.lastIds(row, count).toSet())
                    },
                    modifier = boardModifier.coachTarget(registry, "board"),
                )
            }

            if (landscape) {
                // Wide and short: controls on the left, the board gets the full height on the right.
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        topBar()
                        statusCard()
                    }
                    board(Modifier.weight(1.1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topBar()
                    statusCard()
                    board(Modifier.weight(1f).fillMaxWidth())
                }
            }
        }

        if (state.phase == Phase.COIN_READY || state.phase == Phase.COIN_FLIP || state.phase == Phase.COIN_RESULT) {
            CoinFlipOverlay(
                phase = state.phase,
                humanStarts = state.game.currentPlayer == Player.HUMAN,
                spinMs = GameTimings().coinSpinMs.toInt(),
                onTap = viewModel::flipCoin,
            )
        }

        if (state.phase == Phase.GAME_OVER) {
            GameOverOverlay(
                state = state,
                onPlayAgain = viewModel::startNewGame,
                onMenu = {
                    viewModel.leaveGame()
                    onExitToMenu()
                },
            )
        }

        // The tour starts once the human can actually move (after any coin flip / AI opening).
        if (showTutorial && state.phase == Phase.HUMAN_TURN) {
            CoachMarkOverlay(GameTutorial, registry, onTutorialFinished)
        }
    }

    when (dialog) {
        GameDialog.LEAVE -> ConfirmDialog(
            title = stringResource(R.string.leave_title),
            message = stringResource(R.string.abandon_message),
            confirmText = stringResource(R.string.leave),
            dismissText = stringResource(R.string.keep_playing),
            onConfirm = {
                dialog = GameDialog.NONE
                viewModel.leaveGame()
                onExitToMenu()
            },
            onDismiss = { dialog = GameDialog.NONE },
        )
        GameDialog.RESTART -> ConfirmDialog(
            title = stringResource(R.string.restart_title),
            message = stringResource(R.string.abandon_message),
            confirmText = stringResource(R.string.new_game),
            dismissText = stringResource(R.string.keep_playing),
            onConfirm = {
                dialog = GameDialog.NONE
                viewModel.startNewGame()
            },
            onDismiss = { dialog = GameDialog.NONE },
        )
        GameDialog.NONE -> Unit
    }
}

@Composable
private fun TopBar(aiMode: AiMode, onMenu: () -> Unit, onRestart: () -> Unit, modifier: Modifier = Modifier) {
    val modeLabel = stringResource(if (aiMode == AiMode.PERFECT) R.string.mode_perfect else R.string.mode_learning)
    val menuText = stringResource(R.string.menu_button)
    val newGameText = stringResource(R.string.new_game_button)
    val tightPadding = PaddingValues(horizontal = 8.dp)

    // One line, always: Menu on the left, New game on the right, the mode label centred on the bar.
    // On very narrow screens the two buttons keep only their icons so the label never collides.
    BoxWithConstraints(modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        val iconOnly = maxWidth < 340.dp
        TextButton(
            onClick = onMenu,
            contentPadding = tightPadding,
            modifier = Modifier
                .align(Alignment.CenterStart)
                // Pull the button out by its own padding plus the chevron's built-in left gap, so the
                // visible arrow lines up with the left edge of the cards below.
                .offset(x = if (iconOnly) (-17).dp else (-15).dp)
                .heightIn(min = 48.dp),
        ) {
            if (iconOnly) {
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = menuText, modifier = Modifier.size(26.dp))
            } else {
                IconLabel(R.drawable.ic_chevron_left, menuText)
            }
        }
        Text(
            modeLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.align(Alignment.Center),
        )
        TextButton(
            onClick = onRestart,
            contentPadding = tightPadding,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                // Same idea on the right: the label's last letter meets the cards' right edge.
                .offset(x = if (iconOnly) 12.dp else 8.dp)
                .heightIn(min = 48.dp),
        ) {
            if (iconOnly) {
                Icon(painterResource(R.drawable.ic_refresh), contentDescription = newGameText, modifier = Modifier.size(26.dp))
            } else {
                IconLabel(R.drawable.ic_refresh, newGameText)
            }
        }
    }
}

@Composable
private fun StatusCard(state: GameUiState, selection: Selection?, modifier: Modifier = Modifier) {
    val aiTargets = state.visuals.filterValues { it == StickVisual.AI_TARGET || it == StickVisual.REMOVING }.keys
    val aiRow = (aiTargets.firstOrNull()?.let { state.board.rowOf(it) } ?: -1) + 1

    val headline: String
    val detail: String
    when (state.phase) {
        Phase.IDLE -> {
            headline = stringResource(R.string.status_getting_ready); detail = ""
        }
        Phase.COIN_READY -> {
            headline = stringResource(R.string.status_coin_title); detail = stringResource(R.string.status_coin_ready)
        }
        Phase.COIN_FLIP -> {
            headline = stringResource(R.string.status_coin_title); detail = stringResource(R.string.status_coin_flipping)
        }
        Phase.COIN_RESULT -> {
            headline = stringResource(R.string.status_coin_title)
            detail = stringResource(if (state.game.currentPlayer == Player.HUMAN) R.string.you_start else R.string.ai_starts)
        }
        Phase.HUMAN_TURN -> {
            headline = stringResource(R.string.status_your_turn)
            detail = if (selection != null && selection.count > 0) {
                stringResource(R.string.status_removing_selection, selection.count, selection.row + 1)
            } else {
                stringResource(R.string.status_swipe_hint)
            }
        }
        Phase.HUMAN_REMOVING -> {
            headline = stringResource(R.string.status_your_move); detail = stringResource(R.string.status_removing)
        }
        Phase.AI_THINKING -> {
            headline = stringResource(R.string.status_ai_turn); detail = stringResource(R.string.status_ai_thinking)
        }
        Phase.AI_HIGHLIGHT -> {
            headline = stringResource(R.string.status_ai_turn)
            detail = stringResource(R.string.status_ai_will_take, aiTargets.size, aiRow)
        }
        Phase.AI_REMOVING -> {
            headline = stringResource(R.string.status_ai_turn)
            detail = stringResource(R.string.status_ai_takes, aiTargets.size, aiRow)
        }
        Phase.GAME_OVER -> {
            headline = stringResource(R.string.status_game_over)
            detail = stringResource(if (state.humanWon) R.string.you_win else R.string.ai_wins)
        }
    }
    val left = state.game.totalSticks

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(headline, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (state.phase == Phase.AI_THINKING) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.secondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$left", style = MaterialTheme.typography.headlineSmall)
            Text(
                pluralStringResource(R.plurals.sticks_left, left),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GameOverOverlay(state: GameUiState, onPlayAgain: () -> Unit, onMenu: () -> Unit) {
    val won = state.humanWon
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .safeDrawingPadding()
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(28.dp))
                .padding(24.dp)
                .semantics { liveRegion = LiveRegionMode.Assertive },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (won) R.string.you_win else R.string.ai_wins),
                style = MaterialTheme.typography.displayMedium,
                color = if (won) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(if (won) R.string.over_you_won_reason else R.string.over_ai_won_reason),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.over_rule),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(4.dp))
            PrimaryButton(stringResource(R.string.play_again), onPlayAgain)
            SecondaryButton(stringResource(R.string.menu), onMenu)
        }
    }
}
