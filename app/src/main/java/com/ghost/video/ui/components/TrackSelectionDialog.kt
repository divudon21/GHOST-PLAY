package com.ghost.video.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTrackNameProvider

/**
 * Track selection and video scale, ported from NextPlayer (GPL-3.0):
 * `feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/`
 * (`OverlayView.kt`, `SequentialSelectorView`s, `RadioButtonRow.kt`,
 * `VideoContentScaleSelectorView.kt`).
 *
 * Their shape, kept: a rounded panel over the picture with the title at the top
 * and the rows scrolling beneath it, one row per choice, a radio for "one of
 * these" and a checkbox for "any of these", and their row metrics —
 * `RadioButtonRow`'s 4dp horizontal and 8dp vertical padding with 8dp between the
 * control and its label, on `bodyMedium`.
 *
 * Their split too: **one panel per thing**. Quality, audio and zoom are three
 * separate panels in NextPlayer, each opened by its own button, so opening one
 * never shows the others' choices.
 *
 * Why this replaced the previous picker: that one hosted media3's
 * `TrackSelectionView` — a `View` with its own list padding, inside a `Dialog`,
 * inside a sheet. A `View` there has no bounded height of its own, so the list
 * could not scroll and the dialog grew to the whole screen, and the library's own
 * row insets are what pushed the text away from the edges. This file has no
 * `View` in it: the rows are Compose rows, the scroll is a Compose scroll with a
 * fixed maximum height.
 */
@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun TrackSelectionSheet(
    player: Player,
    trackType: Int,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val nameProvider = remember(context) { DefaultTrackNameProvider(context.resources) }
    val tracks: Tracks = player.currentTracks
    val groups = remember(tracks, trackType) { tracks.groups.filter { it.type == trackType } }

    // Which rows are ticked. media3 allows several tracks of one type to be
    // pinned at once and switches between them, which is what the checkboxes are
    // for; "None" is a radio because it excludes everything else.
    val pinned = remember(tracks, trackType) {
        buildSet {
            groups.forEach { group ->
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) add(group.mediaTrackGroup to i)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = trackTypeTitle(trackType),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Bounded, and scrollable inside the bound: a file with a
                    // dozen audio tracks has to be reachable, and this is the
                    // height the previous dialog could not keep to.
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
            ) {
                // "None" — media3's Disable.
                SelectableRow(
                    selected = pinned.isEmpty(),
                    onClick = {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(trackType, true)
                            .clearOverridesOfType(trackType)
                            .build()
                        onDismiss()
                    },
                    label = "None",
                ) { RadioButton(selected = pinned.isEmpty(), onClick = null) }

                groups.forEach { group ->
                    for (index in 0 until group.length) {
                        val mediaGroup = group.mediaTrackGroup
                        val isPinned = (mediaGroup to index) in pinned
                        val label = nameProvider.getTrackName(group.getTrackFormat(index))
                        SelectableRow(
                            selected = isPinned,
                            label = label,
                            onClick = {
                                val now = if (isPinned) {
                                    pinned - (mediaGroup to index)
                                } else {
                                    pinned + (mediaGroup to index)
                                }
                                player.trackSelectionParameters = player
                                    .trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(trackType, false)
                                    .clearOverridesOfType(trackType)
                                    .apply {
                                        now.groupBy({ it.first }, { it.second })
                                            .forEach { (group, indices) ->
                                                addOverride(
                                                    TrackSelectionOverride(group, indices)
                                                )
                                            }
                                    }
                                    .build()
                                // Ticking a track is the choice; the panel stays up
                                // so a second one can be added, as in the reference.
                            },
                        ) { Checkbox(checked = isPinned, onCheckedChange = null) }
                    }
                }

                if (groups.isEmpty()) {
                    Text(
                        text = "No tracks available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

/**
 * One row of the panel: [control] (a radio or a checkbox) then the label, with
 * NextPlayer's spacing.
 */
@Composable
private fun SelectableRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        control()
        RowLabel(label)
    }
}

/** `RadioButtonRow`'s label, verbatim. */
@Composable
private fun RowLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * NextPlayer's four video scales (`core/model/VideoContentScale.kt`), names and
 * behaviour from `VideoContentScaleSelectorView` and its `nameRes` mapping.
 */
enum class VideoScale(val label: String) {
    BEST_FIT("Best fit"),
    STRETCH("Stretch"),
    CROP("Crop"),
    HUNDRED_PERCENT("100%"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScaleSheet(
    current: VideoScale,
    onChange: (VideoScale) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Video zoom",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Column(modifier = Modifier.selectableGroup()) {
                VideoScale.entries.forEach { scale ->
                    SelectableRow(
                        selected = scale == current,
                        label = scale.label,
                        onClick = {
                            onChange(scale)
                            onDismiss()
                        },
                        control = { RadioButton(selected = scale == current, onClick = null) },
                    )
                }
            }
        }
    }
}

private fun trackTypeTitle(trackType: Int): String = when (trackType) {
    C.TRACK_TYPE_VIDEO -> "Video quality"
    C.TRACK_TYPE_AUDIO -> "Audio track"
    C.TRACK_TYPE_TEXT -> "Subtitles"
    else -> "Tracks"
}
