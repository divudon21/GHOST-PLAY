package com.ghost.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghost.video.R
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextSegmentedListItem
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.ui.components.tabBarClearance

/**
 * The open-source libraries this app is built from, ported from NextPlayer
 * (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/about/LibrariesScreen.kt
 *
 * Rows are [NextSegmentedListItem]s: name and artifact version on one line, the
 * authors under it, and one badge per licence. Tapping a row opens the
 * project's site when it has one.
 *
 * NextPlayer builds this list from `mikepenz.aboutlibraries`, which reads a
 * generated report at build time. That is a whole plugin for a screen that
 * changes when a dependency changes, so the same list is declared by hand in
 * [ComposeLibraries] — it is short, and each entry says something the build file
 * does not (what the project is, what licence it is under).
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun LibrariesScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val libraries = ComposeLibraries

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.libraries),
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding +
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = tabBarClearance(),
                ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(libraries, key = { _, library -> library.name }) { index, library ->
                NextSegmentedListItem(
                    content = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = library.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(weight = 1f),
                            )
                            library.version?.let { Text(text = it) }
                        }
                    },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = library.developer)
                            FlowRow(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                library.licenses.forEach {
                                    Badge(
                                        containerColor = MaterialTheme
                                            .colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            text = it,
                                            modifier = Modifier.padding(horizontal = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    },
                    isFirstItem = index == 0,
                    isLastItem = index == libraries.lastIndex,
                    onClick = {
                        library.website?.takeIf { it.isNotBlank() }?.let {
                            uriHandler.openUriOrShowToast(uri = it, context = context)
                        }
                    },
                )
            }
        }
    }
}

data class OpenSourceLibrary(
    val name: String,
    val version: String?,
    val developer: String,
    val licenses: List<String>,
    val website: String?,
)

/**
 * The third-party code in this app, as `docs/ATTRIBUTION.md` lists it. Ghost Play
 * itself is a port of NextPlayer, and its player screen a port of BitChord; both
 * are named here because both put code in this tree.
 */
val ComposeLibraries: List<OpenSourceLibrary> = listOf(
    OpenSourceLibrary(
        name = "NextPlayer",
        version = null,
        developer = "Anil Beesetti",
        licenses = listOf("GPL-3.0"),
        website = "https://github.com/anilbeesetti/nextplayer",
    ),
    OpenSourceLibrary(
        name = "BitChord",
        version = null,
        developer = "Kushagra Singh",
        licenses = listOf("GPL-3.0"),
        website = "https://github.com/kushagrasinghx/BitChord",
    ),
    OpenSourceLibrary(
        name = "AndroidX / Jetpack Compose",
        version = "compose-bom 2026.01.01",
        developer = "The Android Open Source Project",
        licenses = listOf("Apache-2.0"),
        website = "https://developer.android.com/jetpack",
    ),
    OpenSourceLibrary(
        name = "Media3 / ExoPlayer",
        version = "1.11.1",
        developer = "The Android Open Source Project",
        licenses = listOf("Apache-2.0"),
        website = "https://github.com/androidx/media",
    ),
    OpenSourceLibrary(
        name = "NextLib",
        version = "1.11.1-0.18.0",
        developer = "Anil Beesetti",
        licenses = listOf("GPL-3.0"),
        website = "https://github.com/anilbeesetti/nextlib",
    ),
    OpenSourceLibrary(
        name = "Coil",
        version = "3.3.0",
        developer = "Coil Contributors",
        licenses = listOf("Apache-2.0"),
        website = "https://github.com/coil-kt/coil",
    ),
    OpenSourceLibrary(
        name = "Kotlin & kotlinx.coroutines",
        version = null,
        developer = "JetBrains",
        licenses = listOf("Apache-2.0"),
        website = "https://github.com/JetBrains/kotlin",
    ),
    OpenSourceLibrary(
        name = "KotlinX Serialization",
        version = "1.9.0",
        developer = "JetBrains",
        licenses = listOf("Apache-2.0"),
        website = "https://github.com/Kotlin/kotlinx.serialization",
    ),
    OpenSourceLibrary(
        name = "AndroidX Activity, Lifecycle, Navigation",
        version = null,
        developer = "The Android Open Source Project",
        licenses = listOf("Apache-2.0"),
        website = "https://developer.android.com/jetpack/androidx",
    ),
    OpenSourceLibrary(
        name = "DataStore",
        version = "1.2.0",
        developer = "The Android Open Source Project",
        licenses = listOf("Apache-2.0"),
        website = "https://developer.android.com/jetpack/androidx/releases/datastore",
    ),
    OpenSourceLibrary(
        name = "WorkManager",
        version = "2.10.0",
        developer = "The Android Open Source Project",
        licenses = listOf("Apache-2.0"),
        website = "https://developer.android.com/jetpack/androidx/releases/work",
    ),
    OpenSourceLibrary(
        name = "Cloudy",
        version = "0.1.2",
        developer = "Jaewoong Eum (skydoves)",
        licenses = listOf("Apache-2.0"),
        website = "https://github.com/skydoves/cloudy",
    ),
    OpenSourceLibrary(
        name = "OkHttp",
        version = null,
        developer = "Square",
        licenses = listOf("Apache-2.0"),
        website = "https://github.com/square/okhttp",
    ),
)
