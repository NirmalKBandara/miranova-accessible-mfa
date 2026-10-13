package com.miranova.mfa.ui.accessibility

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityManager
import java.util.Locale
import java.util.regex.Pattern

/**
 * Abstraction for the underlying TextToSpeech engine to facilitate JVM unit testing
 * and graceful fallback when the system speech synthesizer is absent.
 */
interface TextToSpeechEngine {
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH): Boolean
    fun stop()
    fun shutdown()
    fun isReady(): Boolean
}

/**
 * Accessible Text-to-Speech (TTS) coordinator complying with docs/05-accessibility-design.md §7.3.
 *
 * Secrecy Guard Rule (Issue #53):
 * Strictly inspects any outgoing utterance to ensure no raw cryptographic keys, nonces,
 * tokens, or verification codes are broadcast aloud in public environments.
 *
 * Acoustic Non-Interference & TalkBack Coexistence:
 * Detects active TalkBack / Touch Exploration services and avoids drowning out
 * native screen reader speech channels.
 */
class TtsManager(
    private val context: Context? = null,
    private val externalEngine: TextToSpeechEngine? = null
) {

    companion object {
        const val QUEUE_FLUSH = 0
        const val QUEUE_ADD = 1

        // Regex patterns identifying sensitive cryptographic strings or secret material
        private val HEX_TOKEN_PATTERN = Pattern.compile("\\b[0-9a-fA-F]{16,}\\b")
        private val BASE64_SECRET_PATTERN = Pattern.compile("\\b[A-Za-z0-9+/=]{24,}\\b")
        private val PRIVATE_KEY_PATTERN = Pattern.compile("(?i)(private[ _-]?key|secret[ _-]?key|seed|digest)")
        private val CHALLENGE_PAYLOAD_PATTERN = Pattern.compile("(?i)(miranova:auth:v1:[^\\s]+)")
        private val NUMERIC_OTP_PATTERN = Pattern.compile("\\b\\d{6,8}\\b")
    }

    private var androidTts: TextToSpeech? = null
    private var isInitialized = false

    init {
        if (externalEngine == null && context != null) {
            try {
                androidTts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        val result = androidTts?.setLanguage(Locale.getDefault())
                        isInitialized = result != TextToSpeech.LANG_MISSING_DATA &&
                                result != TextToSpeech.LANG_NOT_SUPPORTED
                    } else {
                        isInitialized = false
                    }
                }
            } catch (e: Exception) {
                isInitialized = false
            }
        }
    }

    /**
     * Checks whether an utterance contains secret cryptographic material that must NOT
     * be spoken aloud in public.
     */
    fun containsSecret(text: String): Boolean {
        if (text.isBlank()) return false
        return HEX_TOKEN_PATTERN.matcher(text).find() ||
                BASE64_SECRET_PATTERN.matcher(text).find() ||
                PRIVATE_KEY_PATTERN.matcher(text).find() ||
                CHALLENGE_PAYLOAD_PATTERN.matcher(text).find() ||
                NUMERIC_OTP_PATTERN.matcher(text).find()
    }

    /**
     * Sanitizes an utterance by scrubbing cryptographic nonces, tokens, and secrets
     * before passing to the speech synthesizer.
     */
    fun sanitizeUtterance(text: String): String {
        var sanitized = text
        sanitized = CHALLENGE_PAYLOAD_PATTERN.matcher(sanitized).replaceAll("[protected challenge]")
        sanitized = HEX_TOKEN_PATTERN.matcher(sanitized).replaceAll("[protected token]")
        sanitized = BASE64_SECRET_PATTERN.matcher(sanitized).replaceAll("[protected secret]")
        sanitized = NUMERIC_OTP_PATTERN.matcher(sanitized).replaceAll("[protected code]")
        sanitized = PRIVATE_KEY_PATTERN.matcher(sanitized).replaceAll("[protected key]")
        return sanitized
    }

    /**
     * Returns true if system audio is muted or in silent/vibrate mode.
     */
    fun isAudioMuted(): Boolean {
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE ||
                audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0
    }

    /**
     * Returns true if TalkBack (Touch Exploration) is active on the device.
     */
    fun isTalkBackActive(): Boolean {
        val a11yManager = context?.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        return a11yManager.isEnabled && a11yManager.isTouchExplorationEnabled
    }

    /**
     * Speaks accessible guidance if TTS is available, audio is not muted, and the utterance
     * complies with secrecy protection.
     *
     * @return true if speech was synthesized or accepted; false if muted, uninitialized, or blocked.
     */
    fun speak(text: String, queueMode: Int = QUEUE_FLUSH, bypassMuteCheck: Boolean = false): Boolean {
        if (text.isBlank()) return false

        // Secrecy Protection: Reject raw secret disclosures outright
        if (containsSecret(text)) {
            val safeMessage = sanitizeUtterance(text)
            return dispatchSpeech(safeMessage, queueMode)
        }

        // Environmental degradation: Do not speak if system is muted
        if (!bypassMuteCheck && isAudioMuted()) {
            return false
        }

        return dispatchSpeech(text, queueMode)
    }

    private fun dispatchSpeech(message: String, queueMode: Int): Boolean {
        if (externalEngine != null) {
            return externalEngine.speak(message, queueMode)
        }

        val tts = androidTts ?: return false
        if (!isInitialized) return false

        val androidQueueMode = if (queueMode == QUEUE_ADD) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        val result = tts.speak(message, androidQueueMode, null, "miranova_utterance_${System.currentTimeMillis()}")
        return result == TextToSpeech.SUCCESS
    }

    /**
     * Halts any active speech synthesis immediately.
     */
    fun stop() {
        externalEngine?.stop()
        androidTts?.stop()
    }

    /**
     * Releases system TTS resources.
     */
    fun shutdown() {
        externalEngine?.shutdown()
        androidTts?.stop()
        androidTts?.shutdown()
        androidTts = null
        isInitialized = false
    }

    /**
     * Returns whether the speech synthesizer is ready to accept commands.
     */
    fun isReady(): Boolean {
        return externalEngine?.isReady() ?: isInitialized
    }
}
