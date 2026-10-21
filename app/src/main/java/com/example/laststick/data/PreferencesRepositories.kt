package com.example.laststick.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private fun DataStore<Preferences>.safeData(): Flow<Preferences> =
    data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

private suspend fun DataStore<Preferences>.safeEdit(block: (MutablePreferences) -> Unit) {
    try {
        edit { block(it) }
    } catch (e: IOException) {
        // Persistence failed; the in-memory flow keeps the previous value. Nothing else to do.
    }
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val PERFECT_AI = booleanPreferencesKey("perfect_ai")
        val HAPTICS = booleanPreferencesKey("haptics")
        val COIN_FLIP = booleanPreferencesKey("coin_flip")
        val LANGUAGE = stringPreferencesKey("language")
        val MENU_TUTORIAL = booleanPreferencesKey("tutorial_menu_done")
        val GAME_TUTORIAL = booleanPreferencesKey("tutorial_game_done")
        val SETTINGS_TUTORIAL = booleanPreferencesKey("tutorial_settings_done")
    }

    val settings: Flow<AppSettings> = dataStore.safeData().map { p ->
        AppSettings(
            loaded = true,
            perfectAi = p[Keys.PERFECT_AI] ?: AppSettings().perfectAi,
            haptics = p[Keys.HAPTICS] ?: AppSettings().haptics,
            coinFlip = p[Keys.COIN_FLIP] ?: AppSettings().coinFlip,
            language = AppLanguage.fromName(p[Keys.LANGUAGE]),
            menuTutorialDone = p[Keys.MENU_TUTORIAL] ?: false,
            gameTutorialDone = p[Keys.GAME_TUTORIAL] ?: false,
            settingsTutorialDone = p[Keys.SETTINGS_TUTORIAL] ?: false,
        )
    }

    suspend fun setPerfectAi(enabled: Boolean) = dataStore.safeEdit { it[Keys.PERFECT_AI] = enabled }

    suspend fun setHaptics(enabled: Boolean) = dataStore.safeEdit { it[Keys.HAPTICS] = enabled }

    suspend fun setLanguage(language: AppLanguage) = dataStore.safeEdit { it[Keys.LANGUAGE] = language.name }

    suspend fun setTutorialDone(tutorial: Tutorial, done: Boolean) = dataStore.safeEdit {
        when (tutorial) {
            Tutorial.MENU -> it[Keys.MENU_TUTORIAL] = done
            Tutorial.GAME -> it[Keys.GAME_TUTORIAL] = done
            Tutorial.SETTINGS -> it[Keys.SETTINGS_TUTORIAL] = done
        }
    }

    suspend fun resetTutorials() = dataStore.safeEdit {
        it[Keys.MENU_TUTORIAL] = false
        it[Keys.GAME_TUTORIAL] = false
        it[Keys.SETTINGS_TUTORIAL] = false
    }

    suspend fun setCoinFlip(enabled: Boolean) = dataStore.safeEdit { it[Keys.COIN_FLIP] = enabled }
}

class StatsRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val PLAYED = intPreferencesKey("stats_played")
        val HUMAN_WINS = intPreferencesKey("stats_human_wins")
        val AI_WINS = intPreferencesKey("stats_ai_wins")
        val STREAK = intPreferencesKey("stats_streak")
        val BEST_STREAK = intPreferencesKey("stats_best_streak")
        val RECENT = stringPreferencesKey("stats_recent")
    }

    val stats: Flow<GameStats> = dataStore.safeData().map { it.toStats() }

    /** Atomically records one finished game. */
    suspend fun recordResult(humanWon: Boolean) = dataStore.safeEdit { prefs ->
        prefs.toStats().record(humanWon).writeTo(prefs)
    }

    suspend fun reset() = dataStore.safeEdit { prefs ->
        GameStats().writeTo(prefs)
    }

    private fun Preferences.toStats() = GameStats(
        played = (this[Keys.PLAYED] ?: 0).coerceAtLeast(0),
        humanWins = (this[Keys.HUMAN_WINS] ?: 0).coerceAtLeast(0),
        aiWins = (this[Keys.AI_WINS] ?: 0).coerceAtLeast(0),
        currentStreak = this[Keys.STREAK] ?: 0,
        bestStreak = (this[Keys.BEST_STREAK] ?: 0).coerceAtLeast(0),
        recent = GameStats.decodeRecent(this[Keys.RECENT] ?: ""),
    )

    private fun GameStats.writeTo(prefs: MutablePreferences) {
        prefs[Keys.PLAYED] = played
        prefs[Keys.HUMAN_WINS] = humanWins
        prefs[Keys.AI_WINS] = aiWins
        prefs[Keys.STREAK] = currentStreak
        prefs[Keys.BEST_STREAK] = bestStreak
        prefs[Keys.RECENT] = GameStats.encodeRecent(recent)
    }
}
