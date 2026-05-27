package com.pdfapp.reader.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Renders PDF pages to bitmaps using PdfRenderer (Android framework). Caches temp file per URI. */
class RenderPdfPageUseCase(private val context: Context) {

    private var cachedUri: Uri? = null
    private var cachedTempFile: File? = null

    /** Render all pages for a given URI. More efficient than calling renderPage per page. */
    suspend fun renderAllPages(uri: Uri, width: Int): Pair<List<Bitmap>, List<Pair<Float, Float>>> =
        withContext(Dispatchers.IO) {
            val bitmaps = mutableListOf<Bitmap>()
            val dimensions = mutableListOf<Pair<Float, Float>>()
            var renderer: PdfRenderer? = null
            var fd: ParcelFileDescriptor? = null
            try {
                val tempFile = getOrCreateTempFile(uri) ?: return@withContext Pair(emptyList(), emptyList())
                fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(fd)
                for (i in 0 until renderer.pageCount) {
                    ensureActive()
                    val page = renderer.openPage(i)
                    dimensions.add(Pair(page.width.toFloat(), page.height.toFloat()))
                    val scale = width.toFloat() / page.width
                    val height = (page.height * scale).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bitmap)
                }
            } catch (_: Exception) {
            } finally {
                renderer?.close()
                fd?.close()
            }
            Pair(bitmaps, dimensions)
        }

    /** Get page count for a PDF. */
    suspend fun getPageCount(uri: Uri): Int = withContext(Dispatchers.IO) {
        var renderer: PdfRenderer? = null
        var fd: ParcelFileDescriptor? = null
        try {
            val tempFile = getOrCreateTempFile(uri) ?: return@withContext 0
            fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fd)
            renderer.pageCount
        } catch (_: Exception) { 0 }
        finally { renderer?.close(); fd?.close() }
    }

    fun cleanup() {
        cachedTempFile?.delete()
        cachedTempFile = null
        cachedUri = null
    }

    private fun getOrCreateTempFile(uri: Uri): File? {
        if (uri == cachedUri && cachedTempFile?.exists() == true) return cachedTempFile
        cachedTempFile?.delete()
        val tempFile = File(context.cacheDir, "render_${uri.hashCode()}.pdf")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }
            cachedUri = uri
            cachedTempFile = tempFile
            tempFile
        } catch (_: Exception) {
            tempFile.delete(); null
        }
    }
}
