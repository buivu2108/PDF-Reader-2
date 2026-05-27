package com.pdfapp.reader.ui.editmode

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.pdfapp.reader.ui.viewer.render.PdfRenderConfig
import com.pdfapp.reader.ui.viewer.render.PdfRenderService
import com.pdfapp.reader.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "EditModeLoader"

/**
 * Loads PDF pages for Edit Mode: page count, dimensions (PDF points), and bitmaps.
 * Results are pushed into the EditModeCoordinator.
 */
suspend fun loadPdfForEditMode(
    context: Context,
    uri: Uri,
    coordinator: EditModeCoordinator
) {
    coordinator.setLoading(true)
    try {
        val tempFile = FileUtils.copyToTempFile(context, uri, "editmode")
        if (tempFile == null) {
            Log.e(TAG, "Failed to copy PDF to temp file: $uri")
            coordinator.setLoading(false)
            return
        }

        val fd = withContext(Dispatchers.IO) {
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        val renderService = PdfRenderService(fd)

        try {
            val count = renderService.pageCount
            coordinator.setPageCount(count)
            Log.d(TAG, "PDF has $count pages")

            for (i in 0 until count) {
                val dims = renderService.getPageDimensions(i)
                coordinator.setPageDimensions(i, dims.width.toFloat(), dims.height.toFloat())

                val pixelSize = renderService.getPagePixelSize(i, PdfRenderConfig.DEFAULT_DPI)
                val bitmap = renderService.renderPage(i, pixelSize.width, pixelSize.height)
                coordinator.setPageBitmap(i, bitmap)
            }
            Log.d(TAG, "Loaded all $count pages successfully")
        } finally {
            renderService.close()
            tempFile.delete()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load PDF for edit mode", e)
    } finally {
        coordinator.setLoading(false)
    }
}
