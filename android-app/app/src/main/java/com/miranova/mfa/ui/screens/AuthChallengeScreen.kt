package com.miranova.mfa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.miranova.mfa.ui.accessibility.accessibleControl
import com.miranova.mfa.ui.accessibility.accessibleLiveRegion
import com.miranova.mfa.ui.accessibility.accessibleTouchTarget
import com.miranova.mfa.ui.accessibility.accessibleTraversalGroup
import com.miranova.mfa.ui.accessibility.talkBackHeading

@Composable
fun AuthChallengeScreen(
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var statusMessage by remember { mutableStateOf("Pending authentication request.") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .accessibleTraversalGroup(),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Authentication Challenge",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.talkBackHeading(0f)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleControl(
                    role = Role.Button,
                    label = "Challenge details: Internal Web Portal. Request ID 8831. Challenge expires in 50 seconds.",
                    traversalIndex = 1f
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Request from: Internal Web Portal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Session ID: #8831 | Expires in: 50s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    statusMessage = "Authentication rejected."
                    onReject()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .accessibleTouchTarget(minHeight = 56.dp)
                    .accessibleControl(
                        role = Role.Button,
                        label = "Reject authentication request",
                        onClickLabel = "Double tap to decline this authentication challenge",
                        traversalIndex = 3f
                    ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reject", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = {
                    statusMessage = "Signing challenge with Android Keystore. Please wait."
                    onApprove()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .accessibleTouchTarget(minHeight = 56.dp)
                    .accessibleControl(
                        role = Role.Button,
                        label = "Approve and sign authentication challenge",
                        onClickLabel = "Double tap to sign challenge and approve authentication",
                        traversalIndex = 2f
                    )
            ) {
                Text("Approve", style = MaterialTheme.typography.labelLarge)
            }
        }

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.accessibleLiveRegion(polite = true)
        )
    }
}
