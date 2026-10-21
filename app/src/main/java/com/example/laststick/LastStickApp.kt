package com.example.laststick

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.laststick.data.FileLearningStore
import com.example.laststick.data.LearningRepository
import com.example.laststick.data.SettingsRepository
import com.example.laststick.data.StatsRepository
import java.io.File

private val Context.prefsDataStore by preferencesDataStore(name = "laststick_prefs")

/** Minimal manual dependency container. */
class AppContainer(context: Context) {
    private val dataStore = context.applicationContext.prefsDataStore
    val settings = SettingsRepository(dataStore)
    val stats = StatsRepository(dataStore)
    val learning = LearningRepository(
        FileLearningStore(File(context.applicationContext.filesDir, "q_table.txt")),
    )
}

class LastStickApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
