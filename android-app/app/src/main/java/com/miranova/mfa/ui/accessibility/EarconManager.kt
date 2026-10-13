package com.miranova.mfa.ui.accessibility

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.sin

/**
 * Standardized non-speech earcons specified in docs/05-accessibility-design.md §7.2.
 */
enum class EarconType(
    val earconId: String,
    val description: String
) {
    /** EAR-01: Ascending two-note chime (C5 -> G5, 120ms) indicating device enrollment key generated */
    ENROLLMENT_READY("EAR-01", "Ascending two-note chime for enrollment readiness"),

    /** EAR-02: Harmonious major chord chime (C5 - E5 - G5, 180ms) confirming authentication approval */
    AUTH_APPROVED("EAR-02", "Harmonious chord chime for authentication approval"),

    /** EAR-03: Descending minor tone (E5 -> B4, 200ms) signaling challenge expiry or abort */
    CHALLENGE_ABORTED("EAR-03", "Descending tone for challenge expiry or abort")
}

/**
 * Generates and plays brief, non-intrusive auditory cues (earcons < 250ms)
 * routed through USAGE_ASSISTANCE_SONIFICATION without ducking TalkBack speech.
 */
class EarconManager(
    private val context: Context? = null,
    var isAudioFeedbackEnabled: Boolean = true
) {

    companion object {
        private const val SAMPLE_RATE = 22050
        // Volume attenuated to -6 dB relative to system level to prevent masking TalkBack
        private const val ATTENUATED_AMPLITUDE = 0.5f
    }

    /**
     * Plays the requested earcon sound effect.
     */
    fun play(earcon: EarconType) {
        if (!isAudioFeedbackEnabled) return
        if (context != null) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null && audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                // Respect system mute / vibrate-only / DND settings
                return
            }
        }

        try {
            val pcmData = generatePcmForEarcon(earcon)
            playPcm(pcmData)
        } catch (e: Exception) {
            // Graceful degradation: never crash if audio output fails
        }
    }

    /**
     * Synthesizes short PCM audio buffers corresponding to the earcon frequency specs.
     */
    internal fun generatePcmForEarcon(earcon: EarconType): ShortArray {
        return when (earcon) {
            EarconType.ENROLLMENT_READY -> {
                // C5 (523 Hz) for 60ms, followed by G5 (784 Hz) for 60ms = 120ms total
                val numSamples = (SAMPLE_RATE * 0.12).toInt()
                val half = numSamples / 2
                val buffer = ShortArray(numSamples)
                for (i in 0 until half) {
                    val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / 523.25)
                    buffer[i] = (sin(angle) * Short.MAX_VALUE * ATTENUATED_AMPLITUDE).toInt().toShort()
                }
                for (i in half until numSamples) {
                    val angle = 2.0 * Math.PI * (i - half) / (SAMPLE_RATE / 783.99)
                    buffer[i] = (sin(angle) * Short.MAX_VALUE * ATTENUATED_AMPLITUDE).toInt().toShort()
                }
                buffer
            }

            EarconType.AUTH_APPROVED -> {
                // Major chord (C5 523Hz + E5 659Hz + G5 784Hz) for 180ms
                val numSamples = (SAMPLE_RATE * 0.18).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val sin1 = sin(2.0 * Math.PI * i / (SAMPLE_RATE / 523.25))
                    val sin2 = sin(2.0 * Math.PI * i / (SAMPLE_RATE / 659.25))
                    val sin3 = sin(2.0 * Math.PI * i / (SAMPLE_RATE / 783.99))
                    val mixed = (sin1 + sin2 + sin3) / 3.0
                    buffer[i] = (mixed * Short.MAX_VALUE * ATTENUATED_AMPLITUDE).toInt().toShort()
                }
                buffer
            }

            EarconType.CHALLENGE_ABORTED -> {
                // Descending tone: E5 (659 Hz) for 100ms -> B4 (494 Hz) for 100ms = 200ms total
                val numSamples = (SAMPLE_RATE * 0.20).toInt()
                val half = numSamples / 2
                val buffer = ShortArray(numSamples)
                for (i in 0 until half) {
                    val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / 659.25)
                    buffer[i] = (sin(angle) * Short.MAX_VALUE * ATTENUATED_AMPLITUDE).toInt().toShort()
                }
                for (i in half until numSamples) {
                    val angle = 2.0 * Math.PI * (i - half) / (SAMPLE_RATE / 493.88)
                    buffer[i] = (sin(angle) * Short.MAX_VALUE * ATTENUATED_AMPLITUDE).toInt().toShort()
                }
                buffer
            }
        }
    }

    private fun playPcm(pcmData: ShortArray) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val track = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(pcmData.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcmData, 0, pcmData.size)
        track.play()
    }
}
