package com.ghost.video.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghost.video.ui.player.FadingLine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The music mini player: a frosted pill that rides above the tab bar.
 *
 * Everything here is BitChord's `ui/components/MiniPlayer.kt`, at its own
 * figures:
 *  - the pill is [ROW_PADDING_VERTICAL] around a 40dp cover, which is 56dp tall
 *    and so [shape] at half its own height — a pill, not a rounded rectangle;
 *  - [ROW_PADDING_HORIZONTAL] is deliberately larger than the vertical, because
 *    a pill's ends are semicircles and the edge nearest the cover is not the one
 *    beside it but the one curving away above and below it;
 *  - the cover is 40dp at [ART_CORNER], with a hairline border;
 *  - the title fades out at its end where it overflows rather than ending in an
 *    ellipsis, and the artist line does the same;
 *  - the transport is a [GLYPH_SLOT] touch target with a [GLYPH_SIZE] glyph
 *    grown inside it, so a bigger glyph never makes the bar taller;
 *  - the bar is silent when tapped (the sheet rising is its own confirmation)
 *    while its glyphs tick;
 *  - sideways drags change track, a pull up opens the player, and under a pull
 *    the bar rises at [PULL_LIFT_RATE] of the finger and slows ([pullLift]), so
 *    the pull visibly takes hold of something before the player comes over it.
 *
 * What is Ghost Play's, and had to be: the frost. BitChord samples Hazé over the
 * page's own layer tree; this app has no Hazé, so [MiniAudioPlayer] is a surface
 * with a hairline border at the same alphas — the shape, the measurements and
 * the motion are the port, the material underneath is the theme's.
 */
@Composable
fun MiniAudioPlayer(
    title: String,
    artist: String?,
    isPlaying: Boolean,
    progress: Float,
    artwork: Bitmap?,
    onOpen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // The bar's own rise under a pull, in pixels. Read at draw only, in the
    // graphics layer below: a pull moves the bar without recomposing it.
    val lift = remember { mutableFloatStateOf(0f) }
    val settle = remember { arrayOfNulls<Job>(1) }

    val shape = RoundedCornerShape(percent = 50)
    val gestures = Modifier.miniPlayerGestures(
        pull = object : MiniPlayerPull {
            // The player follows the finger the whole way; this app opens the
            // player on release rather than sliding a sheet with it, so what a
            // drag has to do here is only decide *whether* it opens.
            override fun drag(delta: Float) = Unit
            override fun release(velocity: Float) = Unit
        },
        onNext = {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onNext()
        },
        onPrevious = {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onPrevious()
        },
        onPull = {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onOpen()
        },
        onDismiss = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onDismiss()
        },
        lift = lift,
        settle = settle,
        scope = scope,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BAR_GUTTER)
            // Ahead of the surface, so a pull lifts the whole of it, border and
            // all.
            .then(gestures)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
            // Deliberately silent: the whole bar is the target, so it catches
            // stray taps meant for the page behind it, and the player rising is
            // its own confirmation. The glyphs on it still tick.
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ROW_PADDING_HORIZONTAL,
                    vertical = ROW_PADDING_VERTICAL,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ---- The cover ----
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 40.dp)
                    .clip(RoundedCornerShape(ART_CORNER))
                    .thumbnailBorder(RoundedCornerShape(ART_CORNER))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (artwork != null) {
                    androidx.compose.foundation.Image(
                        bitmap = artwork.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(GLYPH_SIZE),
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // ---- The two lines ----
            Column(Modifier.weight(1f)) {
                FadingLine(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!artist.isNullOrEmpty() && artist != "<unknown>") {
                    FadingLine(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ---- The transport ----
            // Two controls, as BitChord's bar carries: play/pause and next. Back
            // is on the player itself, where there is room for it.
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onTogglePlayPause()
                },
                modifier = Modifier.size(GLYPH_SLOT),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(GLYPH_SIZE),
                )
            }
            Spacer(Modifier.width(TRANSPORT_GAP))
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onNext()
                },
                modifier = Modifier.size(GLYPH_SLOT),
            ) {
                MiniPlayerNextGlyph(tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        // ---- Progress ----
        // A hairline under the row, inside the pill: the bar's own height never
        // depends on it, and it is not a seek control — a tap belongs to opening
        // the player, exactly as in BitChord's bar.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = ROW_PADDING_HORIZONTAL)
                .padding(bottom = 3.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)),
            )
        }
    }
}

