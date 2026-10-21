package com.example.laststick.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.laststick.AppContainer
import androidx.lifecycle.createSavedStateHandle

fun gameViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        GameViewModel(
            container.settings,
            container.stats,
            container.learning,
            createSavedStateHandle(),
        )
    }
}

fun settingsViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer { SettingsViewModel(container.settings, container.stats, container.learning) }
}

fun statsViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer { StatsViewModel(container.stats) }
}
