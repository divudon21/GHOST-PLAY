package com.ghost.video.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Parcelable
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.ui.components.EmptyState
import com.ghost.video.ui.components.tabSwipe
import com.ghost.video.viewmodel.AudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalAudio(val id: Long, val name: String, val uri: String, val artist: String?) : Parcelable

// In-memory cache so the audio list survives tab switches without re-querying.
private var cachedAudios: List<LocalAudio>? = null

private const val AUDIO_PAGE_SIZE = 60

@Composable
fun AudioScreen(
    viewModel: AudioViewModel = viewModel(),
    onSwipeNext: () -> Unit = {},
    onSwipePrevious: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentAudio by viewModel.currentAudio.collectAsState()
    var localAudios by remember { mutableStateOf(cachedAudios ?: emptyList()) }
    var hasPermission by remember { mutableStateOf(checkAudioPermission(context)) }
    var isLoadingPage by remember { mutableStateOf(false) }
    var hasMorePages by remember { mutableStateOf(true) }
    var nextOffset by remember { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }

    fun loadNextPage() {
        if (!hasPermission || isLoadingPage || !hasMorePages) return
        isLoadingPage = true
        val offsetToLoad = nextOffset
        scope.launch {
            val page = loadLocalAudiosPage(
                context = context,
                limit = AUDIO_PAGE_SIZE,
                offset = offsetToLoad
            )
            localAudios = if (offsetToLoad == 0) page else localAudios + page
            nextOffset = offsetToLoad + page.size
            hasMorePages = page.size == AUDIO_PAGE_SIZE
            viewModel.setPlaylist(localAudios)
            cachedAudios = localAudios
            isLoadingPage = false
        }
    }

    LaunchedEffect(hasPermission, refreshKey) {
        if (hasPermission) {
            val cached = cachedAudios
            if (refreshKey > 0) {
                // A rename/delete happened: full re-scan from page one.
                cachedAudios = null
                localAudios = emptyList()
                nextOffset = 0
                hasMorePages = true
                isLoadingPage = false
                loadNextPage()
            } else if (cached == null) {
                localAudios = emptyList()
                nextOffset = 0
                hasMorePages = true
                isLoadingPage = false
                loadNextPage()
            } else {
                // Returning to the tab: restore from cache, no re-query.
                localAudios = cached
                nextOffset = cached.size
                hasMorePages = cached.size % AUDIO_PAGE_SIZE == 0
                viewModel.setPlaylist(cached)
            }
        } else {
            permissionLauncher.launch(permissionToRequest)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .tabSwipe(onSwipeNext, onSwipePrevious)
    ) {
        Text("Local Audio Files", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            !hasPermission -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(48.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
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
                        text = "Allow access to play your local audio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { permissionLauncher.launch(permissionToRequest) },
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
            }

            localAudios.isEmpty() && isLoadingPage -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            localAudios.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Audiotrack,
                    title = "No audio files found",
                    subtitle = "Your local music will appear here."
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(localAudios, key = { it.uri }) { audio ->
                        AudioListItem(
                            audio = audio,
                            isPlaying = currentAudio?.uri == audio.uri,
                            onClick = { viewModel.playAudio(context, audio) },
                            onChanged = { refreshKey++ }
                        )
                    }

                    if (hasMorePages) {
                        item(key = "audio_load_more") {
                            LaunchedEffect(localAudios.size) { loadNextPage() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun checkAudioPermission(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

suspend fun loadLocalAudiosPage(
    context: Context,
    limit: Int,
    offset: Int
): List<LocalAudio> = withContext(Dispatchers.IO) {
    val audios = mutableListOf<LocalAudio>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.ARTIST
    )

    val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.contentResolver.query(
            collection,
            projection,
            android.os.Bundle().apply {
                putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Audio.Media.IS_MUSIC} != 0")
                putStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Audio.Media.DATE_ADDED))
                putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION, android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, offset)
            },
            null
        )
    } else {
        val safeLimit = limit.coerceAtLeast(1)
        val safeOffset = offset.coerceAtLeast(0)
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC LIMIT $safeLimit OFFSET $safeOffset"
        )
    }

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val name = it.getString(nameColumn)
            val artist = it.getString(artistColumn)
            val contentUri = ContentUris.withAppendedId(collection, id)
            audios.add(LocalAudio(id, name, contentUri.toString(), artist))
        }
    }
    audios
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioListItem(
    audio: LocalAudio,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val containerColor by animateColorAsState(
        targetValue = if (isPlaying) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "audioItemContainer"
    )
    var menuExpanded by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var pendingRename by remember { mutableStateOf<Pair<String, String>?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Audio deleted", Toast.LENGTH_SHORT).show()
            onChanged()
        }
    }

    val writeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingRename?.let { (uri, finalName) ->
                applyAudioRename(context, uri, finalName)
                Toast.makeText(context, "Audio renamed", Toast.LENGTH_SHORT).show()
            }
            pendingRename = null
        }
        onChanged()
    }

    // Seed from cache so returning items show art instantly (no re-decode jitter).
    val cacheKey = "audio:${audio.uri}"
    val cached = remember(cacheKey) { com.ghost.video.data.ThumbnailCache.get(cacheKey) }
    var thumbnailBitmap by remember(cacheKey) { mutableStateOf(cached) }

    LaunchedEffect(cacheKey) {
        if (cached == null) {
            thumbnailBitmap = loadAudioThumbnail(context, audio.uri)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (thumbnailBitmap != null) {
                        Image(
                            bitmap = thumbnailBitmap!!.asImageBitmap(),
                            contentDescription = "Album Art",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = audio.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (!audio.artist.isNullOrEmpty() && audio.artist != "<unknown>") {
                        Text(
                            text = audio.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "Now playing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Playing",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Long-press context menu: same options as the video 3-dot menu.
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Play") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    onClick = { menuExpanded = false; onClick() }
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(audio.uri))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share audio"))
                    }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                    onClick = { menuExpanded = false; showRename = true }
                )
                DropdownMenuItem(
                    text = { Text("Details") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    onClick = { menuExpanded = false; showDetails = true }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuExpanded = false
                        deleteAudio(context, audio, deleteLauncher::launch, onChanged)
                    }
                )
            }
        }
    }

    if (showDetails) {
        AudioDetailsDialog(audio = audio, onDismiss = { showDetails = false })
    }

    if (showRename) {
        AudioRenameDialog(
            currentName = audio.name,
            onDismiss = { showRename = false },
            onConfirm = { newName ->
                showRename = false
                val uri = audio.uri
                val ext = audio.name.substringAfterLast('.', "")
                val finalName = if (ext.isNotEmpty() && !newName.endsWith(".$ext")) "$newName.$ext" else newName
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                }
                try {
                    val rows = context.contentResolver.update(Uri.parse(uri), values, null, null)
                    if (rows > 0) {
                        Toast.makeText(context, "Audio renamed", Toast.LENGTH_SHORT).show()
                        onChanged()
                    }
                } catch (e: SecurityException) {
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

/** Delete a local audio file (system confirm dialog on Android 11+). */
private fun deleteAudio(
    context: Context,
    audio: LocalAudio,
    launch: (IntentSenderRequest) -> Unit,
    onChanged: () -> Unit
) {
    val uri = Uri.parse(audio.uri)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
        launch(IntentSenderRequest.Builder(pi.intentSender).build())
    } else {
        try {
            context.contentResolver.delete(uri, null, null)
            Toast.makeText(context, "Audio deleted", Toast.LENGTH_SHORT).show()
            onChanged()
        } catch (e: Exception) {
            // ignore
        }
    }
}

/** Apply an audio rename to MediaStore once write access is available. */
private fun applyAudioRename(context: Context, uri: String, finalName: String): Boolean {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
        }
        context.contentResolver.update(Uri.parse(uri), values, null, null) > 0
    } catch (e: Exception) {
        false
    }
}

