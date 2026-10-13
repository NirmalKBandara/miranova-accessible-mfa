package com.miranova.mfa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.miranova.mfa.data.config.AppConfig
import com.miranova.mfa.data.network.NetworkResult
import com.miranova.mfa.data.repository.HealthRepository
import com.miranova.mfa.ui.accessibility.AccessibleStatusBanner
import com.miranova.mfa.ui.accessibility.StatusSeverity
import com.miranova.mfa.ui.accessibility.accessibleControl
import com.miranova.mfa.ui.accessibility.accessibleMergedCard
import com.miranova.mfa.ui.accessibility.accessibleTouchTarget
import com.miranova.mfa.ui.accessibility.accessibleTraversalGroup
import com.miranova.mfa.ui.accessibility.talkBackHeading
import kotlinx.coroutines.launch

/**
 * TalkBack-first Main Ready screen displaying device binding status and actions.
 * Complies with docs/05-accessibility-design.md §5 and WCAG 2.2 Level AA.
 */
@Composable
fun HomeScreen(
    onTriggerChallenge: () -> Unit,
    onLogout: () -> Unit,
    onReEnroll: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    healthRepository: HealthRepository = remember { HealthRepository() }
) {
    val scope = rememberCoroutineScope()
    val headingFocusRequester = remember { FocusRequester() }

    var healthStatus by remember { mutableStateOf("Backend status: Ready") }
    var healthSeverity by remember { mutableStateOf(StatusSeverity.INFO) }

    LaunchedEffect(key1 = Unit) {
        headingFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .accessibleTraversalGroup(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Miranova MFA Ready",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .focusRequester(headingFocusRequester)
                .talkBackHeading(0f)
                .accessibleControl(
                    label = "Miranova MFA Ready, Heading. Device authenticated and active.",
                    traversalIndex = 0f
                )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleMergedCard(
                    description = "Device status: Hardware bound and authenticated in Android Keystore. Connected backend: ${AppConfig.baseUrl}",
                    traversalIndex = 1f
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Device Bound & Authenticated",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configured Backend: ${AppConfig.baseUrl}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = onTriggerChallenge,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Simulate incoming authentication challenge",
                    onClickLabel = "Double tap to view and cryptographically sign incoming challenge",
                    traversalIndex = 2f
                )
        ) {
            Text("Simulate Auth Challenge", style = MaterialTheme.typography.labelLarge)
        }

        OutlinedButton(
            onClick = {
                healthStatus = "Checking backend health at ${AppConfig.getHealthUrl()}..."
                healthSeverity = StatusSeverity.INFO
                scope.launch {
                    when (val result = healthRepository.checkHealth()) {
                        is NetworkResult.Success -> {
                            healthStatus = "Backend healthy: ${result.data.status} (${result.data.service})"
                            healthSeverity = StatusSeverity.SUCCESS
                        }
                        is NetworkResult.HttpError -> {
                            healthStatus = "Backend HTTP error: code ${result.code}"
                            healthSeverity = StatusSeverity.WARNING
                        }
                        is NetworkResult.NetworkError -> {
                            healthStatus = "Network error reaching backend: ${result.exception.localizedMessage ?: "Connection refused"}"
                            healthSeverity = StatusSeverity.ERROR
                        }
                        is NetworkResult.Timeout -> {
                            healthStatus = "Backend request timed out after ${AppConfig.CONNECT_TIMEOUT_SECONDS}s."
                            healthSeverity = StatusSeverity.ERROR
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Check backend connection health",
                    onClickLabel = "Double tap to ping configured backend server",
                    traversalIndex = 3f
                )
        ) {
            Text("Check Backend Health", style = MaterialTheme.typography.labelLarge)
        }

        if (onReEnroll != null) {
            OutlinedButton(
                onClick = onReEnroll,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .accessibleTouchTarget(minHeight = 56.dp)
                    .accessibleControl(
                        role = Role.Button,
                        label = "Re-enroll device security credentials",
                        onClickLabel = "Double tap to replace or re-register device key",
                        traversalIndex = 4f
                    )
            ) {
                Text("Re-enroll Device", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (onOpenSettings != null) {
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .accessibleTouchTarget(minHeight = 56.dp)
                    .accessibleControl(
                        role = Role.Button,
                        label = "Open Accessibility Settings and Feedback Diagnostics",
                        onClickLabel = "Double tap to customize haptic and audio preferences",
                        traversalIndex = 4.5f
                    )
            ) {
                Text("Accessibility Settings", style = MaterialTheme.typography.labelLarge)
            }
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Log out and reset session",
                    onClickLabel = "Double tap to log out and clear device session",
                    traversalIndex = 4f
                ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Logout Session", style = MaterialTheme.typography.labelLarge)
        }

        AccessibleStatusBanner(
            message = healthStatus,
            severity = healthSeverity,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
