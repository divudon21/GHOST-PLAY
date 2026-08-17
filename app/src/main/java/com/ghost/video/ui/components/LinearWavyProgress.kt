package com.ghost.video.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val WAVE_SMOOTHNESS = 0.48f
private const val GAP_RAMP_DOWN_THRESHOLD = 0.01f

/**
 * Pixel-faithful port of the reference HTML wavy progress bar:
 *  - Flat, thin track (4px), a subtle wavy fill (3px amplitude) that animates,
 *    a small gap between fill and track, and a stop dot at the end.
 * Tap or drag to scrub. [onProgressChange] fires while scrubbing and
 * [onProgressChangeFinished] fires on tap / drag release so the seek commits once.
 */
@Composable
fun DeterminateLinearWavyProgress(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onProgressChangeFinished: () -> Unit = {},
) {
    val waveColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)

    // Phase animation: one wave cycle = wavelength / waveSpeed = 40/40 = 1 second.
    val infiniteTransition = rememberInfiniteTransition(label = "wavyPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(onProgressChange, onProgressChangeFinished) {
                detectTapGestures { offset ->
                    onProgressChange((offset.x / size.width).coerceIn(0f, 1f))
                    onProgressChangeFinished()
                }
            }
            .pointerInput(onProgressChange, onProgressChangeFinished) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onProgressChange((offset.x / size.width).coerceIn(0f, 1f))
                    },
                    onDragEnd = { onProgressChangeFinished() },
                    onDragCancel = { onProgressChangeFinished() },
                ) { change, _ ->
                    change.consume()
                    onProgressChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        val trackLength = size.width
        val thickness = 5.dp.toPx()
        val waveAmplitude = 3.dp.toPx()
        val wavelengthPx = 40.dp.toPx()
        val gapSize = 4.dp.toPx()
        val stopSize = 5.dp.toPx()
        val corner = thickness / 2f
        val p = progress.coerceIn(0f, 1f)

        val wave = buildOfficialWave(trackLength, wavelengthPx)

        val ampFrac = when {
            p < 0.1f -> (p / 0.1f).coerceIn(0f, 1f)
            p > 0.9f -> ((1f - p) / 0.1f).coerceIn(0f, 1f)
            else -> 1f
        }
        val endGap =
            (gapSize * (1f - p.coerceIn(1f - GAP_RAMP_DOWN_THRESHOLD, 1f))) /
                GAP_RAMP_DOWN_THRESHOLD
        val startPx = 0f
        val endPx = p * trackLength - endGap

        translate(0f, size.height / 2f) {
            var drawnEnd: Float? = null
            if (endPx > startPx) {
                val startBlockCenterX = startPx + corner
                val endBlockCenterX = endPx - corner
                if (endBlockCenterX <= startBlockCenterX) {
                    drawLine(
                        color = waveColor,
                        start = Offset(startPx + corner, 0f),
                        end = Offset(endPx - corner, 0f),
                        strokeWidth = thickness,
                        cap = StrokeCap.Round
                    )
                } else {
                    val pts = pointOnWave(
                        wave = wave,
                        startFrac = startBlockCenterX / trackLength,
                        endFrac = endBlockCenterX / trackLength,
                        phaseFraction = phase,
                        trackLength = trackLength,
                        amplitudePx = waveAmplitude * ampFrac
                    )
                    val path = Path()
                    pts.forEachIndexed { i, pt ->
                        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                    }
                    drawPath(
                        path = path,
                        color = waveColor,
                        style = Stroke(
                            width = thickness,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                drawnEnd = endPx
            }

            val trackStart = if (drawnEnd != null) drawnEnd + gapSize + corner else corner
            val trackEnd = trackLength - corner
            if (trackEnd > trackStart) {
                drawLine(
                    color = trackColor,
                    start = Offset(trackStart, 0f),
                    end = Offset(trackEnd, 0f),
                    strokeWidth = thickness,
                    cap = StrokeCap.Round
                )
            }

            if (stopSize > 0f && p < 1f) {
                drawCircle(
                    color = waveColor,
                    radius = stopSize / 2f,
                    center = Offset(trackLength - thickness / 2f, 0f)
                )
            }
        }
    }
}

private data class Pt(val x: Float, val y: Float)
private data class Cubic(val p0: Pt, val p1: Pt, val p2: Pt, val p3: Pt)
private data class Wave(val adjustedWavelength: Float, val segs: List<Cubic>)

private fun buildOfficialWave(trackLength: Float, wavelengthPx: Float): Wave {
    val cycleCount = max(1, floor(trackLength / wavelengthPx).toInt())
    val adjustedWavelength = trackLength / cycleCount
    val s = WAVE_SMOOTHNESS
    val segs = mutableListOf<Cubic>()
    var cur = Pt(0f, 0f)
    fun addCubic(c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) {
        segs.add(Cubic(cur, Pt(c1x, c1y), Pt(c2x, c2y), Pt(x, y)))
        cur = Pt(x, y)
    }
    for (i in 0..cycleCount) {
        addCubic(2f * i + s, 0f, 2f * i + 1f - s, 1f, 2f * i + 1f, 1f)
        addCubic(2f * i + 1f + s, 1f, 2f * i + 2f - s, 0f, 2f * i + 2f, 0f)
    }
    val sx = adjustedWavelength / 2f
    val sy = -2f
    val ty = 1f
    return Wave(
        adjustedWavelength = adjustedWavelength,
        segs = segs.map { g ->
            Cubic(
                Pt(g.p0.x * sx, g.p0.y * sy + ty),
                Pt(g.p1.x * sx, g.p1.y * sy + ty),
                Pt(g.p2.x * sx, g.p2.y * sy + ty),
                Pt(g.p3.x * sx, g.p3.y * sy + ty),
            )
        }
    )
}

private fun cubicPoint(p0: Pt, p1: Pt, p2: Pt, p3: Pt, t: Float): Pt {
    val mt = 1f - t
    return Pt(
        mt * mt * mt * p0.x + 3f * mt * mt * t * p1.x + 3f * mt * t * t * p2.x + t * t * t * p3.x,
        mt * mt * mt * p0.y + 3f * mt * mt * t * p1.y + 3f * mt * t * t * p2.y + t * t * t * p3.y,
    )
}

private fun pointOnWave(
    wave: Wave,
    startFrac: Float,
    endFrac: Float,
    phaseFraction: Float,
    trackLength: Float,
    amplitudePx: Float
): List<Pt> {
    val segs = wave.segs
    val adjustedWavelength = wave.adjustedWavelength
    val cc = trackLength / adjustedWavelength
    val phaseFractionInPath = phaseFraction / cc
    val ratio = cc / (cc + 1f)
    val start = (startFrac + phaseFractionInPath) * ratio
    val end = (endFrac + phaseFractionInPath) * ratio
    val shiftX = -phaseFraction * adjustedWavelength
    val n = segs.size
    val t0 = start.coerceIn(0f, 0.999999f) * n
    val t1 = end.coerceIn(0f, 0.999999f) * n
    val pts = mutableListOf<Pt>()
    val pxSpan = max(2f, (endFrac - startFrac) * trackLength)
    val steps = max(24, ceil(pxSpan).toInt())
    for (i in 0..steps) {
        val t = t0 + ((t1 - t0) * i) / steps
        val si = min(n - 1, floor(t).toInt())
        val lt = (t - si).coerceIn(0f, 1f)
        val g = segs[si]
        val p = cubicPoint(g.p0, g.p1, g.p2, g.p3, lt)
        pts.add(Pt(p.x + shiftX, p.y * amplitudePx))
    }
    return pts
}
