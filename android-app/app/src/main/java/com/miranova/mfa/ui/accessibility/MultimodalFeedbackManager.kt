package com.miranova.mfa.ui.accessibility

import android.content.Context

/**
 * Unified multimodal feedback coordinator combining tactile haptics, non-speech earcons,
 * and spoken TTS guidance.
 *
 * Fulfills:
 * - ACC-12 (Multimodal Confirmation)
 * - ACC-13 (Acoustic Non-Interference)
 * - ACC-14 (Environmental Degradation)
 * - Issue #53 (Accessible complementary channels, secrecy guard, user preferences)
 */
class MultimodalFeedbackManager(
    private val context: Context? = null,
    val haptics: HapticFeedbackManager = HapticFeedbackManager(context),
    val earcons: EarconManager = EarconManager(context),
    val tts: TtsManager = TtsManager(context),
    val preferencesStore: AccessibilityPreferencesStore = if (context != null) {
        SharedPreferencesAccessibilityPreferencesStore(context)
    } else {
        MemoryAccessibilityPreferencesStore()
    },
    private val audioInspector: SystemAudioInspector = SystemAudioInspector(context)
) {

    /**
     * Optional callback for transmitting semantic text equivalents of non-speech cues
     * to TalkBack or captioning displays.
     */
    var onSemanticFallbackAnnouncement: ((String) -> Unit)? = null

    init {
        syncPreferences()
    }

    /**
     * Synchronizes internal engine toggles with current user preferences.
     */
    fun syncPreferences() {
        val prefs = preferencesStore.getPreferences()
        haptics.isHapticsEnabled = prefs.isHapticEnabled
    }

    /**
     * Triggered when a new challenge is received by push or simulated arrival.
     */
    fun notifyChallengeArrived(spokenGuidance: String? = null) {
        val prefs = preferencesStore.getPreferences()
        if (prefs.isHapticEnabled) {
            haptics.trigger(HapticPattern.CHALLENGE_ARRIVED)
        }
        onSemanticFallbackAnnouncement?.invoke("Authentication challenge received")

        if (prefs.isTtsGuidanceEnabled && spokenGuidance != null) {
            speakSafeGuidance(spokenGuidance)
        }
    }

    /**
     * Triggered on successful factor completion and authentication approval.
     */
    fun notifyAuthSuccess(spokenMessage: String? = "Authentication approved") {
        val prefs = preferencesStore.getPreferences()
        if (prefs.isHapticEnabled) {
            haptics.trigger(HapticPattern.SUCCESS)
        }
        if (prefs.isAudioEarconsEnabled && !audioInspector.isSystemSilentOrVibrate()) {
            earcons.play(EarconType.AUTH_APPROVED)
        }
        onSemanticFallbackAnnouncement?.invoke("Authentication approved successfully")

        if (prefs.isTtsGuidanceEnabled && spokenMessage != null) {
            speakSafeGuidance(spokenMessage)
        }
    }

    /**
     * Triggered on device enrollment key generation success.
     */
    fun notifyEnrollmentReady(spokenMessage: String? = "Device enrollment ready") {
        val prefs = preferencesStore.getPreferences()
        if (prefs.isHapticEnabled) {
            haptics.trigger(HapticPattern.SUCCESS)
        }
        if (prefs.isAudioEarconsEnabled && !audioInspector.isSystemSilentOrVibrate()) {
            earcons.play(EarconType.ENROLLMENT_READY)
        }
        onSemanticFallbackAnnouncement?.invoke("Device registration complete")

        if (prefs.isTtsGuidanceEnabled && spokenMessage != null) {
            speakSafeGuidance(spokenMessage)
        }
    }

    /**
     * Triggered on authentication failure, rejection, or cryptographic tamper alert.
     */
    fun notifyAuthFailure(spokenMessage: String? = "Authentication declined") {
        val prefs = preferencesStore.getPreferences()
        if (prefs.isHapticEnabled) {
            haptics.trigger(HapticPattern.FAILURE)
        }
        if (prefs.isAudioEarconsEnabled && !audioInspector.isSystemSilentOrVibrate()) {
            earcons.play(EarconType.CHALLENGE_ABORTED)
        }
        onSemanticFallbackAnnouncement?.invoke("Authentication failed or declined")

        if (prefs.isTtsGuidanceEnabled && spokenMessage != null) {
            speakSafeGuidance(spokenMessage)
        }
    }

    /**
     * Triggered on challenge timeout expiration.
     */
    fun notifyTimeout(spokenMessage: String? = "Authentication request timed out") {
        val prefs = preferencesStore.getPreferences()
        if (prefs.isHapticEnabled) {
            haptics.trigger(HapticPattern.FAILURE)
        }
        if (prefs.isAudioEarconsEnabled && !audioInspector.isSystemSilentOrVibrate()) {
            earcons.play(EarconType.CHALLENGE_ABORTED)
        }
        onSemanticFallbackAnnouncement?.invoke("Challenge timed out")

        if (prefs.isTtsGuidanceEnabled && spokenMessage != null) {
            speakSafeGuidance(spokenMessage)
        }
    }

    /**
     * Triggered on countdown milestone (e.g., 10 seconds remaining) or character entry.
     */
    fun notifyTick() {
        val prefs = preferencesStore.getPreferences()
        if (prefs.isHapticEnabled) {
            haptics.trigger(HapticPattern.TICK)
        }
    }

    /**
     * Triggered on irreversible actions or critical recovery warnings.
     */
    fun notifyWarning(warningMessage: String? = null) {
        val prefs = preferencesStore.getPreferences()
        if (prefs.isHapticEnabled) {
            haptics.trigger(HapticPattern.WARNING)
        }
        onSemanticFallbackAnnouncement?.invoke(warningMessage ?: "Warning: Irreversible action")

        if (prefs.isTtsGuidanceEnabled && warningMessage != null) {
            speakSafeGuidance(warningMessage)
        }
    }

    /**
     * Triggered when progressing through multi-step setup wizards.
     */
    fun notifyNavigationStep(stepIndex: Int, totalSteps: Int, stepTitle: String) {
        val prefs = preferencesStore.getPreferences()
        if (prefs.isHapticEnabled) {
            haptics.trigger(HapticPattern.STEP_NAVIGATION)
        }
        val announcement = "Step $stepIndex of $totalSteps: $stepTitle"
        onSemanticFallbackAnnouncement?.invoke(announcement)

        if (prefs.isTtsGuidanceEnabled) {
            speakSafeGuidance(announcement)
        }
    }

    /**
     * Triggered when secrecy protection scrubbed sensitive cryptographic bytes from audio.
     */
    fun notifySecrecyAlert() {
        val prefs = preferencesStore.getPreferences()
        if (prefs.isHapticEnabled) {
            haptics.trigger(HapticPattern.SECRECY_ALERT)
        }
        if (prefs.isAudioEarconsEnabled && !audioInspector.isSystemSilentOrVibrate()) {
            earcons.play(EarconType.CHALLENGE_ABORTED)
        }
        onSemanticFallbackAnnouncement?.invoke("Privacy alert: Secret cryptographic output protected from audio disclosure")
    }

    /**
     * Speaks sanitized speech guidance adhering to secrecy protection rules.
     */
    fun speakSafeGuidance(text: String, bypassMuteCheck: Boolean = false): Boolean {
        val prefs = preferencesStore.getPreferences()
        if (!prefs.isTtsGuidanceEnabled) return false

        // Guard against secret disclosure
        if (tts.containsSecret(text)) {
            notifySecrecyAlert()
        }
        return tts.speak(text, bypassMuteCheck = bypassMuteCheck)
    }

    /**
     * Halts all audio sonification, TTS speech, and haptic vibrations.
     */
    fun stopAllFeedback() {
        haptics.cancel()
        tts.stop()
    }
}
