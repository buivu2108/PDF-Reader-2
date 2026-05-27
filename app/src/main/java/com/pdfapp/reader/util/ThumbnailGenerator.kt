package com.pdfapp.reader.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ThumbnailGenerator {

    /** Serializes PdfRenderer access — Android can't handle concurrent instances on the same content URI. */
    private val renderMutex = Mutex()

    private const val THUMB_WIDTH = 150
    private const val THUMB_HEIGHT = 200
    private const val THUMB_QUALITY = 80

    /**
     * Generate a thumbnail for page 0 of the given PDF.
     * Returns the absolute path to the cached JPEG, or null on failure.
     */
    suspend fun generateThumbnail(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val thumbFile = File(context.cacheDir, "thumb_${uri.hashCode()}.jpg")
                if (thumbFile.exists()) return@withContext thumbFile.absolutePath

                val fd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: return@withContext null
                fd.use { descriptor ->
                    val renderer = PdfRenderer(descriptor)
                    renderer.use { pdfRenderer ->
                        pdfRenderer.openPage(0).use { page ->
                            val bitmap = Bitmap.createBitmap(
                                THUMB_WIDTH, THUMB_HEIGHT, Bitmap.Config.ARGB_8888
                            )
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(
                                bitmap, null, null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                            FileOutputStream(thumbFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, THUMB_QUALITY, out)
                            }
                            bitmap.recycle()
                        }
                    }
                }
                thumbFile.absolutePath
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Generate a thumbnail for a specific page of an already-opened PDF.
     * Uses LruCache to avoid re-rendering previously viewed pages.
     */
    suspend fun generatePageThumbnail(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        width: Int = 80,
        height: Int = 110
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = BitmapCache.thumbnailKey(uri.hashCode(), pageIndex)
        BitmapCache.get(cacheKey)?.let { return@withContext it }

        // Serialize PdfRenderer access to prevent concurrent content URI descriptor failures
        renderMutex.withLock {
            // Re-check cache after acquiring lock (another coroutine may have rendered this page)
            BitmapCache.get(cacheKey)?.let { return@withContext it }

            try {
                val fd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: return@withContext null
                fd.use { descriptor ->
                    val renderer = PdfRenderer(descriptor)
                    renderer.use { pdfRenderer ->
                        if (pageIndex >= pdfRenderer.pageCount) return@withContext null
                        pdfRenderer.openPage(pageIndex).use { page ->
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(
                                bitmap, null, null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                            BitmapCache.put(cacheKey, bitmap)
                            bitmap
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
