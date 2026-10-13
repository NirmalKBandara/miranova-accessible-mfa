package com.miranova.mfa.ui.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Accessible multi-step progress indicator for guided setup workflows.
 *
 * Screen Reader Accessibility:
 * Emits a single merged semantic announcement:
 * "Enrollment Progress: Step X of Y, [Step Title]"
 * so TalkBack users immediately understand their exact location in the setup wizard.
 */
@Composable
fun AccessibleStepProgressIndicator(
    currentStep: EnrollmentStep,
    modifier: Modifier = Modifier
) {
    val semanticLabel = "Enrollment Progress: Step ${currentStep.stepIndex} of ${currentStep.totalSteps}, ${currentStep.stepTitle}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .accessibleMergedCard(
                description = semanticLabel,
                traversalIndex = 0.5f
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EnrollmentStep.entries.forEach { step ->
                val isCompleted = step.stepIndex < currentStep.stepIndex
                val isCurrent = step == currentStep

                val (badgeColor, textColor, borderColor) = when {
                    isCompleted -> Triple(
                        Color(0xFF2E7D32),
                        Color.White,
                        Color(0xFF4CAF50)
                    )
                    isCurrent -> Triple(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onPrimary,
                        MaterialTheme.colorScheme.primary
                    )
                    else -> Triple(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                        .border(2.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${step.stepIndex}",
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Step ${currentStep.stepIndex} of ${currentStep.totalSteps}: ${currentStep.stepTitle}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.accessibleLiveRegion(polite = true)
        )
    }
}
