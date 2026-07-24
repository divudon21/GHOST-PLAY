package com.ghost.video.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp

/**
 * A lightweight, buttery-smooth toggle switch with an icon inside the thumb:
 *  - ON  → filled track, thumb slides right, shows a check (✓)
 *  - OFF → outlined track, thumb sits left, shows a cross (✕)
 *
 * Performance notes:
 *  - Only cheap single-value animations: [animateDpAsState] for the thumb offset
 *    and [animateColorAsState] for the track/thumb tint. No per-frame physics
 *    loops, no drag gestures, no recomposition storms.
 *  - The icon swap uses a light [Crossfade]; nothing heavy is drawn.
 */
@Composable
fun SmoothSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackWidth = 62.dp
    val trackHeight = 36.dp
    val thumbSize = 28.dp
    val padding = 4.dp

    val cs = MaterialTheme.colorScheme

    // One cheap spring for the slide.
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - padding else padding,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "thumbOffset"
    )

    val colorSpec = tween<Color>(durationMillis = 220)

    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> cs.onSurface.copy(alpha = 0.12f)
            checked -> cs.primary
            else -> Color.Transparent
        },
        animationSpec = colorSpec,
        label = "trackColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> cs.onSurface.copy(alpha = 0.20f)
            checked -> cs.primary
            else -> cs.onSurfaceVariant.copy(alpha = 0.65f)
        },
        animationSpec = colorSpec,
        label = "borderColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = when {
            !enabled -> cs.onSurface.copy(alpha = 0.30f)
            checked -> cs.onPrimary
            else -> cs.onSurfaceVariant.copy(alpha = 0.75f)
        },
        animationSpec = colorSpec,
        label = "thumbColor"
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            !enabled -> cs.surface
            checked -> cs.primary
            else -> cs.surface
        },
        animationSpec = colorSpec,
        label = "iconColor"
    )

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(CircleShape)
            .background(trackColor, CircleShape)
            .border(2.dp, borderColor, CircleShape)
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
                .background(thumbColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = checked, animationSpec = tween(180), label = "thumbIcon") { isOn ->
                Icon(
                    imageVector = if (isOn) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
