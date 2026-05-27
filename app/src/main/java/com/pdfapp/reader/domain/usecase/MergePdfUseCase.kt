package com.pdfapp.reader.domain.usecase

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Merges multiple PDF files into one using PdfBox PDFMergerUtility. */
class MergePdfUseCase(private val context: Context) {

    /**
     * Returns the page count of a PDF at [uri], or throws on error.
     */
    suspend fun getPageCount(uri: Uri): Int = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open PDF")
        val doc = try { PDDocument.load(stream) } finally { stream.close() }
        try { doc.numberOfPages } finally { doc.close() }
    }

    /**
     * Merges [uris] in order, writes result to cacheDir, returns [Uri] pointing to output file.
     */
    suspend fun execute(uris: List<Uri>): Uri = withContext(Dispatchers.IO) {
        require(uris.size >= 2) { "Select at least 2 PDFs" }

        val merger = PDFMergerUtility()
        val streams = uris.map { uri ->
            context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open PDF: $uri")
        }
        try {
            streams.forEach { merger.addSource(it) }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outputFile = File(context.cacheDir, "merged_$timestamp.pdf")
            merger.destinationStream = outputFile.outputStream()
            merger.mergeDocuments(null)

            Uri.fromFile(outputFile)
        } finally {
            streams.forEach { runCatching { it.close() } }
        }
    }
}
