package com.pdfapp.reader.ui.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.AppContainer
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.data.local.dao.BookmarkDao
import com.pdfapp.reader.data.local.entity.BookmarkEntity
import com.pdfapp.reader.domain.model.AnnotationType
import com.pdfapp.reader.domain.model.PageMode
import com.pdfapp.reader.domain.model.QuickAnnotation
import com.pdfapp.reader.domain.model.SearchResult
import com.pdfapp.reader.domain.usecase.ExtractTextBlocksUseCase
import com.pdfapp.reader.domain.usecase.SaveQuickAnnotationsUseCase
import com.pdfapp.reader.ui.viewer.render.BitmapLruPageCache
import com.pdfapp.reader.ui.viewer.render.PdfRenderConfig
import com.pdfapp.reader.ui.viewer.render.PdfRenderService
import com.pdfapp.reader.ui.viewer.selection.SelectionEngine
import com.pdfapp.reader.ui.viewer.selection.SelectionState
import com.pdfapp.reader.util.FileUtils
import com.pdfapp.reader.util.PdfBoxHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "PdfViewerVM"

data class PdfViewerUiState(
    val pdfUri: Uri? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isNightMode: Boolean = false,
    val showToolbar: Boolean = true,
    val showThumbnails: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val currentSearchIndex: Int = 0,
    val annotations: List<QuickAnnotation> = emptyList(),
    val hasUnsavedAnnotations: Boolean = false,
    val pageMode: PageMode = PageMode.CONTINUOUS,
    val isLoading: Boolean = true,
    val error: String? = null,
    val tempFile: File? = null,
    // Text selection state
    val isTextSelected: Boolean = false,
    val selectedText: String = "",
    val selectionLineBounds: List<RectF> = emptyList(),
    val selectionPageIndex: Int = -1,
    val isSelectionPopupVisible: Boolean = false,
    val isDraggingHandle: Boolean = false,
    val dragScreenPosition: Offset? = null,
    val dragDisplayWidth: Float = 0f,
    val dragDisplayHeight: Float = 0f,
    val tappedAnnotationId: String? = null,
    val showSaveDialog: Boolean = false,
    // Screen-space anchor for toolbar positioning (computed from selection bounds)
    val selectionAnchorScreenX: Int = 0,
    val selectionAnchorScreenY: Int = 0
)

