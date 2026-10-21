package com.example.laststick.data

/** In-app language. [tag] is a BCP-47 language tag, or `null` to follow the system language. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    ROMANIAN("ro");

    companion object {
        fun fromName(name: String?): AppLanguage = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/** Which first-run tutorial a flag refers to. */
enum class Tutorial { MENU, GAME, SETTINGS }

data class AppSettings(
    /** False until DataStore has delivered the stored values (avoids flashing defaults). */
    val loaded: Boolean = false,
    val perfectAi: Boolean = false,
    val haptics: Boolean = true,
    val coinFlip: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val menuTutorialDone: Boolean = false,
    val gameTutorialDone: Boolean = false,
    val settingsTutorialDone: Boolean = false,
) {}
