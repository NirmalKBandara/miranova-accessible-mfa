package com.miranova.mfa.ui.accessibility

/**
 * Structured, actionable error models complying with docs/05-accessibility-design.md §5.3.
 *
 * Secrecy Protection Rule:
 * Never exposes raw cryptographic keys, stack traces, or internal server tokens in user messages.
 * Identifies the exact cause and the specific next safe recovery action.
 */
sealed class ActionableError(
    val title: String,
    val explanation: String,
    val primaryActionLabel: String,
    val secondaryActionLabel: String? = "Dismiss"
) {
    /** Scenario A: Challenge Expiration */
    data class Timeout(
        val timeoutSeconds: Int = 60
    ) : ActionableError(
        title = "Challenge Timed Out",
        explanation = "The authentication challenge expired after $timeoutSeconds seconds without signature. The request was aborted for your security.",
        primaryActionLabel = "Request New Challenge"
    )

    /** Scenario B: Network Failure / Backend Unavailable */
    data class Network(
        val details: String = "Server could not be reached. Verify your Wi-Fi or mobile data connection."
    ) : ActionableError(
        title = "Network Connection Error",
        explanation = details,
        primaryActionLabel = "Retry Connection"
    )

    /** Scenario C: Key Invalidation (Lock screen reset or biometric change) */
    data object KeyPermanentlyInvalidated : ActionableError(
        title = "Security Alert: Hardware Key Invalidated",
        explanation = "Your device hardware key was invalidated due to a device lock screen credential reset. Re-enrollment is required to restore access.",
        primaryActionLabel = "Start Device Re-Enrollment"
    )

    /** Authentication Rejection / Denial */
    data object AuthenticationDenied : ActionableError(
        title = "Authentication Rejected",
        explanation = "The authentication challenge was explicitly declined. No signature was generated.",
        primaryActionLabel = "Return Home"
    )

    /** Generic unexpected error with safe sanitized message */
    data class Generic(
        val safeMessage: String
    ) : ActionableError(
        title = "Authentication Notice",
        explanation = safeMessage,
        primaryActionLabel = "OK"
    )
}
