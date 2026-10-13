package com.miranova.mfa

import com.miranova.mfa.ui.accessibility.EarconManager
import com.miranova.mfa.ui.accessibility.EarconType
import com.miranova.mfa.ui.accessibility.HapticFeedbackManager
import com.miranova.mfa.ui.accessibility.HapticPattern
import com.miranova.mfa.ui.accessibility.MultimodalFeedbackManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating Multimodal Feedback (Haptics & Earcons) complying with WCAG 2.2 and docs/05-accessibility-design.md §7.
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
    }
}
