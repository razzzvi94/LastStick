package com.example.laststick.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laststick.ai.NimAi
import com.example.laststick.ai.PerfectAi
import com.example.laststick.data.LearningRepository
import com.example.laststick.data.SettingsRepository
import com.example.laststick.data.StatsRepository
import com.example.laststick.game.GameRules
import com.example.laststick.game.GameState
import com.example.laststick.game.Move
import com.example.laststick.game.Player
import com.example.laststick.ui.board.StickBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Durations (ms) of the AI turn stages and of the stick-removal animation. */
data class GameTimings(
    val thinkMs: Long = 900,
    val highlightMs: Long = 800,
    val removeMs: Long = 450,
    val coinSpinMs: Long = 2200,
    val coinResultMs: Long = 1000,
)

/**
 * Orchestrates a game: validates moves through [GameRules], drives the AI turn and its animations
 * with coroutines, records statistics and feeds the learning agent.
 *
 * Input is accepted only in [Phase.HUMAN_TURN]; the phase flips synchronously inside
 * [onHumanMove], so rapid repeated swipes cannot start two moves.
 */
class GameViewModel(
    private val settings: SettingsRepository,
    private val stats: StatsRepository,
    private val learning: LearningRepository,
    private val savedState: SavedStateHandle,
    private val timings: GameTimings = GameTimings(),
    private val random: Random = Random.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    /** The single coroutine driving the current move/AI turn; cancelled on reset or clear. */
    private var flowJob: Job? = null

    init {
        restoreFromSavedState()
    }

    // region public actions

    fun startNewGame() {
        flowJob?.cancel()
        _state.value = GameUiState()
        clearSaved()
        flowJob = viewModelScope.launch {
            val prefs = settings.settings.first()
            val mode = if (prefs.perfectAi) AiMode.PERFECT else AiMode.LEARNING
            val starter = if (prefs.coinFlip && random.nextBoolean()) Player.AI else Player.HUMAN
            val game = GameState.initial(starter)
            val fresh = GameUiState(
                game = game,
                board = StickBoard.fromCounts(game.rows),
                phase = Phase.HUMAN_TURN,
                aiMode = mode,
            )
            if (prefs.coinFlip) {
                // The starter is already decided; the coin only reveals it once the player taps.
                _state.value = fresh.copy(phase = Phase.COIN_READY)
            } else {
                _state.value = fresh
                persistStart(game, mode)
            }
        }
    }

    /** The player tapped the coin: spin it, announce the starter, then begin play. */
    fun flipCoin() {
        val ready = _state.value
        if (ready.phase != Phase.COIN_READY) return
        _state.value = ready.copy(phase = Phase.COIN_FLIP)
        flowJob = viewModelScope.launch {
            delay(timings.coinSpinMs)
            _state.update { it.copy(phase = Phase.COIN_RESULT) }
            delay(timings.coinResultMs)
            persistStart(ready.game, ready.aiMode)
            if (ready.game.currentPlayer == Player.AI) {
                runAiTurn()
            } else {
                _state.value = ready.copy(phase = Phase.HUMAN_TURN)
            }
        }
    }

    private fun persistStart(game: GameState, mode: AiMode) {
        savedState[KEY_ROWS] = game.rows.toIntArray()
        savedState[KEY_TURN] = game.currentPlayer.name
        savedState[KEY_MODE] = mode.name
    }

    /** Makes sure the game screen has a game to show (e.g. after process death without a save). */
    fun ensureGame() {
        if (_state.value.phase == Phase.IDLE && flowJob?.isActive != true) startNewGame()
    }

    fun leaveGame() {
        flowJob?.cancel()
        flowJob = null
        _state.value = GameUiState()
        clearSaved()
    }

    /**
     * Commits the human's swipe: [stickIds] must all be sticks of [row] that are still on the
     * board. Returns whether the move was accepted; a rejected move changes nothing.
     */
    fun onHumanMove(row: Int, stickIds: Set<Int>): Boolean {
        var accepted: GameUiState? = null
        var rowsBefore: List<Int> = emptyList()
        var move: Move? = null
        _state.update { s ->
            val rowIds = s.board.rows.getOrNull(row)
            if (!s.inputEnabled || s.game.currentPlayer != Player.HUMAN ||
                rowIds == null || stickIds.isEmpty() || !rowIds.containsAll(stickIds)
            ) {
                return@update s
            }
            val m = Move(row, stickIds.size)
            val next = GameRules.apply(s.game, m) ?: return@update s
            rowsBefore = s.game.rows
            move = m
            s.copy(
                game = next,
                phase = Phase.HUMAN_REMOVING,
                visuals = stickIds.associateWith { StickVisual.REMOVING },
            ).also { accepted = it }
        }
        val after = accepted ?: return false
        val played = move ?: return false
        persist()
        if (after.game.isOver) recordGameEnd(after.game, after.aiMode)
        flowJob = viewModelScope.launch {
            learnFrom(rowsBefore, played)
            delay(timings.removeMs)
            removeAnimatedSticks()
            afterMove(after.game)
        }
        return true
    }

    // endregion

    // region game flow

    private suspend fun afterMove(game: GameState) {
        when {
            game.isOver -> {
                _state.update { it.copy(phase = Phase.GAME_OVER, visuals = emptyMap()) }
            }
            game.currentPlayer == Player.AI -> runAiTurn()
            else -> {
                _state.update { it.copy(phase = Phase.HUMAN_TURN, visuals = emptyMap()) }
                persist()
            }
        }
    }

    private suspend fun runAiTurn() {
        val mode = _state.value.aiMode
        _state.update { it.copy(phase = Phase.AI_THINKING, visuals = emptyMap()) }
        delay(timings.thinkMs)

        val rows = _state.value.game.rows
        val chosen = withContext(Dispatchers.Default) { pickAiMove(mode, rows) }
        val move = chosen.takeIf { GameRules.isLegal(_state.value.game, it) }
            ?: GameRules.legalMoves(rows).first()

        val targets = _state.value.board.lastIds(move.row, move.count)
        _state.update {
            it.copy(phase = Phase.AI_HIGHLIGHT, visuals = targets.associateWith { StickVisual.AI_TARGET })
        }
        delay(timings.highlightMs)

        var next: GameState? = null
        _state.update { s ->
            val applied = GameRules.apply(s.game, move) ?: return@update s
            next = applied
            s.copy(
                game = applied,
                phase = Phase.AI_REMOVING,
                visuals = targets.associateWith { StickVisual.REMOVING },
            )
        }
        val after = next ?: return
        persist()
        if (after.isOver) recordGameEnd(after, mode)
        learnFrom(rows, move)
        delay(timings.removeMs)
        removeAnimatedSticks()
        afterMove(after)
    }

    private suspend fun pickAiMove(mode: AiMode, rows: List<Int>): Move {
        val ai: NimAi = when (mode) {
            AiMode.PERFECT -> PerfectAi()
            AiMode.LEARNING -> learning.agent()
        }
        return ai.chooseMove(rows)
    }

    private fun removeAnimatedSticks() {
        _state.update { s ->
            val ids = s.visuals.filterValues { it == StickVisual.REMOVING }.keys
            s.copy(board = s.board.remove(ids), visuals = emptyMap())
        }
    }

    /**
     * Records the result the moment the final move is applied (not after its animation), on a scope
     * that survives a restart/leave, so a finished game is counted exactly once.
     */
    private fun recordGameEnd(game: GameState, mode: AiMode) {
        viewModelScope.launch {
            stats.recordResult(humanWon = game.winner == Player.HUMAN)
            if (mode == AiMode.LEARNING) {
                learning.agent().finishEpisode()
                learning.save()
            }
        }
    }

    private suspend fun learnFrom(rowsBefore: List<Int>, move: Move) {
        if (_state.value.aiMode == AiMode.LEARNING) learning.agent().learn(rowsBefore, move)
    }

    // endregion

    // region process-death support

    private fun persist() {
        val s = _state.value
        if (s.game.isOver || s.phase == Phase.IDLE) {
            clearSaved()
        } else {
            savedState[KEY_ROWS] = s.game.rows.toIntArray()
            savedState[KEY_TURN] = s.game.currentPlayer.name
            savedState[KEY_MODE] = s.aiMode.name
        }
    }

    private fun clearSaved() {
        savedState.remove<IntArray>(KEY_ROWS)
        savedState.remove<String>(KEY_TURN)
        savedState.remove<String>(KEY_MODE)
    }

    private fun restoreFromSavedState() {
        val rows = savedState.get<IntArray>(KEY_ROWS)?.toList() ?: return
        val turn = savedState.get<String>(KEY_TURN)?.let { runCatching { Player.valueOf(it) }.getOrNull() }
        val mode = savedState.get<String>(KEY_MODE)?.let { runCatching { AiMode.valueOf(it) }.getOrNull() }
        val valid = rows.size == GameState.INITIAL_ROWS.size && rows.all { it >= 0 } &&
            rows.indices.all { rows[it] <= GameState.INITIAL_ROWS[it] } && rows.any { it > 0 }
        if (!valid || turn == null || mode == null) {
            clearSaved()
            return
        }
        val game = GameState(rows, turn)
        _state.value = GameUiState(
            game = game,
            board = StickBoard.fromCounts(rows),
            phase = if (turn == Player.HUMAN) Phase.HUMAN_TURN else Phase.AI_THINKING,
            aiMode = mode,
        )
        if (turn == Player.AI) flowJob = viewModelScope.launch { runAiTurn() }
    }

    // endregion

    private companion object {
        const val KEY_ROWS = "rows"
        const val KEY_TURN = "turn"
        const val KEY_MODE = "mode"
    }
}
