package com.ghost.video.data

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Small in-memory LRU cache for decoded thumbnails / album art.
 *
 * Decoding video frames and embedded audio art with MediaMetadataRetriever is
 * expensive. Without a cache, every time a list item scrolls back into view the
 * frame is decoded again, which makes scrolling stutter. This cache keeps recently
 * used bitmaps in memory (sized to a fraction of the app heap) so repeated lookups
 * are instant.
 */
object ThumbnailCache {

    // Use 1/8th of available app memory for the bitmap cache.
    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()

    private val cache = object : LruCache<String, Bitmap>(maxKb) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            // Size in kilobytes.
            return value.byteCount / 1024
        }
    }

    fun get(key: String): Bitmap? = synchronized(cache) { cache.get(key) }

    fun put(key: String, bitmap: Bitmap) {
        synchronized(cache) {
            if (cache.get(key) == null) cache.put(key, bitmap)
        }
    }

    fun clear() {
        synchronized(cache) { cache.evictAll() }
    }
}
