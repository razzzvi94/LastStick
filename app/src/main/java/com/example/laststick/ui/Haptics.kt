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

    /**
     * True for plain on/off motors (typical of tablets and budget devices): no amplitude control and
     * no built-in effects. A 25 ms test pulse is faintly perceptible on such a tablet, but the 22/45 ms
     * game pulses went unnoticed in the middle of a swipe, so these devices get longer, spaced-out
     * pulses that are clearly felt (confirmed by hand on a Galaxy Tab S5e).
     */
    private val basicMotor: Boolean by lazy {
        val v = vibrator ?: return@lazy false
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                !v.hasAmplitudeControl() && !supportsPredefined(v, VibrationEffect.EFFECT_TICK)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> !v.hasAmplitudeControl()
            else -> true
        }
    }

    /** Basic motors: when the pulse in progress ends, so that rapid ticks do not restart it each time. */
    private var busyUntilMs = 0L

    /** A light tick: another stick joined the selection. */
    fun tick() {
        if (basicMotor) {
            val now = System.currentTimeMillis()
            if (now < busyUntilMs) return // already buzzing for the swipe in progress
            busyUntilMs = now + BASIC_TICK_SPACING_MS
            play(VibrationEffect.EFFECT_TICK, BASIC_TICK_MS, 255)
        } else {
            play(VibrationEffect.EFFECT_TICK, 22, 90)
        }
    }

    /** A firmer click: the move was committed. */
    fun confirm() {
        if (basicMotor) {
            busyUntilMs = System.currentTimeMillis() + BASIC_CONFIRM_MS
            play(VibrationEffect.EFFECT_HEAVY_CLICK, BASIC_CONFIRM_MS, 255)
        } else {
            play(VibrationEffect.EFFECT_HEAVY_CLICK, 45, 200)
        }
    }

    private companion object {
        const val BASIC_TICK_MS = 45L
        const val BASIC_CONFIRM_MS = 80L

        /** Minimum gap between selection ticks on basic motors, so a fast swipe does not turn into a buzz. */
        const val BASIC_TICK_SPACING_MS = 120L
    }

    private fun play(predefined: Int, fallbackMs: Long, fallbackAmplitude: Int) {
        val v = vibrator?.takeIf { available } ?: return
        try {
            val effect = when {
                // Predefined TICK/CLICK effects only work if the vibration hardware has them. Many tablets
                // and budget devices have a plain on/off motor and silently drop them, so ask first.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsPredefined(v, predefined) ->
                    VibrationEffect.createPredefined(predefined)
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> // cannot be queried before API 30
                    VibrationEffect.createPredefined(predefined)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                    VibrationEffect.createOneShot(
                        fallbackMs,
                        if (v.hasAmplitudeControl()) fallbackAmplitude else VibrationEffect.DEFAULT_AMPLITUDE,
                    )
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

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private fun supportsPredefined(v: Vibrator, effectId: Int): Boolean =
        v.areAllEffectsSupported(effectId) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES

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
