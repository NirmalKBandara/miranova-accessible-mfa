package com.miranova.mfa.ui.accessibility

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Non-trapping accessible progress indicator complying with docs/05-accessibility-design.md §6.3.
 *
 * Anti-Trap Rule:
 * The spinning wheel itself is marked [accessibleInvisibleToUser] so TalkBack focus cannot get
 * trapped in rotating visual loops during network or Keystore cryptographic operations.
 * Progress is communicated asynchronously via a decoupled polite live region text.
 */
@Composable
fun AccessibleLoadingIndicator(
    loadingMessage: String,
    modifier: Modifier = Modifier,
    indicatorSize: Dp = 48.dp,
    liveRegionPolite: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Visual spinner is completely excluded from TalkBack semantic tree to avoid focus traps
        CircularProgressIndicator(
            modifier = Modifier
                .size(indicatorSize)
                .accessibleInvisibleToUser(),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Decoupled live region announces status cleanly without stealing focus
        Text(
            text = loadingMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.accessibleLiveRegion(polite = liveRegionPolite)
        )
    }
}
