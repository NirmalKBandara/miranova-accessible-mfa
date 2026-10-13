package com.miranova.mfa.ui.accessibility

import android.content.Context

/**
 * Unified multimodal feedback coordinator combining tactile haptics and non-speech earcons.
 * Fulfills ACC-12 (Multimodal Confirmation), ACC-13 (Acoustic Non-Interference), and ACC-14 (Environmental Degradation).
 */
class MultimodalFeedbackManager(
    private val context: Context? = null,
    val haptics: HapticFeedbackManager = HapticFeedbackManager(context),
    val earcons: EarconManager = EarconManager(context)
) {

    /**
     * Triggered when a new challenge is received by push or simulated arrival.
     */
    fun notifyChallengeArrived() {
        haptics.trigger(HapticPattern.CHALLENGE_ARRIVED)
    }

    /**
     * Triggered on successful factor completion and authentication approval.
     */
    fun notifyAuthSuccess() {
        haptics.trigger(HapticPattern.SUCCESS)
        earcons.play(EarconType.AUTH_APPROVED)
    }

    /**
     * Triggered on device enrollment key generation success.
     */
    fun notifyEnrollmentReady() {
        haptics.trigger(HapticPattern.SUCCESS)
        earcons.play(EarconType.ENROLLMENT_READY)
    }

    /**
     * Triggered on authentication failure, rejection, or cryptographic tamper alert.
     */
    fun notifyAuthFailure() {
        haptics.trigger(HapticPattern.FAILURE)
        earcons.play(EarconType.CHALLENGE_ABORTED)
    }

    /**
     * Triggered on challenge timeout expiration.
     */
    fun notifyTimeout() {
        haptics.trigger(HapticPattern.FAILURE)
        earcons.play(EarconType.CHALLENGE_ABORTED)
    }

    /**
     * Triggered on countdown milestone (e.g., 10 seconds remaining) or character entry.
     */
    fun notifyTick() {
        haptics.trigger(HapticPattern.TICK)
    }
}
