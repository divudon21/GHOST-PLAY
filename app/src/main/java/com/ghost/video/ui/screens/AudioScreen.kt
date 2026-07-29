package com.ghost.video.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Parcelable
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.viewmodel.AudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalAudio(val id: Long, val name: String, val uri: String, val artist: String?) : Parcelable

private const val AUDIO_PAGE_SIZE = 60

@Composable
fun AudioScreen(viewModel: AudioViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var localAudios by remember { mutableStateOf<List<LocalAudio>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(checkAudioPermission(context)) }
    var isLoadingPage by remember { mutableStateOf(false) }
    var hasMorePages by remember { mutableStateOf(true) }
    var nextOffset by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
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
            isLoadingPage = false
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            localAudios = emptyList()
            nextOffset = 0
            hasMorePages = true
            isLoadingPage = false
            loadNextPage()
        } else {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Local Audio Files", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            !hasPermission -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Storage permission required to show local audio.")
                }
            }

            localAudios.isEmpty() && isLoadingPage -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            localAudios.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No local audio files found.")
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(localAudios, key = { it.uri }) { audio ->
                        AudioListItem(
                            audio = audio,
                            onClick = { viewModel.playAudio(context, audio) }
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

suspend fun loadLocalAudios(context: Context): List<LocalAudio> = loadLocalAudiosPage(
    context = context,
    limit = Int.MAX_VALUE,
    offset = 0
)

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

@Composable
fun AudioListItem(audio: LocalAudio, onClick: () -> Unit) {
    val context = LocalContext.current
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
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
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
                Text(audio.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                if (!audio.artist.isNullOrEmpty() && audio.artist != "<unknown>") {
                    Text(audio.artist, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
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
