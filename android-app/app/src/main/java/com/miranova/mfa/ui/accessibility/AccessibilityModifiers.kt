package com.miranova.mfa.ui.accessibility

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Enforces WCAG 2.2 Target Size (Minimum 48x48 dp) and padding separation.
 */
fun Modifier.accessibleTouchTarget(
    minWidth: Dp = 48.dp,
    minHeight: Dp = 48.dp
): Modifier = this.sizeIn(minWidth = minWidth, minHeight = minHeight)

/**
 * Marks composable as a top-level accessibility heading for TalkBack navigation.
 */
fun Modifier.talkBackHeading(
    index: Float = 0f
): Modifier = this.semantics {
    heading()
    traversalIndex = index
}

/**
 * Standardizes accessible interactive controls with role, action label, and dynamic state.
 */
fun Modifier.accessibleControl(
    role: Role = Role.Button,
    label: String,
    state: String? = null,
    onClickLabel: String? = null,
    traversalIndex: Float = 0f
): Modifier = this.semantics {
    this.role = role
    this.contentDescription = label
    if (state != null) {
        this.stateDescription = state
    }
    this.traversalIndex = traversalIndex
    if (onClickLabel != null) {
        this.onClick(label = onClickLabel) { true }
    }
}

/**
 * Configures an asynchronous live region container for TalkBack announcements.
 */
fun Modifier.accessibleLiveRegion(
    polite: Boolean = true
): Modifier = this.semantics {
    liveRegion = if (polite) LiveRegionMode.Polite else LiveRegionMode.Assertive
}

/**
 * Groups child semantics into a logical traversal group.
 */
fun Modifier.accessibleTraversalGroup(): Modifier = this.semantics {
    isTraversalGroup = true
}
