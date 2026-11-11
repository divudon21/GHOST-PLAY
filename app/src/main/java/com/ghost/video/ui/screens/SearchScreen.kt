package com.ghost.video.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.R
import com.ghost.video.ui.components.tabBarClearance
import com.ghost.video.ui.components.EmptyState
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.tabSwipe
import com.ghost.video.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Video search, reachable from the bottom bar's Search tab and from the search
 * button on Home. Searches the device's video library (and can open a pasted
 * video link straight away).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onPlayUrl: (String) -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
    onSwipeNext: () -> Unit = {},
    onSwipePrevious: () -> Unit = {},
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    var query by rememberSaveable { mutableStateOf("") }
    var recentSearches by rememberSaveable { mutableStateOf(listOf<String>()) }
    var videos by remember { mutableStateOf(SearchCache.videos) }
    var hasPermission by remember { mutableStateOf(checkVideoPermission(context)) }
    var loading by remember { mutableStateOf(SearchCache.videos.isEmpty()) }

    val thumbnailStrategy by settingsViewModel.thumbnailStrategy.collectAsState()
    val thumbnailPositionPercent by settingsViewModel.thumbnailPositionPercent.collectAsState()
    val loadingIndicatorStyle by settingsViewModel.loadingIndicatorStyle.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions -> hasPermission = permissions.values.all { it } }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(permissions)
        } else if (SearchCache.videos.isEmpty()) {
            loading = true
            val loaded: List<LocalVideo> = withContext(Dispatchers.IO) {
                loadLocalVideos(context).filterNot { it.isAudio }
            }
            SearchCache.videos = loaded
            videos = loaded
            loading = false
        }
    }

    SearchAutoFocus(settingsViewModel.searchAutoKeyboard, focusRequester)

    val trimmedQuery = query.trim()
    val results = remember(videos, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            emptyList()
        } else {
            videos.filter { it.name.contains(trimmedQuery, ignoreCase = true) }
                .sortedBy { it.name.lowercase() }
        }
    }
    val isLink = remember(trimmedQuery) {
        trimmedQuery.startsWith("http://", ignoreCase = true) ||
            trimmedQuery.startsWith("https://", ignoreCase = true)
    }

    fun rememberQuery(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        recentSearches = (listOf(clean) + recentSearches.filterNot { it.equals(clean, true) })
            .take(6)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .tabSwipe(onSwipeNext, onSwipePrevious),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        ) {
            Text(
                text = stringResource(id = R.string.search_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            )

            SearchField(
                query = query,
                onQueryChange = { query = it },
                focusRequester = focusRequester,
                onSubmit = {
                    rememberQuery(query)
                    keyboard?.hide()
                },
            )

            AnimatedVisibility(
                visible = trimmedQuery.isEmpty() && recentSearches.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(id = R.string.recent_searches),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { recentSearches = emptyList() }) {
                            Text(stringResource(id = R.string.clear_all))
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentSearches.forEach { recent ->
                            AssistChip(
                                onClick = {
                                    query = recent
                                    rememberQuery(recent)
                                },
                                label = {
                                    Text(
                                        text = recent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            when {
                !hasPermission -> EmptyState(
                    icon = NextIcons.Lock,
                    title = stringResource(id = R.string.search_permission_title),
                    subtitle = stringResource(id = R.string.search_permission_subtitle),
                    modifier = Modifier.weight(1f),
                )

                isLink -> LinkResult(
                    link = trimmedQuery,
                    onPlay = {
                        rememberQuery(trimmedQuery)
                        onPlayUrl(trimmedQuery)
                    },
                )

                trimmedQuery.isEmpty() -> EmptyState(
                    icon = NextIcons.Search,
                    title = stringResource(id = R.string.search_empty_title),
                    subtitle = stringResource(id = R.string.search_empty_subtitle),
                    modifier = Modifier.weight(1f),
                )

                results.isEmpty() && !loading -> EmptyState(
                    icon = NextIcons.Search,
                    title = stringResource(id = R.string.no_videos_found),
                    subtitle = stringResource(R.string.no_results_for, trimmedQuery),
                    modifier = Modifier.weight(1f),
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 168.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp + tabBarClearance(),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(results, key = { it.uri }) { video ->
                        VideoThumbnailCard(
                            video = video,
                            strategy = thumbnailStrategy,
                            positionPercent = thumbnailPositionPercent,
                            loadingIndicatorStyle = loadingIndicatorStyle,
                            compact = true,
                            onClick = {
                                rememberQuery(trimmedQuery)
                                onPlayUrl(video.uri)
                            },
                            onChanged = {},
                        )
                    }
                }
            }
        }
    }

    // Keep the newest query in the history once results are shown.
    LaunchedEffect(trimmedQuery) {
        if (trimmedQuery.length > 2 && results.isNotEmpty()) rememberQuery(trimmedQuery)
    }
}

/** Apply the persisted choice once per visit, without stealing focus on later updates. */
@Composable
internal fun SearchAutoFocus(enabled: StateFlow<Boolean?>, focusRequester: FocusRequester) {
    LaunchedEffect(enabled, focusRequester) {
        // Wait for DataStore rather than briefly opening the keyboard for a saved off setting.
        if (enabled.filterNotNull().first()) focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester),
            placeholder = { Text(stringResource(id = R.string.search_videos)) },
            leadingIcon = {
                Icon(
                    imageVector = NextIcons.Search,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = NextIcons.Close,
                            contentDescription = stringResource(id = R.string.clear_search),
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSubmit() },
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun LinkResult(link: String, onPlay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.play_this_link),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = link,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                FilledTonalButton(
                    onClick = onPlay,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Icon(
                        imageVector = NextIcons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(id = R.string.play),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

/** Survives tab switches so search results do not re-query MediaStore. */
private object SearchCache {
    var videos: List<LocalVideo> = emptyList()
}