@Composable
private fun AudioDetailsDialog(audio: LocalAudio, onDismiss: () -> Unit) {
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
                Column {
                    Text(
                        text = "Name",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = audio.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!audio.artist.isNullOrEmpty() && audio.artist != "<unknown>") {
                    Column {
                        Text(
                            text = "Artist",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = audio.artist!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AudioRenameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
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
        title = { Text("Rename audio") },
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


suspend fun loadAudioThumbnail(context: Context, uri: String): Bitmap? = withContext(Dispatchers.IO) {
    // Return cached art instantly if we already decoded it.
    com.ghost.video.data.ThumbnailCache.get("audio:$uri")?.let { return@withContext it }

    var retriever: MediaMetadataRetriever? = null
    try {
        retriever = MediaMetadataRetriever()
        // Content URIs must be set with a Context + Uri, NOT a raw string path.
        retriever.setDataSource(context, android.net.Uri.parse(uri))
        val bytes = retriever.embeddedPicture ?: return@withContext null
        // Downscale while decoding to keep memory low (cards are small).
        val opts = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, 256)
        opts.inJustDecodeBounds = false
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        if (bitmap != null) com.ghost.video.data.ThumbnailCache.put("audio:$uri", bitmap)
        bitmap
    } catch (e: Exception) {
        null
    } finally {
        retriever?.release()
    }
}

/** Compute the largest power-of-two sample size that keeps the image >= target px. */
fun calculateInSampleSize(width: Int, height: Int, target: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var inSampleSize = 1
    val smaller = minOf(width, height)
    while (smaller / (inSampleSize * 2) >= target) {
        inSampleSize *= 2
    }
    return inSampleSize
}
