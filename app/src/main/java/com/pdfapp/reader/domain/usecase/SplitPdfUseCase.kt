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

/** Splits a PDF into multiple files using PdfBox. */
class SplitPdfUseCase(private val context: Context) {

    sealed class SplitMethod {
        data class AtPage(val pageNumber: Int) : SplitMethod()
        data class EveryNPages(val n: Int) : SplitMethod()
        data class CustomRanges(val ranges: List<IntRange>) : SplitMethod()
    }

    data class SplitResult(val files: List<Uri>, val fileNames: List<String>)

    suspend fun execute(
        sourceUri: Uri,
        baseName: String,
        method: SplitMethod
    ): SplitResult = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputDir = File(context.filesDir, "split_$timestamp").also { it.mkdirs() }
        val cleanName = baseName.removeSuffix(".pdf")

        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalStateException("Cannot read PDF")

        val document = try {
            PDDocument.load(inputStream)
        } finally {
            inputStream.close()
        }
        try {
            val totalPages = document.numberOfPages
            val ranges = when (method) {
                is SplitMethod.AtPage -> {
                    require(method.pageNumber in 1 until totalPages) { "Invalid page number" }
                    listOf(0 until method.pageNumber, method.pageNumber until totalPages)
                }
                is SplitMethod.EveryNPages -> {
                    require(method.n > 0) { "N must be > 0" }
                    (0 until totalPages).chunked(method.n).map { chunk ->
                        chunk.first()..chunk.last()
                    }
                }
                is SplitMethod.CustomRanges -> {
                    method.ranges.map { range ->
                        require(range.first >= 1 && range.last <= totalPages) { "Range out of bounds" }
                        (range.first - 1)..(range.last - 1)  // convert to 0-indexed
                    }
                }
            }

            val resultFiles = mutableListOf<Uri>()
            val resultNames = mutableListOf<String>()

            // Save all splits while source document is still open to avoid
            // resource invalidation from importPage()'s shallow copy.
            val splitDocs = ranges.mapIndexed { index, range ->
                val newDoc = PDDocument()
                for (pageIndex in range) {
                    newDoc.importPage(document.getPage(pageIndex))
                }
                val fileName = "${cleanName}_part${index + 1}.pdf"
                val outputFile = File(outputDir, fileName)
                newDoc.save(outputFile)
                newDoc.close()
                Pair(Uri.fromFile(outputFile), fileName)
            }
            splitDocs.forEach { (uri, name) ->
                resultFiles.add(uri)
                resultNames.add(name)
            }

            SplitResult(resultFiles, resultNames)
        } finally {
            document.close()
        }
    }

    /** Get total page count for validation. */
    suspend fun getPageCount(uri: Uri): Int = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot read PDF")
        val document = try {
            PDDocument.load(inputStream)
        } finally {
            inputStream.close()
        }
        try {
            document.numberOfPages
        } finally {
            document.close()
        }
    }

    companion object {
        /** Parse custom range string like "1-3, 4-7, 8-10" into list of IntRange. */
        fun parseRanges(input: String): List<IntRange> {
            return try {
                input.split(",").map { part ->
                    val trimmed = part.trim().replace("–", "-").replace("—", "-")
                    if (trimmed.contains("-")) {
                        val parts = trimmed.split("-").map { it.trim().toInt() }
                        require(parts.size == 2 && parts[0] <= parts[1]) { "Invalid range: $trimmed" }
                        parts[0]..parts[1]
                    } else {
                        val page = trimmed.toInt()
                        page..page
                    }
                }
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Invalid range format: use numbers like 1-3, 4-7")
            }
        }
    }
}
