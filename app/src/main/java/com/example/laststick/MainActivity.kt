package com.example.laststick

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.laststick.data.Tutorial
import com.example.laststick.ui.ProvideAppLanguage
import com.example.laststick.ui.screens.GameScreen
import com.example.laststick.ui.screens.LeaderboardScreen
import com.example.laststick.ui.screens.MenuScreen
import com.example.laststick.ui.screens.SettingsScreen
import com.example.laststick.ui.theme.LastStickTheme
import com.example.laststick.viewmodel.GameViewModel
import com.example.laststick.viewmodel.SettingsViewModel
import com.example.laststick.viewmodel.StatsViewModel
import com.example.laststick.viewmodel.gameViewModelFactory
import com.example.laststick.viewmodel.settingsViewModelFactory
import com.example.laststick.viewmodel.statsViewModelFactory

private enum class Screen { MENU, GAME, LEADERBOARD, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val container = (application as LastStickApp).container
        setContent {
            LastStickTheme {
                LastStickRoot(container)
            }
        }
    }
}

@Composable
private fun LastStickRoot(container: AppContainer) {
    val gameViewModel: GameViewModel = viewModel(factory = gameViewModelFactory(container))
    val settingsViewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory(container))
    val statsViewModel: StatsViewModel = viewModel(factory = statsViewModelFactory(container))

    // A game restored after process death resumes straight on the game screen.
    var screen by rememberSaveable {
        mutableStateOf(if (gameViewModel.state.value.inProgress) Screen.GAME else Screen.MENU)
    }

    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val gameState by gameViewModel.state.collectAsStateWithLifecycle()

    // Wait for stored settings so we never flash the wrong language or a tutorial returning users saw.
    if (!settings.loaded) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
        return
    }

    ProvideAppLanguage(settings.language) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Crossfade(targetState = screen, label = "screen") { current ->
            when (current) {
                Screen.MENU -> MenuScreen(
                    perfectAi = settings.perfectAi,
                    onPerfectAiChange = settingsViewModel::setPerfectAi,
                    onPlay = {
                        gameViewModel.startNewGame()
                        screen = Screen.GAME
                    },
                    onLeaderboard = { screen = Screen.LEADERBOARD },
                    onSettings = { screen = Screen.SETTINGS },
                    showTutorial = !settings.menuTutorialDone,
                    onTutorialFinished = { settingsViewModel.markTutorialDone(Tutorial.MENU) },
                )

                Screen.GAME -> GameScreen(
                    viewModel = gameViewModel,
                    state = gameState,
                    haptics = settings.haptics,
                    showTutorial = !settings.gameTutorialDone,
                    onTutorialFinished = { settingsViewModel.markTutorialDone(Tutorial.GAME) },
                    onExitToMenu = { screen = Screen.MENU },
                )

                Screen.LEADERBOARD -> {
                    BackHandler { screen = Screen.MENU }
                    val stats by statsViewModel.stats.collectAsStateWithLifecycle()
                    LeaderboardScreen(stats = stats, onBack = { screen = Screen.MENU })
                }

                Screen.SETTINGS -> {
                    BackHandler { screen = Screen.MENU }
                    val learningInfo by settingsViewModel.learningInfo.collectAsStateWithLifecycle()
                    val training by settingsViewModel.training.collectAsStateWithLifecycle()
                    SettingsScreen(
                        settings = settings,
                        learningInfo = learningInfo,
                        training = training,
                        onPerfectAi = settingsViewModel::setPerfectAi,
                        onHaptics = settingsViewModel::setHaptics,
                        onCoinFlip = settingsViewModel::setCoinFlip,
                        onLanguage = settingsViewModel::setLanguage,
                        onReplayTutorial = settingsViewModel::replayTutorials,
                        showTutorial = !settings.settingsTutorialDone,
                        onTutorialFinished = { settingsViewModel.markTutorialDone(Tutorial.SETTINGS) },
                        onTrain = settingsViewModel::trainLearningAi,
                        onResetStats = settingsViewModel::resetStats,
                        onResetLearning = settingsViewModel::resetLearning,
                        onBack = { screen = Screen.MENU },
                    )
                }
            }
        }
    }
    }
}