/**
 * The bar's next glyph, sized against the play glyph beside it the way the full
 * player sizes its own — a shade under a full slot, squashed by the same 0.85
 * the player's skip glyphs carry, so the two players' next buttons read as one
 * glyph at two sizes.
 */
@Composable
private fun MiniPlayerNextGlyph(tint: Color) {
    Icon(
        imageVector = Icons.Rounded.SkipNext,
        contentDescription = "Next",
        tint = tint,
        modifier = Modifier
            .size(NEXT_GLYPH_SIZE)
            .graphicsLayer { scaleY = 0.85f },
    )
}

/**
 * Pulling the mini player up into the full player, the way the full player is
 * pulled back down into it.
 *
 * Implemented by the host for BitChord, which owns the player's sheet. Here the
 * player is a route rather than a sheet, so there is nothing to pull — [onPull]
 * is how a pull is answered instead, and it opens the player on release.
 */
interface MiniPlayerPull {
    /** The finger moved this far, in pixels — negative is upwards. */
    fun drag(delta: Float)

    /** The finger lifted, moving this fast in pixels a second. */
    fun release(velocity: Float)
}

/**
 * Both of the bar's drags: sideways through the queue — a left swipe advances, a
 * right one goes back — and up into the player.
 *
 * One detector rather than one per axis, so a gesture is decided once, by the
 * way it is heading as it passes the touch slop. Two detectors raced each other
 * to their own axis's slop, and the sideways one was asked first — see
 * [PULL_BIAS] for what that did to pulls. The tap to expand is left to the
 * clickable, and is cancelled by this consuming the drag.
 *
 * Track swipes wait until the finger lifts, so one long gesture never skips more
 * than one item.
 *
 * Ported from BitChord (`ui/components/MiniPlayer.kt`, `miniPlayerGestures`).
 * [lift], [settle] and [scope] are passed in so the bar's own graphics layer can
 * read the lift at draw time.
 */
