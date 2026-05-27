package com.pdfapp.reader.ui.editmode

import android.graphics.Bitmap
import android.graphics.RectF
import com.pdfapp.reader.domain.model.EditAction
import com.pdfapp.reader.domain.model.EditAnnotation
import com.pdfapp.reader.domain.model.EditTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared state holder for the redesigned Edit Mode.
 * Owns the undo/redo manager, annotation list, tab selection, and page bitmaps.
 * Not a ViewModel -- instantiated per edit session and passed to tab ViewModels.
 */
class EditModeCoordinator {

    val undoRedoManager = UndoRedoManager()

    private val _annotations = MutableStateFlow<List<EditAnnotation>>(emptyList())
    val annotations: StateFlow<List<EditAnnotation>> = _annotations.asStateFlow()

    private val _selectedTab = MutableStateFlow(EditTab.MARK)
    val selectedTab: StateFlow<EditTab> = _selectedTab.asStateFlow()

    private val _selectedElementId = MutableStateFlow<String?>(null)
    val selectedElementId: StateFlow<String?> = _selectedElementId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _pageBitmaps = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val pageBitmaps: StateFlow<Map<Int, Bitmap>> = _pageBitmaps.asStateFlow()

    private val _pageDimensions = MutableStateFlow<Map<Int, Pair<Float, Float>>>(emptyMap())
    val pageDimensions: StateFlow<Map<Int, Pair<Float, Float>>> = _pageDimensions.asStateFlow()

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    fun setCurrentPage(pageIndex: Int) {
        if (pageIndex in 0 until _pageCount.value) {
            _currentPageIndex.value = pageIndex
        }
    }

    fun nextPage() { setCurrentPage(_currentPageIndex.value + 1) }
    fun previousPage() { setCurrentPage(_currentPageIndex.value - 1) }

    // -- Annotation mutations --

    fun addAnnotation(annotation: EditAnnotation) {
        _annotations.value = _annotations.value + annotation
        undoRedoManager.pushAction(EditAction.Add(annotation))
    }

    fun removeAnnotation(id: String) {
        val annotation = _annotations.value.find { it.id == id } ?: return
        _annotations.value = _annotations.value.filter { it.id != id }
        undoRedoManager.pushAction(EditAction.Remove(annotation))
    }

    fun updateAnnotation(updated: EditAnnotation) {
        _annotations.value = _annotations.value.map {
            if (it.id == updated.id) updated else it
        }
    }

    /** Batch update multiple annotations in a single StateFlow emission (avoids per-item recomposition). */
    fun updateAnnotationsBatch(updatedMap: Map<String, EditAnnotation>) {
        _annotations.value = _annotations.value.map { updatedMap[it.id] ?: it }
    }

    fun getAnnotationsForPage(pageIndex: Int): List<EditAnnotation> =
        _annotations.value.filter { it.pageIndex == pageIndex }

    // -- Undo / Redo --

    fun applyUndo() {
        val action = undoRedoManager.undo() ?: return
        when (action) {
            is EditAction.Add -> _annotations.value = _annotations.value.filter { it.id != action.annotation.id }
            is EditAction.Remove -> _annotations.value = _annotations.value + action.annotation
            is EditAction.Move -> updateBounds(action.id, action.oldBounds)
            is EditAction.Resize -> updateBounds(action.id, action.oldBounds)
            is EditAction.ChangeColor -> updateAnnotationProperty(action.id) { setColor(it, action.oldColor) }
            is EditAction.ChangeWidth -> updateAnnotationProperty(action.id) { setStrokeWidth(it, action.oldWidth) }
            is EditAction.ChangeOpacity -> updateAnnotationProperty(action.id) { setOpacity(it, action.oldOpacity) }
            is EditAction.ChangeFontSize -> updateAnnotationProperty(action.id) { setFontSize(it, action.oldSize) }
            is EditAction.GroupMove -> applyGroupOffset(action.ids, -action.deltaX, -action.deltaY)
        }
    }

