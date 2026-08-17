package com.ghost.video.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.ghost.video.R
import com.ghost.video.data.SettingsRepository
import com.ghost.video.data.ThumbnailStrategy
import com.ghost.video.data.ViewLayout
import com.ghost.video.data.VideoThumbnailPipeline
import com.ghost.video.ui.components.AppLoadingIndicator
import com.ghost.video.ui.components.EmptyState
import com.ghost.video.ui.components.tabSwipe
import com.ghost.video.viewmodel.SettingsViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image

data class LocalVideo(
    val id: Long,
    val name: String,
    val uri: String,
    val isAudio: Boolean = false,
    val duration: Long = 0,
    val size: Long = 0,
    val width: Int = 0,
    val height: Int = 0
)

// In-memory cache so the video list survives tab switches without re-querying.
private var cachedVideos: List<LocalVideo>? = null

@Composable
fun HomeScreen(
    onPlayUrl: (String) -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
    onSwipeNext: () -> Unit = {},
    onSwipePrevious: () -> Unit = {}
) {
    var url by remember { mutableStateOf("") }
    var showUrlInput by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    // Cache keeps the list alive across tab switches, so coming back to Home
    // does NOT re-query MediaStore (the old reload caused tab-switch jitter).
    var localVideos by remember { mutableStateOf(cachedVideos ?: emptyList()) }
    var hasPermission by remember { mutableStateOf(checkVideoPermission(context)) }
    var refreshKey by remember { mutableStateOf(0) }
    val viewLayout by settingsViewModel.viewLayout.collectAsState()
    // Read thumbnail settings ONCE at screen level and pass them down. Reading a
    // DataStore repository inside every list item (as before) created dozens of
    // flow collectors and caused scroll/tap jitter.
    val thumbnailStrategy by settingsViewModel.thumbnailStrategy.collectAsState()
    val thumbnailPositionPercent by settingsViewModel.thumbnailPositionPercent.collectAsState()
    // Battery saver skips thumbnail decoding (the most power-hungry work on this
    // screen) and shows a lightweight placeholder instead.
    val batterySaver by settingsViewModel.batterySaver.collectAsState()
    val loadingIndicatorStyle by settingsViewModel.loadingIndicatorStyle.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
    }

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    LaunchedEffect(hasPermission, refreshKey) {
        if (hasPermission) {
            val cached = cachedVideos
            if (cached == null || refreshKey > 0) {
                val loaded = loadLocalVideos(context)
                cachedVideos = loaded
                localVideos = loaded
            } else {
                localVideos = cached
            }
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    val reload: () -> Unit = { refreshKey++ }

    // Search filtering is memoized: it only recomputes when the query or the
    // video list actually changes, so typing is smooth and cheap.
    val filteredVideos = remember(localVideos, searchQuery) {
        if (searchQuery.isBlank()) localVideos
        else localVideos.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .tabSwipe(onSwipeNext, onSwipePrevious)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_fireplay),
                    contentDescription = "Ghost Play",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (showSearch) {
                        showSearch = false
                        focusManager.clearFocus()
                    } else {
                        showSearch = true
                        showUrlInput = false
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Search videos",
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(onClick = {
                    if (showUrlInput) {
                        showUrlInput = false
                        focusManager.clearFocus()
                    } else {
                        showUrlInput = true
                        showSearch = false
                    }
                }) {
                    Icon(painterResource(id = R.drawable.ic_url), contentDescription = "Add URL", modifier = Modifier.size(28.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = showSearch,
            enter = fadeIn(animationSpec = tween(260)) + expandVertically(animationSpec = tween(260)),
            exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search videos…") },
                leadingIcon = {
                    Icon(painterResource(id = R.drawable.ic_search), contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp)
            )
        }
        
        AnimatedVisibility(
            visible = showUrlInput,
            enter = fadeIn(animationSpec = tween(260)) + expandVertically(animationSpec = tween(260)),
            exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Enter Video URL") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (url.isNotEmpty()) onPlayUrl(url)
                        focusManager.clearFocus()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (url.isNotEmpty()) {
                            IconButton(onClick = { url = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                            IconButton(onClick = { onPlayUrl(url) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                            }
                        } else {
                            IconButton(onClick = {
                                clipboardManager.getText()?.text?.let { url = it }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste from Clipboard")
                            }
                        }
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (!hasPermission) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Storage permission required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Allow access to show your local videos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { permissionLauncher.launch(permissionsToRequest) },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Grant Permission")
                    }
                    OutlinedButton(
                        onClick = { openAppSettings() },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Open Settings")
                    }
                }
            }
        } else if (localVideos.isEmpty()) {
            EmptyState(
                icon = Icons.Default.VideoFile,
                title = "No videos found",
                subtitle = "Your local videos will appear here once they're detected."
            )
        } else if (filteredVideos.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Search,
                title = "No matches",
                subtitle = "Try a different search term."
            )
        } else {
            when (viewLayout) {
                ViewLayout.LIST -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredVideos, key = { it.uri }) { video ->
                            VideoListItem(
                                video = video,
                                strategy = thumbnailStrategy,
                                positionPercent = thumbnailPositionPercent,
                                batterySaver = batterySaver,
                                loadingIndicatorStyle = loadingIndicatorStyle,
                                onClick = { onPlayUrl(video.uri) },
                                onChanged = reload
                            )
                        }
                    }
                }
                ViewLayout.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredVideos, key = { it.uri }) { video ->
                            VideoThumbnailCard(
                                video = video,
                                strategy = thumbnailStrategy,
                                positionPercent = thumbnailPositionPercent,
                                batterySaver = batterySaver,
                                loadingIndicatorStyle = loadingIndicatorStyle,
                                onClick = { onPlayUrl(video.uri) },
                                onChanged = reload
                            )
                        }
                    }
                }
                ViewLayout.COMPACT_GRID -> {
                    // Compact is a clean thumbnail-first mosaic: titles sit on the
                    // artwork, avoiding cramped metadata below each tiny card.
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredVideos, key = { it.uri }) { video ->
                            VideoThumbnailCard(
                                video = video,
                                strategy = thumbnailStrategy,
                                positionPercent = thumbnailPositionPercent,
                                batterySaver = batterySaver,
                                loadingIndicatorStyle = loadingIndicatorStyle,
                                compact = true,
                                onClick = { onPlayUrl(video.uri) },
                                onChanged = reload
                            )
                        }
                    }
                }
                ViewLayout.CINEMA -> {
                    // Cinema = "featured first": the newest video spans the full
                    // width as a hero card, the rest fill a 2-column grid below.
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (filteredVideos.isNotEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                val video = filteredVideos.first()
                                VideoThumbnailCard(
                                    video = video,
                                    strategy = thumbnailStrategy,
                                    positionPercent = thumbnailPositionPercent,
                                    batterySaver = batterySaver,
                                    loadingIndicatorStyle = loadingIndicatorStyle,
                                    onClick = { onPlayUrl(video.uri) },
                                    onChanged = reload
                                )
                            }
                        }
                        items(filteredVideos.drop(1), key = { it.uri }) { video ->
                            VideoThumbnailCard(
                                video = video,
                                strategy = thumbnailStrategy,
                                positionPercent = thumbnailPositionPercent,
                                batterySaver = batterySaver,
                                loadingIndicatorStyle = loadingIndicatorStyle,
                                onClick = { onPlayUrl(video.uri) },
                                onChanged = reload
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoThumbnailCard(
    video: LocalVideo,
    strategy: ThumbnailStrategy,
    positionPercent: Int,
    batterySaver: Boolean,
    loadingIndicatorStyle: com.ghost.video.data.LoadingIndicatorStyle,
    compact: Boolean = false,
    onClick: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    // Seed from the in-memory cache so a cached thumbnail shows instantly with no
    // loader flash (which used to cause a visible flicker/jitter).
    val cacheKey = "video:${video.uri}:$strategy:$positionPercent"
    val cached = remember(cacheKey) { com.ghost.video.data.ThumbnailCache.get(cacheKey) }
    var thumbnailBitmap by remember(cacheKey) { mutableStateOf(cached) }
    var isLoading by remember(cacheKey, batterySaver) { mutableStateOf(cached == null && !batterySaver) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(cacheKey, batterySaver) {
        // The bounded pipeline starts with currently composed (visible) cards,
        // delivers a quick preview first, then crossfades to the final frame.
        if (cached == null && !batterySaver) {
            isLoading = true
            VideoThumbnailPipeline.load(
                context = context,
                uri = video.uri,
                durationMs = video.duration,
                strategy = strategy,
                positionPercent = positionPercent,
                onPreview = { thumbnailBitmap = it },
                onFinal = {
                    thumbnailBitmap = it
                    isLoading = false
                }
            )
        }
    }

    val displayName = remember(video.name) { cleanVideoName(video.name) }

    VideoActions(video = video, expanded = menuExpanded, onDismiss = { menuExpanded = false }, onPlay = onClick, onChanged = onChanged) { menuActions ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color(0xFF0E0E10))
                ) {
                    if (thumbnailBitmap != null) {
                        Crossfade(
                            targetState = thumbnailBitmap,
                            animationSpec = tween(durationMillis = 180),
                            label = "videoThumbnail"
                        ) { bitmap ->
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Video Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AppLoadingIndicator(
                                style = loadingIndicatorStyle,
                                size = 40.dp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    if (video.duration > 0) {
                        DurationBadge(
                            text = formatDurationMillis(video.duration),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        )
                    }
                    if (compact) {
                        Text(
                            text = displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 6.dp, bottom = 6.dp, end = 56.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                if (!compact) Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        MetadataRow(video = video)
                    }
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        menuActions()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoListItem(
    video: LocalVideo,
    strategy: ThumbnailStrategy,
    positionPercent: Int,
    batterySaver: Boolean,
    loadingIndicatorStyle: com.ghost.video.data.LoadingIndicatorStyle,
    onClick: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val cacheKey = "video:${video.uri}:$strategy:$positionPercent"
    val cached = remember(cacheKey) { com.ghost.video.data.ThumbnailCache.get(cacheKey) }
    var thumbnailBitmap by remember(cacheKey) { mutableStateOf(cached) }
    var isLoading by remember(cacheKey, batterySaver) { mutableStateOf(cached == null && !batterySaver) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(cacheKey, batterySaver) {
        // The bounded pipeline starts with currently composed (visible) cards,
        // delivers a quick preview first, then crossfades to the final frame.
        if (cached == null && !batterySaver) {
            isLoading = true
            VideoThumbnailPipeline.load(
                context = context,
                uri = video.uri,
                durationMs = video.duration,
                strategy = strategy,
                positionPercent = positionPercent,
                onPreview = { thumbnailBitmap = it },
                onFinal = {
                    thumbnailBitmap = it
                    isLoading = false
                }
            )
        }
    }

    val displayName = remember(video.name) { cleanVideoName(video.name) }

    VideoActions(video = video, expanded = menuExpanded, onDismiss = { menuExpanded = false }, onPlay = onClick, onChanged = onChanged) { menuActions ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(148.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0E0E10))
                ) {
                    if (thumbnailBitmap != null) {
                        Crossfade(
                            targetState = thumbnailBitmap,
                            animationSpec = tween(durationMillis = 180),
                            label = "videoThumbnail"
                        ) { bitmap ->
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Video Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AppLoadingIndicator(
                                style = loadingIndicatorStyle,
                                size = 40.dp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    if (video.duration > 0) {
                        DurationBadge(
                            text = formatDurationMillis(video.duration),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    MetadataRow(video = video)
                }

                Box(contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    menuActions()
                }
            }
        }
    }
}

/**
 * Clean metadata row: size and a compact resolution "pill", separated by a subtle
 * dot. Keeps the card easy to scan without a long run-on subtitle string.
 */
@Composable
private fun MetadataRow(video: LocalVideo) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val resolution = if (video.width > 0 && video.height > 0)
            resolutionLabel(video.width, video.height) else ""
        if (resolution.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = resolution,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (video.size > 0) {
            Text(
                text = formatFileSize(video.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** Small, rounded duration badge sitting on a subtle gradient in the corner. */
@Composable
private fun DurationBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Wraps a card and provides its dropdown menu + all the dialogs/launchers needed
 * for Play / Share / Rename / Details / Delete actions.
 */
@Composable
private fun VideoActions(
    video: LocalVideo,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onChanged: () -> Unit,
    content: @Composable (menu: @Composable () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showDetails by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Video deleted", Toast.LENGTH_SHORT).show()
            onChanged()
        }
    }

    var pendingRename by remember { mutableStateOf<Pair<String, String>?>(null) }

    val writeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Write permission granted: apply the rename that was waiting.
            pendingRename?.let { (uri, finalName) ->
                applyMediaRename(context, uri, finalName)
                Toast.makeText(context, "Video renamed", Toast.LENGTH_SHORT).show()
            }
            pendingRename = null
        }
        onChanged()
    }

    content {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            DropdownMenuItem(
                text = { Text("Play") },
                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                onClick = { onDismiss(); onPlay() }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = {
                    onDismiss()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/*"
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(video.uri))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share video"))
                }
            )
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                onClick = { onDismiss(); showRename = true }
            )
            DropdownMenuItem(
                text = { Text("Details") },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                onClick = { onDismiss(); showDetails = true }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    onDismiss()
                    deleteVideo(context, video, deleteLauncher::launch, onChanged)
                }
            )
        }
    }

    if (showDetails) {
        VideoDetailsDialog(video = video, onDismiss = { showDetails = false })
    }

    if (showRename) {
        RenameDialog(
            currentName = video.name,
            onDismiss = { showRename = false },
            onConfirm = { newName ->
                showRename = false
                val uri = video.uri
                // Preserve the original extension (.mp4 etc).
                val ext = video.name.substringAfterLast('.', "")
                val finalName = if (ext.isNotEmpty() && !newName.endsWith(".$ext")) "$newName.$ext" else newName
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                }
                try {
                    val rows = context.contentResolver.update(Uri.parse(uri), values, null, null)
                    if (rows > 0) {
                        Toast.makeText(context, "Video renamed", Toast.LENGTH_SHORT).show()
                        onChanged()
                    }
                } catch (e: SecurityException) {
                    // Android 11+: ask for write access, then re-apply the rename
                    // in the writeLauncher callback (this retry was the missing
                    // piece that made rename appear broken).
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            pendingRename = uri to finalName
                            val pi = MediaStore.createWriteRequest(context.contentResolver, listOf(Uri.parse(uri)))
                            writeLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }
            }
        )
    }
}

@Composable
private fun VideoDetailsDialog(video: LocalVideo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        title = { Text("Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Name", cleanVideoName(video.name))
                DetailRow("File name", video.name)
                if (video.size > 0) DetailRow("Size", formatFileSize(video.size))
                if (video.width > 0 && video.height > 0) {
                    DetailRow("Resolution", "${video.width} x ${video.height} (${resolutionLabel(video.width, video.height)})")
                }
                if (video.duration > 0) DetailRow("Duration", formatDurationMillis(video.duration))
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RenameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    // Show the FULL file name (including the extension) so the user can see the
    // ".mp4" etc. The extension is preserved automatically on rename.
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) })
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Rename",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        title = { Text("Rename video") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("New name") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "The file extension is kept automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

private fun deleteVideo(
    context: Context,
    video: LocalVideo,
    launch: (IntentSenderRequest) -> Unit,
    onChanged: () -> Unit
) {
    val uri = Uri.parse(video.uri)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
        launch(IntentSenderRequest.Builder(pi.intentSender).build())
    } else {
        try {
            context.contentResolver.delete(uri, null, null)
            Toast.makeText(context, "Video deleted", Toast.LENGTH_SHORT).show()
            onChanged()
        } catch (e: Exception) {
            // ignore
        }
    }
}

/** Apply a rename to a MediaStore item once write access is available. */
private fun applyMediaRename(context: Context, uri: String, finalName: String): Boolean {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
        }
        context.contentResolver.update(Uri.parse(uri), values, null, null) > 0
    } catch (e: Exception) {
        false
    }
}

/** Strip extension, replace separators with spaces, tidy up common camera prefixes. */
fun cleanVideoName(raw: String): String {
    var name = raw.substringBeforeLast('.', raw)
    name = name.replace('_', ' ').replace('-', ' ').replace('.', ' ')
    name = name.replace(Regex("\\s+"), " ").trim()
    return if (name.isEmpty()) raw else name
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return if (digitGroups == 0) "${value.toInt()} ${units[digitGroups]}"
    else String.format("%.1f %s", value, units[digitGroups])
}

/** Map pixel height to a friendly label like 1080p / 4K. */
fun resolutionLabel(width: Int, height: Int): String {
    val shortSide = minOf(width, height)
    return when {
        shortSide >= 2160 -> "4K"
        shortSide >= 1440 -> "1440p"
        shortSide >= 1080 -> "1080p"
        shortSide >= 720 -> "720p"
        shortSide >= 480 -> "480p"
        shortSide >= 360 -> "360p"
        shortSide > 0 -> "${shortSide}p"
        else -> ""
    }
}

fun formatDurationMillis(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

fun checkVideoPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

suspend fun loadLocalVideos(context: Context): List<LocalVideo> = withContext(Dispatchers.IO) {
    val videos = mutableListOf<LocalVideo>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT
    )

    context.contentResolver.query(
        collection,
        projection,
        null,
        null,
        "${MediaStore.Video.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
        val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val duration = cursor.getLong(durationColumn)
            val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
            val width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 0
            val height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 0
            val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            videos.add(LocalVideo(id, name, contentUri.toString(), isAudio = false, duration = duration, size = size, width = width, height = height))
        }
    }
    
    videos
}
