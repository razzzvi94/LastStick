package com.example.laststick.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LastStickColors = darkColorScheme(
    primary = Amber,
    onPrimary = Night,
    primaryContainer = AmberDeep,
    onPrimaryContainer = Mist,
    secondary = Mint,
    onSecondary = Night,
    tertiary = Coral,
    onTertiary = Night,
    background = Night,
    onBackground = Mist,
    surface = NightRaised,
    onSurface = Mist,
    surfaceVariant = NightHigh,
    onSurfaceVariant = MistDim,
    surfaceContainer = NightRaised,
    surfaceContainerHigh = NightHigh,
    error = Danger,
    outline = MistDim,
)

/** LastStick always uses its own dark palette so the sticks keep a consistent look. */
@Composable
fun LastStickTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LastStickColors,
        typography = Typography,
        content = content,
    )
}
