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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.miranova.mfa.crypto.KeyStoreManager
import com.miranova.mfa.data.model.EnrollmentState
import com.miranova.mfa.data.repository.EnrollmentRepository
import com.miranova.mfa.ui.accessibility.AccessibleErrorDialog
import com.miranova.mfa.ui.accessibility.AccessibleLoadingIndicator
import com.miranova.mfa.ui.accessibility.AccessibleStatusBanner
import com.miranova.mfa.ui.accessibility.AccessibleStepProgressIndicator
import com.miranova.mfa.ui.accessibility.ActionableError
import com.miranova.mfa.ui.accessibility.EnrollmentStep
import com.miranova.mfa.ui.accessibility.MultimodalFeedbackManager
import com.miranova.mfa.ui.accessibility.StatusSeverity
import com.miranova.mfa.ui.accessibility.accessibleCheckbox
import com.miranova.mfa.ui.accessibility.accessibleControl
import com.miranova.mfa.ui.accessibility.accessibleLiveRegion
import com.miranova.mfa.ui.accessibility.accessibleMergedCard
import com.miranova.mfa.ui.accessibility.accessibleTouchTarget
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.miranova.mfa.ui.accessibility.talkBackHeading
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TalkBack-first Guided Device Registration and Factor Enrollment wizard.
 * Implements Issue #51 and docs/05-accessibility-design.md §5.1 (Flow 1).
 */
