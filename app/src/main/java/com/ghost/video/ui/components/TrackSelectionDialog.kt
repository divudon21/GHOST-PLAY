package com.ghost.video.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTrackNameProvider
import androidx.media3.ui.TrackSelectionView

/**
 * Track selection, ported from media3's own demo app (Apache-2.0):
 * https://github.com/androidx/media/blob/main/demos/main/src/main/java/androidx/media3/demo/main/TrackSelectionDialog.java
 *
 * That dialog is what the reference player this app's player screen follows
 * shows: a tab per track type that has tracks — Video, Audio, Subtitles — with
 * media3's own `TrackSelectionView` inside each one (`None` and `Auto` as radios,
 * then one checkbox per concrete format, so several can be pinned at once), and
 * CANCEL / OK.
 *
 * Differences from the demo, and why:
 *
 * - The tabs are Compose's `TabRow`, and each type's state lives in Compose
 *   instead of a retained fragment. The demo needs fragments because a ViewPager
 *   destroys pages; here a tab's `TrackSelectionView` is re-created from the
 *   saved state when it is shown again, so ticks made in one tab survive a look
 *   at the other.
 * - `exo_track_selection_dialog` is not used. That layout is internal to the UI
 *   library and carries its own title and buttons; `TrackSelectionView` is public
 *   API and is hosted directly, inside this dialog's own surface.
 * - `DefaultTrackNameProvider` is set explicitly, so rows read the way the demo's
 *   do ("English", "1024 × 576, 1.68 Mbps") rather than whatever the default is.
 */
@OptIn(UnstableApi::class)
@Composable
fun TabbedTrackSelectionDialog(
    player: Player,
    onDismissRequest: () -> Unit,
    initialTrackType: Int = C.TRACK_TYPE_VIDEO,
) {
    val tracks: Tracks = player.currentTracks
    val parameters: TrackSelectionParameters = player.trackSelectionParameters

    // One tab per type that has tracks, in the demo's order. Remembered on the
    // track list so the dialog does not re-shuffle under the finger while it is
    // open.
    val trackTypes = remember(tracks) {
        SUPPORTED_TRACK_TYPES.filter { type -> tracks.groups.any { it.type == type } }
    }

    // The four fields the demo's fragment carries, per type.
    class TabState(
        val groups: List<Tracks.Group>,
        disabled: Boolean,
        overrides: Map<TrackGroup, TrackSelectionOverride>,
    ) {
        var isDisabled by mutableStateOf(disabled)
        var overrides by mutableStateOf(overrides)
    }

    val states = remember(tracks, parameters) {
        trackTypes.associateWith { type ->
            val groups = tracks.groups.filter { it.type == type }
            TabState(
                groups = groups,
                disabled = parameters.disabledTrackTypes.contains(type),
                // Filtered here as well as in TrackSelectionView, for a tab the
                // user never opens — the demo notes the same requirement.
                overrides = TrackSelectionView.filterOverrides(
                    parameters.overrides,
                    groups,
                    /* allowMultipleOverrides = */ false,
                ),
            )
        }
    }

    val startIndex = trackTypes.indexOf(initialTrackType).coerceAtLeast(0)
    var selectedIndex by remember(trackTypes) { mutableIntStateOf(startIndex) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Select tracks",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )

                // The demo hides the tab strip when there is only one type.
                if (trackTypes.size > 1) {
                    PrimaryTabRow(selectedTabIndex = selectedIndex) {
                        trackTypes.forEachIndexed { index, type ->
                            Tab(
                                selected = index == selectedIndex,
                                onClick = { selectedIndex = index },
                                text = { Text(trackTypeTitle(type)) },
                            )
                        }
                    }
                }

                val currentType = trackTypes.getOrNull(selectedIndex)
                val current = currentType?.let { states[it] }
                if (currentType != null && current != null) {
                    // keyed so switching tabs builds a fresh view initialised from
                    // that tab's saved state while the previous one goes away.
                    key(currentType) {
                        AndroidView(
                            factory = { context ->
                                TrackSelectionView(context).apply {
                                    setShowDisableOption(true)
                                    setAllowMultipleOverrides(false)
                                    setAllowAdaptiveSelections(true)
                                    setTrackNameProvider(
                                        DefaultTrackNameProvider(context.resources)
                                    )
                                    init(
                                        current.groups,
                                        current.isDisabled,
                                        current.overrides,
                                        /* trackFormatComparator = */ null,
                                        /* listener = */ TrackSelectionView.TrackSelectionListener {
                                            disabled, overrides ->
                                            current.isDisabled = disabled
                                            current.overrides = overrides
                                        },
                                    )
                                }
                            },
                            update = { /* nothing: the view owns its own ticks */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.textButtonColors(),
                    ) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            // The demo's OK, track type by track type.
                            val builder = parameters.buildUpon()
                            for (type in SUPPORTED_TRACK_TYPES) {
                                val state = states[type] ?: continue
                                builder.setTrackTypeDisabled(type, state.isDisabled)
                                builder.clearOverridesOfType(type)
                                state.overrides.values.forEach { builder.addOverride(it) }
                            }
                            player.trackSelectionParameters = builder.build()
                            onDismissRequest()
                        }
                    ) { Text("OK") }
                }
            }
        }
    }
}

/** The demo's supported types, in its order. */
private val SUPPORTED_TRACK_TYPES = listOf(
    C.TRACK_TYPE_VIDEO,
    C.TRACK_TYPE_AUDIO,
    C.TRACK_TYPE_TEXT,
    C.TRACK_TYPE_IMAGE,
)

private fun trackTypeTitle(trackType: Int): String = when (trackType) {
    C.TRACK_TYPE_VIDEO -> "Video"
    C.TRACK_TYPE_AUDIO -> "Audio"
    C.TRACK_TYPE_TEXT -> "Subtitles"
    C.TRACK_TYPE_IMAGE -> "Image"
    else -> "Track"
}
