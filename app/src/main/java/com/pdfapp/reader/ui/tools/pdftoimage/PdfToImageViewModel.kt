package com.pdfapp.reader.ui.tools.pdftoimage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class ImageFormat(val extension: String, val compressFormat: Bitmap.CompressFormat) {
    PNG("png", Bitmap.CompressFormat.PNG),
    JPEG("jpg", Bitmap.CompressFormat.JPEG)
}

data class PdfToImageUiState(
    val selectedUri: Uri? = null,
    val fileName: String = "",
    val pageCount: Int = 0,
    val format: ImageFormat = ImageFormat.PNG,
    val isExporting: Boolean = false,
    val currentPage: Int = 0,
    val exportedCount: Int = 0,
    val error: String? = null
)

class PdfToImageViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfToImageUiState())
    val uiState: StateFlow<PdfToImageUiState> = _uiState

    fun selectPdf(uri: Uri) {
        val name = FileUtils.getFileName(context.contentResolver, uri) ?: "document.pdf"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@launch
                val renderer = PdfRenderer(fd)
                val count = renderer.pageCount
                renderer.close()
                fd.close()
                _uiState.update { it.copy(selectedUri = uri, fileName = name, pageCount = count, error = null, exportedCount = 0) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setFormat(format: ImageFormat) {
        _uiState.update { it.copy(format = format) }
    }

    fun exportAll() {
        val state = _uiState.value
        val uri = state.selectedUri ?: return
        if (state.isExporting) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExporting = true, currentPage = 0, error = null) }
            try {
                val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: throw Exception("Cannot open file")
                val renderer = PdfRenderer(fd)
                val baseName = state.fileName.substringBeforeLast(".")
                val dpi = 300
                val scale = dpi / 72f
                var exported = 0

                for (i in 0 until renderer.pageCount) {
                    _uiState.update { it.copy(currentPage = i + 1) }
                    val page = renderer.openPage(i)
                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()

                    saveBitmapToGallery(bitmap, "${baseName}_page_${i + 1}", state.format)
                    bitmap.recycle()
                    exported++
                }

                renderer.close()
                fd.close()
                _uiState.update { it.copy(isExporting = false, exportedCount = exported) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message) }
            }
        }
    }

    /** Save bitmap to Pictures/PdfReader/ via MediaStore (API 29+) or direct file (older). */
    private fun saveBitmapToGallery(bitmap: Bitmap, name: String, format: ImageFormat) {
        val fileName = "$name.${format.extension}"
        val mimeType = if (format == ImageFormat.PNG) "image/png" else "image/jpeg"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PdfReader")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            imageUri?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    bitmap.compress(format.compressFormat, 95, os)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PdfReader")
            dir.mkdirs()
            val file = File(dir, fileName)
            file.outputStream().use { os -> bitmap.compress(format.compressFormat, 95, os) }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(exportedCount = 0) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PdfToImageViewModel(context.applicationContext) as T
    }
}