    fun applyRedo() {
        val action = undoRedoManager.redo() ?: return
        when (action) {
            is EditAction.Add -> _annotations.value = _annotations.value + action.annotation
            is EditAction.Remove -> _annotations.value = _annotations.value.filter { it.id != action.annotation.id }
            is EditAction.Move -> updateBounds(action.id, action.newBounds)
            is EditAction.Resize -> updateBounds(action.id, action.newBounds)
            is EditAction.ChangeColor -> updateAnnotationProperty(action.id) { setColor(it, action.newColor) }
            is EditAction.ChangeWidth -> updateAnnotationProperty(action.id) { setStrokeWidth(it, action.newWidth) }
            is EditAction.ChangeOpacity -> updateAnnotationProperty(action.id) { setOpacity(it, action.newOpacity) }
            is EditAction.ChangeFontSize -> updateAnnotationProperty(action.id) { setFontSize(it, action.newSize) }
            is EditAction.GroupMove -> applyGroupOffset(action.ids, action.deltaX, action.deltaY)
        }
    }

    /** Offset multiple annotations by (dx, dy). Used for GroupMove undo/redo. */
    private fun applyGroupOffset(ids: List<String>, dx: Float, dy: Float) {
        _annotations.value = _annotations.value.map { ann ->
            if (ann.id !in ids) ann
            else offsetAnnotation(ann, dx, dy)
        }
    }

    /** Offset a single annotation by (dx, dy), including internal points. */
    private fun offsetAnnotation(ann: EditAnnotation, dx: Float, dy: Float): EditAnnotation {
        val newBounds = RectF(ann.bounds).apply { offset(dx, dy) }
        val moved = replaceAnnotationBounds(ann, newBounds)
        return when (moved) {
            is EditAnnotation.FreehandStroke -> moved.copy(points = moved.points.map { android.graphics.PointF(it.x + dx, it.y + dy) })
            is EditAnnotation.PolygonShape -> moved.copy(points = moved.points.map { android.graphics.PointF(it.x + dx, it.y + dy) })
            is EditAnnotation.LineShape -> moved.copy(startPoint = android.graphics.PointF(moved.startPoint.x + dx, moved.startPoint.y + dy), endPoint = android.graphics.PointF(moved.endPoint.x + dx, moved.endPoint.y + dy))
            is EditAnnotation.ArrowShape -> moved.copy(startPoint = android.graphics.PointF(moved.startPoint.x + dx, moved.startPoint.y + dy), endPoint = android.graphics.PointF(moved.endPoint.x + dx, moved.endPoint.y + dy))
            is EditAnnotation.ZigzagShape -> moved.copy(startPoint = android.graphics.PointF(moved.startPoint.x + dx, moved.startPoint.y + dy), endPoint = android.graphics.PointF(moved.endPoint.x + dx, moved.endPoint.y + dy))
            else -> moved
        }
    }

    private fun updateBounds(id: String, bounds: RectF) {
        _annotations.value = _annotations.value.map { ann ->
            if (ann.id != id) ann
            else replaceAnnotationBounds(ann, bounds)
        }
    }

    private fun replaceAnnotationBounds(ann: EditAnnotation, bounds: RectF): EditAnnotation =
        when (ann) {
            is EditAnnotation.Highlight -> ann.copy(bounds = bounds)
            is EditAnnotation.Underline -> ann.copy(bounds = bounds)
            is EditAnnotation.Strikethrough -> ann.copy(bounds = bounds)
            is EditAnnotation.FreehandStroke -> ann.copy(bounds = bounds)
            is EditAnnotation.OvalShape -> ann.copy(bounds = bounds)
            is EditAnnotation.RectShape -> ann.copy(bounds = bounds)
            is EditAnnotation.SignatureElement -> ann.copy(bounds = bounds)
            is EditAnnotation.ImageElement -> ann.copy(bounds = bounds)
            is EditAnnotation.DateStamp -> ann.copy(bounds = bounds)
            is EditAnnotation.TextElement -> ann.copy(bounds = bounds)
            is EditAnnotation.StickyNote -> ann.copy(bounds = bounds)
            is EditAnnotation.LineShape -> ann.copy(bounds = bounds)
            is EditAnnotation.ArrowShape -> ann.copy(bounds = bounds)
            is EditAnnotation.ZigzagShape -> ann.copy(bounds = bounds)
            is EditAnnotation.TriangleShape -> ann.copy(bounds = bounds)
            is EditAnnotation.PolygonShape -> ann.copy(bounds = bounds)
        }