private fun Modifier.miniPlayerGestures(
    pull: MiniPlayerPull?,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onPull: () -> Unit,
    onDismiss: () -> Unit,
    lift: MutableFloatState,
    settle: Array<Job?>,
    scope: kotlinx.coroutines.CoroutineScope,
): Modifier {
    return this.pointerInput(Unit) {
        val trackThreshold = TRACK_SWIPE_THRESHOLD.toPx()
        val liftEase = PULL_LIFT_EASE.toPx()
        val maxVelocity = viewConfiguration.maximumFlingVelocity
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val pointer = down.id
            // Up to the slop: which way this is heading, or nothing at all.
            var start: PointerInputChange? = null
            var vertical = false
            while (start == null) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer }
                    ?: return@awaitEachGesture
                // Lifted inside the slop is a tap, and the tap is the clickable's.
                if (!change.pressed || change.isConsumed) return@awaitEachGesture
                val moved = change.position - down.position
                if (moved.getDistance() < viewConfiguration.touchSlop) continue
                vertical = moved.y < 0f && abs(moved.x) < -moved.y * PULL_BIAS
                // Downwards has nothing to do on the bar, and nor has a pull
                // with no player to pull: left as they always were, a drag that
                // ends inside the bar still counts as its tap.
                if (!vertical && abs(moved.x) < abs(moved.y)) return@awaitEachGesture
                if (vertical && pull == null) return@awaitEachGesture
                change.consume()
                start = change
            }
            val slopped = start ?: return@awaitEachGesture

            if (!vertical) {
                var dragged = slopped.position.x - down.position.x
                // Read before consuming: a consumed change reports no movement.
                val finished = drag(pointer) { change ->
                    dragged += change.positionChange().x
                    change.consume()
                }
                if (!finished) return@awaitEachGesture
                when {
                    dragged <= -trackThreshold -> onNext()
                    dragged >= trackThreshold -> onPrevious()
                }
                return@awaitEachGesture
            }

            val target = pull ?: return@awaitEachGesture
            settle[0]?.cancel()
            val tracker = VelocityTracker()
            tracker.addPointerInputChange(down)
            tracker.addPointerInputChange(slopped)
            target.drag(slopped.position.y - down.position.y)
            var pulled = 0f
            val finished = drag(pointer) { change ->
                val delta = change.positionChange().y
                change.consume()
                tracker.addPointerInputChange(change)
                pulled -= delta
                target.drag(delta)
                lift.floatValue = pullLift(pulled, liftEase)
            }
            val velocity = if (finished) {
                tracker.calculateVelocity().y.coerceIn(-maxVelocity, maxVelocity)
            } else {
                0f
            }
            // A pull that carried the bar most of the way up, or that was thrown
            // upwards, meant the player; a drag that carried it down as far meant
            // "put it away"; everything else springs home having meant nothing.
            val committed = pulled > liftEase * PULL_COMMIT_SHARE || velocity < -PULL_FLICK_VELOCITY
            val dismissed = !committed && pulled < -liftEase * PULL_COMMIT_SHARE
            target.release(velocity)
            val from = lift.floatValue
            settle[0] = scope.launch {
                animate(
                    initialValue = from,
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.65f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { value, _ -> lift.floatValue = value }
                when {
                    committed -> onPull()
                    dismissed -> onDismiss()
                }
            }
        }
    }.graphicsLayer { translationY = -lift.floatValue }
}

/** Loads a hairline border on a thumbnail, light or dark with the theme. */
private fun Modifier.thumbnailBorder(shape: androidx.compose.ui.graphics.Shape): Modifier = this.composed {
    border(
        width = 1.dp,
        color = if (androidx.compose.foundation.isSystemInDarkTheme()) {
            Color.White.copy(alpha = 0.15f)
        } else {
            Color.Black.copy(alpha = 0.15f)
        },
        shape = shape,
    )
}

/**
 * The bar's margin at either side of the screen.
 *
 * A quarter of BitChord's page gutter, and the same as its floating tab bar's —
 * the bar and the tabs underneath it are one stack and share an edge.
 */
private val BAR_GUTTER = 12.dp

/**
 * The transport buttons' touch target. Material's default 48dp is what a bar
 * this slim is really made of, so it sets the height on its own.
 */
private val GLYPH_SLOT = 40.dp

/**
 * The play and skip glyphs themselves.
 *
 * Deliberately grown inside [GLYPH_SLOT] rather than by growing the slot: the
 * slot is level with the 40dp artwork opposite it, and it is the taller of the
 * two that sets the row's height — so a bigger slot would make the whole bar
 * taller, which is not what a bigger glyph is being asked for.
 */
private val GLYPH_SIZE = 26.dp

/**
 * The gap between the two transport controls.
 *
 * Material asks for at least 8dp between adjacent touch targets, and these had
 * none: two [GLYPH_SLOT] boxes sharing an edge, so the boundary between "pause"
 * and "skip" was a line with nothing either side of it.
 */
private val TRANSPORT_GAP = 8.dp

/**
 * Vertical padding, which with the 40dp artwork sets the bar's height at 56dp
 * and so its pill radius at 28.
 */
private val ROW_PADDING_VERTICAL = 8.dp

