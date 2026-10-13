package com.miranova.mfa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.miranova.mfa.data.config.AppConfig
import com.miranova.mfa.data.network.NetworkResult
import com.miranova.mfa.data.repository.HealthRepository
import com.miranova.mfa.ui.accessibility.accessibleControl
import com.miranova.mfa.ui.accessibility.accessibleLiveRegion
import com.miranova.mfa.ui.accessibility.accessibleTouchTarget
import com.miranova.mfa.ui.accessibility.accessibleTraversalGroup
import com.miranova.mfa.ui.accessibility.talkBackHeading
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onTriggerChallenge: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    healthRepository: HealthRepository = remember { HealthRepository() }
) {
    val scope = rememberCoroutineScope()
    var healthStatus by remember { mutableStateOf("Backend status: Not checked") }

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
            modifier = Modifier.talkBackHeading(0f)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    onClickLabel = "Double tap to view and sign incoming challenge",
                    traversalIndex = 1f
                )
        ) {
            Text("Simulate Auth Challenge", style = MaterialTheme.typography.labelLarge)
        }

        OutlinedButton(
            onClick = {
                healthStatus = "Checking backend health at ${AppConfig.getHealthUrl()}..."
                scope.launch {
                    when (val result = healthRepository.checkHealth()) {
                        is NetworkResult.Success -> {
                            healthStatus = "Backend healthy: ${result.data.status} (service: ${result.data.service})"
                        }
                        is NetworkResult.HttpError -> {
                            healthStatus = "Backend HTTP error: code ${result.code}"
                        }
                        is NetworkResult.NetworkError -> {
                            healthStatus = "Network error reaching backend: ${result.exception.localizedMessage}"
                        }
                        is NetworkResult.Timeout -> {
                            healthStatus = "Backend request timed out after ${AppConfig.CONNECT_TIMEOUT_SECONDS}s."
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
                    label = "Check backend health endpoint",
                    onClickLabel = "Double tap to send ping to backend health check",
                    traversalIndex = 2f
                )
        ) {
            Text("Verify Backend Connection", style = MaterialTheme.typography.labelLarge)
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Logout and remove device key",
                    onClickLabel = "Double tap to clear local session and logout",
                    traversalIndex = 3f
                )
        ) {
            Text("Logout & Reset", style = MaterialTheme.typography.labelLarge)
        }

        Text(
            text = healthStatus,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.accessibleLiveRegion(polite = true)
        )
    }
}
