package com.miranova.mfa.data.model

import com.miranova.mfa.ui.accessibility.ActionableError

/**
 * State hierarchy representing device enrollment lifecycle.
 * Complies with Issue #51 acceptance criteria for consistent local and server state.
 */
sealed class EnrollmentState {
    /** Device is not enrolled; ready to begin guided registration */
    data object Unenrolled : EnrollmentState()

    /** Generating non-exportable hardware key in Android Keystore */
    data class GeneratingKey(
        val username: String,
        val biometricRequired: Boolean
    ) : EnrollmentState()

    /** Sending public key and device metadata to backend /api/auth/register */
    data class RegisteringWithBackend(
        val username: String,
        val keyAlias: String
    ) : EnrollmentState()

    /** Device is actively enrolled and cryptographically bound to user identity */
    data class Enrolled(
        val userId: String,
        val username: String,
        val deviceId: String,
        val registeredAt: String,
        val keyAlias: String,
        val isBiometricProtected: Boolean
    ) : EnrollmentState()

    /** Enrollment failed; tracks if orphaned Keystore key was safely rolled back */
    data class EnrollmentFailed(
        val error: ActionableError,
        val isRollbackCompleted: Boolean
    ) : EnrollmentState()

    /** Key invalidated or device replaced; guided re-enrollment required */
    data class ReEnrollmentRequired(
        val reason: String,
        val previousKeyAlias: String? = null
    ) : EnrollmentState()
}