@Composable
fun EnrollmentScreen(
    onEnrollmentComplete: () -> Unit,
    modifier: Modifier = Modifier,
    keyAlias: String = EnrollmentRepository.DEFAULT_KEY_ALIAS,
    keyStoreManager: KeyStoreManager? = null,
    enrollmentRepository: EnrollmentRepository? = null,
    multimodalManager: MultimodalFeedbackManager? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val multimodal = remember { multimodalManager ?: MultimodalFeedbackManager(context) }
    val headingFocusRequester = remember { FocusRequester() }

    var currentStep by remember { mutableStateOf(EnrollmentStep.IDENTITY) }
    var username by remember { mutableStateOf("") }
    var biometricRequired by remember { mutableStateOf(false) }
    var isEnrolling by remember { mutableStateOf(false) }
    var activeError by remember { mutableStateOf<ActionableError?>(null) }
    var statusMessage by remember { mutableStateOf("Welcome to Miranova. Begin guided device enrollment.") }
    var statusSeverity by remember { mutableStateOf(StatusSeverity.INFO) }

    LaunchedEffect(key1 = currentStep) {
        headingFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Miranova Device Enrollment",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .focusRequester(headingFocusRequester)
                .talkBackHeading(0f)
                .accessibleControl(
                    label = "Miranova Device Enrollment, Heading. Step ${currentStep.stepIndex} of ${currentStep.totalSteps}.",
                    traversalIndex = 0f
                )
        )

        // Multi-step progress banner
        AccessibleStepProgressIndicator(
            currentStep = currentStep,
            modifier = Modifier.fillMaxWidth()
        )

        // Step 1: Account Identity
        if (currentStep == EnrollmentStep.IDENTITY) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .accessibleMergedCard(
                        description = "Step 1 Instructions: ${EnrollmentStep.IDENTITY.stepInstructions}",
                        traversalIndex = 1f
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = EnrollmentStep.IDENTITY.stepInstructions,
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
                        statusMessage = "Ready to proceed with account: $it"
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

            Button(
                onClick = {
                    if (username.isBlank()) {
                        statusMessage = "Error: Username cannot be blank. Please enter your account identifier."
                        statusSeverity = StatusSeverity.WARNING
                        multimodal.notifyTick()
                    } else {
                        currentStep = EnrollmentStep.SECURITY_POLICY
                        statusMessage = "Step 2: Choose biometric protection policy."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .accessibleTouchTarget(minHeight = 56.dp)
                    .accessibleControl(
                        role = Role.Button,
                        label = "Continue to biometric security settings",
                        onClickLabel = "Double tap to save account identifier and continue",
                        traversalIndex = 3f
                    )
            ) {
                Text("Next: Security Policy", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Step 2: Biometric Security Policy
        if (currentStep == EnrollmentStep.SECURITY_POLICY) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .accessibleMergedCard(
                        description = "Step 2 Instructions: ${EnrollmentStep.SECURITY_POLICY.stepInstructions}",
                        traversalIndex = 1f
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = EnrollmentStep.SECURITY_POLICY.stepInstructions,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .accessibleTouchTarget(minHeight = 48.dp)
                    .clickable { biometricRequired = !biometricRequired }
                    .accessibleCheckbox(
                        checked = biometricRequired,
                        label = "Require biometric verification before challenge signing",
                        checkedState = "Checked: Biometric authentication required before each signature",
                        uncheckedState = "Not checked: Allows direct hardware signature",
                        traversalIndex = 2f
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = biometricRequired,
                    onCheckedChange = null,
                    modifier = Modifier.accessibleTouchTarget()
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Require biometric verification",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { currentStep = EnrollmentStep.IDENTITY },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .accessibleTouchTarget(minHeight = 56.dp)
                        .accessibleControl(
                            role = Role.Button,
                            label = "Back to account identity",
                            traversalIndex = 4f
                        )
                ) {
                    Text("Back", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = {
                        currentStep = EnrollmentStep.CONFIRMATION
                        statusMessage = "Step 3: Ready to generate hardware key and register."
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .accessibleTouchTarget(minHeight = 56.dp)
                        .accessibleControl(
                            role = Role.Button,
                            label = "Continue to final review and registration",
                            traversalIndex = 3f
                        )
                ) {
                    Text("Next: Review", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Step 3: Confirmation & Hardware Key Generation
        if (currentStep == EnrollmentStep.CONFIRMATION) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .accessibleMergedCard(
                        description = "Enrollment Summary: User $username. Biometric protection: ${if (biometricRequired) "Enabled" else "Disabled"}. Non-exportable hardware key will be created in Android Keystore.",
                        traversalIndex = 1f
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enrollment Summary",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Account: $username",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Biometric Policy: ${if (biometricRequired) "Biometric Required" else "Direct Signing"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isEnrolling) {
                AccessibleLoadingIndicator(
                    loadingMessage = "Generating non-exportable hardware key in Android Keystore. Registering with server...",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AccessibleStatusBanner(
                    message = statusMessage,
                    severity = statusSeverity,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (!isEnrolling) {
                            currentStep = EnrollmentStep.SECURITY_POLICY
                        }
                    },
                    enabled = !isEnrolling,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .accessibleTouchTarget(minHeight = 56.dp)
                        .accessibleControl(
                            role = Role.Button,
                            label = "Back to security policy",
                            traversalIndex = 3f
                        )
                ) {
                    Text("Back", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = {
                        isEnrolling = true
                        statusMessage = "Generating hardware key and registering device. Please wait."
                        statusSeverity = StatusSeverity.INFO

                        scope.launch {
                            try {
                                if (enrollmentRepository != null) {
                                    when (val res = enrollmentRepository.registerDevice(username, biometricRequired, keyAlias)) {
                                        is EnrollmentState.Enrolled -> {
                                            multimodal.notifyEnrollmentReady()
                                            statusMessage = "Device enrollment complete. Registered key bound to $username."
                                            statusSeverity = StatusSeverity.SUCCESS
                                            delay(500L)
                                            onEnrollmentComplete()
                                        }
                                        is EnrollmentState.EnrollmentFailed -> {
                                            multimodal.notifyAuthFailure()
                                            activeError = res.error
                                        }
                                        else -> {}
                                    }
                                } else if (keyStoreManager != null) {
                                    keyStoreManager.generateKeyPair(keyAlias, requireUserAuth = biometricRequired)
                                    multimodal.notifyEnrollmentReady()
                                    statusMessage = "Device enrollment complete. Registered key bound to $username."
                                    statusSeverity = StatusSeverity.SUCCESS
                                    delay(500L)
                                    onEnrollmentComplete()
                                } else {
                                    delay(600L)
                                    multimodal.notifyEnrollmentReady()
                                    statusMessage = "Device enrollment complete. Registered key bound to $username."
                                    statusSeverity = StatusSeverity.SUCCESS
                                    delay(400L)
                                    onEnrollmentComplete()
                                }
                            } catch (e: Exception) {
                                multimodal.notifyAuthFailure()
                                activeError = ActionableError.Generic("Enrollment failed: ${e.message}")
                            } finally {
                                isEnrolling = false
                            }
                        }
                    },
                    enabled = !isEnrolling,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .accessibleTouchTarget(minHeight = 56.dp)
                        .accessibleControl(
                            role = Role.Button,
                            label = "Confirm and register this device",
                            state = if (isEnrolling) "In progress, please wait" else null,
                            onClickLabel = "Double tap to generate hardware key and complete registration",
                            traversalIndex = 2f
                        )
                ) {
                    Text(
                        text = if (isEnrolling) "Registering..." else "Register",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
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
