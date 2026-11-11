package com.ghost.video.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghost.video.R
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * The player's seek bar and its two times — Apple Music's scrubber over
 * Apple Music's timestamps.
 *
 * [shown] is a lambda so the playhead's tick recomposes the bar and its two
 * times rather than the player the bar sits in.
 *
 * Ported from BitChord (`ui/player/PlayerControls.kt`, `PlayerScrubber`). The
 * centre slot is kept: BitChord puts the stream's quality badge there, this
 * puts the elapsed time's companion label there when a seek is resolving.
 */
@Composable
fun PlayerScrubber(
    shown: () -> Float,
    durationMs: Long,
    loading: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
    centerLabel: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
) {
    val value = shown()
    Column(modifier.fillMaxWidth()) {
        ThinSlider(
            value = value,
            onValueChange = onScrub,
            onValueChangeFinished = onScrubFinished,
            loading = loading,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // The slider's touch target extends well past the drawn bar,
                // so pull the labels back up under it.
                .offset(y = -SCRUBBER_LABEL_LIFT),
        ) {
            val timeColor = chromeWhite(0.55f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatTime((value * durationMs).toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = timeColor,
                )
                Text(
                    text = "-" + formatTime(durationMs - (value * durationMs).toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = timeColor,
                )
            }
            // Pinned to the box's own centre rather than squeezed into the gap
            // between the two timestamps: that gap's width changes by a digit's
            // worth every time a minute rolls over.
            Box(
                modifier = Modifier.matchParentSize(),
            ) {
                centerLabel()
            }
        }
    }
}

/** How far the timestamps sit above their slot, under the slider's touch target. */
val SCRUBBER_LABEL_LIFT = 9.dp

/** The skip glyphs' width, and their touch box, beside the larger play button. */
private val PLAYER_SKIP_ICON_SIZE = 53.dp
private val PLAYER_SKIP_TOUCH_SIZE = 53.dp

/**
 * The skip glyphs are drawn a little flatter than they are wide, so the pair
 * reads lower and longer next to the play button without losing any width.
 */
private const val PLAYER_SKIP_HEIGHT_SCALE = 0.85f

/**
 * The transport: skip back, play/pause, skip forward.
 *
 * SpaceAround, not SpaceEvenly: the outer margins take half a gap, so the
 * three buttons spread a little further apart from each other.
 *
 * [previousEnabled] and [nextEnabled] fade rather than hide — the row keeps its
 * shape at the ends of a queue.
 *
 * Ported from BitChord (`ui/player/PlayerControls.kt`, `TransportRow`). The one
 * change is the play size: BitChord's play glyph is its own asset drawn at
 * 74dp, this app's Material glyph needs a larger box to read at the same
 * weight, so the sizes are carried over to 88dp / 106dp.
 */
@Composable
fun TransportRow(
    isPlaying: Boolean,
    /**
     * While a track is being resolved or buffered, the play glyph would be a
     * lie.
     */
    isLoading: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    compact: Boolean = false,
) {
    val playSize = if (compact) 58.dp else 88.dp
    val playTouch = if (compact) 76.dp else 106.dp
    val skipSize = if (compact) 44.dp else PLAYER_SKIP_ICON_SIZE
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportGlyph(
            glyph = painterResource(R.drawable.ic_player_previous),
            contentDescription = "Previous",
            size = skipSize,
            touchSize = PLAYER_SKIP_TOUCH_SIZE,
            heightScale = PLAYER_SKIP_HEIGHT_SCALE,
            onClick = onPrevious,
            enabled = previousEnabled,
        )
        if (isLoading) {
            // Same footprint as the play/pause target — a smaller box here
            // would shunt everything below it on every load.
            Box(Modifier.size(playTouch), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(if (compact) 30.dp else 38.dp),
                )
            }
        } else {
            TransportGlyph(
                // BitChord's own glyphs, with the horizontal crossfade it uses
                // when the state changes — see [PlayPauseGlyph].
                glyph = null,
                playPause = isPlaying,
                contentDescription = if (isPlaying) "Pause" else "Play",
                size = playSize,
                touchSize = playTouch,
                onClick = onPlayPause,
            )
        }
        TransportGlyph(
            glyph = painterResource(R.drawable.ic_player_next),
            contentDescription = "Next",
            size = skipSize,
            touchSize = PLAYER_SKIP_TOUCH_SIZE,
            heightScale = PLAYER_SKIP_HEIGHT_SCALE,
            onClick = onNext,
            enabled = nextEnabled,
        )
    }
}

/**
 * Transport glyphs. The circular clip belongs on the touch target, never on the
 * [Icon] — clipping the icon itself shaves the corners off wide glyphs like
 * fast-forward.
 */
