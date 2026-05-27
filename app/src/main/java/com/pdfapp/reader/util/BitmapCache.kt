package com.pdfapp.reader.util

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Simple LruCache wrapper for page thumbnail bitmaps.
 * Uses 1/8 of available heap for cache by default.
 */
object BitmapCache {

    private val cache: LruCache<String, Bitmap> by lazy {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8

        object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int =
                bitmap.byteCount / 1024

            // Let GC handle bitmap recycling to avoid race conditions
            // where a caller may still be drawing the bitmap
        }
    }

    fun get(key: String): Bitmap? {
        val bitmap = cache.get(key)
        return if (bitmap != null && !bitmap.isRecycled) bitmap else {
            if (bitmap != null) cache.remove(key)
            null
        }
    }

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun evictAll() {
        cache.evictAll()
    }

    fun thumbnailKey(uriHash: Int, pageIndex: Int): String =
        "thumb_${uriHash}_$pageIndex"
}
