package com.miranova.mfa

import com.miranova.mfa.ui.accessibility.EnrollmentStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test validating multi-step accessible guided enrollment wizard models,
 * TalkBack announcements, 1-based indexing, and state representations.
 */
class EnrollmentStepTest {

    @Test
    fun `step progression and total count conform to 3-step wizard specification`() {
        assertEquals(3, EnrollmentStep.entries.size)
        assertEquals(EnrollmentStep.IDENTITY, EnrollmentStep.entries[0])
        assertEquals(EnrollmentStep.SECURITY_POLICY, EnrollmentStep.entries[1])
        assertEquals(EnrollmentStep.CONFIRMATION, EnrollmentStep.entries[2])
    }

    @Test
    fun `1-based step indexing is consistent for screen reader accessibility`() {
        assertEquals(1, EnrollmentStep.IDENTITY.stepIndex)
        assertEquals(2, EnrollmentStep.SECURITY_POLICY.stepIndex)
        assertEquals(3, EnrollmentStep.CONFIRMATION.stepIndex)

        EnrollmentStep.entries.forEach { step ->
            assertEquals(3, step.totalSteps)
            assertTrue(step.stepIndex in 1..step.totalSteps)
        }
    }

    @Test
    fun `accessibleAnnouncement formats unified title and step ratio`() {
        assertEquals("Step 1 of 3: Account Identity", EnrollmentStep.IDENTITY.accessibleAnnouncement)
        assertEquals("Step 2 of 3: Biometric Protection Preference", EnrollmentStep.SECURITY_POLICY.accessibleAnnouncement)
        assertEquals("Step 3 of 3: Generate Key and Register", EnrollmentStep.CONFIRMATION.accessibleAnnouncement)
    }

    @Test
    fun `step instructions provide clear non-technical guidance without jargon`() {
        EnrollmentStep.entries.forEach { step ->
            assertTrue(step.stepInstructions.isNotBlank())
            assertFalse(step.stepInstructions.contains("null"))
        }
        assertTrue(EnrollmentStep.IDENTITY.stepInstructions.contains("username"))
        assertTrue(EnrollmentStep.SECURITY_POLICY.stepInstructions.contains("biometric"))
    }
}
