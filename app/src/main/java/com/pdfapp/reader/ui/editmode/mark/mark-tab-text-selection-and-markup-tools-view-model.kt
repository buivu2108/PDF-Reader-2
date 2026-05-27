package com.pdfapp.reader.ui.editmode.mark

import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.pdfapp.reader.domain.model.CharInfo
import com.pdfapp.reader.domain.model.EditAction
import com.pdfapp.reader.domain.model.EditAnnotation
import com.pdfapp.reader.domain.usecase.ExtractTextBlocksUseCase
import com.pdfapp.reader.ui.editmode.EditModeCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "MarkTab"

/** Active markup tool for the Mark tab. */
enum class MarkTool { HIGHLIGHT, UNDERLINE, STRIKETHROUGH, NOTE, NONE }

/**
 * ViewModel for Mark tab. Manages text selection, tool state, and annotation creation.
 * Pushes completed annotations to EditModeCoordinator.
 */
class MarkTabViewModel(
    private val coordinator: EditModeCoordinator,
    private val extractUseCase: ExtractTextBlocksUseCase,
    private val coroutineScope: CoroutineScope
) {
    private val _activeTool = MutableStateFlow(MarkTool.NONE)
    val activeTool: StateFlow<MarkTool> = _activeTool.asStateFlow()

    /** Shared color for all mark tools (Highlight, Underline, Strikethrough). */
    private val _markColor = MutableStateFlow(0xFFFFEB3B.toInt())
    val markColor: StateFlow<Int> = _markColor.asStateFlow()

    /** Shared opacity for all mark tools. */
    private val _markOpacity = MutableStateFlow(0.4f)
    val markOpacity: StateFlow<Float> = _markOpacity.asStateFlow()

    private val _selectionState = MutableStateFlow<SelectionResult?>(null)
    val selectionState: StateFlow<SelectionResult?> = _selectionState.asStateFlow()

    private val _noTextAvailable = MutableStateFlow(false)
    val noTextAvailable: StateFlow<Boolean> = _noTextAvailable.asStateFlow()

    private val _selectionPageIndex = MutableStateFlow(-1)
    val selectionPageIndex: StateFlow<Int> = _selectionPageIndex.asStateFlow()

    private val _isDragging = MutableStateFlow(false)
    val isDragging: StateFlow<Boolean> = _isDragging.asStateFlow()

    private val _dragTouchPosition = MutableStateFlow(Offset.Unspecified)
    val dragTouchPosition: StateFlow<Offset> = _dragTouchPosition.asStateFlow()

    private val _showApplyPopup = MutableStateFlow(false)
    val showApplyPopup: StateFlow<Boolean> = _showApplyPopup.asStateFlow()

    private val _wordBoundaryCrossed = MutableStateFlow(false)
    val wordBoundaryCrossed: StateFlow<Boolean> = _wordBoundaryCrossed.asStateFlow()

    private var dragStartCharIndex: Int = -1

    private val charsByPage = ConcurrentHashMap<Int, List<CharInfo>>()

    fun selectTool(tool: MarkTool) {
        _activeTool.value = if (_activeTool.value == tool) MarkTool.NONE else tool
        _selectionState.value = null
    }

    fun loadCharsForPage(uri: Uri, pageIndex: Int) {
        if (charsByPage.containsKey(pageIndex)) return
        coroutineScope.launch {
            val chars = extractUseCase.extractCharactersForPage(uri, pageIndex)
            charsByPage[pageIndex] = chars
            Log.d(TAG, "Loaded ${chars.size} chars for page $pageIndex")
        }
    }

    fun getCharsForPage(pageIndex: Int): List<CharInfo> = charsByPage[pageIndex] ?: emptyList()

    /** Handle long-press at PDF coordinates on a page. */
    /** Place a sticky note at the tapped position. Note icon is 24x24 PDF points. */
    fun placeStickyNote(pageIndex: Int, pdfX: Float, pdfY: Float) {
        val noteSize = 24f
        val bounds = RectF(pdfX - noteSize / 2, pdfY - noteSize / 2, pdfX + noteSize / 2, pdfY + noteSize / 2)
        val annotation = EditAnnotation.StickyNote(
            pageIndex = pageIndex,
            bounds = bounds,
            color = _markColor.value
        )
        coordinator.addAnnotation(annotation)
        coordinator.selectElement(annotation.id)
    }

    fun onLongPress(pageIndex: Int, pdfX: Float, pdfY: Float) {
        if (_activeTool.value == MarkTool.NONE || _activeTool.value == MarkTool.NOTE) return
        val chars = charsByPage[pageIndex]
        if (chars.isNullOrEmpty()) {
            _noTextAvailable.value = true
            return
        }
        val wordRange = MarkTextSelectionEngine.findWordAtPoint(chars, pdfX, pdfY) ?: return
        val result = MarkTextSelectionEngine.expandSelectionToHandles(
            chars, wordRange.first, wordRange.second
        )
        _selectionState.value = result
        _selectionPageIndex.value = pageIndex
        _showApplyPopup.value = true
    }

    /** Handle drag on start or end handle with word-snap. */
    fun onHandleDrag(pageIndex: Int, pdfX: Float, pdfY: Float, isStartHandle: Boolean) {
        val chars = charsByPage[pageIndex] ?: return
        val current = _selectionState.value ?: return
        val rawIdx = MarkTextSelectionEngine.findCharIndexAtPoint(chars, pdfX, pdfY)
        val snappedIdx = MarkTextSelectionEngine.snapToWordBoundary(chars, rawIdx, isStart = isStartHandle)

        val prevStart = current.startCharIndex
        val prevEnd = current.endCharIndex

        val newStart: Int
        val newEnd: Int
        if (isStartHandle) {
            newStart = snappedIdx.coerceAtMost(current.endCharIndex)
            newEnd = current.endCharIndex
        } else {
            newStart = current.startCharIndex
            newEnd = snappedIdx.coerceAtLeast(current.startCharIndex)
        }

        // Detect word boundary crossing for haptic trigger
        if (newStart != prevStart || newEnd != prevEnd) {
            _wordBoundaryCrossed.value = true
        }

        _selectionState.value = MarkTextSelectionEngine.expandSelectionToHandles(
            chars, newStart, newEnd
        )
    }

    fun onDragStart() { _isDragging.value = true; _showApplyPopup.value = false }
    fun onDragMove(screenPos: Offset) { _dragTouchPosition.value = screenPos }
    fun onDragEnd() { _isDragging.value = false; _showApplyPopup.value = true }
    fun dismissPopup() { _showApplyPopup.value = false }
    fun consumeWordBoundaryCrossed() { _wordBoundaryCrossed.value = false }

    fun onPageDragStart(pageIndex: Int, pdfX: Float, pdfY: Float) {
        if (_activeTool.value == MarkTool.NONE) return
        val chars = charsByPage[pageIndex]
        if (chars.isNullOrEmpty()) return
        val rawIdx = MarkTextSelectionEngine.findCharIndexAtPoint(chars, pdfX, pdfY)
        if (rawIdx >= 0) {
            dragStartCharIndex = rawIdx
            _selectionState.value = MarkTextSelectionEngine.expandSelectionToHandles(chars, rawIdx, rawIdx)
            _selectionPageIndex.value = pageIndex
            _showApplyPopup.value = false
        } else {
            dragStartCharIndex = -1
        }
    }

    fun onPageDragMove(pageIndex: Int, pdfX: Float, pdfY: Float) {
        if (_activeTool.value == MarkTool.NONE) return
        if (dragStartCharIndex < 0) return
        if (_selectionPageIndex.value != pageIndex) return
        val chars = charsByPage[pageIndex] ?: return
        
        val rawIdx = MarkTextSelectionEngine.findCharIndexAtPoint(chars, pdfX, pdfY)
        if (rawIdx >= 0) {
            val start = minOf(dragStartCharIndex, rawIdx)
            val end = maxOf(dragStartCharIndex, rawIdx)
            _selectionState.value = MarkTextSelectionEngine.expandSelectionToHandles(chars, start, end)
        }
    }

    fun onPageDragEnd(pageIndex: Int) {
        dragStartCharIndex = -1
        if (_selectionState.value != null && _selectionPageIndex.value == pageIndex) {
            applyCurrentTool(pageIndex)
        }
    }

    /** Apply the current tool's annotation from the active selection. */
    fun applyCurrentTool(pageIndex: Int) {
        val selection = _selectionState.value ?: return
        val tool = _activeTool.value
        if (tool == MarkTool.NONE || tool == MarkTool.NOTE) return

        val existingAnnotations = coordinator.annotations.value.filter { it.pageIndex == pageIndex }
        
        // Find existing text quads for the SAME tool type
        val existingQuads = existingAnnotations.flatMap { ann ->
            when (tool) {
                MarkTool.HIGHLIGHT -> if (ann is EditAnnotation.Highlight) ann.textQuads else emptyList()
                MarkTool.UNDERLINE -> if (ann is EditAnnotation.Underline) ann.textQuads else emptyList()
                MarkTool.STRIKETHROUGH -> if (ann is EditAnnotation.Strikethrough) ann.textQuads else emptyList()
                else -> emptyList()
            }
        }
        
        // Filter out quads from our current selection that already exist in existingQuads
        val filteredQuads = mutableListOf<RectF>()
        for (newQuad in selection.quads) {
            var overlaps = false
            val newArea = newQuad.width() * newQuad.height()
            
            for (existingQuad in existingQuads) {
                val intersection = RectF()
                if (intersection.setIntersect(newQuad, existingQuad)) {
                    val intersectArea = intersection.width() * intersection.height()
                    if (intersectArea > 0.5f * newArea) {
                        overlaps = true
                        break
                    }
                }
            }
            if (!overlaps) {
                filteredQuads.add(newQuad)
            }
        }

        if (filteredQuads.isEmpty()) {
            _selectionState.value = null
            _showApplyPopup.value = false
            return // Everything is already highlighted!
        }

        val finalBounds = MarkTextSelectionEngine.boundingRect(filteredQuads)
        val annotation = createAnnotation(tool, pageIndex, finalBounds, filteredQuads)
        coordinator.addAnnotation(annotation)
        _selectionState.value = null
        _showApplyPopup.value = false
    }

    /** Legacy: create annotation from current selection (used by onTap flow). */
    fun onSelectionComplete(pageIndex: Int) {
        applyCurrentTool(pageIndex)
    }

    /** Tap on existing annotation: select it. */
    fun onAnnotationTap(id: String) {
        coordinator.selectElement(id)
    }

    fun onPageTap(pageIndex: Int, pdfX: Float, pdfY: Float) {
        if (_selectionState.value != null) {
            clearSelection()
            return
        }
        val annotations = coordinator.annotations.value.filter { it.pageIndex == pageIndex }
        for (ann in annotations.reversed()) {
            val hit = when (ann) {
                is EditAnnotation.Highlight -> ann.textQuads.any { containsWithTolerance(it, pdfX, pdfY, 15f) }
                is EditAnnotation.Underline -> ann.textQuads.any { containsWithTolerance(it, pdfX, pdfY, 15f) }
                is EditAnnotation.Strikethrough -> ann.textQuads.any { containsWithTolerance(it, pdfX, pdfY, 15f) }
                is EditAnnotation.StickyNote -> containsWithTolerance(ann.bounds, pdfX, pdfY, 10f)
                else -> false
            }
            if (hit) {
                coordinator.selectElement(ann.id)
                return
            }
        }
        // NOTE tool: place a new sticky note at tap position
        if (_activeTool.value == MarkTool.NOTE) {
            placeStickyNote(pageIndex, pdfX, pdfY)
            return
        }
        coordinator.selectElement(null)
    }

    private fun containsWithTolerance(rect: android.graphics.RectF, x: Float, y: Float, tolerance: Float): Boolean {
        return x >= rect.left - tolerance && x <= rect.right + tolerance &&
               y >= rect.top - tolerance && y <= rect.bottom + tolerance
    }

    fun getAnnotationText(id: String): String {
        val ann = coordinator.annotations.value.find { it.id == id } ?: return ""
        val (pageIndex, quads) = when (ann) {
            is EditAnnotation.Highlight -> ann.pageIndex to ann.textQuads
            is EditAnnotation.Underline -> ann.pageIndex to ann.textQuads
            is EditAnnotation.Strikethrough -> ann.pageIndex to ann.textQuads
            else -> return ""
        }
        val chars = charsByPage[pageIndex] ?: return ""
        
        val builder = StringBuilder()
        var lastY = -1f
        for (c in chars) {
            val cx = c.x + c.width / 2f
            val cy = c.y - c.height / 2f
            if (quads.any { it.contains(cx, cy) }) {
                if (lastY != -1f && kotlin.math.abs(c.y - lastY) > c.height * 0.5f) {
                    builder.append(" ")
                }
                builder.append(c.char)
                lastY = c.y
            }
        }
        return builder.toString()
    }

    fun updateAnnotationNote(id: String, note: String?) {
        val ann = coordinator.annotations.value.find { it.id == id } ?: return
        val updated = when (ann) {
            is EditAnnotation.Highlight -> ann.copy(note = note)
            is EditAnnotation.Underline -> ann.copy(note = note)
            is EditAnnotation.Strikethrough -> ann.copy(note = note)
            is EditAnnotation.StickyNote -> ann.copy(text = note ?: "")
            else -> return
        }
        coordinator.updateAnnotation(updated)
    }

    /** Change color of selected annotation. */
    fun changeAnnotationColor(id: String, newColor: Int) {
        val ann = coordinator.annotations.value.find { it.id == id } ?: return
        val oldColor = when (ann) {
            is EditAnnotation.Highlight -> ann.color
            is EditAnnotation.Underline -> ann.color
            is EditAnnotation.Strikethrough -> ann.color
            is EditAnnotation.StickyNote -> ann.color
            else -> return
        }
        coordinator.undoRedoManager.pushAction(EditAction.ChangeColor(id, oldColor, newColor))
        coordinator.updateAnnotation(
            when (ann) {
                is EditAnnotation.Highlight -> ann.copy(color = newColor)
                is EditAnnotation.Underline -> ann.copy(color = newColor)
                is EditAnnotation.Strikethrough -> ann.copy(color = newColor)
                is EditAnnotation.StickyNote -> ann.copy(color = newColor)
                else -> return
            }
        )
    }

    /** Delete selected annotation. */
    fun deleteAnnotation(id: String) {
        coordinator.removeAnnotation(id)
        coordinator.selectElement(null)
    }

    fun setMarkColor(color: Int) { _markColor.value = color }
    fun setMarkOpacity(opacity: Float) { _markOpacity.value = opacity }
    fun clearSelection() {
        _selectionState.value = null
        _selectionPageIndex.value = -1
        _showApplyPopup.value = false
        _isDragging.value = false
    }
    fun dismissNoTextMessage() { _noTextAvailable.value = false }

    private fun createAnnotation(
        tool: MarkTool, pageIndex: Int, bounds: RectF, quads: List<RectF>
    ): EditAnnotation = when (tool) {
        MarkTool.HIGHLIGHT -> EditAnnotation.Highlight(
            pageIndex = pageIndex, bounds = bounds,
            textQuads = quads, color = _markColor.value, opacity = _markOpacity.value
        )
        MarkTool.UNDERLINE -> EditAnnotation.Underline(
            pageIndex = pageIndex, bounds = bounds,
            textQuads = quads, color = _markColor.value, opacity = _markOpacity.value
        )
        MarkTool.STRIKETHROUGH -> EditAnnotation.Strikethrough(
            pageIndex = pageIndex, bounds = bounds,
            textQuads = quads, color = _markColor.value, opacity = _markOpacity.value
        )
        MarkTool.NOTE -> error("Note tool uses placeStickyNote(), not createAnnotation()")
        MarkTool.NONE -> error("Cannot create annotation with NONE tool")
    }
}
