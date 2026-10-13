package com.miranova.mfa.ui.accessibility

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager

/**
 * Data model capturing user customisation for complementary accessibility channels.
 */
data class AccessibilityPreferences(
    val isHapticEnabled: Boolean = true,
    val isAudioEarconsEnabled: Boolean = true,
    val isTtsGuidanceEnabled: Boolean = true,
    val speechRate: Float = 1.0f
)

/**
 * Interface contract for persisting and retrieving accessibility channel preferences.
 */
interface AccessibilityPreferencesStore {
    fun getPreferences(): AccessibilityPreferences
    fun updateHapticEnabled(enabled: Boolean)
    fun updateAudioEarconsEnabled(enabled: Boolean)
    fun updateTtsGuidanceEnabled(enabled: Boolean)
    fun updateSpeechRate(rate: Float)
    fun resetToDefaults()
}

/**
 * In-memory preferences store for fast JVM unit testing.
 */
class MemoryAccessibilityPreferencesStore(
    private var preferences: AccessibilityPreferences = AccessibilityPreferences()
) : AccessibilityPreferencesStore {
    override fun getPreferences(): AccessibilityPreferences = preferences

    override fun updateHapticEnabled(enabled: Boolean) {
        preferences = preferences.copy(isHapticEnabled = enabled)
    }

    override fun updateAudioEarconsEnabled(enabled: Boolean) {
        preferences = preferences.copy(isAudioEarconsEnabled = enabled)
    }

    override fun updateTtsGuidanceEnabled(enabled: Boolean) {
        preferences = preferences.copy(isTtsGuidanceEnabled = enabled)
    }

    override fun updateSpeechRate(rate: Float) {
        preferences = preferences.copy(speechRate = rate)
    }

    override fun resetToDefaults() {
        preferences = AccessibilityPreferences()
    }
}

/**
 * Android SharedPreferences implementation for on-device persistence.
 */
class SharedPreferencesAccessibilityPreferencesStore(
    context: Context
) : AccessibilityPreferencesStore {

    companion object {
        private const val PREFS_NAME = "miranova_accessibility_prefs"
        private const val KEY_HAPTIC = "pref_haptic_enabled"
        private const val KEY_EARCONS = "pref_earcons_enabled"
        private const val KEY_TTS = "pref_tts_enabled"
        private const val KEY_SPEECH_RATE = "pref_speech_rate"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getPreferences(): AccessibilityPreferences {
        return AccessibilityPreferences(
            isHapticEnabled = prefs.getBoolean(KEY_HAPTIC, true),
            isAudioEarconsEnabled = prefs.getBoolean(KEY_EARCONS, true),
            isTtsGuidanceEnabled = prefs.getBoolean(KEY_TTS, true),
            speechRate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        )
    }

    override fun updateHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
    }

    override fun updateAudioEarconsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EARCONS, enabled).apply()
    }

    override fun updateTtsGuidanceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS, enabled).apply()
    }

    override fun updateSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
    }

    override fun resetToDefaults() {
        prefs.edit()
            .putBoolean(KEY_HAPTIC, true)
            .putBoolean(KEY_EARCONS, true)
            .putBoolean(KEY_TTS, true)
            .putFloat(KEY_SPEECH_RATE, 1.0f)
            .apply()
    }
}

/**
 * System audio environment inspector checking hardware ringer mode and system volume.
 */
class SystemAudioInspector(private val context: Context? = null) {

    fun isSystemSilentOrVibrate(): Boolean {
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE
    }

    fun isNotificationVolumeZero(): Boolean {
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return try {
            audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0
        } catch (e: Exception) {
            false
        }
    }
}
