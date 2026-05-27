package com.pdfapp.reader.ui.tools.unlock

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.usecase.UnlockPdfUseCase
import com.pdfapp.reader.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UnlockPdfUiState(
    val selectedUri: Uri? = null,
    val fileName: String = "",
    val isEncrypted: Boolean? = null,
    val isCheckingEncryption: Boolean = false,
    val password: String = "",
    val isProcessing: Boolean = false,
    val resultUri: Uri? = null,
    val error: String? = null
)

class UnlockPdfViewModel(private val context: Context) : ViewModel() {

    private val useCase = UnlockPdfUseCase(context.applicationContext)

    private val _uiState = MutableStateFlow(UnlockPdfUiState())
    val uiState: StateFlow<UnlockPdfUiState> = _uiState

    fun selectPdf(uri: Uri) {
        val name = FileUtils.getFileName(context.contentResolver, uri) ?: "document.pdf"
        _uiState.update {
            it.copy(
                selectedUri = uri, fileName = name, error = null,
                resultUri = null, isEncrypted = null, isCheckingEncryption = true
            )
        }

        viewModelScope.launch {
            try {
                val encrypted = useCase.isEncrypted(uri)
                _uiState.update { it.copy(isEncrypted = encrypted, isCheckingEncryption = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCheckingEncryption = false, error = e.message) }
            }
        }
    }

    fun setPassword(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun unlockPdf() {
        val state = _uiState.value
        val uri = state.selectedUri ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val result = useCase.execute(uri, state.fileName, state.password)
                _uiState.update { it.copy(isProcessing = false, resultUri = result, password = "") }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(isProcessing = false, error = context.getString(R.string.unlock_wrong_password), password = "") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message, password = "") }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(resultUri = null) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            UnlockPdfViewModel(context.applicationContext) as T
    }
}
