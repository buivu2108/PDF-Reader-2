package com.pdfapp.reader.ui.tools.imagetopdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.domain.usecase.ImageToPdfUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImageToPdfUiState(
    val selectedImages: List<Uri> = emptyList(),
    val isCreating: Boolean = false,
    val resultUri: Uri? = null,
    val error: String? = null
)

class ImageToPdfViewModel(context: Context) : ViewModel() {

    private val useCase = ImageToPdfUseCase(context.applicationContext)

    private val _uiState = MutableStateFlow(ImageToPdfUiState())
    val uiState: StateFlow<ImageToPdfUiState> = _uiState

    fun addImages(uris: List<Uri>) {
        _uiState.update { state ->
            val combined = (state.selectedImages + uris).take(20)
            state.copy(selectedImages = combined, error = null)
        }
    }

    fun removeImage(index: Int) {
        _uiState.update { state ->
            state.copy(selectedImages = state.selectedImages.toMutableList().apply { removeAt(index) })
        }
    }

    fun reorderImages(from: Int, to: Int) {
        _uiState.update { state ->
            val list = state.selectedImages.toMutableList()
            val item = list.removeAt(from)
            list.add(to, item)
            state.copy(selectedImages = list)
        }
    }

    fun createPdf() {
        val images = _uiState.value.selectedImages
        if (images.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }
            try {
                val resultUri = useCase.execute(images)
                _uiState.update { it.copy(isCreating = false, resultUri = resultUri) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreating = false, error = e.message) }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(resultUri = null) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ImageToPdfViewModel(context.applicationContext) as T
    }
}
