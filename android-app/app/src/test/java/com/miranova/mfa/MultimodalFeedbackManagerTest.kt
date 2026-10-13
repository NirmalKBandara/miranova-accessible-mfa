package com.miranova.mfa

import com.miranova.mfa.ui.accessibility.AccessibilityPreferences
import com.miranova.mfa.ui.accessibility.EarconManager
import com.miranova.mfa.ui.accessibility.EarconType
import com.miranova.mfa.ui.accessibility.HapticFeedbackManager
import com.miranova.mfa.ui.accessibility.HapticPattern
import com.miranova.mfa.ui.accessibility.MemoryAccessibilityPreferencesStore
import com.miranova.mfa.ui.accessibility.MultimodalFeedbackManager
import com.miranova.mfa.ui.accessibility.TextToSpeechEngine
import com.miranova.mfa.ui.accessibility.TtsManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating Multimodal Feedback (Haptics, Earcons, & TTS) complying with WCAG 2.2,
 * docs/05-accessibility-design.md §7, and Issue #53 requirements.
 */
class MultimodalFeedbackManagerTest {

    @Test
    fun testHapticPatternWaveformSpecifications() {
        // HAP-01: Triple crisp tick
        assertEquals("HAP-01", HapticPattern.SUCCESS.patternId)
        assertArrayEquals(longArrayOf(0, 30, 40, 30, 40, 50), HapticPattern.SUCCESS.timings)
        assertArrayEquals(intArrayOf(0, 160, 0, 200, 0, 255), HapticPattern.SUCCESS.amplitudes)

        // HAP-02: Prominent double pulse
        assertEquals("HAP-02", HapticPattern.CHALLENGE_ARRIVED.patternId)
        assertArrayEquals(longArrayOf(0, 100, 80, 120), HapticPattern.CHALLENGE_ARRIVED.timings)
        assertArrayEquals(intArrayOf(0, 180, 0, 220), HapticPattern.CHALLENGE_ARRIVED.amplitudes)

        // HAP-03: Long heavy warning buzz
        assertEquals("HAP-03", HapticPattern.FAILURE.patternId)
        assertArrayEquals(longArrayOf(0, 250), HapticPattern.FAILURE.timings)
        assertArrayEquals(intArrayOf(0, 255), HapticPattern.FAILURE.amplitudes)

        // HAP-04: Subtle tick
        assertEquals("HAP-04", HapticPattern.TICK.patternId)
        assertArrayEquals(longArrayOf(0, 40), HapticPattern.TICK.timings)
        assertArrayEquals(intArrayOf(0, 140), HapticPattern.TICK.amplitudes)

        // HAP-05: Warning dual buzz
        assertEquals("HAP-05", HapticPattern.WARNING.patternId)
        assertArrayEquals(longArrayOf(0, 150, 100, 150), HapticPattern.WARNING.timings)
        assertArrayEquals(intArrayOf(0, 200, 0, 240), HapticPattern.WARNING.amplitudes)

        // HAP-06: Step Navigation step notch
        assertEquals("HAP-06", HapticPattern.STEP_NAVIGATION.patternId)
        assertArrayEquals(longArrayOf(0, 25), HapticPattern.STEP_NAVIGATION.timings)
        assertArrayEquals(intArrayOf(0, 180), HapticPattern.STEP_NAVIGATION.amplitudes)

        // HAP-07: Secrecy Privacy Alert triple buzz
        assertEquals("HAP-07", HapticPattern.SECRECY_ALERT.patternId)
        assertArrayEquals(longArrayOf(0, 80, 50, 80, 50, 120), HapticPattern.SECRECY_ALERT.timings)
        assertArrayEquals(intArrayOf(0, 255, 0, 255, 0, 255), HapticPattern.SECRECY_ALERT.amplitudes)
    }

    @Test
    fun testEarconPcmSynthesisBufferLengths() {
        val earconManager = EarconManager(context = null)

        // EAR-01: 120ms total at 22050 Hz = ~2646 samples
        val pcmEnroll = earconManager.generatePcmForEarcon(EarconType.ENROLLMENT_READY)
        assertNotNull(pcmEnroll)
        assertTrue(pcmEnroll.isNotEmpty())
        assertEquals((22050 * 0.12).toInt(), pcmEnroll.size)

        // EAR-02: 180ms total at 22050 Hz = ~3969 samples
        val pcmAuth = earconManager.generatePcmForEarcon(EarconType.AUTH_APPROVED)
        assertNotNull(pcmAuth)
        assertTrue(pcmAuth.isNotEmpty())
        assertEquals((22050 * 0.18).toInt(), pcmAuth.size)

        // EAR-03: 200ms total at 22050 Hz = ~4410 samples
        val pcmAbort = earconManager.generatePcmForEarcon(EarconType.CHALLENGE_ABORTED)
        assertNotNull(pcmAbort)
        assertTrue(pcmAbort.isNotEmpty())
        assertEquals((22050 * 0.20).toInt(), pcmAbort.size)
    }

