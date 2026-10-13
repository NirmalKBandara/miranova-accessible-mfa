package com.miranova.mfa

import com.miranova.mfa.ui.accessibility.ActionableError
import com.miranova.mfa.ui.accessibility.StatusSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating ActionableError models and secrecy guarantees.
 */
class ActionableErrorTest {

    @Test
    fun testTimeoutErrorProvidesSafeRecoveryAction() {
        val error = ActionableError.Timeout(timeoutSeconds = 60)
        assertEquals("Challenge Timed Out", error.title)
        assertTrue(error.explanation.contains("60 seconds"))
        assertEquals("Request New Challenge", error.primaryActionLabel)
        assertEquals("Dismiss", error.secondaryActionLabel)

        // Secrecy check: must not leak stack traces or secret keys
        assertFalse(error.explanation.contains("Exception"))
        assertFalse(error.explanation.contains("Key"))
    }

    @Test
    fun testNetworkErrorProvidesActionableRetry() {
        val error = ActionableError.Network("Server unavailable")
        assertEquals("Network Connection Error", error.title)
        assertEquals("Retry Connection", error.primaryActionLabel)
        assertTrue(error.explanation.contains("Server unavailable"))
    }

    @Test
    fun testKeyPermanentlyInvalidatedGuidesToReEnrollment() {
        val error = ActionableError.KeyPermanentlyInvalidated
        assertEquals("Security Alert: Hardware Key Invalidated", error.title)
        assertTrue(error.explanation.contains("Re-enrollment is required"))
        assertEquals("Start Device Re-Enrollment", error.primaryActionLabel)
    }

    @Test
    fun testAuthenticationDeniedReturnsHome() {
        val error = ActionableError.AuthenticationDenied
        assertEquals("Authentication Rejected", error.title)
        assertEquals("Return Home", error.primaryActionLabel)
    }

    @Test
    fun testStatusSeverityDefinitions() {
        val severities = StatusSeverity.entries
        assertEquals(4, severities.size)
        assertTrue(severities.contains(StatusSeverity.INFO))
        assertTrue(severities.contains(StatusSeverity.SUCCESS))
        assertTrue(severities.contains(StatusSeverity.WARNING))
        assertTrue(severities.contains(StatusSeverity.ERROR))
    }
}
