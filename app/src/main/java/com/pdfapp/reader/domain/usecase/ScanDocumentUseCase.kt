package com.pdfapp.reader.domain.usecase

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Copies the scanned PDF from ML Kit's temp URI to the app's output directory.
 * Returns the final file URI.
 */
class ScanDocumentUseCase(private val context: Context) {

    suspend fun copyScannedPdf(sourceUri: Uri): Uri = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "scanned").also { it.mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(outputDir, "scan_$timestamp.pdf")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot read scanned PDF")

        Uri.fromFile(outputFile)
    }
}
