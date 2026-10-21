package com.example.laststick.ui

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationAttributes
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Game haptics via the vibrator service. Compose's `HapticFeedback` is only a hint that many
 * devices ignore, and anything classed as touch feedback is dropped when the user turns "Touch
 * feedback" off, so explicit media/game vibrations are used. Needs the normal `VIBRATE` permission.
 */
class Haptics(context: Context) {
    private val vibrator: Vibrator? = try {
        val app = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: RuntimeException) {
        null
    }

    private val available: Boolean get() = vibrator?.hasVibrator() == true

    /** A light tick: another stick joined the selection. */
    fun tick() = play(
        predefined = VibrationEffect.EFFECT_TICK,
        fallbackMs = 14,
        fallbackAmplitude = 70,
    )

    /** A firmer click: the move was committed. */
    fun confirm() = play(
        predefined = VibrationEffect.EFFECT_HEAVY_CLICK,
        fallbackMs = 40,
        fallbackAmplitude = 180,
    )

    private fun play(predefined: Int, fallbackMs: Long, fallbackAmplitude: Int) {
        val v = vibrator?.takeIf { available } ?: return
        try {
            val effect = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> VibrationEffect.createPredefined(predefined)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                    VibrationEffect.createOneShot(fallbackMs, fallbackAmplitude)
                else -> null
            }
            when {
                // Gameplay haptics are "media" vibrations, not touch feedback: they follow the user's
                // media-vibration setting and are not dropped when "Touch feedback" is switched off.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && effect != null ->
                    v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
                effect != null -> @Suppress("DEPRECATION") v.vibrate(effect, gameAudioAttributes)
                else -> @Suppress("DEPRECATION") v.vibrate(fallbackMs, gameAudioAttributes)
            }
        } catch (e: RuntimeException) {
            // Vibration is a nicety; never let it crash a move.
        }
    }

    private val gameAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember(context.applicationContext) { Haptics(context) }
}