    @Test
    fun testMultimodalFeedbackManagerGracefulDegradationOnJvm() {
        val haptics = HapticFeedbackManager(context = null, isHapticsEnabled = false)
        val earcons = EarconManager(context = null, isAudioFeedbackEnabled = false)
        val multimodal = MultimodalFeedbackManager(
            context = null,
            haptics = haptics,
            earcons = earcons
        )

        // Should execute silently without throwing NullPointerException or hardware exceptions
        multimodal.notifyChallengeArrived()
        multimodal.notifyAuthSuccess()
        multimodal.notifyEnrollmentReady()
        multimodal.notifyAuthFailure()
        multimodal.notifyTimeout()
        multimodal.notifyTick()
        multimodal.notifyWarning()
        multimodal.notifyNavigationStep(1, 3, "Overview")
        multimodal.notifySecrecyAlert()
        multimodal.stopAllFeedback()
    }

    @Test
    fun testSemanticFallbackAnnouncements() {
        val announcements = mutableListOf<String>()
        val multimodal = MultimodalFeedbackManager(context = null)
        multimodal.onSemanticFallbackAnnouncement = { announcements.add(it) }

        multimodal.notifyChallengeArrived()
        multimodal.notifyAuthSuccess()
        multimodal.notifyEnrollmentReady()
        multimodal.notifyAuthFailure()
        multimodal.notifyTimeout()
        multimodal.notifyWarning("Device reset warning")
        multimodal.notifyNavigationStep(2, 4, "Key Exchange")
        multimodal.notifySecrecyAlert()

        assertEquals(8, announcements.size)
        assertEquals("Authentication challenge received", announcements[0])
        assertEquals("Authentication approved successfully", announcements[1])
        assertEquals("Device registration complete", announcements[2])
        assertEquals("Authentication failed or declined", announcements[3])
        assertEquals("Challenge timed out", announcements[4])
        assertEquals("Device reset warning", announcements[5])
        assertEquals("Step 2 of 4: Key Exchange", announcements[6])
        assertTrue(announcements[7].contains("Privacy alert"))
    }

    @Test
    fun testUserPreferencesDisablingChannels() {
        val prefsStore = MemoryAccessibilityPreferencesStore(
            AccessibilityPreferences(
                isHapticEnabled = false,
                isAudioEarconsEnabled = false,
                isTtsGuidanceEnabled = false
            )
        )
        val spokenWords = mutableListOf<String>()
        val fakeEngine = object : TextToSpeechEngine {
            override fun isReady(): Boolean = true
            override fun speak(text: String, queueMode: Int): Boolean {
                spokenWords.add(text)
                return true
            }
            override fun stop() {}
            override fun shutdown() {}
        }
        val tts = TtsManager(context = null, externalEngine = fakeEngine)
        val multimodal = MultimodalFeedbackManager(
            context = null,
            tts = tts,
            preferencesStore = prefsStore
        )

        val success = multimodal.speakSafeGuidance("Testing disabled TTS")
        assertFalse("Safe guidance should return false when TTS is disabled in preferences", success)
        assertTrue("Spoken words list should remain empty", spokenWords.isEmpty())

        // Re-enable TTS in preferences
        prefsStore.updateTtsGuidanceEnabled(true)
        val successEnabled = multimodal.speakSafeGuidance("Testing enabled TTS", bypassMuteCheck = true)
        assertTrue("Safe guidance should return true when TTS is re-enabled", successEnabled)
        assertEquals(1, spokenWords.size)
        assertEquals("Testing enabled TTS", spokenWords[0])
    }

    @Test
    fun testSpeakSafeGuidanceTriggersSecrecyAlertOnSecretOutput() {
        val prefsStore = MemoryAccessibilityPreferencesStore(
            AccessibilityPreferences(
                isHapticEnabled = true,
                isAudioEarconsEnabled = true,
                isTtsGuidanceEnabled = true
            )
        )
        val announcements = mutableListOf<String>()
        val spokenWords = mutableListOf<String>()
        val fakeEngine = object : TextToSpeechEngine {
            override fun isReady(): Boolean = true
            override fun speak(text: String, queueMode: Int): Boolean {
                spokenWords.add(text)
                return true
            }
            override fun stop() {}
            override fun shutdown() {}
        }
        val tts = TtsManager(context = null, externalEngine = fakeEngine)
        val multimodal = MultimodalFeedbackManager(
            context = null,
            tts = tts,
            preferencesStore = prefsStore
        )
        multimodal.onSemanticFallbackAnnouncement = { announcements.add(it) }

        // Attempt to speak raw secret nonce: 32 hex chars
        val unsafeText = "Your approval nonce is 4a9f8b1c0e2d3f4a5b6c7d8e9f0a1b2c"
        val dispatched = multimodal.speakSafeGuidance(unsafeText, bypassMuteCheck = true)

        assertTrue(dispatched)
        // Verify privacy alert was triggered
        assertTrue(
            "Privacy alert should have been triggered in announcements",
            announcements.any { it.contains("Privacy alert") }
        )
        // Verify spoken word was sanitized: raw hex nonce scrubbed
        assertFalse(
            "Spoken words must not contain the raw hex nonce",
            spokenWords[0].contains("4a9f8b1c0e2d3f4a5b6c7d8e9f0a1b2c")
        )
        assertTrue(
            "Spoken words must contain [protected token]",
            spokenWords[0].contains("[protected token]")
        )
    }
}

