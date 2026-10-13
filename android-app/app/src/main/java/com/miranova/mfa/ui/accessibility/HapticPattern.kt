package com.miranova.mfa.ui.accessibility

/**
 * Standardized tactile vibration patterns defined in docs/05-accessibility-design.md §7.1.
 *
 * Extended in Issue #53 for discrete complementary feedback across authentication,
 * step navigation, irreversible warnings, and secrecy protection alerts.
 */
enum class HapticPattern(
    val patternId: String,
    val timings: LongArray,
    val amplitudes: IntArray,
    val description: String
) {
    /**
     * HAP-01: Triple crisp tick with crescendo confirming factor completion / authentication approval.
     */
    SUCCESS(
        patternId = "HAP-01",
        timings = longArrayOf(0, 30, 40, 30, 40, 50),
        amplitudes = intArrayOf(0, 160, 0, 200, 0, 255),
        description = "Triple crisp tick confirming approval"
    ),

    /**
     * HAP-02: Prominent double pulse indicating incoming authentication challenge push.
     */
    CHALLENGE_ARRIVED(
        patternId = "HAP-02",
        timings = longArrayOf(0, 100, 80, 120),
        amplitudes = intArrayOf(0, 180, 0, 220),
        description = "Prominent double pulse for incoming challenge"
    ),

    /**
     * HAP-03: Heavy single buzz signaling authentication failure, timeout, or security denial.
     */
    FAILURE(
        patternId = "HAP-03",
        timings = longArrayOf(0, 250),
        amplitudes = intArrayOf(0, 255),
        description = "Heavy buzz signaling rejection or failure"
    ),

    /**
     * HAP-04: Subtle tactile tick for countdown milestone (10s) or silent character entry tap.
     */
    TICK(
        patternId = "HAP-04",
        timings = longArrayOf(0, 40),
        amplitudes = intArrayOf(0, 140),
        description = "Subtle tick for timer milestone or input feedback"
    ),

    /**
     * HAP-05: Pulsing cautionary double buzz for irreversible re-enrollment warnings.
     */
    WARNING(
        patternId = "HAP-05",
        timings = longArrayOf(0, 150, 100, 150),
        amplitudes = intArrayOf(0, 200, 0, 240),
        description = "Cautionary pulsing buzz for irreversible actions or alerts"
    ),

    /**
     * HAP-06: Crisp single bump when progressing between guided setup steps.
     */
    STEP_NAVIGATION(
        patternId = "HAP-06",
        timings = longArrayOf(0, 25),
        amplitudes = intArrayOf(0, 180),
        description = "Crisp step transition bump"
    ),

    /**
     * HAP-07: Staccato alarm pulse signaling secrecy protection interception.
     */
    SECRECY_ALERT(
        patternId = "HAP-07",
        timings = longArrayOf(0, 80, 50, 80, 50, 120),
        amplitudes = intArrayOf(0, 255, 0, 255, 0, 255),
        description = "Firm staccato alarm pulse for privacy and secrecy protection"
    )
}
