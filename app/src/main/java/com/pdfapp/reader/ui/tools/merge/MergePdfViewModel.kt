package com.pdfapp.reader.ui.tools.merge

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.domain.usecase.MergePdfUseCase
import com.pdfapp.reader.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PdfFileItem(val uri: Uri, val name: String, val pageCount: Int)

data class MergePdfUiState(
    val selectedFiles: List<PdfFileItem> = emptyList(),
    val isMerging: Boolean = false,
    val resultUri: Uri? = null,
    val error: String? = null
)

class MergePdfViewModel(private val context: Context) : ViewModel() {

    private val useCase = MergePdfUseCase(context.applicationContext)

    private val _uiState = MutableStateFlow(MergePdfUiState())
    val uiState: StateFlow<MergePdfUiState> = _uiState

    /** Open PDF via PdfBox to get page count, then add to list. */
    fun addPdf(uri: Uri) {
        viewModelScope.launch {
            try {
                val name = FileUtils.getFileName(context.contentResolver, uri) ?: "document.pdf"
                val pageCount = useCase.getPageCount(uri)
                val item = PdfFileItem(uri, name, pageCount)
                _uiState.update { it.copy(selectedFiles = it.selectedFiles + item, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun removePdf(index: Int) {
        _uiState.update { state ->
            state.copy(selectedFiles = state.selectedFiles.toMutableList().also { it.removeAt(index) })
        }
    }

    /** Swap item at [from] with item at [to]. */
    fun movePdf(from: Int, to: Int) {
        val list = _uiState.value.selectedFiles.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        _uiState.update { it.copy(selectedFiles = list) }
    }

    fun mergePdfs() {
        val files = _uiState.value.selectedFiles
        if (files.size < 2) {
            _uiState.update { it.copy(error = "Select at least 2 PDFs") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isMerging = true, error = null) }
            try {
                val resultUri = useCase.execute(files.map { it.uri })
                _uiState.update { it.copy(isMerging = false, resultUri = resultUri) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isMerging = false, error = e.message) }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(resultUri = null) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MergePdfViewModel(context.applicationContext) as T
    }
}
