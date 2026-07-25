package com.ghost.video.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Bounded, visible-first thumbnail pipeline for the video library.
 * Lazy list items request work only after composition, so the viewport naturally
 * enters this FIFO queue before items further down the list. Limiting decoding to
 * two jobs prevents MediaMetadataRetriever from competing with scrolling/UI work.
 */
object VideoThumbnailPipeline {
    private const val PREVIEW_EDGE = 240
    private const val FINAL_EDGE = 640
    private val decodeSlots = Semaphore(permits = 2)

    suspend fun load(
        context: Context,
        uri: String,
        durationMs: Long,
        strategy: ThumbnailStrategy,
        positionPercent: Int,
        onPreview: (Bitmap) -> Unit,
        onFinal: (Bitmap) -> Unit
    ) {
        val finalKey = "video:$uri:$strategy:$positionPercent"
        ThumbnailCache.get(finalKey)?.let {
            onFinal(it)
            return
        }

        decodeSlots.withPermit {
            // Another visible card may have completed this exact request while
            // this job was waiting its turn in the queue.
            ThumbnailCache.get(finalKey)?.let {
                onFinal(it)
                return@withPermit
            }

            val selection = withContext(Dispatchers.IO) {
                selectBestFrame(context, uri, durationMs, strategy, positionPercent)
            } ?: return@withPermit

            onPreview(selection.preview)

            val finalBitmap = withContext(Dispatchers.IO) {
                extractAt(context, uri, selection.timeUs, FINAL_EDGE)
            } ?: selection.preview

            ThumbnailCache.put(finalKey, finalBitmap)
            onFinal(finalBitmap)
        }
    }

    private data class FrameSelection(val timeUs: Long, val preview: Bitmap)

    private fun selectBestFrame(
        context: Context,
        uri: String,
        durationMs: Long,
        strategy: ThumbnailStrategy,
        positionPercent: Int
    ): FrameSelection? {
        val requestedUs = (durationMs.coerceAtLeast(0L) * 1000L * positionPercent.coerceIn(0, 100)) / 100L
        val candidates = when (strategy) {
            ThumbnailStrategy.FIRST_FRAME -> listOf(0L)
            ThumbnailStrategy.FRAME_AT_POSITION -> listOf(requestedUs, 0L)
            ThumbnailStrategy.HYBRID -> listOf(
                0L,
                durationMs * 100L, // 10%
                durationMs * 250L, // 25%
                requestedUs,
                durationMs * 350L // 35%
            ).distinct()
        }

        var fallback: FrameSelection? = null
        for (timeUs in candidates) {
            val bitmap = extractAt(context, uri, timeUs, PREVIEW_EDGE) ?: continue
            val frame = FrameSelection(timeUs, bitmap)
            if (fallback == null) fallback = frame
            if (!isMostlySolid(bitmap)) {
                // A meaningful frame is preferred over a black intro/logo frame.
                if (fallback !== frame) fallback?.preview?.recycle()
                return frame
            }
            if (fallback !== frame) bitmap.recycle()
        }
        return fallback
    }

    private fun extractAt(context: Context, uri: String, timeUs: Long, maxEdge: Int): Bitmap? {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(uri))
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.let { scaleDown(it, maxEdge) }
        } catch (_: Exception) {
            null
        } finally {
            retriever?.release()
        }
    }

    private fun scaleDown(source: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= maxEdge) return source
        val ratio = maxEdge.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).toInt().coerceAtLeast(1),
            (source.height * ratio).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== source) source.recycle()
        return scaled
    }

    private fun isMostlySolid(bitmap: Bitmap): Boolean {
        if (bitmap.width < 2 || bitmap.height < 2) return false
        val points = listOf(
            bitmap.width / 4 to bitmap.height / 4,
            bitmap.width * 3 / 4 to bitmap.height / 4,
            bitmap.width / 4 to bitmap.height * 3 / 4,
            bitmap.width * 3 / 4 to bitmap.height * 3 / 4,
            bitmap.width / 2 to bitmap.height / 2
        )
        val first = bitmap.getPixel(points.first().first, points.first().second)
        return points.count { (x, y) ->
            val color = bitmap.getPixel(x, y)
            abs(android.graphics.Color.red(color) - android.graphics.Color.red(first)) < 15 &&
                abs(android.graphics.Color.green(color) - android.graphics.Color.green(first)) < 15 &&
                abs(android.graphics.Color.blue(color) - android.graphics.Color.blue(first)) < 15
        } >= 4
    }
}
