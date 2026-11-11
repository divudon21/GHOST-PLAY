package com.ghost.video.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Album art reader shared by the mini player and the full audio player.
 *
 * Results are memoised in [ThumbnailCache], so a track's artwork is decoded once
 * per process no matter how many times it is shown while scrolling or reopening
 * the player. Decoding always happens on [Dispatchers.IO].
 */
object MusicArtworkLoader {

    @Volatile
    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * The artwork for [uri] if this process has already decoded it, without
     * touching the file. The mini player and the full player both come through
     * here, so opening the player can paint a sleeve it already has on its first
     * frame rather than blanking it and fading one in.
     */
    fun cached(uri: String): Bitmap? = ThumbnailCache.get("audio:$uri")

    suspend fun load(uri: String, targetPx: Int = 512): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "audio:$uri"
        ThumbnailCache.get(cacheKey)?.let { return@withContext it }
        val context = appContext ?: return@withContext null
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            // content:// URIs need the Context overload; a raw path fails.
            retriever.setDataSource(context, Uri.parse(uri))
            val bytes = retriever.embeddedPicture ?: return@withContext null
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            var width = bounds.outWidth
            var height = bounds.outHeight
            while (width / (sample * 2) >= targetPx || height / (sample * 2) >= targetPx) {
                sample *= 2
            }
            val options = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = sample
                inJustDecodeBounds = false
            }
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (bitmap != null) ThumbnailCache.put(cacheKey, bitmap)
            bitmap
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever?.release() }
        }
    }
}
