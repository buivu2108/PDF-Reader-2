package com.pdfapp.reader.domain.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.pdfapp.reader.domain.model.EditAnnotation
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "SaveAnnotatedPdf"

/** Output mode for save operation. */
sealed class SaveOutputMode {
    /** Overwrite the original file at [uri]. */
    data class Overwrite(val uri: Uri) : SaveOutputMode()
    /** Save as new file with [fileName] in the same directory (or app-internal fallback). */
    data class SaveAs(val sourceUri: Uri, val fileName: String) : SaveOutputMode()
}

/**
 * Flattens all EditAnnotation types into PdfBox PDPageContentStream (APPEND mode)
 * and writes the result to an output file. Supports overwrite and save-as modes.
 */
class SaveAnnotatedPdfUseCase(private val context: Context) {

    /**
     * @param sourceUri       Original PDF content URI
     * @param annotations     All annotations across all pages
     * @param pageDimensions  Map of pageIndex → (pdfWidth, pdfHeight) in PDF points
     * @param pageBitmapSizes Map of pageIndex → (bitmapWidth, bitmapHeight) in pixels
     * @param outputMode      Overwrite or SaveAs
     * @return Result with saved URI on success, or exception on failure
     */
    /**
     * @param insertedPages Map of pageIndex → saved bitmap path for pages inserted from external PDF/image
     */
    suspend fun save(
        sourceUri: Uri,
        annotations: List<EditAnnotation>,
        pageDimensions: Map<Int, Pair<Float, Float>>,
        pageBitmapSizes: Map<Int, Pair<Int, Int>>,
        outputMode: SaveOutputMode,
        insertedPages: Map<Int, String> = emptyMap()
    ): Result<Uri> = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Cannot open PDF"))
            document = inputStream.use { PDDocument.load(it) }

            // Insert pages from external PDF/image at correct positions (ascending order)
            for (insertedIndex in insertedPages.keys.sorted()) {
                val dims = pageDimensions[insertedIndex]
                val width = dims?.first ?: 595f
                val height = dims?.second ?: 842f
                val mediaBox = com.tom_roush.pdfbox.pdmodel.common.PDRectangle(width, height)
                val newPage = com.tom_roush.pdfbox.pdmodel.PDPage(mediaBox)

                if (insertedIndex >= document.numberOfPages) {
                    document.addPage(newPage)
                } else {
                    val existingPage = document.getPage(insertedIndex)
                    document.pages.insertBefore(newPage, existingPage)
                }

                // Draw saved bitmap as full-page image content
                val imagePath = insertedPages[insertedIndex]
                if (imagePath != null) {
                    drawInsertedPageImage(document, document.getPage(insertedIndex), imagePath, width, height)
                }
            }

