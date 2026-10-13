package com.miranova.mfa.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.miranova.mfa.ui.screens.AuthChallengeScreen
import com.miranova.mfa.ui.screens.EnrollmentScreen
import com.miranova.mfa.ui.screens.ErrorRecoveryScreen
import com.miranova.mfa.ui.screens.HomeScreen

/**
 * Accessible Navigation Graph managing route transitions and TalkBack context handoffs.
 */
@Composable
fun MiranovaNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Enrollment.route,
        modifier = modifier
    ) {
        composable(Screen.Enrollment.route) {
            EnrollmentScreen(
                onEnrollmentComplete = {
                    navController.navigate(Screen.HomeSession.route) {
                        popUpTo(Screen.Enrollment.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.HomeSession.route) {
            HomeScreen(
                onTriggerChallenge = {
                    navController.navigate(Screen.AuthChallenge.route)
                },
                onLogout = {
                    navController.navigate(Screen.Enrollment.route) {
                        popUpTo(Screen.HomeSession.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AuthChallenge.route) {
            AuthChallengeScreen(
                onApprove = {
                    navController.navigate(Screen.HomeSession.route) {
                        popUpTo(Screen.AuthChallenge.route) { inclusive = true }
                    }
                },
                onReject = {
                    navController.navigate(Screen.ErrorRecovery.route)
                }
            )
        }

        composable(Screen.ErrorRecovery.route) {
            ErrorRecoveryScreen(
                onRetry = {
                    navController.navigate(Screen.AuthChallenge.route) {
                        popUpTo(Screen.ErrorRecovery.route) { inclusive = true }
                    }
                },
                onReturnHome = {
                    navController.navigate(Screen.HomeSession.route) {
                        popUpTo(Screen.ErrorRecovery.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
