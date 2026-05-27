package com.pdfapp.reader.domain.usecase

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Encrypts a PDF with a user-provided password using PdfBox (128-bit AES). */
class LockPdfUseCase(private val context: Context) {

    suspend fun execute(sourceUri: Uri, baseName: String, password: String): Uri =
        withContext(Dispatchers.IO) {
            require(password.length >= 4) { "Password must be at least 4 characters" }

            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalStateException("Cannot read PDF")

            val document = try {
                PDDocument.load(inputStream)
            } finally {
                inputStream.close()
            }
            try {
                if (document.isEncrypted) {
                    throw IllegalStateException("PDF is already encrypted")
                }

                val permission = AccessPermission()
                val ownerPassword = UUID.randomUUID().toString()
                val policy = StandardProtectionPolicy(ownerPassword, password, permission)
                policy.encryptionKeyLength = 128
                document.protect(policy)

                val outputDir = File(context.filesDir, "locked").also { it.mkdirs() }
                val cleanName = baseName.removeSuffix(".pdf")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val outputFile = File(outputDir, "${cleanName}_locked_$timestamp.pdf")

                document.save(outputFile)
                Uri.fromFile(outputFile)
            } finally {
                document.close()
            }
        }
}