@Composable
private fun TransportGlyph(
    /** BitChord's own drawable. Null for the play/pause pair — see [playPause]. */
    glyph: Painter?,
    contentDescription: String,
    size: Dp,
    touchSize: Dp = size,
    onClick: () -> Unit,
    enabled: Boolean = true,
    /** Vertical squash of the glyph alone; its width and touch box are untouched. */
    heightScale: Float = 1f,
    /** Whether this is the play/pause pair, drawn as a crossfade. */
    playPause: Boolean? = null,
) {
    val haptics = LocalHapticFeedback.current
    // Faded rather than hidden: the row keeps its shape at the ends of a queue.
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.3f,
        label = "transportAlpha",
    )
    Box(
        modifier = Modifier
            .size(touchSize)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (playPause != null) {
            PlayPauseGlyph(
                isPlaying = playPause,
                size = size,
                tint = Color.White.copy(alpha = alpha),
                contentDescription = contentDescription,
            )
        } else if (glyph != null) {
            Icon(
                painter = glyph,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier
                    .size(size)
                    .then(if (heightScale != 1f) Modifier.graphicsLayer { scaleY = heightScale } else Modifier),
            )
        }
    }
}

/**
 * The play/pause glyph, crossfaded rather than swapped — BitChord's
 * `TransportGlyph` draws its two own assets the same way, and a glyph that
 * changes shape in one frame is the one part of the transport the eye catches.
 */
@Composable
private fun PlayPauseGlyph(
    isPlaying: Boolean,
    size: Dp,
    tint: Color,
    contentDescription: String,
) {
    Crossfade(
        targetState = isPlaying,
        animationSpec = tween(durationMillis = 180),
        label = "playPauseGlyph",
    ) { playing ->
        Icon(
            painter = painterResource(
                if (playing) R.drawable.ic_player_pause else R.drawable.ic_player_play,
            ),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size),
        )
    }
}

/**
 * The title over the artist under the sleeve, Apple Music's two-line heading.
 *
 * A title too long for its line fades out at its end rather than ending in an
 * ellipsis, as Apple's players let it; the artist line does the same.
 */
@Composable
fun PlayerCredits(
    title: String,
    artist: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        FadingLine(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        if (!artist.isNullOrEmpty() && artist != "<unknown>") {
            Spacer(Modifier.height(2.dp))
            FadingLine(
                text = artist,
                style = MaterialTheme.typography.titleMedium,
                color = chromeWhite(0.55f),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * White at [alpha], for the second line of a player heading: the artist under
 * the title, and the player's two timestamps.
 */
fun chromeWhite(alpha: Float): Color = Color.White.copy(alpha = alpha)

/** Apple Music's timestamps: no hours, because no track runs that long. */
fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(Locale.ROOT, minutes, seconds)
}

// ---- The player's own measurements ----
//
// Taken from BitChord's `NowPlayingScreen.kt`, which is where these live there
// because its player and its controls are one screen. Here the sleeve is laid
// out by `AudioPlayerScreen` and the controls by this file, so they are shared
// from here.

/**
 * The player's side margin. Scrollable panels reach back across it.
 *
 * BitChord's figure, unchanged: 30dp is what makes the sleeve read as a card
 * laid on the screen rather than as the screen.
 */
val PLAYER_GUTTER = 30.dp

/**
 * How wide the player's content is ever allowed to get. A sleeve and a volume
 * slider stretched right across a tablet aren't a bigger player, just a coarser
 * one; past this the column stops growing and centres itself instead. Phones
 * are narrower than this, so for them it does nothing.
 */
val PLAYER_MAX_WIDTH = 560.dp

/**
 * The handle strip above the artwork, which always hands drags to the player.
 *
 * It isn't the only place that does — the bottom control closes it too, which is
 * what makes the whole top of the player closable rather than just its topmost
 * 32dp.
 */
val DISMISS_STRIP_HEIGHT = 32.dp

/** The breathing room above the sleeve, needed twice: once to apply, once to measure past. */
val ART_BOX_TOP_PAD = 8.dp

/** The gap between the sleeve's bottom edge and the credits under it. */
val ART_TITLE_GAP = 20.dp

/**
 * The band the credits row owns.
 *
 * Its own height, not its contents': a second line that comes and goes with the
 * track must not move the sleeve above it or the scrubber below it.
 */
val HEADER_HEIGHT = 60.dp

/** The sleeve's corner. BitChord's 8dp, and the figure every tile in this app uses. */
val SLEEVE_CORNER = 8.dp

/** The smallest the sleeve is ever allowed to get. */
val THUMB_SIZE = 54.dp
