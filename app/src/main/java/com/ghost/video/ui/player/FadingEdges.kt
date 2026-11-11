package com.ghost.video.ui.player

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Fades what is drawn here out toward its start and end edges, each over
 * [width] along [smootherStep] — the way Apple lets a line too long for its
 * space run out, rather than ending it in an ellipsis or cutting it square.
 *
 * [start] and [end] are how much of each fade is on, from 0 to 1. They are read
 * as it draws, so a crawling line can bring its leading fade in as it moves
 * without recomposing anything.
 *
 * It draws into an offscreen layer of its own and erases from that, so it
 * belongs on the content that fades, never on a surface the content sits over.
 *
 * Ported from BitChord (`ui/graphics/FadingEdges.kt`).
 */
fun Modifier.fadingEdges(width: Dp, start: () -> Float = NoFade, end: () -> Float = NoFade): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
            val fade = width.toPx().coerceAtMost(size.width / 2f)
            // Erasing all of the content at the edge and none of it a fade in.
            val stops = Array(FADE_CURVE_STOPS + 1) { i ->
                val t = i / FADE_CURVE_STOPS.toFloat()
                t to Color.Black.copy(alpha = 1f - smootherStep(t))
            }
            val leftBrush = Brush.horizontalGradient(*stops, startX = 0f, endX = fade)
            val rightBrush = Brush.horizontalGradient(*stops, startX = size.width, endX = size.width - fade)
            val rtl = layoutDirection == LayoutDirection.Rtl
            onDrawWithContent {
                drawContent()
                val left = (if (rtl) end() else start()).coerceIn(0f, 1f)
                if (left > 0f) {
                    drawRect(leftBrush, Offset.Zero, Size(fade, size.height), alpha = left, blendMode = BlendMode.DstOut)
                }
                val right = (if (rtl) start() else end()).coerceIn(0f, 1f)
                if (right > 0f) {
                    drawRect(
                        rightBrush,
                        Offset(size.width - fade, 0f),
                        Size(fade, size.height),
                        alpha = right,
                        blendMode = BlendMode.DstOut,
                    )
                }
            }
        }

private val NoFade = { 0f }

/**
 * How far a line too long for its space takes to fade out: measured off Apple
 * Music's mini player, about 37pt, ending at the edge of the line's space.
 */
val LINE_FADE = 36.dp

/**
 * One line of text that, where it is too long for its width, fades out at its
 * end rather than ending in an ellipsis — see [fadingEdges].
 *
 * Ported from BitChord (`ui/components/SongText.kt`, `FadingLine`). The explicit
 * badge that sits ahead of it there has no meaning for a local file, so what is
 * here is the line itself.
 */
@Composable
fun FadingLine(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
) {
    var overflowing by remember { mutableStateOf(false) }
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        fontWeight = fontWeight,
        onTextLayout = { overflowing = it.didOverflowWidth },
        // Only where it overflows: the fade is a layer of its own, and a line
        // that fits has nothing to fade.
        modifier = modifier.then(if (overflowing) Modifier.fadingEdges(LINE_FADE, end = FullFade) else Modifier),
    )
}

private val FullFade = { 1f }
