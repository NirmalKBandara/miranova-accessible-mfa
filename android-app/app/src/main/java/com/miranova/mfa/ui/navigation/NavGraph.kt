package com.miranova.mfa.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.miranova.mfa.crypto.AndroidKeyStoreManager
import com.miranova.mfa.data.repository.EnrollmentRepository
import com.miranova.mfa.data.store.SharedPreferencesEnrollmentStore
import com.miranova.mfa.ui.screens.AuthChallengeScreen
import com.miranova.mfa.ui.screens.EnrollmentScreen
import com.miranova.mfa.ui.screens.ErrorRecoveryScreen
import com.miranova.mfa.ui.screens.HomeScreen
import com.miranova.mfa.ui.screens.ReEnrollmentScreen
import com.miranova.mfa.ui.screens.SettingsScreen

/**
 * Accessible Navigation Graph managing route transitions and TalkBack context handoffs.
 */
@Composable
fun MiranovaNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    enrollmentRepository: EnrollmentRepository? = null
) {
    val context = LocalContext.current
    val repository = remember(enrollmentRepository) {
        enrollmentRepository ?: EnrollmentRepository(
            keyStoreManager = AndroidKeyStoreManager(),
            enrollmentStore = SharedPreferencesEnrollmentStore(context)
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Enrollment.route,
        modifier = modifier
    ) {
        composable(Screen.Enrollment.route) {
            EnrollmentScreen(
                enrollmentRepository = repository,
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
                onReEnroll = {
                    navController.navigate(Screen.ReEnrollment.route)
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLogout = {
                    navController.navigate(Screen.Enrollment.route) {
                        popUpTo(Screen.HomeSession.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ReEnrollment.route) {
            ReEnrollmentScreen(
                repository = repository,
                onReEnrollmentSuccess = {
                    navController.navigate(Screen.HomeSession.route) {
                        popUpTo(Screen.ReEnrollment.route) { inclusive = true }
                    }
                },
                onCancel = {
                    navController.popBackStack()
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

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
