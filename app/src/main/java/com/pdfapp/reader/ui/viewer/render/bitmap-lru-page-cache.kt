package com.pdfapp.reader.ui.viewer.render

import android.graphics.Bitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * LRU bitmap cache for rendered PDF pages.
 *
 * Evicted bitmaps are sent to [bitmapPool] for reuse instead of immediate recycling,
 * reducing GC pressure during fast scroll.
 * Thread-safe via [Mutex] — safe for concurrent coroutine access.
 *
 * @param maxSize maximum pages to keep cached (default from [PdfRenderConfig])
 * @param bitmapPool pool for reusing evicted bitmaps instead of allocating new ones
 */
class BitmapLruPageCache(
    private val maxSize: Int = PdfRenderConfig.DEFAULT_CACHE_SIZE,
    val bitmapPool: ReusableBitmapPool = ReusableBitmapPool()
) {
    // Pending evictions collected during LinkedHashMap.put() — released to pool after lock
    private val pendingEvictions = mutableListOf<Bitmap>()

    private val cache = object : LinkedHashMap<Int, Bitmap>(maxSize + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Bitmap>?): Boolean {
            if (size > maxSize) {
                eldest?.value?.let { if (!it.isRecycled) pendingEvictions.add(it) }
                return true
            }
            return false
        }
    }

    private val mutex = Mutex()

    /** Releases pending evictions to the bitmap pool. Must be called outside mutex. */
    private suspend fun drainPendingEvictions() {
        val evictions = mutex.withLock {
            if (pendingEvictions.isEmpty()) return
            pendingEvictions.toList().also { pendingEvictions.clear() }
        }
        evictions.forEach { bitmapPool.release(it) }
    }

    /** Returns cached bitmap for [pageIndex], or null if not cached or recycled. */
    suspend fun get(pageIndex: Int): Bitmap? = mutex.withLock {
        val bitmap = cache[pageIndex]
        if (bitmap != null && bitmap.isRecycled) {
            cache.remove(pageIndex)
            return@withLock null
        }
        bitmap
    }

    /** Tracks pages currently being rendered to prevent duplicate renders. */
    private val renderingPages = mutableSetOf<Int>()

    /**
     * Returns cached bitmap or renders and caches it.
     * Lock is released during render so other pages can be served from cache concurrently.
     * Render lambda receives an optional reuse bitmap from the pool.
     */
    suspend fun getOrRender(
        pageIndex: Int,
        render: suspend (reuseBitmap: Bitmap?) -> Bitmap
    ): Bitmap {
        // Fast path: check cache
        mutex.withLock {
            val cached = cache[pageIndex]
            if (cached != null && !cached.isRecycled) return cached
            cache.remove(pageIndex)
            // Skip if another coroutine is already rendering this page
            if (pageIndex in renderingPages) return@withLock null
            renderingPages.add(pageIndex)
        } ?: return mutex.withLock { cache[pageIndex] ?: throw IllegalStateException("concurrent render") }

        // Drain any evictions that happened above so pool has bitmaps available
        drainPendingEvictions()

        // Render outside the lock so cache reads aren't blocked
        return try {
            val bitmap = render(null) // pool acquisition happens inside render lambda via service
            mutex.withLock {
                cache[pageIndex] = bitmap
                renderingPages.remove(pageIndex)
            }
            drainPendingEvictions()
            bitmap
        } catch (e: Exception) {
            mutex.withLock { renderingPages.remove(pageIndex) }
            throw e
        }
    }

    /** Releases all cached bitmaps to pool, then clears pool. */
    suspend fun evictAll() {
        mutex.withLock {
            cache.values.forEach { if (!it.isRecycled) pendingEvictions.add(it) }
            cache.clear()
        }
        drainPendingEvictions()
        bitmapPool.clear()
    }

    /** Trims cache to [targetSize], releasing evicted bitmaps to pool. */
    suspend fun trimTo(targetSize: Int) {
        mutex.withLock {
            val iter = cache.entries.iterator()
            while (cache.size > targetSize && iter.hasNext()) {
                val entry = iter.next()
                if (!entry.value.isRecycled) pendingEvictions.add(entry.value)
                iter.remove()
            }
        }
        drainPendingEvictions()
    }

    /** Number of pages currently cached. */
    suspend fun size(): Int = mutex.withLock { cache.size }
}
