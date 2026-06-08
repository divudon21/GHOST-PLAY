package com.ghost.io.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.ghost.io.R
import com.ghost.io.data.SettingsRepository
import com.ghost.io.data.ThumbnailStrategy
import com.ghost.io.data.ViewLayout
import com.ghost.io.viewmodel.SettingsViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image

data class LocalVideo(val id: Long, val name: String, val uri: String, val isAudio: Boolean = false, val duration: Long = 0)

@Composable
fun HomeScreen(
    onPlayUrl: (String) -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    var url by remember { mutableStateOf("") }
    var showUrlInput by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var localVideos by remember { mutableStateOf<List<LocalVideo>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(checkVideoPermission(context)) }
    val viewLayout by settingsViewModel.viewLayout.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            localVideos = loadLocalVideos(context)
        } else {
            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                Text(
                    "GHOST PLAY",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { showUrlInput = !showUrlInput }) {
                Icon(painterResource(id = R.drawable.ic_url), contentDescription = "Add URL", modifier = Modifier.size(28.dp))
            }
        }
        
        AnimatedVisibility(
            visible = showUrlInput,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300), initialOffsetY = { -it }),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300), targetOffsetY = { -it })
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Enter Video URL") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                trailingIcon = {
                    IconButton(onClick = { if (url.isNotEmpty()) onPlayUrl(url) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (!hasPermission) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Storage permission required to show local videos.")
            }
        } else if (localVideos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No local videos found.")
            }
        } else {
            if (viewLayout == ViewLayout.LIST) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(localVideos) { video ->
                        VideoListItem(
                            video = video,
                            onClick = { onPlayUrl(video.uri) }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(localVideos) { video ->
                        VideoThumbnailCard(
                            video = video,
                            onClick = { onPlayUrl(video.uri) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoThumbnailCard(video: LocalVideo, onClick: () -> Unit) {
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val settingsRepository = remember { SettingsRepository(context) }
    val thumbnailStrategy by settingsRepository.thumbnailStrategy.collectAsState(initial = ThumbnailStrategy.HYBRID)
    val positionPercent by settingsRepository.thumbnailPositionPercent.collectAsState(initial = 33)

    LaunchedEffect(video.uri, thumbnailStrategy, positionPercent) {
        isLoading = true
        thumbnailBitmap = getVideoThumbnail(context, video.uri, video.duration, thumbnailStrategy, positionPercent)
        isLoading = false
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            } else if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                // Fallback when no thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            // Video name overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Play icon overlay
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

suspend fun getVideoThumbnail(
    context: Context,
    uri: String,
    duration: Long,
    strategy: ThumbnailStrategy = ThumbnailStrategy.HYBRID,
    positionPercent: Int = 33
): Bitmap? = withContext(Dispatchers.IO) {
    var retriever: MediaMetadataRetriever? = null
    try {
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, android.net.Uri.parse(uri))

        when (strategy) {
            ThumbnailStrategy.FIRST_FRAME -> {
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
            ThumbnailStrategy.FRAME_AT_POSITION -> {
                val positionUs = (duration * 1000L * positionPercent) / 100L
                retriever.getFrameAtTime(positionUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
            ThumbnailStrategy.HYBRID -> {
                // Try first frame
                val firstFrame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (firstFrame != null && !isMostlySolidColor(firstFrame)) {
                    firstFrame
                } else {
                    // First frame is solid color, try position-based frame
                    val positionUs = (duration * 1000L * positionPercent) / 100L
                    retriever.getFrameAtTime(positionUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: firstFrame
                        ?: retriever.getFrameAtTime(-1)
                }
            }
        }
    } catch (e: Exception) {
        null
    } finally {
        retriever?.release()
    }
}

private fun isMostlySolidColor(bitmap: Bitmap): Boolean {
    val width = bitmap.width
    val height = bitmap.height
    if (width < 2 || height < 2) return false

    // Sample pixels from corners and center
    val samplePoints = listOf(
        Pair(width / 4, height / 4),
        Pair(width * 3 / 4, height / 4),
        Pair(width / 4, height * 3 / 4),
        Pair(width * 3 / 4, height * 3 / 4),
        Pair(width / 2, height / 2)
    )

    val colors = samplePoints.map { (x, y) -> bitmap.getPixel(x, y) }
    val firstColor = colors.first()

    // Check if all sampled pixels are very similar to the first one
    val similarCount = colors.count { color ->
        val rDiff = kotlin.math.abs(android.graphics.Color.red(color) - android.graphics.Color.red(firstColor))
        val gDiff = kotlin.math.abs(android.graphics.Color.green(color) - android.graphics.Color.green(firstColor))
        val bDiff = kotlin.math.abs(android.graphics.Color.blue(color) - android.graphics.Color.blue(firstColor))
        rDiff < 15 && gDiff < 15 && bDiff < 15
    }

    return similarCount >= 4 // At least 4 out of 5 points are similar
}

@Composable
fun VideoListItem(video: LocalVideo, onClick: () -> Unit) {
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val settingsRepository = remember { SettingsRepository(context) }
    val thumbnailStrategy by settingsRepository.thumbnailStrategy.collectAsState(initial = ThumbnailStrategy.HYBRID)
    val positionPercent by settingsRepository.thumbnailPositionPercent.collectAsState(initial = 33)

    LaunchedEffect(video.uri, thumbnailStrategy, positionPercent) {
        isLoading = true
        thumbnailBitmap = getVideoThumbnail(context, video.uri, video.duration, thumbnailStrategy, positionPercent)
        isLoading = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = "Video Thumbnail",
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Duration badge
                if (video.duration > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDurationMillis(video.duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title and path
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.uri,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
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
        MediaStore.Video.Media.DURATION
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

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val duration = cursor.getLong(durationColumn)
            val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            videos.add(LocalVideo(id, name, contentUri.toString(), isAudio = false, duration = duration))
        }
    }
    
    videos
}
