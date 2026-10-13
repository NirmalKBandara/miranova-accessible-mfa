package com.miranova.mfa.ui.accessibility

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manages tactile vibration waveforms complying with WCAG 2.2 and docs/05-accessibility-design.md §7.1.
 * Supports graceful degradation (ACC-14) if hardware vibrator is missing, muted, or permission-restricted.
 */
class HapticFeedbackManager(
    private val context: Context? = null,
    var isHapticsEnabled: Boolean = true
) {

    private val vibrator: Vibrator? by lazy {
        context?.let { ctx ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Checks if physical vibration hardware exists on this device.
     */
    fun hasVibrator(): Boolean {
        return try {
            vibrator?.hasVibrator() == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if linear resonant amplitude control is supported by the hardware.
     */
    fun hasAmplitudeControl(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.hasAmplitudeControl() == true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Triggers the specified tactile pattern.
     */
    fun trigger(pattern: HapticPattern) {
        if (!isHapticsEnabled) return
        val vib = vibrator ?: return

        try {
            if (!vib.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (vib.hasAmplitudeControl() && pattern.amplitudes.size == pattern.timings.size) {
                    VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1)
                } else {
                    // Fallback to timing-only waveform for devices without variable amplitude LRA
                    val totalDuration = pattern.timings.sum().coerceAtLeast(40L)
                    VibrationEffect.createOneShot(totalDuration.coerceAtMost(500L), VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern.timings, -1)
            }
        } catch (e: Exception) {
            // Graceful degradation: never crash if vibration fails or permissions restricted
        }
    }

    /**
     * Cancels any active ongoing vibration effect.
     */
    fun cancel() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            // Graceful degradation
        }
    }
}
