package com.miranova.mfa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.miranova.mfa.ui.accessibility.accessibleControl
import com.miranova.mfa.ui.accessibility.accessibleTouchTarget
import com.miranova.mfa.ui.accessibility.accessibleTraversalGroup
import com.miranova.mfa.ui.accessibility.talkBackHeading

@Composable
fun ErrorRecoveryScreen(
    errorMessage: String = "Authentication challenge timed out after 60 seconds.",
    onRetry: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .accessibleTraversalGroup(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error Recovery",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.talkBackHeading(0f)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleControl(
                    role = Role.Button,
                    label = "Error details: $errorMessage",
                    traversalIndex = 1f
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Authentication Alert",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Retry authentication request",
                    onClickLabel = "Double tap to request a fresh challenge",
                    traversalIndex = 2f
                )
        ) {
            Text("Request New Challenge", style = MaterialTheme.typography.labelLarge)
        }

        OutlinedButton(
            onClick = onReturnHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Dismiss error and return home",
                    onClickLabel = "Double tap to return to main screen",
                    traversalIndex = 3f
                )
        ) {
            Text("Return to Ready Screen", style = MaterialTheme.typography.labelLarge)
        }
    }
}
