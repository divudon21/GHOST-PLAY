package com.ghost.video.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ghost.video.ui.player.ART_BOX_TOP_PAD
import com.ghost.video.ui.player.ART_TITLE_GAP
import com.ghost.video.ui.player.ArtworkMeshBackdrop
import com.ghost.video.ui.player.DISMISS_STRIP_HEIGHT
import com.ghost.video.ui.player.HEADER_HEIGHT
import com.ghost.video.ui.player.THUMB_SIZE
import com.ghost.video.ui.player.PLAYER_GUTTER
import com.ghost.video.ui.player.PLAYER_MAX_WIDTH
import com.ghost.video.ui.player.SLEEVE_CORNER
import com.ghost.video.ui.player.PlayerCredits
import com.ghost.video.ui.player.PlayerScrubber
import com.ghost.video.ui.player.TransportRow
import com.ghost.video.ui.player.rememberArtworkMesh
import com.ghost.video.viewmodel.AudioViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The full audio player — Apple Music's now-playing screen, laid out the way
 * BitChord lays its own out.
 *
 * What is BitChord's, taken from `ui/player/NowPlayingScreen.kt`:
 *  - the sleeve bounded by whichever axis runs out first (the player's width, or
 *    the height left over once the credits row and the gap above it have had
 *    theirs), then held to the box's own height for the single frame the
 *    control gaps take to catch up;
 *  - [PLAYER_GUTTER] / [PLAYER_MAX_WIDTH] — 30dp of margin, a 560dp column that
 *    stops growing and centres itself on a tablet rather than stretching;
 *  - the artwork-and-credits block centred above a control deck pinned to the
 *    foot of the screen, so the controls hold the same place on every device;
 *  - the credits row ([ART_TITLE_GAP] under the sleeve, [HEADER_HEIGHT] tall);
 *  - the pull-down handle strip at the top, which is also the dismiss target;
 *  - the backdrop: the sleeve's own colours, upside down, as a mesh — see
 *    [ArtworkMeshBackdrop];
 *  - the scrubber and the transport, from `ui/player/PlayerControls.kt` and
 *    `ui/player/ThinSlider.kt`.
 *
 * What is Ghost Play's, and had to be: the dismiss motion. BitChord's player is
 * a Material 3 sheet in a window of its own and is moved by `PlayerSheetMotion`;
 * this one is a navigation destination, so there is no sheet to ask and the
 * screen follows the finger itself — on the same critically damped spring, so
 * the movement is indistinguishable. The playhead likewise comes from this
 * app's own MediaSession-backed service rather than from BitChord's playback
 * connection.
 */
