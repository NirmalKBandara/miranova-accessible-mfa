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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.miranova.mfa.ui.accessibility.AccessibilityPreferences
import com.miranova.mfa.ui.accessibility.AccessibleStatusBanner
import com.miranova.mfa.ui.accessibility.MultimodalFeedbackManager
import com.miranova.mfa.ui.accessibility.StatusSeverity
import com.miranova.mfa.ui.accessibility.SystemAudioInspector
import com.miranova.mfa.ui.accessibility.accessibleControl
import com.miranova.mfa.ui.accessibility.accessibleMergedCard
import com.miranova.mfa.ui.accessibility.accessibleTouchTarget
import com.miranova.mfa.ui.accessibility.talkBackHeading

/**
 * TalkBack-first Accessibility Settings & Feedback Diagnostics screen.
 * Implements Issue #53 and docs/05-accessibility-design.md §7.
 *
 * Provides granular control over complementary feedback channels (tactile haptics,
 * synthesized earcons, spoken TTS guidance) and interactive audition tests.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    multimodalManager: MultimodalFeedbackManager? = null
) {
    val context = LocalContext.current
    val multimodal = remember { multimodalManager ?: MultimodalFeedbackManager(context) }
    val store = multimodal.preferencesStore
    val audioInspector = remember { SystemAudioInspector(context) }
    val scrollState = rememberScrollState()

    var prefs by remember { mutableStateOf(store.getPreferences()) }
    var diagnosticStatus by remember { mutableStateOf("Ready to audition feedback channels.") }
    var statusSeverity by remember { mutableStateOf(StatusSeverity.INFO) }

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
            text = "Accessibility Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.talkBackHeading(0f)
        )

        // Informative guidance card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .accessibleMergedCard(
                    description = "Complementary feedback settings. Miranova supports independent tactile haptics, non-speech earcons, and spoken speech guidance. Authentication remains 100% accessible when any channel is disabled."
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Multimodal Channels",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tactile and acoustic cues complement TalkBack screen reader exploration. All flows remain completely usable when channels are silenced.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // System Environment Banner
        if (audioInspector.isSystemSilentOrVibrate()) {
            AccessibleStatusBanner(
                message = "Device is in Silent or Vibrate mode. Audio earcons will be silenced automatically.",
                severity = StatusSeverity.WARNING,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Preference 1: Haptic Vibration
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val newHaptic = !prefs.isHapticEnabled
                    store.updateHapticEnabled(newHaptic)
                    prefs = store.getPreferences()
                    multimodal.syncPreferences()
                    diagnosticStatus = if (newHaptic) "Haptic feedback enabled." else "Haptic feedback disabled."
                    statusSeverity = StatusSeverity.INFO
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tactile Haptic Feedback",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "LRA vibration pulses for approval, challenge arrival, and countdown ticks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = prefs.isHapticEnabled,
                onCheckedChange = { checked ->
                    store.updateHapticEnabled(checked)
                    prefs = store.getPreferences()
                    multimodal.syncPreferences()
                },
                modifier = Modifier
                    .accessibleTouchTarget()
                    .accessibleControl(
                        role = Role.Switch,
                        label = "Tactile Haptic Feedback switch",
                        state = if (prefs.isHapticEnabled) "On" else "Off"
                    )
            )
        }

        // Preference 2: Non-speech Earcons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val newEarcons = !prefs.isAudioEarconsEnabled
                    store.updateAudioEarconsEnabled(newEarcons)
                    prefs = store.getPreferences()
                    multimodal.syncPreferences()
                    diagnosticStatus = if (newEarcons) "Audio earcons enabled." else "Audio earcons disabled."
                    statusSeverity = StatusSeverity.INFO
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Acoustic Earcon Chimes",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Concise non-speech audio tones (<250ms) routed without TalkBack ducking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = prefs.isAudioEarconsEnabled,
                onCheckedChange = { checked ->
                    store.updateAudioEarconsEnabled(checked)
                    prefs = store.getPreferences()
                    multimodal.syncPreferences()
                },
                modifier = Modifier
                    .accessibleTouchTarget()
                    .accessibleControl(
                        role = Role.Switch,
                        label = "Acoustic Earcon Chimes switch",
                        state = if (prefs.isAudioEarconsEnabled) "On" else "Off"
                    )
            )
        }

        // Preference 3: Spoken TTS Guidance
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val newTts = !prefs.isTtsGuidanceEnabled
                    store.updateTtsGuidanceEnabled(newTts)
                    prefs = store.getPreferences()
                    multimodal.syncPreferences()
                    diagnosticStatus = if (newTts) "Spoken guidance enabled." else "Spoken guidance disabled."
                    statusSeverity = StatusSeverity.INFO
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Spoken Speech Guidance",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Synthesized verbal guidance scrubbed of secrets and cryptographic tokens",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = prefs.isTtsGuidanceEnabled,
                onCheckedChange = { checked ->
                    store.updateTtsGuidanceEnabled(checked)
                    prefs = store.getPreferences()
                    multimodal.syncPreferences()
                },
                modifier = Modifier
                    .accessibleTouchTarget()
                    .accessibleControl(
                        role = Role.Switch,
                        label = "Spoken Speech Guidance switch",
                        state = if (prefs.isTtsGuidanceEnabled) "On" else "Off"
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Diagnostic Audition Section
        Text(
            text = "Feedback Channel Diagnostics",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.talkBackHeading(0f)
        )

        Button(
            onClick = {
                diagnosticStatus = "Auditioning Success Cues: Triple tick haptic and approved earcon."
                statusSeverity = StatusSeverity.SUCCESS
                multimodal.notifyAuthSuccess("Auditioning success confirmation")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Test Approval Feedback: Triggers success haptics, earcon chime, and spoken confirmation"
                )
        ) {
            Text("Test Approval Cues")
        }

        OutlinedButton(
            onClick = {
                diagnosticStatus = "Auditioning Warning Cues: Cautionary double buzz."
                statusSeverity = StatusSeverity.WARNING
                multimodal.notifyWarning("Auditioning warning feedback")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Test Warning Feedback: Triggers cautionary vibration pulse"
                )
        ) {
            Text("Test Warning Buzz")
        }

        OutlinedButton(
            onClick = {
                diagnosticStatus = "Auditioning Secrecy Guard: Secret token was intercepted and scrubbed from audio."
                statusSeverity = StatusSeverity.INFO
                multimodal.speakSafeGuidance("Signing challenge miranova:auth:v1:a1b2c3d4e5f67890abcdef1234567890")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Test Secrecy Guard: Demonstrates that cryptographic nonces are blocked from spoken audio"
                )
        ) {
            Text("Test Secrecy Guard Filter")
        }

        OutlinedButton(
            onClick = {
                store.resetToDefaults()
                prefs = store.getPreferences()
                multimodal.syncPreferences()
                diagnosticStatus = "All accessibility settings reset to factory defaults."
                statusSeverity = StatusSeverity.INFO
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Reset Settings: Restores all sensory channels to default enabled state"
                )
        ) {
            Text("Reset to Defaults")
        }

        AccessibleStatusBanner(
            message = diagnosticStatus,
            severity = statusSeverity,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Return to Previous Screen"
                )
        ) {
            Text("Return to App")
        }
    }
}