    private fun updateAnnotationProperty(id: String, transform: (EditAnnotation) -> EditAnnotation) {
        _annotations.value = _annotations.value.map { if (it.id == id) transform(it) else it }
    }

    private fun setColor(ann: EditAnnotation, color: Int): EditAnnotation = when (ann) {
        is EditAnnotation.Highlight -> ann.copy(color = color)
        is EditAnnotation.Underline -> ann.copy(color = color)
        is EditAnnotation.Strikethrough -> ann.copy(color = color)
        is EditAnnotation.FreehandStroke -> ann.copy(color = color)
        is EditAnnotation.OvalShape -> ann.copy(color = color)
        is EditAnnotation.RectShape -> ann.copy(color = color)
        is EditAnnotation.DateStamp -> ann.copy(color = color)
        is EditAnnotation.TextElement -> ann.copy(color = color)
        is EditAnnotation.StickyNote -> ann.copy(color = color)
        is EditAnnotation.LineShape -> ann.copy(color = color)
        is EditAnnotation.ArrowShape -> ann.copy(color = color)
        is EditAnnotation.ZigzagShape -> ann.copy(color = color)
        is EditAnnotation.TriangleShape -> ann.copy(color = color)
        is EditAnnotation.PolygonShape -> ann.copy(color = color)
        is EditAnnotation.SignatureElement, is EditAnnotation.ImageElement -> ann
    }

    private fun setStrokeWidth(ann: EditAnnotation, width: Float): EditAnnotation = when (ann) {
        is EditAnnotation.FreehandStroke -> ann.copy(strokeWidth = width)
        is EditAnnotation.OvalShape -> ann.copy(strokeWidth = width)
        is EditAnnotation.RectShape -> ann.copy(strokeWidth = width)
        is EditAnnotation.LineShape -> ann.copy(strokeWidth = width)
        is EditAnnotation.ArrowShape -> ann.copy(strokeWidth = width)
        is EditAnnotation.ZigzagShape -> ann.copy(strokeWidth = width)
        is EditAnnotation.TriangleShape -> ann.copy(strokeWidth = width)
        is EditAnnotation.PolygonShape -> ann.copy(strokeWidth = width)
        else -> ann
    }

    private fun setOpacity(ann: EditAnnotation, opacity: Float): EditAnnotation = when (ann) {
        is EditAnnotation.Highlight -> ann.copy(opacity = opacity)
        else -> ann
    }

    private fun setFontSize(ann: EditAnnotation, size: Float): EditAnnotation = when (ann) {
        is EditAnnotation.DateStamp -> ann.copy(fontSize = size)
        else -> ann
    }

    // -- Tab / Selection --

    fun selectTab(tab: EditTab) {
        _selectedElementId.value = null
        _selectedTab.value = tab
    }

    fun selectElement(id: String?) {
        _selectedElementId.value = id
    }

    fun hasChanges(): Boolean = undoRedoManager.hasChanges() || _insertedPages.value.isNotEmpty()

    // -- Page state setters --

    fun setLoading(loading: Boolean) { _isLoading.value = loading }
    fun setSaving(saving: Boolean) { _isSaving.value = saving }
    fun setPageCount(count: Int) { _pageCount.value = count }

    fun setPageBitmap(pageIndex: Int, bitmap: Bitmap) {
        _pageBitmaps.value = _pageBitmaps.value + (pageIndex to bitmap)
    }

    fun setPageDimensions(pageIndex: Int, width: Float, height: Float) {
        _pageDimensions.value = _pageDimensions.value + (pageIndex to (width to height))
    }

    // -- Inserted pages (from external PDF or image) --
    // Maps inserted page index → saved bitmap file path (for save process)
    private val _insertedPages = MutableStateFlow<Map<Int, String>>(emptyMap())
    val insertedPages: StateFlow<Map<Int, String>> = _insertedPages.asStateFlow()

