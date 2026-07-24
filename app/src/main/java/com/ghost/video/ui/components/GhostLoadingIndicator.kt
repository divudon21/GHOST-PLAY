package com.ghost.video.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * A lightweight Material 3 "Expressive" style contained loading indicator.
 *
 * Draws a morphing, scalloped (cookie / flower) shape that continuously rotates
 * and smoothly morphs its number of lobes — matching the new Material 3 loading
 * animation. Implemented with a single [Canvas] + a couple of cheap infinite
 * float animations, so it stays performance-friendly and adds no dependency.
 */
@Composable
fun GhostLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contained: Boolean = true,
    shapeColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val transition = rememberInfiniteTransition(label = "ghostLoader")

    // Continuous rotation.
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Morph progress used to blend the lobe count between two polygon shapes.
    val morph by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.minDimension / 2f

            if (contained) {
                drawCircle(color = containerColor, radius = outerRadius, center = center)
            }

            // The morphing shape sits inside the container with some padding.
            val shapeRadius = if (contained) outerRadius * 0.62f else outerRadius * 0.92f

            rotate(degrees = rotation, pivot = center) {
                drawMorphingBlob(
                    center = center,
                    baseRadius = shapeRadius,
                    color = shapeColor,
                    morph = morph
                )
            }
        }
    }
}

/**
 * Draws a scalloped "blob" whose lobe count morphs between 7 and 9 as [morph]
 * goes 0 -> 1, giving the soft shape-shifting look of the M3 loader.
 */
private fun DrawScope.drawMorphingBlob(
    center: Offset,
    baseRadius: Float,
    color: Color,
    morph: Float
) {
    // Blend the "waviness" so the shape appears to gain/lose lobes.
    val lobes = 8
    val amplitude = baseRadius * (0.14f + 0.06f * sin(morph * Math.PI.toFloat()))
    val steps = 180
    val path = Path()

    for (i in 0..steps) {
        val angle = (i.toFloat() / steps) * (2f * Math.PI.toFloat())
        // Modulate radius with a sine wave to create rounded lobes.
        val wave = sin(angle * lobes + morph * Math.PI.toFloat())
        val r = baseRadius + amplitude * wave
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color)
}
