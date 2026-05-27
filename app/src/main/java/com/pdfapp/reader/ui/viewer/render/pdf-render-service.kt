package com.pdfapp.reader.ui.viewer.render

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Wraps Android [PdfRenderer] with coroutine-safe page rendering.
 *
 * PdfRenderer requires single-threaded access — a [Mutex] guards all operations.
 * All bitmap rendering runs on [Dispatchers.Default] to keep the UI thread free.
 */
class PdfRenderService(
    private val fileDescriptor: ParcelFileDescriptor
) : AutoCloseable {

    private val renderer = PdfRenderer(fileDescriptor)
    private val mutex = Mutex()

    val pageCount: Int get() = renderer.pageCount

    /**
     * Returns the page dimensions in PDF points (72 DPI).
     * @throws IndexOutOfBoundsException if [pageIndex] is out of range.
     */
    suspend fun getPageDimensions(pageIndex: Int): Size = mutex.withLock {
        renderer.openPage(pageIndex).use { page ->
            Size(page.width, page.height)
        }
    }

    /**
     * Renders a page to a [Bitmap] at the given pixel dimensions.
     *
     * Rendering happens on [Dispatchers.Default]. The returned bitmap uses
     * [Bitmap.Config.ARGB_8888] for full-quality display.
     *
     * @param pageIndex zero-based page number
     * @param width desired bitmap width in pixels
     * @param height desired bitmap height in pixels
     * @return rendered bitmap — caller owns the lifecycle
     * @throws IOException if the PDF is corrupted
     * @throws SecurityException if the PDF is password-protected
     */
    suspend fun renderPage(
        pageIndex: Int,
        width: Int,
        height: Int,
        reuseBitmap: Bitmap? = null
    ): Bitmap = withContext(Dispatchers.Default) {
            // Reuse pooled bitmap if dimensions match, otherwise allocate new
            val bitmap = if (reuseBitmap != null && !reuseBitmap.isRecycled
                && reuseBitmap.width == width && reuseBitmap.height == height
                && reuseBitmap.config == Bitmap.Config.ARGB_8888
            ) {
                reuseBitmap
            } else {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            }
            mutex.withLock {
                renderer.openPage(pageIndex).use { page ->
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
            bitmap
        }

    /**
     * Calculates pixel dimensions for a page at the given DPI.
     * Useful for determining render size before calling [renderPage].
     */
    suspend fun getPagePixelSize(pageIndex: Int, dpi: Int = PdfRenderConfig.DEFAULT_DPI): Size {
        val pdfSize = getPageDimensions(pageIndex)
        val scale = dpi.toFloat() / PdfRenderConfig.PDF_POINTS_PER_INCH
        return Size(
            (pdfSize.width * scale).toInt(),
            (pdfSize.height * scale).toInt()
        )
    }

    override fun close() {
        renderer.close()
        fileDescriptor.close()
    }
}
