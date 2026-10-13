package com.miranova.mfa.ui.navigation

/**
 * Standard navigation routes for the Miranova MFA application.
 */
sealed class Screen(val route: String, val title: String) {
    data object Enrollment : Screen("enrollment", "Device Enrollment")
    data object AuthChallenge : Screen("auth_challenge", "Authentication Challenge")
    data object HomeSession : Screen("home_session", "Session Active")
    data object ErrorRecovery : Screen("error_recovery", "Error Recovery")
    data object Settings : Screen("settings", "Settings & Diagnostics")
}