    /**
     * Insert a page after [afterIndex]. Shifts all subsequent pages, annotations,
     * and page data. Returns the index of the newly inserted page.
     *
     * @param afterIndex Insert after this page index (-1 to insert at the beginning)
     * @param bitmap     Page bitmap to display during editing
     * @param pdfWidth   PDF point width of the inserted page
     * @param pdfHeight  PDF point height of the inserted page
     * @param savedPath  Path to saved bitmap for the save process
     */
    fun insertPageAt(
        afterIndex: Int, bitmap: Bitmap, pdfWidth: Float, pdfHeight: Float, savedPath: String
    ): Int {
        val insertIndex = afterIndex + 1
        val oldPageCount = _pageCount.value

        // 1. Shift annotations on pages >= insertIndex
        _annotations.value = _annotations.value.map { ann ->
            if (ann.pageIndex >= insertIndex) shiftAnnotationPage(ann, 1) else ann
        }

        // 2. Shift page bitmaps
        val newBitmaps = mutableMapOf<Int, Bitmap>()
        for ((idx, bmp) in _pageBitmaps.value) {
            newBitmaps[if (idx >= insertIndex) idx + 1 else idx] = bmp
        }
        newBitmaps[insertIndex] = bitmap
        _pageBitmaps.value = newBitmaps

        // 3. Shift page dimensions
        val newDims = mutableMapOf<Int, Pair<Float, Float>>()
        for ((idx, dim) in _pageDimensions.value) {
            newDims[if (idx >= insertIndex) idx + 1 else idx] = dim
        }
        newDims[insertIndex] = pdfWidth to pdfHeight
        _pageDimensions.value = newDims

        // 4. Shift existing inserted pages tracking
        val newInserted = mutableMapOf<Int, String>()
        for ((idx, path) in _insertedPages.value) {
            newInserted[if (idx >= insertIndex) idx + 1 else idx] = path
        }
        newInserted[insertIndex] = savedPath
        _insertedPages.value = newInserted

        // 5. Update page count and navigate to inserted page
        _pageCount.value = oldPageCount + 1
        _currentPageIndex.value = insertIndex
        return insertIndex
    }

    /** Create a copy of the annotation with its pageIndex shifted by [delta]. */
    private fun shiftAnnotationPage(ann: EditAnnotation, delta: Int): EditAnnotation {
        val p = ann.pageIndex + delta
        return when (ann) {
            is EditAnnotation.Highlight -> ann.copy(pageIndex = p)
            is EditAnnotation.Underline -> ann.copy(pageIndex = p)
            is EditAnnotation.Strikethrough -> ann.copy(pageIndex = p)
            is EditAnnotation.StickyNote -> ann.copy(pageIndex = p)
            is EditAnnotation.FreehandStroke -> ann.copy(pageIndex = p)
            is EditAnnotation.OvalShape -> ann.copy(pageIndex = p)
            is EditAnnotation.RectShape -> ann.copy(pageIndex = p)
            is EditAnnotation.LineShape -> ann.copy(pageIndex = p)
            is EditAnnotation.ArrowShape -> ann.copy(pageIndex = p)
            is EditAnnotation.ZigzagShape -> ann.copy(pageIndex = p)
            is EditAnnotation.TriangleShape -> ann.copy(pageIndex = p)
            is EditAnnotation.PolygonShape -> ann.copy(pageIndex = p)
            is EditAnnotation.SignatureElement -> ann.copy(pageIndex = p)
            is EditAnnotation.ImageElement -> ann.copy(pageIndex = p)
            is EditAnnotation.DateStamp -> ann.copy(pageIndex = p)
            is EditAnnotation.TextElement -> ann.copy(pageIndex = p)
        }
    }

    /** Clear all state on screen disposal. Bitmaps are NOT recycled here because
     *  the Compose exit animation may still be rendering them. GC handles cleanup. */
    fun dispose() {
        _pageBitmaps.value = emptyMap()
        _annotations.value = emptyList()
        _insertedPages.value = emptyMap()
        undoRedoManager.clear()
    }
}
