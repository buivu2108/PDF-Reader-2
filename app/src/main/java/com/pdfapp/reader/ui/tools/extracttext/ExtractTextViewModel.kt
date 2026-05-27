package com.pdfapp.reader.ui.tools.extracttext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.util.FileUtils
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ExtractTextUiState(
    val selectedUri: Uri? = null,
    val fileName: String = "",
    val pageCount: Int = 0,
    val isExtracting: Boolean = false,
    val extractedText: String? = null,
    val error: String? = null,
    val savedPath: String? = null
)

class ExtractTextViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtractTextUiState())
    val uiState: StateFlow<ExtractTextUiState> = _uiState

    fun selectPdf(uri: Uri) {
        val name = FileUtils.getFileName(context.contentResolver, uri) ?: "document.pdf"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val document = PDDocument.load(inputStream)
                val count = document.numberOfPages
                document.close()
                _uiState.update {
                    it.copy(selectedUri = uri, fileName = name, pageCount = count,
                        error = null, extractedText = null, savedPath = null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun extractText() {
        val uri = _uiState.value.selectedUri ?: return
        if (_uiState.value.isExtracting) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExtracting = true, error = null) }
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()

                val trimmed = text.trim()
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        extractedText = trimmed.ifEmpty { null },
                        error = if (trimmed.isEmpty()) "No text found in this PDF" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExtracting = false, error = e.message) }
            }
        }
    }

    fun copyToClipboard(): Boolean {
        val text = _uiState.value.extractedText ?: return false
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("PDF Text", text))
        return true
    }

    fun saveAsFile() {
        val text = _uiState.value.extractedText ?: return
        val baseName = _uiState.value.fileName.substringBeforeLast(".")
        val fileName = "$baseName.txt"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.Files.FileColumns.MIME_TYPE, "text/plain")
                        put(MediaStore.Files.FileColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/PdfReader")
                    }
                    val fileUri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
                    fileUri?.let { uri ->
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(text.toByteArray(Charsets.UTF_8))
                        }
                    }
                    _uiState.update { it.copy(savedPath = "Documents/PdfReader/$fileName") }
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PdfReader")
                    dir.mkdirs()
                    val file = File(dir, fileName)
                    file.writeText(text, Charsets.UTF_8)
                    _uiState.update { it.copy(savedPath = file.absolutePath) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExtractTextViewModel(context.applicationContext) as T
    }
}
