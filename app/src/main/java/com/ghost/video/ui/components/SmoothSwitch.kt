package com.ghost.video.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A lightweight, buttery-smooth toggle switch.
 *
 * Performance notes:
 *  - Uses only [animateDpAsState] (thumb offset) + [animateColorAsState] (track /
 *    thumb tint). Both are cheap, single-value spring animations — no per-frame
 *    manual physics loops, no recomposition storms.
 *  - A single low-stiffness spring gives a natural, non-janky glide with a tiny
 *    bounce that feels premium without dropping frames.
 */
@Composable
fun SmoothSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val thumbSize = 22.dp
    val padding = 4.dp

    val cs = MaterialTheme.colorScheme

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - padding else padding,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "thumbOffset"
    )

    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> cs.onSurface.copy(alpha = 0.12f)
            checked -> cs.primary
            else -> cs.surfaceVariant
        },
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow),
        label = "trackColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = when {
            !enabled -> cs.onSurface.copy(alpha = 0.35f)
            checked -> cs.onPrimary
            else -> cs.outline
        },
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow),
        label = "thumbColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked || !enabled) Color.Transparent else cs.outline.copy(alpha = 0.5f),
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow),
        label = "borderColor"
    )

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(CircleShape)
            .background(trackColor, CircleShape)
            .border(1.5.dp, borderColor, CircleShape)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interaction,
                indication = null,
                onValueChange = onCheckedChange
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(thumbColor, CircleShape)
        )
    }
}
