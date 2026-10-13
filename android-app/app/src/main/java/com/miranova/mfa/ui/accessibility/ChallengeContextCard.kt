package com.miranova.mfa.ui.accessibility

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Accessible Challenge Context Card adhering to docs/05-accessibility-design.md §5.2.
 *
 * Merged Semantics:
 * Merges descendant elements into a single coherent TalkBack item so users hear the full context
 * in one clean announcement rather than fragmented snippets.
 *
 * Milestone Timer Policy:
 * To avoid overwhelming screen reader users by speaking every single second, remaining time
 * is announced only at discrete milestones (arrival, 30s, 10s) via a polite live region,
 * and triggers a tactile nudge tick at 10s.
 */
@Composable
fun ChallengeContextCard(
    serviceOrigin: String,
    sessionNonce: String,
    initialTimeoutSeconds: Int = 60,
    onTimeout: () -> Unit = {},
    onMilestoneTick: () -> Unit = {},
    modifier: Modifier = Modifier,
    traversalIndex: Float = 1f
) {
    var secondsRemaining by remember { mutableIntStateOf(initialTimeoutSeconds) }
    var milestoneAnnouncement by remember {
        mutableStateOf("Authentication challenge received from $serviceOrigin. Expires in $initialTimeoutSeconds seconds.")
    }

    LaunchedEffect(key1 = sessionNonce) {
        secondsRemaining = initialTimeoutSeconds
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--

            when (secondsRemaining) {
                30 -> {
                    milestoneAnnouncement = "Notice: 30 seconds remaining to approve authentication challenge."
                }
                10 -> {
                    milestoneAnnouncement = "Warning: 10 seconds remaining before authentication challenge expires."
                    onMilestoneTick()
                }
                0 -> {
                    milestoneAnnouncement = "Authentication challenge expired after $initialTimeoutSeconds seconds."
                    onTimeout()
                }
            }
        }
    }

    val semanticSummary = "Authentication request from $serviceOrigin. Request ID $sessionNonce. Challenge expires in $secondsRemaining seconds."

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleMergedCard(
                    description = semanticSummary,
                    role = Role.Button,
                    traversalIndex = traversalIndex
                ),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(
                width = 1.dp,
                color = if (secondsRemaining <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Origin: $serviceOrigin",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Challenge Nonce: ${sessionNonce.take(16)}...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Time Remaining:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${secondsRemaining}s",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (secondsRemaining <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Decoupled polite live region for milestone announcements (avoids 1s spam)
        Text(
            text = milestoneAnnouncement,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .padding(top = 8.dp)
                .accessibleLiveRegion(polite = true)
        )
    }
}
