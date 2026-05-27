package com.pdfapp.reader.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Converts a list of image URIs into a multi-page PDF using Android PdfDocument API. */
class ImageToPdfUseCase(private val context: Context) {

    private companion object {
        const val MAX_DIMENSION = 4096
    }

    suspend fun execute(imageUris: List<Uri>): Uri = withContext(Dispatchers.IO) {
        require(imageUris.isNotEmpty()) { "No images selected" }

        val pdfDocument = PdfDocument()
        try {
            imageUris.forEachIndexed { index, uri ->
                val bitmap = decodeBitmap(uri)
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }

            val outputDir = File(context.filesDir, "converted").also { it.mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outputFile = File(outputDir, "images_$timestamp.pdf")

            outputFile.outputStream().use { out ->
                pdfDocument.writeTo(out)
            }
            Uri.fromFile(outputFile)
        } finally {
            pdfDocument.close()
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

        val width = options.outWidth
        val height = options.outHeight
        var sampleSize = 1
        while (width / sampleSize > MAX_DIMENSION || height / sampleSize > MAX_DIMENSION) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalStateException("Cannot decode image: $uri")
    }
}
