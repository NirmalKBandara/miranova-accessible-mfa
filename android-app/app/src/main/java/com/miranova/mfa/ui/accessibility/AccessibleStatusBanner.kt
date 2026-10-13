package com.miranova.mfa.ui.accessibility

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Visual and semantic severity levels for accessible status communication.
 */
enum class StatusSeverity {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

/**
 * Accessible status banner communicating dynamic system states to both sighted and TalkBack users.
 * Combines high-contrast styling with semantic live region announcements.
 */
@Composable
fun AccessibleStatusBanner(
    message: String,
    severity: StatusSeverity = StatusSeverity.INFO,
    modifier: Modifier = Modifier
) {
    if (message.isBlank()) return

    val (bgColor, textColor, borderColor, polite) = when (severity) {
        StatusSeverity.INFO -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outlineVariant,
            true
        )
        StatusSeverity.SUCCESS -> Quadruple(
            Color(0xFF1B3A24),
            Color(0xFFE8F5E9),
            Color(0xFF4CAF50),
            false // Assertive for completion
        )
        StatusSeverity.WARNING -> Quadruple(
            Color(0xFF3E2723),
            Color(0xFFFFF3E0),
            Color(0xFFFFB74D),
            false // Assertive
        )
        StatusSeverity.ERROR -> Quadruple(
            Color(0xFF3B1D1D),
            Color(0xFFFFEBEE),
            MaterialTheme.colorScheme.error,
            false // Assertive
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .accessibleLiveRegion(polite = polite)
            .accessibleMergedCard(
                description = "Status update: $message",
                traversalIndex = 1f
            ),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val prefix = when (severity) {
                StatusSeverity.INFO -> "[Info]"
                StatusSeverity.SUCCESS -> "[Approved]"
                StatusSeverity.WARNING -> "[Warning]"
                StatusSeverity.ERROR -> "[Alert]"
            }

            Text(
                text = prefix,
                style = MaterialTheme.typography.labelMedium,
                color = borderColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
