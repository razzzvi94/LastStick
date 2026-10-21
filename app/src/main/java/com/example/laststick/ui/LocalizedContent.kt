package com.example.laststick.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.example.laststick.data.AppLanguage
import java.util.Locale

/**
 * Re-provides the Compose locals that `stringResource` / `pluralStringResource` read, using a
 * context configured for [language], so the whole UI switches language instantly without
 * recreating the activity. [AppLanguage.SYSTEM] keeps the device language.
 */
@Composable
fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val localized = remember(base, language) { base.withLanguage(language) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        LocalResources provides localized.resources,
        content = content,
    )
}

private fun Context.withLanguage(language: AppLanguage): Context {
    val tag = language.tag ?: return this
    val config = Configuration(resources.configuration)
    val locale = Locale.forLanguageTag(tag)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return createConfigurationContext(config)
}
