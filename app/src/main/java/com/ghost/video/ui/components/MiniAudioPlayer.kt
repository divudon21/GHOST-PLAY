package com.ghost.video.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghost.video.data.MusicArtworkLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Music mini player.
 *
 * Behaviour (same model as YouTube Music / Spotify):
 *  - tap the bar, or swipe it up, to open the full player;
 *  - swipe it down to dismiss it (stops playback);
 *  - the drag follows the finger, then settles with a spring;
 *  - every control answers with a spring press-scale and a haptic tick.
 *
 * Motion uses Material 3 Expressive parameters (low-bouncy springs for
 * containers, medium-bouncy springs for controls), so nothing here animates with
 * a linear tween.
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
    val density = LocalDensity.current

    // Drag offset in pixels; positive means "down" (dismiss direction).
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val resetOffset = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { 96.dp.toPx() }
    val openThresholdPx = with(density) { 40.dp.toPx() }

    // Progress is animated with a spring instead of jumping on every poll.
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "miniProgress",
    )

    val artShape by animateDpAsState(
        targetValue = if (isPlaying) 14.dp else 12.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "miniArtShape",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .graphicsLayer {
                // Follow the finger, and shrink slightly as it is dragged away so
                // the gesture feels physical rather than a hard cut.
                translationY = dragOffset + resetOffset.value
                val dragFraction = (kotlin.math.abs(dragOffset + resetOffset.value) / (dismissThresholdPx * 2f))
                    .coerceIn(0f, 0.08f)
                scaleX = 1f - dragFraction
                scaleY = 1f - dragFraction
                alpha = 1f - dragFraction * 6f
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        val total = dragOffset
                        when {
                            total < -openThresholdPx -> {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                dragOffset = 0f
                                onOpen()
                            }

                            total > dismissThresholdPx -> {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                            }

                            else -> scope.launch {
                                resetOffset.snapTo(dragOffset)
                                dragOffset = 0f
                                resetOffset.animateTo(
                                    0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            resetOffset.snapTo(dragOffset)
                            dragOffset = 0f
                            resetOffset.animateTo(
                                0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            )
                        }
                    },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragOffset = (dragOffset + amount).coerceIn(
                            -openThresholdPx * 1.6f,
                            dismissThresholdPx * 1.6f,
                        )
                    },
                )
            }
            .semantics { contentDescription = "Mini player. Swipe up to open." },
        shape = RoundedCornerShape(
            animateDpAsState(
                targetValue = 22.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "miniShape",
            ).value
        ),
        // Dynamic container tint: reads as part of the current theme instead of a
        // flat grey slab, and picks up the expressive surface roles.
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onOpen()
        },
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniArtwork(artwork = artwork, isPlaying = isPlaying, shape = artShape)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!artist.isNullOrEmpty() && artist != "<unknown>") {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SpringIconButton(
                    onClick = onPrevious,
                    icon = { Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous") },
                )

                SpringFilledPlayButton(isPlaying = isPlaying, onClick = onTogglePlayPause)

                SpringIconButton(
                    onClick = onNext,
                    icon = { Icon(Icons.Rounded.SkipNext, contentDescription = "Next") },
                )

                SpringIconButton(
                    onClick = onDismiss,
                    icon = {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close mini player",
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }

            // Progress as a thin expressive bar. It is not a seek control: tapping
            // the bar is reserved for opening the player, exactly like the reference.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun MiniArtwork(artwork: Bitmap?, isPlaying: Boolean, shape: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(shape))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        // Playing marker so the state is readable even while paused with artwork.
        AnimatedVisibility(
            visible = isPlaying,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.6f),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.6f),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SpringIconButton(onClick: () -> Unit, icon: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "iconScale",
    )
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        icon()
    }
}

@Composable
private fun SpringFilledPlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "playScale",
    )
    FilledIconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        // Crossfade between the two states instead of swapping the icon abruptly.
        androidx.compose.animation.Crossfade(
            targetState = isPlaying,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "playIcon",
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
            )
        }
    }
}

/**
 * Loads album art through the shared [MusicArtworkLoader] so the mini player and
 * the full player never decode the same file twice in parallel.
 */
@Composable
fun rememberMiniPlayerArtwork(uri: String?): Bitmap? {
    var artwork by remember(uri) { androidx.compose.runtime.mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) {
        if (uri == null) return@LaunchedEffect
        // A tiny delay keeps opening a track from re-decoding during a list scroll.
        delay(60)
        artwork = MusicArtworkLoader.load(uri)
    }
    return artwork
}

/** Shared entry animation for the mini player container. */
internal fun miniPlayerEnter() = slideInVertically(
    initialOffsetY = { it },
    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
) + fadeIn(tween(160)) + scaleIn(initialScale = 0.96f)

internal fun miniPlayerExit() = slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(200, easing = FastOutSlowInEasing),
) + fadeOut(tween(160)) + scaleOut(targetScale = 0.96f)
