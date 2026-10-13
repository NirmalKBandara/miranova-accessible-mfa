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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.miranova.mfa.ui.accessibility.ActionableError
import com.miranova.mfa.ui.accessibility.accessibleControl
import com.miranova.mfa.ui.accessibility.accessibleLiveRegion
import com.miranova.mfa.ui.accessibility.accessibleMergedCard
import com.miranova.mfa.ui.accessibility.accessibleTouchTarget
import com.miranova.mfa.ui.accessibility.accessibleTraversalGroup
import com.miranova.mfa.ui.accessibility.talkBackHeading

/**
 * TalkBack-first Error Recovery screen adhering to docs/05-accessibility-design.md §5.3.
 *
 * Focus Placement & Secrecy Rule:
 * Initial focus is placed onto the error heading so the user immediately hears the diagnosis.
 * The error message explicitly states the next safe action without leaking secrets.
 */
@Composable
fun ErrorRecoveryScreen(
    error: ActionableError = ActionableError.Timeout(60),
    onRetry: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headingFocusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = error) {
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
            text = error.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .focusRequester(headingFocusRequester)
                .talkBackHeading(0f)
                .accessibleControl(
                    label = "Error Alert: ${error.title}, Heading",
                    traversalIndex = 0f
                )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleMergedCard(
                    description = "Error details: ${error.explanation}",
                    traversalIndex = 1f
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Actionable Guidance",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = error.primaryActionLabel,
                    onClickLabel = "Double tap to ${error.primaryActionLabel}",
                    traversalIndex = 2f
                )
        ) {
            Text(error.primaryActionLabel, style = MaterialTheme.typography.labelLarge)
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
                    onClickLabel = "Double tap to return to ready screen",
                    traversalIndex = 3f
                )
        ) {
            Text("Return to Ready Screen", style = MaterialTheme.typography.labelLarge)
        }

        Text(
            text = "Status: ${error.explanation}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.accessibleLiveRegion(polite = false) // Assertive delivery for error
        )
    }
}