@Composable
fun AudioPlayerScreen(
    audioViewModel: AudioViewModel,
    onBack: () -> Unit
) {
    val currentAudio by audioViewModel.currentAudio.collectAsState()
    val isPlaying by audioViewModel.isPlaying.collectAsState()

    // If playback stops/clears, leave the screen.
    LaunchedEffect(currentAudio) {
        if (currentAudio == null) onBack()
    }

    val audio = currentAudio ?: return

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // The sleeve, and the mesh the player stands on, from the same decode.
    val artwork: Bitmap? = rememberAudioArtwork(audio.uri)
    val mesh = rememberArtworkMesh(artwork, audio.uri)

    // Where the sleeve's bottom edge sits, in the backdrop's own pixels. Two
    // window-space reads subtracted from each other, so the drag translation the
    // whole screen carries cancels out of the answer.
    var rootTopPx by remember { mutableIntStateOf(0) }
    var artBottomPx by remember { mutableIntStateOf(0) }
    val seamPx = (artBottomPx - rootTopPx).coerceAtLeast(0)

    // Track position + duration. Only polled while actually playing — a paused
    // player's playhead cannot move, and a 2 Hz read of the session is not free.
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPreview by remember { mutableFloatStateOf(0f) }
    // A seek the session has been told about but has not reported yet. The bar
    // is held where the finger left it until the playhead agrees, so a release
    // never snaps back to the old position for a tick.
    var pendingSeekMs by remember { mutableLongStateOf(-1L) }

    LaunchedEffect(audio.uri, isPlaying) {
        // Refresh once immediately so the bar is correct even when paused.
        if (!isUserSeeking) {
            positionMs = audioViewModel.getCurrentPosition()
            durationMs = audioViewModel.getDuration()
        }
        while (isPlaying) {
            if (!isUserSeeking) {
                val reported = audioViewModel.getCurrentPosition()
                if (pendingSeekMs >= 0 &&
                    abs(reported - pendingSeekMs) <= SEEK_SETTLE_TOLERANCE_MS
                ) {
                    pendingSeekMs = -1L
                }
                if (pendingSeekMs < 0) positionMs = reported
                durationMs = audioViewModel.getDuration()
            }
            delay(500)
        }
    }

    val effectiveDuration = if (durationMs > 0) durationMs else 1L
    val shownFraction: () -> Float = {
        if (isUserSeeking) {
            seekPreview
        } else {
            val at = if (pendingSeekMs >= 0) pendingSeekMs else positionMs
            (at.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
        }
    }

    // ---- Drag to dismiss ----
    // The player follows the finger, then either springs home or leaves on the
    // same spring BitChord's sheet closes on: critically damped, stiffness 195,
    // so it never bounces and has no fast end.
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = with(density) { 120.dp.toPx() }

    fun closePlayer() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            val travel = artBottomPx.toFloat().coerceAtLeast(1f) + with(density) { 480.dp.toPx() }
            animate(
                initialValue = dragOffset,
                targetValue = travel,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = SLIDE_STIFFNESS,
                    visibilityThreshold = SLIDE_SETTLED_PX,
                ),
            ) { value, _ -> dragOffset = value }
            onBack()
        }
    }

    fun settleHome(velocity: Float) {
        scope.launch {
            animate(
                initialValue = dragOffset,
                targetValue = 0f,
                initialVelocity = velocity * 0.2f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = SLIDE_STIFFNESS,
                    visibilityThreshold = SLIDE_SETTLED_PX,
                ),
            ) { value, _ -> dragOffset = value }
        }
    }

    val dismissDrag = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { delta ->
            dragOffset = (dragOffset + delta).coerceAtLeast(0f)
        },
        onDragStopped = { velocity ->
            if (dragOffset > dismissThresholdPx || velocity > 900f) closePlayer()
            else {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                settleHome(velocity)
            }
        },
    )

    BackHandler { closePlayer() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates -> rootTopPx = coordinates.boundsInWindow().top.roundToInt() }
            .graphicsLayer { translationY = dragOffset },
    ) {
        ArtworkMeshBackdrop(
            mesh = mesh,
            seam = { with(density) { seamPx.toDp() } },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = PLAYER_GUTTER),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ---- Top: the dismiss handle ----
            // The only strip that takes a downward drag, so the player closes
            // from the handle and not from a stray swipe on the artwork or the
            // controls. BitChord's strip also carries the playback-origin
            // caption, which a local file has no answer for.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DISMISS_STRIP_HEIGHT)
                    .then(dismissDrag),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(38.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.70f)),
                )
            }

            // ---- Artwork, and the credits under it ----
            // One weighted box, so the controls below it never move.
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = ART_BOX_TOP_PAD, bottom = 18.dp),
            ) {
                // The sleeve is square, so it is bounded by whichever of the two
                // axes runs out first: the player's width on a phone, or on a
                // tablet, where there is width to spare, the height left over
                // once the credits row and the gap above it have had theirs.
                // Sizing it off the width alone is what pushed the credits down
                // across the scrubber on anything but a phone.
                val wantArt = minOf(maxWidth, maxHeight - ART_TITLE_GAP - HEADER_HEIGHT)
                // Held to what the box has actually got, for the single frame it
                // takes the gaps below to catch up with a change in their own
                // height: a sleeve a few dp under for one frame is a better
                // failure than a credits row overhanging the scrubber.
                val artSize = wantArt.coerceAtLeast(THUMB_SIZE)
                // Artwork and the title row travel together as one block, so the
                // pair sits centred in whatever the controls couldn't take.
                val groupTop = ((maxHeight - artSize - ART_TITLE_GAP - HEADER_HEIGHT) / 2)
                    .coerceAtLeast(0.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, with(density) { groupTop.roundToPx() }) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // ---- The sleeve ----
                    Box(
                        modifier = Modifier
                            .size(artSize)
                            .clip(RoundedCornerShape(SLEEVE_CORNER))
                            .background(Color.White.copy(alpha = 0.06f))
                            // The mesh starts where the sleeve ends.
                            .onGloballyPositioned { coordinates ->
                                artBottomPx = coordinates.boundsInWindow().bottom.roundToInt()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (artwork != null) {
                            Image(
                                bitmap = artwork.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(ART_TITLE_GAP))

                    // ---- The credits ----
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HEADER_HEIGHT),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlayerCredits(
                            title = audio.name,
                            artist = audio.artist,
                        )
                    }
                }
            }

            // ---- Bottom: scrubber, transport, close ----
            Column(
                modifier = Modifier
                    .widthIn(max = PLAYER_MAX_WIDTH)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PlayerScrubber(
                    shown = shownFraction,
                    durationMs = durationMs,
                    // A seek the session has not reported back yet has no
                    // measurable fraction to draw, so the bar's own sheen
                    // claims it — see [ThinSlider].
                    loading = pendingSeekMs >= 0,
                    onScrub = { fraction ->
                        isUserSeeking = true
                        seekPreview = fraction.coerceIn(0f, 1f)
                    },
                    onScrubFinished = {
                        val target = (seekPreview * effectiveDuration).toLong()
                        audioViewModel.seekTo(target)
                        pendingSeekMs = target
                        positionMs = target
                        isUserSeeking = false
                    },
                )

                // The transport rides midway between the two blocks it
                // separates: the scrubber above it, and the close control
                // below, which sit close enough together to read as one.
                Spacer(Modifier.height(8.dp + CONTROL_SPREAD / 2))
                TransportRow(
                    isPlaying = isPlaying,
                    isLoading = effectiveDuration <= 1L,
                    previousEnabled = true,
                    nextEnabled = true,
                    onPrevious = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        audioViewModel.skipToPrevious()
                    },
                    onPlayPause = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        audioViewModel.togglePlayPause()
                    },
                    onNext = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        audioViewModel.skipToNext()
                    },
                )
                Spacer(Modifier.height(12.dp + CONTROL_SPREAD / 2))

                // Put the player away. The handle and a downward drag both do
                // the same thing; this is the one that is visible, in the place
                // BitChord's bottom row puts its last glyph.
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .then(dismissDrag),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Close player",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

/**
 * The spring every close runs on: critically damped, so it never bounces, with
 * about 0.45s of response — the way Apple Music's player sheet moves. Unlike a
 * timed ease it has no fast end: it leaves briskly and then spends most of its
 * time settling, slower and slower, into place.
 *
 * The same figure as BitChord's `PlayerSheetMotion.SLIDE_STIFFNESS`.
 */
private const val SLIDE_STIFFNESS = 195f

/** Close enough to stop, in pixels — rather than a spring's whole long tail. */
private const val SLIDE_SETTLED_PX = 0.5f

/** How close the playhead has to get to the dropped handle before it hands over. */
private const val SEEK_SETTLE_TOLERANCE_MS = 1_500L

/** The gap the transport keeps from the two blocks it separates. */
private val CONTROL_SPREAD = 8.dp

/** [uri]'s artwork, decoded once and cached by the shared loader. */
@Composable
private fun rememberAudioArtwork(uri: String?): Bitmap? {
    var artwork by remember(uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) {
        if (uri == null) return@LaunchedEffect
        artwork = com.ghost.video.data.MusicArtworkLoader.load(uri)
    }
    return artwork
}
