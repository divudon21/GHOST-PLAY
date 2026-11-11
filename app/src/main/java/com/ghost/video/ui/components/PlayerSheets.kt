package com.ghost.video.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Audio sync and pitch, as a Material 3 modal bottom sheet.
 *
 * A sheet because these are two continuous values being judged by ear: it can be
 * dragged down and out of the way without committing anything, and it leaves the
 * picture visible above it. [rememberModalBottomSheetState] with
 * `skipPartiallyExpanded = false` is what gives it the two stopping points a
 * swipe can leave it at — half and full — which is the behaviour asked for.
 *
 * Each control is a value, a stepper and a slider: the slider for a rough move,
 * the steppers for the one tick that matters, because a swipe cannot land on
 * "exactly 0 ms" and +/- on a touch screen can.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioAdjustSheet(
    audioSyncMs: Float,
    audioPitch: Float,
    onAudioSyncChange: (Float) -> Unit,
    onAudioPitchChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Audio sync and pitch",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Sync shifts the audio against the picture. Pitch changes the " +
                    "voice without touching speed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            SteppedValueRow(
                title = "Sync",
                valueText = "${audioSyncMs.toInt()} ms",
                value = audioSyncMs,
                onValueChange = onAudioSyncChange,
                valueRange = -1000f..1000f,
                step = 25f,
                restartAt = 0f,
            )

            SteppedValueRow(
                title = "Pitch",
                valueText = String.format("%.2fx", audioPitch),
                value = audioPitch,
                onValueChange = onAudioPitchChange,
                valueRange = 0.5f..2f,
                step = 0.05f,
                restartAt = 1f,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onReset) { Text("Reset") }
                Spacer(modifier = Modifier.width(8.dp))
                // The sheet's own drag is how it is dismissed; this is the same
                // exit for people who would rather tap. Hidden downwards with the
                // sheet, not cut, so the two ways out look alike.
                TextButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (sheetState.currentValue == SheetValue.Hidden) onDismiss()
                        }
                    }
                ) { Text("Done") }
            }
        }
    }
}

@Composable
private fun SteppedValueRow(
    title: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    restartAt: Float,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StepperButton(
                icon = NextIcons.Remove,
                description = "Decrease $title",
                enabled = value > valueRange.start,
            ) {
                onValueChange((value - step).coerceIn(valueRange.start, valueRange.endInclusive))
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(),
            )
            StepperButton(
                icon = NextIcons.Add,
                description = "Increase $title",
                enabled = value < valueRange.endInclusive,
            ) {
                onValueChange((value + step).coerceIn(valueRange.start, valueRange.endInclusive))
            }
        }
        // A way back to the value that means "no change at all", which a slider
        // cannot be trusted to land on.
        if (value != restartAt) {
            TextButton(
                onClick = { onValueChange(restartAt) },
                modifier = Modifier.align(Alignment.End),
            ) { Text("Reset to default") }
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(imageVector = icon, contentDescription = description, modifier = Modifier.size(20.dp))
    }
}

/**
 * A single horizontal choice row — the track pickers' and the aspect ratios'.
 *
 * [RadioButton] is the Material 3 control for "one of these", and it animates its
 * own selection; [Surface] with `selectable` semantics supplies the touch target
 * and the metric ripple. The app's own colour scheme is used rather than the
 * player's separate light/dark dialog palette, so a picker looks like the rest of
 * the app on every theme.
 */
@Composable
fun PlayerChoiceRow(
    label: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (selected) NextIcons.CheckCircle else NextIcons.CheckBoxOutline,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * The mute control, on the player's control row.
 *
 * It changes its glyph with the state and turns over as it does, so the new state
 * is the thing you watch happen rather than a swap between two drawings. The
 * glyphs are Material 3's `VolumeOff` / `VolumeUp`, not the hand-drawn vectors
 * this used to set as a drawable resource.
 */
@Composable
fun PlayerMuteButton(
    muted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val turn by animateFloatAsState(
        targetValue = if (muted) 180f else 0f,
        animationSpec = spring(),
        label = "muteTurn",
    )
    FilledTonalIconButton(onClick = onToggle, modifier = modifier.size(40.dp)) {
        Icon(
            imageVector = if (muted) NextIcons.VolumeOff else NextIcons.VolumeUp,
            contentDescription = if (muted) "Unmute" else "Mute",
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { rotationZ = turn },
        )
    }
}

/** A round transparent hit area, for the player's floating rows. */
@Composable
fun PlayerGlyphButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * One picker, used for video quality, audio track, subtitles and aspect ratio.
 *
 * A sheet rather than a fixed-size dialog: a title, a list that scrolls if it
 * needs to, and nothing else — the size follows the number of rows instead of
 * being 460x400 whatever is in it, and a swipe down closes it. [rows] is built by
 * the caller so this file needs nothing from Media3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerTrackSheet(
    title: String,
    onDismiss: () -> Unit,
    rows: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { rows() }
        }
    }
}