class PdfViewerViewModel(
    private val context: Context,
    private val appContainer: AppContainer,
    private val bookmarkDao: BookmarkDao = appContainer.bookmarkDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfViewerUiState())
    val uiState: StateFlow<PdfViewerUiState> = _uiState

    private val extractUseCase = ExtractTextBlocksUseCase(context)
    private val selectionEngine = SelectionEngine(extractUseCase)
    private val saveQuickAnnotationsUseCase = SaveQuickAnnotationsUseCase(context)

    // Last known display dimensions for computing toolbar anchor position
    private var lastDisplayWidth: Float = 0f
    private var lastDisplayHeight: Float = 0f

    // --- Rendering infrastructure (Phase 1) ---
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var handleDragJob: Job? = null
    private var renderService: PdfRenderService? = null
    private val bitmapCache = BitmapLruPageCache()

    /** Page bitmaps keyed by page index. Observed by the Compose screen. */
    private val _pageBitmaps = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val pageBitmaps: StateFlow<Map<Int, Bitmap>> = _pageBitmaps

    /** Page aspect ratios (width/height) keyed by page index. */
    private val _pageAspectRatios = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val pageAspectRatios: StateFlow<Map<Int, Float>> = _pageAspectRatios

    /** Pages that failed to render (page index → error message). */
    private val _pageErrors = MutableStateFlow<Map<Int, String>>(emptyMap())
    val pageErrors: StateFlow<Map<Int, String>> = _pageErrors

    /** PDF page dimensions in points (from PdfBox mediaBox). Indexed by page index. */
    private val _pdfPageDimensions = MutableStateFlow<Map<Int, Pair<Float, Float>>>(emptyMap())
    val pdfPageDimensions: StateFlow<Map<Int, Pair<Float, Float>>> = _pdfPageDimensions

    // --- Bookmarks ---
    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookmarks

    /** True when current page is bookmarked. */
    val isCurrentPageBookmarked: StateFlow<Boolean> = combine(_bookmarks, _uiState) { bms, state ->
        bms.any { it.pageIndex == state.currentPage }
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private var bookmarkJob: kotlinx.coroutines.Job? = null

    // --- Memory pressure handling ---
    private val memoryCallback = object : ComponentCallbacks2 {
        @Suppress("DEPRECATION")
        override fun onTrimMemory(level: Int) {
            viewModelScope.launch {
                when {
                    level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                        Log.w(TAG, "Memory CRITICAL — clearing all bitmap cache")
                        bitmapCache.evictAll()
                        _pageBitmaps.value = emptyMap()
                    }
                    level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                        Log.w(TAG, "Memory LOW — trimming bitmap cache to 3")
                        bitmapCache.trimTo(3)
                        bitmapCache.bitmapPool.clear()
                    }
                    level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                        Log.w(TAG, "Memory running low — trimming bitmap cache to 5")
                        bitmapCache.trimTo(5)
                        bitmapCache.bitmapPool.clear()
                    }
                }
            }
        }
        override fun onConfigurationChanged(newConfig: Configuration) {}
        override fun onLowMemory() {
            viewModelScope.launch {
                Log.w(TAG, "onLowMemory — clearing all bitmap cache")
                bitmapCache.evictAll()
                _pageBitmaps.value = emptyMap()
            }
        }
    }

    init {
        context.registerComponentCallbacks(memoryCallback)
    }

    fun loadPdf(uri: Uri) {
        Log.d(TAG, "loadPdf: uri=$uri, scheme=${uri.scheme}")

        val previousJob = loadJob
        val previousService = renderService
        loadJob?.cancel()
        renderService = null
        // Clear stale bitmap cache so edited/saved content is re-rendered fresh
        _pageBitmaps.value = emptyMap()
        _pageErrors.value = emptyMap()
        _uiState.update { it.copy(pdfUri = uri, isLoading = true, error = null, tempFile = null) }
        loadJob = viewModelScope.launch {
            // Wait for previous rendering to finish cancelling before closing its service
            previousJob?.join()
            try { previousService?.close() } catch (e: Exception) {
                Log.w(TAG, "Error closing previous render service", e)
            }
            bitmapCache.evictAll() // Clear cached bitmaps from previous file
            val tempFile = FileUtils.copyToTempFile(context, uri, "viewer")

            // Get page count from the temp file (avoids permission issues with original URI)
            val count = if (tempFile != null) {
                PdfBoxHelper.getPageCount(context, Uri.fromFile(tempFile))
            } else {
                0
            }

            val errorMsg = if (tempFile == null) {
                Log.e(TAG, "loadPdf FAILED: tempFile is null for uri=$uri")
                buildErrorMessage(uri)
            } else {
                Log.d(TAG, "loadPdf SUCCESS: ${tempFile.absolutePath}, size=${tempFile.length()}")
                null
            }

            // Load PDF page dimensions for coordinate mapping
            if (tempFile != null) {
                val dims = PdfBoxHelper.getPageDimensions(context, Uri.fromFile(tempFile))
                _pdfPageDimensions.value = dims.mapIndexed { i, pair -> i to pair }.toMap()
            }

            _uiState.update {
                it.copy(
                    totalPages = count,
                    tempFile = tempFile,
                    isLoading = false,
                    error = errorMsg
                )
            }

            // Initialize PdfRenderService from the temp file
            if (tempFile != null) {
                try {
                    renderService = appContainer.createPdfRenderService(Uri.fromFile(tempFile))
                    loadPageAspectRatios()
                    renderPage(0)
                } catch (e: SecurityException) {
                    Log.e(TAG, "PDF is password-protected", e)
                    _uiState.update { it.copy(error = "This PDF is password-protected and cannot be opened.") }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create PdfRenderService", e)
                    _uiState.update { it.copy(error = e.message ?: "Failed to open PDF") }
                }
            }
        }
    }

    private suspend fun loadPageAspectRatios() {
        val service = renderService ?: return
        val ratios = mutableMapOf<Int, Float>()
        for (i in 0 until service.pageCount) {
            val size = service.getPageDimensions(i)
            ratios[i] = size.width.toFloat() / size.height.toFloat()
        }
        _pageAspectRatios.value = ratios
    }

    /** Renders a page bitmap and updates state. Tracks per-page errors. */
    fun renderPage(pageIndex: Int) {
        val service = renderService ?: return
        if (pageIndex < 0 || pageIndex >= service.pageCount) return
        // Skip if already errored
        if (_pageErrors.value.containsKey(pageIndex)) return
        viewModelScope.launch {
            try {
                val bitmap = bitmapCache.getOrRender(pageIndex) { _ ->
                    val pixelSize = service.getPagePixelSize(pageIndex, PdfRenderConfig.DEFAULT_DPI)
                    // Try to reuse a pooled bitmap matching these dimensions
                    val pooled = bitmapCache.bitmapPool.acquire(
                        pixelSize.width, pixelSize.height, Bitmap.Config.ARGB_8888
                    )
                    service.renderPage(pageIndex, pixelSize.width, pixelSize.height, pooled)
                }
                _pageBitmaps.update { it + (pageIndex to bitmap) }
            } catch (e: SecurityException) {
                Log.e(TAG, "Page $pageIndex is password-protected", e)
                _pageErrors.update { it + (pageIndex to "Password-protected page") }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to render page $pageIndex", e)
                _pageErrors.update { it + (pageIndex to "Failed to render page") }
            }
        }
    }

    /** Pre-render pages around [pageIndex] (current, +1..+3 ahead, -1 behind).
     *  Asymmetric: more look-ahead for downward scroll which is the common direction. */
    fun preRenderAdjacentPages(pageIndex: Int) {
        val service = renderService ?: return
        val max = service.pageCount
        listOf(pageIndex, pageIndex + 1, pageIndex + 2, pageIndex + 3, pageIndex - 1)
            .filter { it in 0 until max }
            .forEach { renderPage(it) }
    }

    private fun buildErrorMessage(uri: Uri): String {
        // Check if storage permission is the likely cause
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            return "Storage access required.\nGrant \"All files access\" in app Settings."
        }
        // Check if the file might have been deleted
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return "PDF file not found. It may have been moved or deleted."
                }
            } ?: return "PDF file not found. It may have been moved or deleted."
        } catch (_: Exception) { }

        return "Cannot open PDF file"
    }

    override fun onCleared() {
        context.unregisterComponentCallbacks(memoryCallback)
        _uiState.value.tempFile?.delete()
        renderService?.close()
        CoroutineScope(Dispatchers.Default).launch { bitmapCache.evictAll() }
        super.onCleared()
    }

    fun setCurrentPage(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
        preRenderAdjacentPages(page)
    }

    /** Jump to a specific page with animated scroll (for thumbnails, nav buttons). */
    fun jumpToPage(page: Int) {
        setCurrentPage(page)
        _scrollToPage.value = page
    }

    fun toggleNightMode() {
        _uiState.update { it.copy(isNightMode = !it.isNightMode) }
    }

    fun toggleToolbar() {
        _uiState.update { it.copy(showToolbar = !it.showToolbar) }
    }

    fun toggleThumbnails() {
        _uiState.update { it.copy(showThumbnails = !it.showThumbnails) }
    }

    fun toggleSearch() {
        _uiState.update { state ->
            if (state.isSearchActive) {
                state.copy(isSearchActive = false, searchQuery = "", searchResults = emptyList())
            } else {
                state.copy(isSearchActive = true)
            }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), currentSearchIndex = 0) }
            return
        }
        // Use tempFile URI for search (avoids permission issues with original URI)
        val searchUri = _uiState.value.tempFile?.let { Uri.fromFile(it) }
            ?: _uiState.value.pdfUri ?: return
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300L) // Debounce fast typing
            val results = PdfBoxHelper.searchText(context, searchUri, query)
            _uiState.update { it.copy(searchResults = results, currentSearchIndex = 0) }
            // Auto-scroll to first result's page
            if (results.isNotEmpty()) {
                navigateToSearchResult(results, 0)
            }
        }
    }

    fun nextSearchResult() {
        val state = _uiState.value
        if (state.searchResults.isEmpty()) return
        val next = (state.currentSearchIndex + 1) % state.searchResults.size
        _uiState.update { it.copy(currentSearchIndex = next) }
        navigateToSearchResult(state.searchResults, next)
    }

    fun previousSearchResult() {
        val state = _uiState.value
        if (state.searchResults.isEmpty()) return
        val prev = (state.currentSearchIndex - 1 + state.searchResults.size) % state.searchResults.size
        _uiState.update { it.copy(currentSearchIndex = prev) }
        navigateToSearchResult(state.searchResults, prev)
    }

    /** Navigate to the page containing the search result at [index]. */
    private fun navigateToSearchResult(results: List<SearchResult>, index: Int) {
        if (index < 0 || index >= results.size) return
        val targetPage = results[index].pageIndex
        if (targetPage != _uiState.value.currentPage) {
            setCurrentPage(targetPage)
            _scrollToPage.value = targetPage
        }
    }

    /** Emits target page index when search navigation needs to scroll the viewer. */
    private val _scrollToPage = MutableStateFlow(-1)
    val scrollToPage: StateFlow<Int> = _scrollToPage

    /** Called by the UI after completing a scroll-to-page animation. */
    fun onScrollToPageConsumed() {
        _scrollToPage.value = -1
    }

    fun addAnnotation(annotation: QuickAnnotation) {
        _uiState.update { state ->
            state.copy(annotations = state.annotations + annotation, hasUnsavedAnnotations = true)
        }
    }

    fun removeAnnotation(id: String) {
        _uiState.update { state ->
            val updated = state.annotations.filterNot { it.id == id }
            state.copy(annotations = updated, hasUnsavedAnnotations = updated.isNotEmpty())
        }
    }

    fun setPageMode(mode: PageMode) {
        _uiState.update { it.copy(pageMode = mode) }
    }

    fun goToNextPage() {
        val state = _uiState.value
        if (state.totalPages <= 0) return
        val next = (state.currentPage + 1).coerceAtMost(state.totalPages - 1)
        jumpToPage(next)
    }

    fun goToPreviousPage() {
        val state = _uiState.value
        if (state.totalPages <= 0) return
        val prev = (state.currentPage - 1).coerceAtLeast(0)
        jumpToPage(prev)
    }

    // --- Text Selection ---

    /**
     * Called when user long-presses on a page at screen coordinates.
     * Converts screen→PDF coords and triggers word selection.
     */
    fun onLongPressScreen(pageIndex: Int, screenX: Float, screenY: Float,
                          displayWidth: Float, displayHeight: Float) {
        lastDisplayWidth = displayWidth
        lastDisplayHeight = displayHeight
        val dims = _pdfPageDimensions.value[pageIndex] ?: return
        val pdfX = screenX / displayWidth * dims.first
        val pdfY = screenY / displayHeight * dims.second
        onLongPress(pageIndex, pdfX, pdfY)
    }

    /**
     * Called when user drags a selection handle to a new screen position.
     * Converts screen→PDF coords and updates selection.
     */
    fun onHandleDragScreen(isStartHandle: Boolean, screenX: Float, screenY: Float,
                           displayWidth: Float, displayHeight: Float) {
        lastDisplayWidth = displayWidth
        lastDisplayHeight = displayHeight
        _uiState.update { it.copy(
            dragScreenPosition = Offset(screenX, screenY),
            dragDisplayWidth = displayWidth,
            dragDisplayHeight = displayHeight
        ) }
        val state = _uiState.value
        val dims = _pdfPageDimensions.value[state.selectionPageIndex] ?: return
        val pdfX = screenX / displayWidth * dims.first
        val pdfY = screenY / displayHeight * dims.second
        onHandleDrag(isStartHandle, pdfX, pdfY)
    }

    fun onLongPress(pageIndex: Int, pdfX: Float, pdfY: Float) {
        val uri = _uiState.value.tempFile?.let { Uri.fromFile(it) }
            ?: _uiState.value.pdfUri ?: return
        viewModelScope.launch {
            val selection = selectionEngine.selectWordAt(uri, pageIndex, pdfX, pdfY)
            if (selection != null) applySelection(selection)
        }
    }

    fun onHandleDrag(isStartHandle: Boolean, pdfX: Float, pdfY: Float) {
        val uri = _uiState.value.tempFile?.let { Uri.fromFile(it) }
            ?: _uiState.value.pdfUri ?: return
        _uiState.update { it.copy(isDraggingHandle = true) }
        handleDragJob?.cancel()
        handleDragJob = viewModelScope.launch {
            val selection = selectionEngine.updateSelection(uri, isStartHandle, pdfX, pdfY)
            if (selection != null) applySelection(selection)
        }
    }

    fun onHandleDragEnd() {
        _uiState.update { it.copy(isDraggingHandle = false, dragScreenPosition = null) }
    }

    fun clearSelection() {
        selectionEngine.clearSelection()
        _uiState.update {
            it.copy(
                isTextSelected = false,
                selectedText = "",
                selectionLineBounds = emptyList(),
                selectionPageIndex = -1,
                isSelectionPopupVisible = false,
                isDraggingHandle = false,
                dragScreenPosition = null,
                tappedAnnotationId = null
            )
        }
    }

    private fun applySelection(selection: SelectionState) {
        // Compute screen-space anchor for toolbar positioning
        val (anchorX, anchorY) = computeSelectionScreenAnchor(selection)
        _uiState.update {
            it.copy(
                isTextSelected = true,
                selectedText = selection.selectedText,
                selectionLineBounds = selection.lineBounds,
                selectionPageIndex = selection.pageIndex,
                isSelectionPopupVisible = true,
                tappedAnnotationId = null,
                selectionAnchorScreenX = anchorX,
                selectionAnchorScreenY = anchorY
            )
        }
    }

    /** Convert the top-center of the first selection line from PDF→screen coords. */
    private fun computeSelectionScreenAnchor(selection: SelectionState): Pair<Int, Int> {
        if (selection.lineBounds.isEmpty() || lastDisplayWidth <= 0f) return Pair(100, 200)
        val dims = _pdfPageDimensions.value[selection.pageIndex] ?: return Pair(100, 200)
        val firstLine = selection.lineBounds.first()
        val centerX = (firstLine.left + firstLine.right) / 2f
        val topY = firstLine.top
        val screenX = (centerX / dims.first * lastDisplayWidth).toInt()
        val screenY = (topY / dims.second * lastDisplayHeight).toInt()
        return Pair(screenX, screenY)
    }

    /** Returns the union of all selection line bounds in PDF coordinates, or null if no selection. */
    fun getSelectionBoundsUnion(): RectF? {
        val bounds = _uiState.value.selectionLineBounds
        if (bounds.isEmpty()) return null
        val union = RectF(bounds.first())
        for (rect in bounds) { union.union(rect) }
        return union
    }

    // --- Selection Actions ---

    fun onCopyText() {
        val text = _uiState.value.selectedText
        if (text.isNotBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("PDF Text", text))
            Toast.makeText(context, R.string.viewer_copied, Toast.LENGTH_SHORT).show()
        }
        clearSelection()
    }

    fun onApplyAnnotation(type: AnnotationType) {
        val state = _uiState.value
        if (!state.isTextSelected || state.selectionLineBounds.isEmpty()) return

        val color = when (type) {
            AnnotationType.HIGHLIGHT -> QuickAnnotation.DEFAULT_HIGHLIGHT_COLOR
            AnnotationType.UNDERLINE -> QuickAnnotation.UNDERLINE_COLOR
            AnnotationType.STRIKETHROUGH -> QuickAnnotation.STRIKETHROUGH_COLOR
        }

        val annotation = QuickAnnotation(
            pageIndex = state.selectionPageIndex,
            type = type,
            lineBounds = state.selectionLineBounds,
            selectedText = state.selectedText,
            color = color
        )
        addAnnotation(annotation)
        clearSelection()
    }

    fun onSelectAll() {
        val uri = _uiState.value.tempFile?.let { Uri.fromFile(it) }
            ?: _uiState.value.pdfUri ?: return
        val pageIndex = _uiState.value.selectionPageIndex.takeIf { it >= 0 }
            ?: _uiState.value.currentPage
        viewModelScope.launch {
            val selection = selectionEngine.selectAllOnPage(uri, pageIndex)
            if (selection != null) applySelection(selection)
        }
    }

    fun onTranslateText() {
        val text = _uiState.value.selectedText
        if (text.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(
                    "https://translate.google.com/?sl=auto&tl=en&text=${android.net.Uri.encode(text)}"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open translate", e)
            Toast.makeText(context, "Cannot open translator", Toast.LENGTH_SHORT).show()
        }
        clearSelection()
    }

    fun onShareText() {
        val text = _uiState.value.selectedText
        if (text.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, null).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open share", e)
            Toast.makeText(context, "Cannot share text", Toast.LENGTH_SHORT).show()
        }
        clearSelection()
    }

    // --- Annotation Interaction ---

    fun onTapAnnotation(annotationId: String) {
        _uiState.update { it.copy(tappedAnnotationId = annotationId, isSelectionPopupVisible = false) }
    }

    fun dismissAnnotationPopup() {
        _uiState.update { it.copy(tappedAnnotationId = null) }
    }

    fun updateAnnotationColor(annotationId: String, newColor: Int) {
        _uiState.update { state ->
            val updated = state.annotations.map { ann ->
                if (ann.id == annotationId) ann.copy(color = newColor) else ann
            }
            state.copy(annotations = updated)
        }
    }

    /** Check if a tap at PDF coordinates hits an annotation. */
    fun hitTestAnnotation(pdfX: Float, pdfY: Float, pageIndex: Int): String? {
        return _uiState.value.annotations
            .filter { it.pageIndex == pageIndex }
            .firstOrNull { annotation ->
                annotation.lineBounds.any { rect ->
                    pdfX >= rect.left && pdfX <= rect.right &&
                        pdfY >= rect.top && pdfY <= rect.bottom
                }
            }?.id
    }

    // --- Save Dialog ---

    fun showSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = true) }
    }

    fun dismissSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = false) }
    }

    fun saveAnnotations() {
        val uri = _uiState.value.pdfUri ?: return
        val annotations = _uiState.value.annotations
        if (annotations.isEmpty()) return
        viewModelScope.launch {
            val path = saveQuickAnnotationsUseCase.save(uri, annotations)
            if (path != null) {
                Toast.makeText(context, R.string.edit_save_success, Toast.LENGTH_SHORT).show()
                _uiState.update { it.copy(hasUnsavedAnnotations = false, showSaveDialog = false) }
            } else {
                Toast.makeText(context, R.string.edit_save_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun discardAnnotations() {
        _uiState.update {
            it.copy(
                annotations = emptyList(),
                hasUnsavedAnnotations = false,
                showSaveDialog = false
            )
        }
    }

    // --- Bookmarks ---

    /** Start collecting bookmarks for the given PDF URI. Call once when PDF is opened. */
    fun loadBookmarks(pdfUri: String) {
        bookmarkJob?.cancel()
        bookmarkJob = viewModelScope.launch {
            bookmarkDao.getBookmarksForPdf(pdfUri).collect { list ->
                _bookmarks.value = list
            }
        }
    }

    /** Add or remove a bookmark for the current page. */
    fun toggleBookmark(pdfUri: String, pageIndex: Int) {
        viewModelScope.launch {
            if (bookmarkDao.isBookmarked(pdfUri, pageIndex)) {
                bookmarkDao.delete(pdfUri, pageIndex)
            } else {
                bookmarkDao.insert(BookmarkEntity(pdfUri = pdfUri, pageIndex = pageIndex))
            }
        }
    }

    class Factory(
        private val context: Context,
        private val appContainer: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PdfViewerViewModel(context.applicationContext, appContainer) as T
    }
}
