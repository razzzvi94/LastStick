package com.example.laststick.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laststick.data.AppLanguage
import com.example.laststick.data.AppSettings
import com.example.laststick.data.Tutorial
import com.example.laststick.data.GameStats
import com.example.laststick.data.LearningInfo
import com.example.laststick.data.LearningRepository
import com.example.laststick.data.SettingsRepository
import com.example.laststick.data.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val statsRepo: StatsRepository,
    private val learningRepo: LearningRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val learningInfo: StateFlow<LearningInfo?> = learningRepo.info

    private val _training = MutableStateFlow(false)
    val training: StateFlow<Boolean> = _training.asStateFlow()

    init {
        // Make sure the learning info is available for display.
        viewModelScope.launch { learningRepo.agent() }
    }

    fun setPerfectAi(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setPerfectAi(enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepo.setLanguage(language) }
    }

    fun markTutorialDone(tutorial: Tutorial) {
        viewModelScope.launch { settingsRepo.setTutorialDone(tutorial, true) }
    }

    fun replayTutorials() {
        viewModelScope.launch { settingsRepo.resetTutorials() }
    }

    fun setCoinFlip(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setCoinFlip(enabled) }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setHaptics(enabled) }
    }

    fun resetStats() {
        viewModelScope.launch { statsRepo.reset() }
    }

    fun resetLearning() {
        viewModelScope.launch { learningRepo.reset() }
    }

    fun trainLearningAi(games: Int) {
        if (_training.value) return
        viewModelScope.launch {
            _training.value = true
            try {
                learningRepo.trainSelfPlay(games)
            } finally {
                _training.value = false
            }
        }
    }
}

class StatsViewModel(statsRepo: StatsRepository) : ViewModel() {
    val stats: StateFlow<GameStats> = statsRepo.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameStats())
}
