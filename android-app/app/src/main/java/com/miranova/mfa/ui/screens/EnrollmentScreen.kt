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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.miranova.mfa.crypto.CryptoException
import com.miranova.mfa.crypto.KeyStoreManager
import com.miranova.mfa.ui.accessibility.AccessibleErrorDialog
import com.miranova.mfa.ui.accessibility.AccessibleLoadingIndicator
import com.miranova.mfa.ui.accessibility.AccessibleStatusBanner
import com.miranova.mfa.ui.accessibility.ActionableError
import com.miranova.mfa.ui.accessibility.MultimodalFeedbackManager
import com.miranova.mfa.ui.accessibility.StatusSeverity
import com.miranova.mfa.ui.accessibility.accessibleCheckbox
import com.miranova.mfa.ui.accessibility.accessibleControl
import com.miranova.mfa.ui.accessibility.accessibleMergedCard
import com.miranova.mfa.ui.accessibility.accessibleTouchTarget
import com.miranova.mfa.ui.accessibility.accessibleTraversalGroup
import com.miranova.mfa.ui.accessibility.talkBackHeading
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TalkBack-first Device Registration and Factor Enrollment screen.
 * Implements docs/05-accessibility-design.md §5.1 (Flow 1) and WCAG 2.2 Level AA.
 */
@Composable
fun EnrollmentScreen(
    onEnrollmentComplete: () -> Unit,
    modifier: Modifier = Modifier,
    keyAlias: String = "miranova_device_key",
    keyStoreManager: KeyStoreManager? = null,
    multimodalManager: MultimodalFeedbackManager? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val multimodal = remember { multimodalManager ?: MultimodalFeedbackManager(context) }

    var username by remember { mutableStateOf("") }
    var biometricRequired by remember { mutableStateOf(false) }
    var isEnrolling by remember { mutableStateOf(false) }
    var activeError by remember { mutableStateOf<ActionableError?>(null) }
    var statusMessage by remember { mutableStateOf("Enter username to register device.") }
    var statusSeverity by remember { mutableStateOf(StatusSeverity.INFO) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .accessibleTraversalGroup(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Miranova Device Enrollment",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.talkBackHeading(0f)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleMergedCard(
                    description = "Enrollment info: Registers a non-exportable hardware cryptographic key in Android Keystore bound to your identity.",
                    traversalIndex = 1f
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = "Registers a non-exportable cryptographic key in Android Keystore bound to your identity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                if (it.isNotBlank()) {
                    statusMessage = "Ready to register device key for $it"
                    statusSeverity = StatusSeverity.INFO
                }
            },
            label = { Text("Username or Email") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .accessibleControl(
                    role = Role.Button,
                    label = "Username or Email account identifier",
                    state = if (username.isBlank()) "Required, currently empty" else "Entered: $username",
                    traversalIndex = 2f
                )
        )

        // Biometric Protection Preference Checkbox (Flow 1 Step 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleTouchTarget(minHeight = 48.dp)
                .clickable { biometricRequired = !biometricRequired }
                .accessibleCheckbox(
                    checked = biometricRequired,
                    label = "Require biometric verification before challenge signing",
                    checkedState = "Checked: Requires biometric authentication before each challenge",
                    uncheckedState = "Not checked: Allows direct device signature",
                    traversalIndex = 3f
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = biometricRequired,
                onCheckedChange = null, // Handled by accessible Row clickable
                modifier = Modifier.accessibleTouchTarget()
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Require biometric verification",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Asynchronous non-trapping loading indicator or status banner
        if (isEnrolling) {
            AccessibleLoadingIndicator(
                loadingMessage = "Generating non-exportable hardware key on your device. Please wait.",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            AccessibleStatusBanner(
                message = statusMessage,
                severity = statusSeverity,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                if (username.isBlank()) {
                    statusMessage = "Error: Username cannot be blank. Enter username to register."
                    statusSeverity = StatusSeverity.WARNING
                    multimodal.notifyTick()
                    return@Button
                }

                isEnrolling = true
                statusMessage = "Generating hardware key in Android Keystore. Please wait."
                statusSeverity = StatusSeverity.INFO

                scope.launch {
                    try {
                        if (keyStoreManager != null) {
                            keyStoreManager.generateKeyPair(
                                alias = keyAlias,
                                requireUserAuth = biometricRequired
                            )
                        } else {
                            delay(600L) // Preview simulation
                        }

                        multimodal.notifyEnrollmentReady()
                        statusMessage = "Device enrollment complete. Registered key bound to $username."
                        statusSeverity = StatusSeverity.SUCCESS
                        delay(600L)
                        onEnrollmentComplete()
                    } catch (e: CryptoException) {
                        multimodal.notifyAuthFailure()
                        activeError = ActionableError.Generic("Key generation failed: ${e.message}")
                    } catch (e: Exception) {
                        multimodal.notifyAuthFailure()
                        activeError = ActionableError.Generic("Failed to complete enrollment: ${e.message}")
                    } finally {
                        isEnrolling = false
                    }
                }
            },
            enabled = !isEnrolling,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Register device hardware key",
                    state = if (isEnrolling) "In progress, please wait" else null,
                    onClickLabel = "Double tap to generate hardware key in Android Keystore and send public key to Miranova server",
                    traversalIndex = 4f
                )
        ) {
            Text(
                text = if (isEnrolling) "Generating Key..." else "Register This Device",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    activeError?.let { err ->
        AccessibleErrorDialog(
            error = err,
            onPrimaryAction = { activeError = null },
            onDismiss = { activeError = null }
        )
    }
}
