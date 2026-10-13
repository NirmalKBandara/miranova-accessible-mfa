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
import androidx.compose.material3.OutlinedTextField
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
fun EnrollmentScreen(
    onEnrollmentComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Enter username to register device.") }

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
            modifier = Modifier.fillMaxWidth(),
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
            onValueChange = { username = it },
            label = { Text("Username or Email") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .accessibleControl(
                    role = Role.Button,
                    label = "Username or Email input field",
                    traversalIndex = 1f
                )
        )

        Button(
            onClick = {
                if (username.isNotBlank()) {
                    statusMessage = "Registering device key for $username. Please wait."
                    onEnrollmentComplete()
                } else {
                    statusMessage = "Error: Username cannot be blank. Enter username to register."
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .accessibleTouchTarget(minHeight = 56.dp)
                .accessibleControl(
                    role = Role.Button,
                    label = "Register device hardware key",
                    onClickLabel = "Double tap to generate hardware key and register with server",
                    traversalIndex = 2f
                )
        ) {
            Text("Register This Device", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.accessibleLiveRegion(polite = true)
        )
    }
}
