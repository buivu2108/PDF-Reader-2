package com.pdfapp.reader.ui.tools.pagemanager

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.util.FileUtils
import com.tom_roush.pdfbox.pdmodel.PDDocument
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

/** Represents a page in the manager — tracks original index, pending rotation, and delete flag. */
data class PageInfo(
    val index: Int,
    val rotation: Int = 0,
    val deleted: Boolean = false
)

data class PageManagerUiState(
    val selectedUri: Uri? = null,
    val fileName: String = "",
    val pages: List<PageInfo> = emptyList(),
    val selectedPageIndex: Int? = null,
    val isProcessing: Boolean = false,
    val resultUri: Uri? = null,
    val error: String? = null
)

class PageManagerViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(PageManagerUiState())
    val uiState: StateFlow<PageManagerUiState> = _uiState

    /** Open PdfRenderer, get page count, initialize pages list. */
    fun selectPdf(ctx: Context, uri: Uri) {
        val name = FileUtils.getFileName(ctx.contentResolver, uri) ?: "document.pdf"
        _uiState.update { it.copy(selectedUri = uri, fileName = name, error = null, resultUri = null) }
        viewModelScope.launch {
            try {
                val count = getPageCount(ctx, uri)
                _uiState.update { it.copy(pages = List(count) { i -> PageInfo(i) }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to open PDF: ${e.message}") }
            }
        }
    }

    /** Cycle rotation +90° (mod 360) for the page at [pageListIndex] in current list order. */
    fun rotatePage(pageListIndex: Int) {
        _uiState.update { state ->
            val updated = state.pages.toMutableList()
            if (pageListIndex !in updated.indices) return@update state
            val page = updated[pageListIndex]
            updated[pageListIndex] = page.copy(rotation = (page.rotation + 90) % 360)
            state.copy(pages = updated)
        }
    }

    /** Toggle deleted flag for the page at [pageListIndex]. */
    fun deletePage(pageListIndex: Int) {
        _uiState.update { state ->
            val updated = state.pages.toMutableList()
            if (pageListIndex !in updated.indices) return@update state
            val page = updated[pageListIndex]
            updated[pageListIndex] = page.copy(deleted = !page.deleted)
            state.copy(pages = updated)
        }
    }

    /** Move page in the display list from [from] to [to]. */
    fun movePage(from: Int, to: Int) {
        _uiState.update { state ->
            val updated = state.pages.toMutableList()
            if (from !in updated.indices || to !in updated.indices) return@update state
            val item = updated.removeAt(from)
            updated.add(to, item)
            state.copy(pages = updated, selectedPageIndex = null)
        }
    }

    /** Select or deselect a page thumbnail. */
    fun selectPage(pageListIndex: Int) {
        _uiState.update { state ->
            val next = if (state.selectedPageIndex == pageListIndex) null else pageListIndex
            state.copy(selectedPageIndex = next)
        }
    }

    /** Apply all pending rotations, deletions, and reorder using PdfBox; save to app files dir. */
    fun applyChanges(ctx: Context) {
        val state = _uiState.value
        val uri = state.selectedUri ?: return
        val pages = state.pages
        if (pages.all { !it.deleted && it.rotation == 0 } && pages.map { it.index } == pages.indices.toList()) {
            _uiState.update { it.copy(error = "No changes to apply") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val result = applyPdfChanges(ctx, uri, state.fileName, pages)
                _uiState.update { it.copy(isProcessing = false, resultUri = result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = "Failed: ${e.message}") }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(resultUri = null) }
    }

    private suspend fun getPageCount(ctx: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val inputStream = ctx.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot read PDF")
        val doc = try { PDDocument.load(inputStream) } finally { inputStream.close() }
        try { doc.numberOfPages } finally { doc.close() }
    }

    private suspend fun applyPdfChanges(
        ctx: Context,
        uri: Uri,
        fileName: String,
        pages: List<PageInfo>
    ): Uri = withContext(Dispatchers.IO) {
        val inputStream = ctx.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot read PDF")

        val sourceDoc = try { PDDocument.load(inputStream) } finally { inputStream.close() }
        try {
            // Build new document in display order, skipping deleted pages
            val newDoc = PDDocument()
            val activePage = pages.filter { !it.deleted }
            for (pageInfo in activePage) {
                val srcPage = sourceDoc.getPage(pageInfo.index)
                val importedPage = newDoc.importPage(srcPage)
                // Apply accumulated rotation on top of any existing rotation
                importedPage.rotation = (srcPage.rotation + pageInfo.rotation) % 360
            }

            val outputDir = File(ctx.filesDir, "pagemanager").also { it.mkdirs() }
            val cleanName = fileName.removeSuffix(".pdf")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outputFile = File(outputDir, "${cleanName}_edited_$timestamp.pdf")
            newDoc.save(outputFile)
            newDoc.close()
            Uri.fromFile(outputFile)
        } finally {
            sourceDoc.close()
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PageManagerViewModel(context.applicationContext) as T
    }
}
