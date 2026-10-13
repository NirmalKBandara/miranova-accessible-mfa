package com.miranova.mfa.ui.accessibility

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Accessible alert dialog complying with WCAG 2.2 criteria and docs/05-accessibility-design.md §5.3.
 *
 * Focus Trapping & Containment:
 * When visible, TalkBack is contained inside the dialog. The title is declared as a heading
 * so TalkBack immediately announces the modal arrival and problem context.
 */
@Composable
fun AccessibleErrorDialog(
    error: ActionableError,
    onPrimaryAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = error.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.talkBackHeading(0f)
            )
        },
        text = {
            Text(
                text = error.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .accessibleControl(
                        role = Role.Button,
                        label = "Error explanation: ${error.explanation}",
                        traversalIndex = 1f
                    )
            )
        },
        confirmButton = {
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .height(48.dp)
                    .accessibleTouchTarget(minHeight = 48.dp)
                    .accessibleControl(
                        role = Role.Button,
                        label = error.primaryActionLabel,
                        onClickLabel = "Double tap to ${error.primaryActionLabel}",
                        traversalIndex = 2f
                    )
            ) {
                Text(error.primaryActionLabel, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            if (error.secondaryActionLabel != null) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .height(48.dp)
                        .accessibleTouchTarget(minHeight = 48.dp)
                        .accessibleControl(
                            role = Role.Button,
                            label = error.secondaryActionLabel,
                            onClickLabel = "Double tap to dismiss dialog",
                            traversalIndex = 3f
                        ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(error.secondaryActionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        modifier = modifier.accessibleTraversalGroup()
    )
}