            // Flatten annotations into content streams per page
            for (pageIndex in 0 until document.numberOfPages) {
                val pageAnns = annotations.filter { it.pageIndex == pageIndex }
                if (pageAnns.isEmpty()) continue

                val dims = pageDimensions[pageIndex] ?: continue
                val pdfW = dims.first
                val pdfH = dims.second
                // Annotations are already in PDF point space — no bitmap-to-PDF scaling needed.
                // Only Y-axis inversion (via pdfH) is applied in transformToPdfSpace.
                val scaleX = 1f
                val scaleY = 1f

                val page = document.getPage(pageIndex)
                val cs = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)
                try {
                    pageAnns.forEach { ann ->
                        renderAnnotation(cs, document, ann, scaleX, scaleY, pdfH)
                    }
                } finally {
                    cs.close()
                }
            }

            // Write output
            val resultUri = writeOutput(document, sourceUri, outputMode)
            Log.d(TAG, "Save successful: $resultUri")
            Result.success(resultUri)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM while saving PDF", e)
            Result.failure(Exception("File too large to save"))
        } catch (e: Exception) {
            Log.e(TAG, "Error saving PDF", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
        }
    }

    /** Dispatch to the correct renderer based on annotation type. */
    private fun renderAnnotation(
        cs: PDPageContentStream, doc: PDDocument,
        ann: EditAnnotation, scaleX: Float, scaleY: Float, pdfH: Float
    ) {
        when (ann) {
            is EditAnnotation.Highlight -> renderHighlight(cs, doc, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.Underline -> renderUnderline(cs, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.Strikethrough -> renderStrikethrough(cs, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.FreehandStroke -> renderFreehandStroke(cs, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.OvalShape -> renderOvalShape(cs, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.RectShape -> renderRectShape(cs, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.SignatureElement -> renderSignatureOrImage(
                cs, doc, context, ann.imagePath, ann.bounds, ann.rotation, scaleX, scaleY, pdfH
            )
            is EditAnnotation.ImageElement -> renderSignatureOrImage(
                cs, doc, context, ann.imagePath, ann.bounds, ann.rotation, scaleX, scaleY, pdfH
            )
            is EditAnnotation.DateStamp -> renderDateStamp(cs, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.TextElement -> renderTextElement(cs, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.StickyNote -> renderStickyNote(cs, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.LineShape -> renderLineShape(cs, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.ArrowShape -> renderArrowShape(cs, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.ZigzagShape -> renderZigzagShape(cs, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.TriangleShape -> renderTriangleShape(cs, ann, scaleX, scaleY, pdfH)
            is EditAnnotation.PolygonShape -> renderPolygonShape(cs, ann, scaleX, scaleY, pdfH)
        }
    }

    /** Draw a saved bitmap as full-page image on an inserted blank page. */
    private fun drawInsertedPageImage(
        doc: PDDocument, page: com.tom_roush.pdfbox.pdmodel.PDPage,
        imagePath: String, width: Float, height: Float
    ) {
        val file = File(imagePath)
        if (!file.exists()) return
        val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath) ?: return
        val baos = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, baos)
        bitmap.recycle()
        val pdImage = com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
            .createFromByteArray(doc, baos.toByteArray(), "inserted_page")
        val cs = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)
        try {
            cs.drawImage(pdImage, 0f, 0f, width, height)
        } finally {
            cs.close()
        }
    }

    /** Write the modified PDDocument to the appropriate output location. */
    private fun writeOutput(doc: PDDocument, sourceUri: Uri, mode: SaveOutputMode): Uri {
        return when (mode) {
            is SaveOutputMode.Overwrite -> {
                // Write to temp file first, then copy to original URI
                val tempFile = File(context.cacheDir, "save_temp_${System.currentTimeMillis()}.pdf")
                try {
                    FileOutputStream(tempFile).use { doc.save(it) }
                    context.contentResolver.openOutputStream(mode.uri, "wt")?.use { os ->
                        tempFile.inputStream().use { it.copyTo(os) }
                    } ?: throw Exception("Cannot write to original file")
                    mode.uri
                } finally {
                    tempFile.delete()
                }
            }
            is SaveOutputMode.SaveAs -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Save to Downloads via MediaStore so it appears in home screen scan
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "${mode.fileName}.pdf")
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(
                        android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY),
                        values
                    ) ?: throw Exception("Cannot create file in Downloads")
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        doc.save(os)
                    } ?: throw Exception("Cannot write to Downloads")
                    uri
                } else {
                    // Pre-Q: save to public Downloads directory directly
                    @Suppress("DEPRECATION")
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    ).apply { mkdirs() }
                    val outputFile = File(downloadsDir, "${mode.fileName}.pdf")
                    FileOutputStream(outputFile).use { doc.save(it) }
                    // Notify MediaStore about the new file
                    android.media.MediaScannerConnection.scanFile(
                        context, arrayOf(outputFile.absolutePath), arrayOf("application/pdf"), null
                    )
                    Uri.fromFile(outputFile)
                }
            }
        }
    }
}
