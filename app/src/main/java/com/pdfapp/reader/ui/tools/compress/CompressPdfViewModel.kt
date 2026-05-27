package com.pdfapp.reader.ui.tools.compress

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.util.FileUtils
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CompressPdfUiState(
    val selectedUri: Uri? = null,
    val fileName: String = "",
    val originalSize: Long = 0,
    val compressedSize: Long = 0,
    val quality: Int = 50,
    val isCompressing: Boolean = false,
    val resultUri: Uri? = null,
    val error: String? = null
)

class CompressPdfViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(CompressPdfUiState())
    val uiState: StateFlow<CompressPdfUiState> = _uiState

    fun selectPdf(uri: Uri) {
        val name = FileUtils.getFileName(context.contentResolver, uri) ?: "document.pdf"
        val size = FileUtils.getFileSize(context.contentResolver, uri)
        _uiState.update {
            it.copy(selectedUri = uri, fileName = name, originalSize = size,
                compressedSize = 0, resultUri = null, error = null)
        }
    }

    fun setQuality(quality: Int) {
        _uiState.update { it.copy(quality = quality.coerceIn(1, 100)) }
    }

    fun compressPdf() {
        val state = _uiState.value
        val uri = state.selectedUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCompressing = true, error = null) }
            try {
                val result = performCompression(uri, state.fileName, state.quality)
                val compressedSize = result.second
                _uiState.update {
                    it.copy(isCompressing = false, resultUri = result.first, compressedSize = compressedSize)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCompressing = false, error = e.message ?: "Compression failed") }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(resultUri = null) }
    }

    private suspend fun performCompression(uri: Uri, fileName: String, quality: Int): Pair<Uri, Long> =
        withContext(Dispatchers.IO) {
            val tempInput = FileUtils.copyToTempFile(context, uri, "compress_in")
                ?: throw IllegalStateException("Cannot read PDF")
            try {
                compressWithPdfBox(tempInput, fileName, quality)
            } finally {
                tempInput.delete()
            }
        }

    private fun compressWithPdfBox(inputFile: File, fileName: String, quality: Int): Pair<Uri, Long> {
        val document = PDDocument.load(inputFile)
        return try {
            // Re-encode images at lower quality
            val jpegQuality = quality / 100f
            for (page in document.pages) {
                val resources = page.resources ?: continue
                for (name in resources.xObjectNames) {
                    try {
                        val xObject = resources.getXObject(name)
                        if (xObject is PDImageXObject) {
                            val bitmap = xObject.image ?: continue
                            val newImage = JPEGFactory.createFromImage(document, bitmap, jpegQuality)
                            resources.put(name, newImage)
                        }
                    } catch (_: Exception) {
                        // Skip images that can't be re-encoded
                    }
                }
            }
            // Strip metadata
            document.documentInformation = PDDocumentInformation()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val cleanName = fileName.removeSuffix(".pdf")
            val outputFileName = "${cleanName}_compressed_$timestamp.pdf"
            val outputFile = File(context.cacheDir, outputFileName)
            document.save(outputFile)

            val savedUri = saveToDownloads(outputFile, outputFileName)
            val compressedSize = outputFile.length()
            outputFile.delete()
            Pair(savedUri, compressedSize)
        } finally {
            document.close()
        }
    }

    private fun saveToDownloads(file: File, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/PdfReader")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Cannot create output file in Downloads")
        resolver.openOutputStream(insertUri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(insertUri, values, null, null)
        return insertUri
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CompressPdfViewModel(context.applicationContext) as T
    }
}
