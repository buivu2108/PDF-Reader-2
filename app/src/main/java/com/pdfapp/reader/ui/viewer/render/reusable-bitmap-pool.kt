package com.pdfapp.reader.ui.viewer.render

import android.graphics.Bitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Bounded pool of reusable [Bitmap] instances to reduce GC pressure during PDF scroll.
 *
 * Instead of recycling evicted cache bitmaps, they are stored here for reuse by new renders.
 * When a new page needs rendering, [acquire] returns a matching bitmap from the pool
 * (if available), avoiding a fresh [Bitmap.createBitmap] allocation.
 *
 * Thread-safe via [Mutex] for coroutine access.
 *
 * @param maxSize maximum bitmaps to hold in pool (default 3 ≈ 16.5MB for A4@120DPI)
 */
class ReusableBitmapPool(private val maxSize: Int = 3) {

    private val pool = ArrayDeque<Bitmap>(maxSize)
    private val mutex = Mutex()

    /**
     * Returns a pooled bitmap matching [width]×[height]×[config], or null if none available.
     * The returned bitmap is removed from the pool — caller owns it.
     */
    suspend fun acquire(width: Int, height: Int, config: Bitmap.Config): Bitmap? = mutex.withLock {
        val iterator = pool.iterator()
        while (iterator.hasNext()) {
            val bitmap = iterator.next()
            if (bitmap.isRecycled) {
                iterator.remove()
                continue
            }
            if (bitmap.width == width && bitmap.height == height && bitmap.config == config) {
                iterator.remove()
                // Erase previous content to prevent visual artifacts
                bitmap.eraseColor(0)
                return@withLock bitmap
            }
        }
        null
    }

    /**
     * Returns a bitmap to the pool for future reuse.
     * If pool is full or bitmap is recycled, the bitmap is recycled instead.
     */
    suspend fun release(bitmap: Bitmap) = mutex.withLock {
        if (bitmap.isRecycled || pool.size >= maxSize) {
            if (!bitmap.isRecycled) bitmap.recycle()
        } else {
            pool.addLast(bitmap)
        }
    }

    /** Recycles and removes all pooled bitmaps. Call on memory pressure or cleanup. */
    suspend fun clear() = mutex.withLock {
        pool.forEach { if (!it.isRecycled) it.recycle() }
        pool.clear()
    }
}
