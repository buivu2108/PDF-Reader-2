package com.pdfapp.reader.domain.usecase

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Decrypts a password-protected PDF using PdfBox. */
class UnlockPdfUseCase(private val context: Context) {

    /** Check whether the PDF at [uri] is encrypted. */
    suspend fun isEncrypted(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext false
            document = try {
                PDDocument.load(inputStream)
            } finally {
                inputStream.close()
            }
            document.isEncrypted
        } catch (_: Exception) {
            true
        } finally {
            document?.close()
        }
    }

    /** Decrypt the PDF with the given password. Returns URI of unlocked file. */
    suspend fun execute(sourceUri: Uri, baseName: String, password: String): Uri =
        withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalStateException("Cannot read PDF")

            val document = try {
                PDDocument.load(inputStream, password)
            } catch (e: Exception) {
                throw IllegalArgumentException("Incorrect password")
            } finally {
                inputStream.close()
            }

            try {
                document.isAllSecurityToBeRemoved = true

                val outputDir = File(context.filesDir, "unlocked").also { it.mkdirs() }
                val cleanName = baseName.removeSuffix(".pdf")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val outputFile = File(outputDir, "${cleanName}_unlocked_$timestamp.pdf")

                document.save(outputFile)
                Uri.fromFile(outputFile)
            } finally {
                document.close()
            }
        }
}
