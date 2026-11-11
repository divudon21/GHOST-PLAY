package com.ghost.video.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * A preference row that owns a list of options and opens it directly beneath
 * itself.
 *
 * The row carries a chevron that turns over while its list is open, so the two
 * say the same thing: this row expands, here, and here is what it opened. The
 * list is drawn under the row rather than at the foot of the page — a picker
 * parked under the last switch reads as a setting of its own — and it is never a
 * dialog, per the app's settings-shell rule.
 *
 * Extracted from the Preferred audio language row, which is where this shape was
 * first got right, so every expanding preference in Settings is the same one.
 */
@Composable
fun ExpandingPreferenceItem(
    title: String,
    description: String?,
    icon: ImageVector?,
    expanded: Boolean,
    onToggle: () -> Unit,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    options: @Composable () -> Unit,
) {
    // Turned, not swapped for a second glyph: a rotate is a draw-only
    // property here, and the two frames of a swap are the only thing the eye
    // would catch.
    val turn by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "expandingPreferenceChevron",
    )
    PreferenceItem(
        title = title,
        description = description,
        icon = icon,
        enabled = true,
        onClick = onToggle,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        trailingContent = {
            Icon(
                imageVector = NextIcons.ChevronDown,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(turn),
            )
        },
    )

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        InlineOptionGroup { options() }
    }
}
