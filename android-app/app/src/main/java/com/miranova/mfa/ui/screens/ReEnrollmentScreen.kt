package com.miranova.mfa.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.miranova.mfa.data.model.EnrollmentState
import com.miranova.mfa.data.repository.EnrollmentRepository
import com.miranova.mfa.ui.accessibility.AccessibleErrorDialog
import com.miranova.mfa.ui.accessibility.AccessibleLoadingIndicator
import com.miranova.mfa.ui.accessibility.AccessibleStatusBanner
import com.miranova.mfa.ui.accessibility.ActionableError
import com.miranova.mfa.ui.accessibility.HapticPattern
import com.miranova.mfa.ui.accessibility.MultimodalFeedbackManager
import com.miranova.mfa.ui.accessibility.StatusSeverity
import com.miranova.mfa.ui.accessibility.accessibleCheckbox
import com.miranova.mfa.ui.accessibility.accessibleControl
import com.miranova.mfa.ui.accessibility.accessibleMergedCard
import com.miranova.mfa.ui.accessibility.accessibleTouchTarget
import com.miranova.mfa.ui.accessibility.talkBackHeading
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TalkBack-first Re-enrollment and Invalidated-Key Recovery screen.
 * Implements Issue #51 and docs/05-accessibility-design.md §5.1 (Flow 1 recovery).
 *
 * Enforces explicit warnings for irreversible key revocation and guides users
 * through re-establishing biometric hardware binding with the authentication backend.
 */
@Composable
fun ReEnrollmentScreen(
    repository: EnrollmentRepository,
    reason: String = "Security key requires renewal or was invalidated",
    initialUsername: String = "",
    onReEnrollmentSuccess: (username: String) -> Unit = {},
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val multimodal = remember { MultimodalFeedbackManager(context) }
    val scrollState = rememberScrollState()

    val currentRecord = remember { repository.enrollmentStore.getEnrollment() }
    var username by remember {
        mutableStateOf(
            initialUsername.ifBlank { currentRecord?.username ?: "" }
        )
    }
    var requireBiometric by remember {
        mutableStateOf(currentRecord?.isBiometricProtected ?: false)
    }
    var userAcknowledgedRevocation by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var activeError by remember { mutableStateOf<ActionableError?>(null) }

    val enrollmentState = repository.enrollmentState.value
    val isProcessing = enrollmentState is EnrollmentState.GeneratingKey ||
            enrollmentState is EnrollmentState.RegisteringWithBackend

    LaunchedEffect(Unit) {
        multimodal.notifyWarning("Warning: Re-enrolling will permanently revoke this device's current security key.")
    }

    // Observe state changes for accessibility announcements and terminal success
    LaunchedEffect(enrollmentState) {
        when (enrollmentState) {
            is EnrollmentState.Enrolled -> {
                multimodal.notifyEnrollmentReady("Device re-enrollment complete. Security key re-established.")
                delay(600)
                onReEnrollmentSuccess(enrollmentState.username)
            }
            is EnrollmentState.EnrollmentFailed -> {
                multimodal.notifyAuthFailure("Device re-enrollment failed.")
                activeError = enrollmentState.error
            }
            else -> {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
            .semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Heading with TalkBack heading semantics
        Text(
            text = "Device Re-enrollment",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.talkBackHeading(0f)
        )

        // Irreversible warning banner
        AccessibleStatusBanner(
            message = "Irreversible Action: Re-enrolling will permanently revoke this device's current security key and register a new key.",
            severity = StatusSeverity.WARNING,
            modifier = Modifier.fillMaxWidth()
        )

        // Reason Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleMergedCard(
                    description = "Reason for re-enrollment: $reason. A fresh registration is required to restore access."
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Reason for Re-enrollment",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Username Field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Account Username") },
            placeholder = { Text("e.g. alice") },
            enabled = !isProcessing,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .accessibleControl(
                    role = Role.Button,
                    label = "Account Username input field. Current value: ${username.ifEmpty { "Empty" }}"
                )
        )

        // Biometric Policy Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleCheckbox(
                    checked = requireBiometric,
                    label = "Require biometric authentication (fingerprint or face) to sign authentication challenges"
                )
                .clickable(enabled = !isProcessing) {
                    requireBiometric = !requireBiometric
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = requireBiometric,
                onCheckedChange = { requireBiometric = it },
                enabled = !isProcessing,
                modifier = Modifier.accessibleTouchTarget()
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Require Biometric Verification",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Enforces fingerprint or face unlock before challenge signing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Explicit Acknowledgment Checkbox (Safety Barrier)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleCheckbox(
                    checked = userAcknowledgedRevocation,
                    label = "I understand that my existing security credential will be permanently deleted and replaced"
                )
                .clickable(enabled = !isProcessing) {
                    userAcknowledgedRevocation = !userAcknowledgedRevocation
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = userAcknowledgedRevocation,
                onCheckedChange = { userAcknowledgedRevocation = it },
                enabled = !isProcessing,
                modifier = Modifier.accessibleTouchTarget()
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "I acknowledge that existing keys will be replaced",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Live Processing Indicator
        if (isProcessing) {
            val statusText = when (enrollmentState) {
                is EnrollmentState.GeneratingKey -> "Revoking previous key and generating new hardware key..."
                is EnrollmentState.RegisteringWithBackend -> "Registering new credentials with authentication server..."
                else -> "Processing re-enrollment..."
            }
            AccessibleLoadingIndicator(
                loadingMessage = statusText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        val canProceed = username.isNotBlank() && userAcknowledgedRevocation && !isProcessing
        Button(
            onClick = {
                multimodal.haptics.trigger(HapticPattern.TICK)
                showConfirmDialog = true
            },
            enabled = canProceed,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .fillMaxWidth()
                .accessibleTouchTarget()
                .accessibleControl(
                    role = Role.Button,
                    label = if (canProceed) "Proceed with Re-enrollment"
                    else "Proceed with Re-enrollment disabled. Enter username and confirm key replacement."
                )
        ) {
            Text("Re-enroll Device")
        }

        OutlinedButton(
            onClick = {
                multimodal.haptics.trigger(HapticPattern.TICK)
                onCancel()
            },
            enabled = !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .accessibleTouchTarget()
                .accessibleControl(
                    role = Role.Button,
                    label = "Cancel and Return"
                )
        ) {
            Text("Cancel")
        }
    }

    // Two-stage Confirmation Modal Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Key Revocation?",
                    modifier = Modifier.talkBackHeading(0f)
                )
            },
            text = {
                Text(
                    text = "This action cannot be undone. Your old cryptographic keys on this device will be erased, and the backend server will be updated with your new key. Are you sure you wish to continue?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        multimodal.haptics.trigger(HapticPattern.TICK)
                        scope.launch {
                            repository.reEnrollDevice(
                                username = username.trim(),
                                requireBiometric = requireBiometric
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.accessibleTouchTarget()
                ) {
                    Text("Yes, Re-enroll")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmDialog = false },
                    modifier = Modifier.accessibleTouchTarget()
                ) {
                    Text("No, Cancel")
                }
            }
        )
    }

    // Actionable Error Dialog
    activeError?.let { err ->
        AccessibleErrorDialog(
            error = err,
            onDismiss = { activeError = null },
            onPrimaryAction = {
                activeError = null
                scope.launch {
                    repository.reEnrollDevice(
                        username = username.trim(),
                        requireBiometric = requireBiometric
                    )
                }
            }
        )
    }
}
