package com.pdfapp.reader.ui.tools.split

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.domain.usecase.SplitPdfUseCase
import com.pdfapp.reader.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SplitMethod { AT_PAGE, EVERY_N, CUSTOM }

data class SplitPdfUiState(
    val selectedUri: Uri? = null,
    val fileName: String = "",
    val totalPages: Int = 0,
    val splitMethod: SplitMethod = SplitMethod.AT_PAGE,
    val splitValue: String = "",
    val isProcessing: Boolean = false,
    val resultFiles: List<String> = emptyList(),
    val resultUris: List<Uri> = emptyList(),
    val error: String? = null
)

class SplitPdfViewModel(private val context: Context) : ViewModel() {

    private val useCase = SplitPdfUseCase(context.applicationContext)

    private val _uiState = MutableStateFlow(SplitPdfUiState())
    val uiState: StateFlow<SplitPdfUiState> = _uiState

    fun selectPdf(uri: Uri) {
        val name = FileUtils.getFileName(context.contentResolver, uri) ?: "document.pdf"
        _uiState.update { it.copy(selectedUri = uri, fileName = name, error = null, resultFiles = emptyList()) }

        viewModelScope.launch {
            try {
                val pages = useCase.getPageCount(uri)
                _uiState.update { it.copy(totalPages = pages) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setSplitMethod(method: SplitMethod) {
        _uiState.update { it.copy(splitMethod = method, splitValue = "", error = null) }
    }

    fun setSplitValue(value: String) {
        _uiState.update { it.copy(splitValue = value, error = null) }
    }

    fun executeSplit() {
        val state = _uiState.value
        val uri = state.selectedUri ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val method = when (state.splitMethod) {
                    SplitMethod.AT_PAGE -> {
                        val page = state.splitValue.toIntOrNull()
                            ?: throw IllegalArgumentException("Invalid page number")
                        SplitPdfUseCase.SplitMethod.AtPage(page)
                    }
                    SplitMethod.EVERY_N -> {
                        val n = state.splitValue.toIntOrNull()
                            ?: throw IllegalArgumentException("Invalid number")
                        SplitPdfUseCase.SplitMethod.EveryNPages(n)
                    }
                    SplitMethod.CUSTOM -> {
                        val ranges = SplitPdfUseCase.parseRanges(state.splitValue)
                        SplitPdfUseCase.SplitMethod.CustomRanges(ranges)
                    }
                }

                val result = useCase.execute(uri, state.fileName, method)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        resultFiles = result.fileNames,
                        resultUris = result.files
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SplitPdfViewModel(context.applicationContext) as T
    }
}
