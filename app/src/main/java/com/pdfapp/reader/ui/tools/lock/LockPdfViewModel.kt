package com.pdfapp.reader.ui.tools.lock

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.usecase.LockPdfUseCase
import com.pdfapp.reader.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockPdfUiState(
    val selectedUri: Uri? = null,
    val fileName: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isProcessing: Boolean = false,
    val resultUri: Uri? = null,
    val error: String? = null
)

class LockPdfViewModel(private val context: Context) : ViewModel() {

    private val useCase = LockPdfUseCase(context.applicationContext)

    private val _uiState = MutableStateFlow(LockPdfUiState())
    val uiState: StateFlow<LockPdfUiState> = _uiState

    fun selectPdf(uri: Uri) {
        val name = FileUtils.getFileName(context.contentResolver, uri) ?: "document.pdf"
        _uiState.update { it.copy(selectedUri = uri, fileName = name, error = null, resultUri = null) }
    }

    fun setPassword(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun setConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, error = null) }
    }

    fun lockPdf() {
        val state = _uiState.value
        val uri = state.selectedUri ?: return

        if (state.password.length < 4) {
            _uiState.update { it.copy(error = context.getString(R.string.lock_password_min)) }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = context.getString(R.string.lock_password_mismatch)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val result = useCase.execute(uri, state.fileName, state.password)
                _uiState.update {
                    it.copy(isProcessing = false, resultUri = result, password = "", confirmPassword = "")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message, password = "", confirmPassword = "") }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(resultUri = null) }
    }

    /** Reset state so user can lock another PDF */
    fun resetForNewLock() {
        _uiState.update { LockPdfUiState() }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LockPdfViewModel(context.applicationContext) as T
    }
}
