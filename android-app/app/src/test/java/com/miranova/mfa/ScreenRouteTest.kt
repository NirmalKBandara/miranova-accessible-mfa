package com.miranova.mfa

import com.miranova.mfa.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRouteTest {

    @Test
    fun testScreenRoutesAreUnique() {
        val screens = listOf(
            Screen.Enrollment,
            Screen.AuthChallenge,
            Screen.HomeSession,
            Screen.ErrorRecovery,
            Screen.ReEnrollment,
            Screen.Settings
        )

        val routes = screens.map { it.route }
        assertEquals("Each screen route must be unique", routes.toSet().size, routes.size)

        for (screen in screens) {
            assertTrue("Route string must not be blank", screen.route.isNotBlank())
            assertTrue("Screen accessibility title must not be blank", screen.title.isNotBlank())
            assertFalse("Route must not contain spaces", screen.route.contains(" "))
        }
    }

    @Test
    fun testStartDestinationIsEnrollment() {
        assertEquals("enrollment", Screen.Enrollment.route)
        assertEquals("Device Enrollment", Screen.Enrollment.title)
    }
}