/**
 * Horizontal padding, deliberately larger than the vertical.
 *
 * A pill's ends are semicircles, so the edge nearest the artwork is not the one
 * beside it but the one curving away above and below it. At the artwork's top
 * corner that edge has already come 8.4dp in from the left — level with where
 * square corners would have put the whole side. Padding the ends by the vertical
 * figure would leave the artwork touching the curve; 12 clears it with room, and
 * reads as centred rather than jammed into the round.
 */
private val ROW_PADDING_HORIZONTAL = 12.dp

/**
 * The artwork's corner, on the 8dp every other thumbnail in the app carries.
 */
private val ART_CORNER = 8.dp

/** Distance that makes a horizontal drag an intentional track change. */
private val TRACK_SWIPE_THRESHOLD = 72.dp

/**
 * How much further a drag may go sideways than up and still be a pull. A thumb
 * swiping up from the bottom of the screen travels on an arc, and judged level,
 * 1:1, a good share of real pulls came out sideways — too short to skip a
 * track, so they did nothing at all. Steeper than about 34 degrees above level
 * is a pull.
 */
private const val PULL_BIAS = 1.5f

/**
 * The finger distance over which the bar's own rise slows to about half its
 * starting rate. It is the player that follows the finger; the bar gives a
 * little and then less, so the pull visibly takes hold of something before the
 * player has come up over it.
 */
private val PULL_LIFT_EASE = 72.dp

/** The bar's rise per pixel of finger as a pull starts. */
private const val PULL_LIFT_RATE = 0.5f

/** Share of [PULL_LIFT_EASE] a pull has to cover for a release to carry on opening. */
private const val PULL_COMMIT_SHARE = 1.6f

/** How fast, in pixels a second, an upward release has to be to decide on its own. */
private const val PULL_FLICK_VELOCITY = 450f

/** The next glyph, at the mini player's scale. */
private val NEXT_GLYPH_SIZE = 25.dp

/**
 * How far the bar rises for [pulled] pixels of finger: [PULL_LIFT_RATE] of the
 * finger to begin with, slowing the further it goes — a rubber band with no end
 * to it, so the bar keeps creeping up for as long as the finger does rather than
 * meeting a ceiling. Its rate is RATE / sqrt(1 + pulled / ease).
 */
private fun pullLift(pulled: Float, ease: Float): Float =
    if (pulled <= 0f || ease <= 0f) {
        0f
    } else {
        PULL_LIFT_RATE * 2f * ease * (sqrt(1f + pulled / ease) - 1f)
    }

/**
 * Loads album art through the shared loader so the mini player and the full
 * player never decode the same file twice in parallel.
 */
@Composable
fun rememberMiniPlayerArtwork(uri: String?): Bitmap? {
    var artwork by remember(uri) { androidx.compose.runtime.mutableStateOf<Bitmap?>(null) }
    androidx.compose.runtime.LaunchedEffect(uri) {
        if (uri == null) return@LaunchedEffect
        // A tiny delay keeps opening a track from re-decoding during a list scroll.
        kotlinx.coroutines.delay(60)
        artwork = com.ghost.video.data.MusicArtworkLoader.load(uri)
    }
    return artwork
}

/** Shared entry animation for the mini player container. */
internal fun miniPlayerEnter() = androidx.compose.animation.slideInVertically(
    initialOffsetY = { it },
    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
) + androidx.compose.animation.fadeIn(
    androidx.compose.animation.core.tween(160),
) + androidx.compose.animation.scaleIn(initialScale = 0.96f)

internal fun miniPlayerExit() = androidx.compose.animation.slideOutVertically(
    targetOffsetY = { it },
    animationSpec = androidx.compose.animation.core.tween(
        200,
        easing = androidx.compose.animation.core.FastOutSlowInEasing,
    ),
) + androidx.compose.animation.fadeOut(
    androidx.compose.animation.core.tween(160),
) + androidx.compose.animation.scaleOut(targetScale = 0.96f)
