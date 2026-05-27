package com.pdfapp.reader.ui.editmode.annotate

import android.graphics.PointF
import android.graphics.RectF
import com.pdfapp.reader.domain.model.AnnotateTool
import com.pdfapp.reader.domain.model.EditAction
import com.pdfapp.reader.domain.model.EditAnnotation
import com.pdfapp.reader.ui.editmode.EditModeCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Annotate tab. Manages freehand drawing, shape creation,
 * tool selection, and annotation selection/editing.
 */
class AnnotateTabViewModel(
    private val coordinator: EditModeCoordinator
) {
    // -- Tool state --
    private val _activeTool = MutableStateFlow(AnnotateTool.NONE)
    val activeTool: StateFlow<AnnotateTool> = _activeTool.asStateFlow()

    // -- Drawing state --
    private val _isDrawing = MutableStateFlow(false)
    val isDrawing: StateFlow<Boolean> = _isDrawing.asStateFlow()

    private val _currentPoints = MutableStateFlow<List<PointF>>(emptyList())
    val currentPoints: StateFlow<List<PointF>> = _currentPoints.asStateFlow()

    private val _shapeStartPoint = MutableStateFlow<PointF?>(null)
    val shapeStartPoint: StateFlow<PointF?> = _shapeStartPoint.asStateFlow()

    private val _shapeCurrentPoint = MutableStateFlow<PointF?>(null)
    val shapeCurrentPoint: StateFlow<PointF?> = _shapeCurrentPoint.asStateFlow()

    // -- Tool settings --
    private val _strokeColor = MutableStateFlow(0xFFF44336.toInt())
    val strokeColor: StateFlow<Int> = _strokeColor.asStateFlow()

    private val _strokeWidth = MutableStateFlow(3f)
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _fillEnabled = MutableStateFlow(false)
    val fillEnabled: StateFlow<Boolean> = _fillEnabled.asStateFlow()

    private val _fillColor = MutableStateFlow(0xFF2196F3.toInt())
    val fillColor: StateFlow<Int> = _fillColor.asStateFlow()

    private val _fillOpacity = MutableStateFlow(0.3f)
    val fillOpacity: StateFlow<Float> = _fillOpacity.asStateFlow()

    // -- Single selection state --
    private val _selectedAnnotationId = MutableStateFlow<String?>(null)
    val selectedAnnotationId: StateFlow<String?> = _selectedAnnotationId.asStateFlow()

    // -- Multi-select (region drag) state --
    private val _selectedAnnotationIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedAnnotationIds: StateFlow<Set<String>> = _selectedAnnotationIds.asStateFlow()

    /** Live selection rectangle during drag (PDF coords). Null when not dragging. */
    private val _regionSelectRect = MutableStateFlow<RectF?>(null)
    val regionSelectRect: StateFlow<RectF?> = _regionSelectRect.asStateFlow()

    private var _regionSelectStart: PointF? = null

    private var _drawingPageIndex: Int = 0

    // -- Tool selection --

    fun selectTool(tool: AnnotateTool) {
        _activeTool.value = if (_activeTool.value == tool) AnnotateTool.NONE else tool
        clearDrawingState()
        _selectedAnnotationId.value = null
        _selectedAnnotationIds.value = emptySet()
        _regionSelectRect.value = null
        coordinator.selectElement(null)
    }

    // -- Drawing lifecycle --

    fun onDrawStart(pageIndex: Int, pdfX: Float, pdfY: Float) {
        if (_activeTool.value == AnnotateTool.NONE || _activeTool.value == AnnotateTool.SELECT) return
        _isDrawing.value = true
        _drawingPageIndex = pageIndex
        _selectedAnnotationId.value = null
        coordinator.selectElement(null)
        when (_activeTool.value) {
            AnnotateTool.DRAW, AnnotateTool.POLYGON -> _currentPoints.value = listOf(PointF(pdfX, pdfY))
            AnnotateTool.ERASER -> eraseAtPoint(pdfX, pdfY) // Erase immediately, no visual trail
            AnnotateTool.CIRCLE, AnnotateTool.RECTANGLE, AnnotateTool.LINE,
            AnnotateTool.ARROW, AnnotateTool.ZIGZAG, AnnotateTool.TRIANGLE -> {
                _shapeStartPoint.value = PointF(pdfX, pdfY)
                _shapeCurrentPoint.value = PointF(pdfX, pdfY)
            }
            else -> {}
        }
    }

    fun onDrawMove(pdfX: Float, pdfY: Float) {
        if (!_isDrawing.value) return
        when (_activeTool.value) {
            AnnotateTool.DRAW, AnnotateTool.POLYGON -> _currentPoints.value = _currentPoints.value + PointF(pdfX, pdfY)
            AnnotateTool.ERASER -> eraseAtPoint(pdfX, pdfY) // Just erase, no line trail
            AnnotateTool.CIRCLE, AnnotateTool.RECTANGLE, AnnotateTool.LINE,
            AnnotateTool.ARROW, AnnotateTool.ZIGZAG, AnnotateTool.TRIANGLE -> {
                _shapeCurrentPoint.value = PointF(pdfX, pdfY)
            }
            else -> {}
        }
    }

    fun onDrawEnd() {
        if (!_isDrawing.value) return
        _isDrawing.value = false
        commitAnnotation()
    }

    // -- Annotation commit --

    private fun commitAnnotation() {
        val tool = _activeTool.value
        val pageIndex = _drawingPageIndex
        when (tool) {
            AnnotateTool.DRAW -> {
                val points = _currentPoints.value
                if (points.size < 2) { clearDrawingState(); return }
                val bounds = computeBoundsFromPoints(points)
                coordinator.addAnnotation(
                    EditAnnotation.FreehandStroke(
                        pageIndex = pageIndex, bounds = bounds, points = points,
                        color = _strokeColor.value, strokeWidth = _strokeWidth.value
                    )
                )
            }
            AnnotateTool.POLYGON -> {
                val points = _currentPoints.value
                if (points.size < 3) { clearDrawingState(); return }
                val bounds = computeBoundsFromPoints(points)
                coordinator.addAnnotation(
                    EditAnnotation.PolygonShape(
                        pageIndex = pageIndex, bounds = bounds, points = points,
                        color = _strokeColor.value, strokeWidth = _strokeWidth.value,
                        fillColor = if (_fillEnabled.value) _fillColor.value else null,
                        fillOpacity = _fillOpacity.value
                    )
                )
            }
            AnnotateTool.CIRCLE -> commitShape(pageIndex) { bounds ->
                EditAnnotation.OvalShape(
                    pageIndex = pageIndex, bounds = bounds,
                    color = _strokeColor.value, strokeWidth = _strokeWidth.value,
                    fillColor = if (_fillEnabled.value) _fillColor.value else null,
                    fillOpacity = _fillOpacity.value
                )
            }
            AnnotateTool.RECTANGLE -> commitShape(pageIndex) { bounds ->
                EditAnnotation.RectShape(
                    pageIndex = pageIndex, bounds = bounds,
                    color = _strokeColor.value, strokeWidth = _strokeWidth.value,
                    fillColor = if (_fillEnabled.value) _fillColor.value else null,
                    fillOpacity = _fillOpacity.value
                )
            }
            AnnotateTool.LINE -> commitTwoPointShape(pageIndex) { start, end, bounds ->
                EditAnnotation.LineShape(
                    pageIndex = pageIndex, bounds = bounds, startPoint = start, endPoint = end,
                    color = _strokeColor.value, strokeWidth = _strokeWidth.value
                )
            }
            AnnotateTool.ARROW -> commitTwoPointShape(pageIndex) { start, end, bounds ->
                EditAnnotation.ArrowShape(
                    pageIndex = pageIndex, bounds = bounds, startPoint = start, endPoint = end,
                    color = _strokeColor.value, strokeWidth = _strokeWidth.value
                )
            }
            AnnotateTool.ZIGZAG -> commitTwoPointShape(pageIndex) { start, end, bounds ->
                EditAnnotation.ZigzagShape(
                    pageIndex = pageIndex, bounds = bounds, startPoint = start, endPoint = end,
                    color = _strokeColor.value, strokeWidth = _strokeWidth.value
                )
            }
            AnnotateTool.TRIANGLE -> commitShape(pageIndex) { bounds ->
                EditAnnotation.TriangleShape(
                    pageIndex = pageIndex, bounds = bounds,
                    color = _strokeColor.value, strokeWidth = _strokeWidth.value,
                    fillColor = if (_fillEnabled.value) _fillColor.value else null,
                    fillOpacity = _fillOpacity.value
                )
            }
            AnnotateTool.ERASER -> {} // Eraser removes during drag, nothing to commit
            else -> {}
        }
        clearDrawingState()
    }

    private inline fun commitShape(pageIndex: Int, create: (RectF) -> EditAnnotation) {
        val start = _shapeStartPoint.value ?: return
        val end = _shapeCurrentPoint.value ?: return
        val bounds = rectFromTwoPoints(start, end)
        if (bounds.width() < 5f && bounds.height() < 5f) return
        coordinator.addAnnotation(create(bounds))
    }

    /** Commit shape that stores original start/end points (Line, Arrow, Zigzag). */
    private inline fun commitTwoPointShape(pageIndex: Int, create: (PointF, PointF, RectF) -> EditAnnotation) {
        val start = _shapeStartPoint.value ?: return
        val end = _shapeCurrentPoint.value ?: return
        val bounds = rectFromTwoPoints(start, end)
        val dx = end.x - start.x; val dy = end.y - start.y
        if (dx * dx + dy * dy < 25f) return // min 5px distance
        coordinator.addAnnotation(create(start, end, bounds))
    }

    /** Eraser: remove any annotate annotation under the touch point. */
    private fun eraseAtPoint(pdfX: Float, pdfY: Float) {
        val pageAnns = coordinator.getAnnotationsForPage(_drawingPageIndex).filter { isAnnotateType(it) }
        val hit = pageAnns.reversed().firstOrNull { hitTest(it, pdfX, pdfY) }
        if (hit != null) coordinator.removeAnnotation(hit.id)
    }

    // -- Selection & editing --

    fun onAnnotationTap(pageIndex: Int, pdfX: Float, pdfY: Float): Boolean {
        // Clear group selection on any tap
        _selectedAnnotationIds.value = emptySet()
        // Eraser: tap to erase single annotation
        if (_activeTool.value == AnnotateTool.ERASER) {
            val anns = coordinator.getAnnotationsForPage(pageIndex).filter { isAnnotateType(it) }
            val hit = anns.reversed().firstOrNull { hitTest(it, pdfX, pdfY) }
            if (hit != null) { coordinator.removeAnnotation(hit.id); return true }
            return false
        }
        val annotations = coordinator.getAnnotationsForPage(pageIndex).filter { isAnnotateType(it) }
        val hit = annotations.reversed().firstOrNull { hitTest(it, pdfX, pdfY) }
        _selectedAnnotationId.value = hit?.id
        coordinator.selectElement(hit?.id)
        return hit != null
    }

    fun moveAnnotation(id: String, deltaX: Float, deltaY: Float) {
        val ann = coordinator.annotations.value.find { it.id == id } ?: return
        val oldBounds = RectF(ann.bounds)
        val newBounds = RectF(ann.bounds).apply { offset(deltaX, deltaY) }
        val updated = moveAnnotationImpl(ann, newBounds, deltaX, deltaY) ?: return
        coordinator.updateAnnotation(updated)
        coordinator.undoRedoManager.pushAction(EditAction.Move(id, oldBounds, newBounds))
    }

    private fun moveAnnotationImpl(ann: EditAnnotation, newBounds: RectF, dx: Float, dy: Float): EditAnnotation? {
        return when (ann) {
            is EditAnnotation.FreehandStroke -> ann.copy(bounds = newBounds, points = ann.points.map { PointF(it.x + dx, it.y + dy) })
            is EditAnnotation.PolygonShape -> ann.copy(bounds = newBounds, points = ann.points.map { PointF(it.x + dx, it.y + dy) })
            is EditAnnotation.OvalShape -> ann.copy(bounds = newBounds)
            is EditAnnotation.RectShape -> ann.copy(bounds = newBounds)
            is EditAnnotation.LineShape -> ann.copy(bounds = newBounds, startPoint = PointF(ann.startPoint.x + dx, ann.startPoint.y + dy), endPoint = PointF(ann.endPoint.x + dx, ann.endPoint.y + dy))
            is EditAnnotation.ArrowShape -> ann.copy(bounds = newBounds, startPoint = PointF(ann.startPoint.x + dx, ann.startPoint.y + dy), endPoint = PointF(ann.endPoint.x + dx, ann.endPoint.y + dy))
            is EditAnnotation.ZigzagShape -> ann.copy(bounds = newBounds, startPoint = PointF(ann.startPoint.x + dx, ann.startPoint.y + dy), endPoint = PointF(ann.endPoint.x + dx, ann.endPoint.y + dy))
            is EditAnnotation.TriangleShape -> ann.copy(bounds = newBounds)
            else -> null
        }
    }

    fun resizeAnnotation(id: String, newBounds: RectF) {
        val ann = coordinator.annotations.value.find { it.id == id } ?: return
        val oldBounds = RectF(ann.bounds)
        val sx = newBounds.width() / oldBounds.width().coerceAtLeast(1f)
        val sy = newBounds.height() / oldBounds.height().coerceAtLeast(1f)
        fun scalePoint(pt: PointF) = PointF(newBounds.left + (pt.x - oldBounds.left) * sx, newBounds.top + (pt.y - oldBounds.top) * sy)
        val updated = when (ann) {
            is EditAnnotation.FreehandStroke -> ann.copy(bounds = newBounds, points = ann.points.map { scalePoint(it) })
            is EditAnnotation.PolygonShape -> ann.copy(bounds = newBounds, points = ann.points.map { scalePoint(it) })
            is EditAnnotation.OvalShape -> ann.copy(bounds = newBounds)
            is EditAnnotation.RectShape -> ann.copy(bounds = newBounds)
            is EditAnnotation.LineShape -> ann.copy(bounds = newBounds, startPoint = scalePoint(ann.startPoint), endPoint = scalePoint(ann.endPoint))
            is EditAnnotation.ArrowShape -> ann.copy(bounds = newBounds, startPoint = scalePoint(ann.startPoint), endPoint = scalePoint(ann.endPoint))
            is EditAnnotation.ZigzagShape -> ann.copy(bounds = newBounds, startPoint = scalePoint(ann.startPoint), endPoint = scalePoint(ann.endPoint))
            is EditAnnotation.TriangleShape -> ann.copy(bounds = newBounds)
            else -> return
        }
        coordinator.updateAnnotation(updated)
        coordinator.undoRedoManager.pushAction(EditAction.Resize(id, oldBounds, newBounds))
    }

    fun deleteSelectedAnnotation() {
        val id = _selectedAnnotationId.value ?: return
        coordinator.removeAnnotation(id)
        _selectedAnnotationId.value = null
    }

    // -- Region selection (multi-select by dragging a rectangle) --

    fun onRegionSelectStart(pageIndex: Int, pdfX: Float, pdfY: Float) {
        _selectedAnnotationId.value = null
        _selectedAnnotationIds.value = emptySet()
        coordinator.selectElement(null)
        _drawingPageIndex = pageIndex
        _regionSelectStart = PointF(pdfX, pdfY)
        _regionSelectRect.value = RectF(pdfX, pdfY, pdfX, pdfY)
    }

    fun onRegionSelectMove(pdfX: Float, pdfY: Float) {
        val start = _regionSelectStart ?: return
        _regionSelectRect.value = rectFromTwoPoints(start, PointF(pdfX, pdfY))
    }

    fun onRegionSelectEnd() {
        val rect = _regionSelectRect.value
        _regionSelectRect.value = null
        _regionSelectStart = null
        if (rect == null || (rect.width() < 5f && rect.height() < 5f)) return

        // Find all annotate-type annotations on the page that intersect the selection rect
        val pageAnnotations = coordinator.getAnnotationsForPage(_drawingPageIndex).filter { isAnnotateType(it) }
        val hitIds = pageAnnotations.filter { RectF.intersects(it.bounds, rect) }.map { it.id }.toSet()
        _selectedAnnotationIds.value = hitIds
    }

    /** Move all selected annotations by delta. Single StateFlow emission for performance. */
    fun moveSelectedGroup(deltaX: Float, deltaY: Float) {
        val ids = _selectedAnnotationIds.value
        if (ids.isEmpty()) return
        val updatedMap = mutableMapOf<String, EditAnnotation>()
        for (ann in coordinator.annotations.value) {
            if (ann.id !in ids) continue
            val newBounds = RectF(ann.bounds).apply { offset(deltaX, deltaY) }
            val moved = moveAnnotationImpl(ann, newBounds, deltaX, deltaY) ?: continue
            updatedMap[ann.id] = moved
        }
        if (updatedMap.isNotEmpty()) coordinator.updateAnnotationsBatch(updatedMap)
    }

    /** Push a single GroupMove undo action after drag ends. */
    fun commitGroupMove(totalDeltaX: Float, totalDeltaY: Float) {
        val ids = _selectedAnnotationIds.value.toList()
        if (ids.isEmpty() || (totalDeltaX == 0f && totalDeltaY == 0f)) return
        coordinator.undoRedoManager.pushAction(EditAction.GroupMove(ids, totalDeltaX, totalDeltaY))
    }

    fun deleteSelectedGroup() {
        val ids = _selectedAnnotationIds.value
        for (id in ids) coordinator.removeAnnotation(id)
        _selectedAnnotationIds.value = emptySet()
    }

    fun clearGroupSelection() {
        _selectedAnnotationIds.value = emptySet()
        _regionSelectRect.value = null
        _regionSelectStart = null
    }

    // -- Settings setters --
    fun setStrokeColor(color: Int) { _strokeColor.value = color }
    fun setStrokeWidth(width: Float) { _strokeWidth.value = width }
    fun setFillEnabled(enabled: Boolean) { _fillEnabled.value = enabled }
    fun setFillColor(color: Int) { _fillColor.value = color }
    fun setFillOpacity(opacity: Float) { _fillOpacity.value = opacity }

    // -- Utilities --

    private fun hitTest(annotation: EditAnnotation, pdfX: Float, pdfY: Float): Boolean {
        val threshold = 15f
        return when (annotation) {
            is EditAnnotation.OvalShape, is EditAnnotation.RectShape, is EditAnnotation.TriangleShape -> {
                RectF(annotation.bounds).apply { inset(-threshold, -threshold) }.contains(pdfX, pdfY)
            }
            is EditAnnotation.FreehandStroke, is EditAnnotation.PolygonShape -> {
                val pts = when (annotation) {
                    is EditAnnotation.FreehandStroke -> annotation.points
                    is EditAnnotation.PolygonShape -> annotation.points
                    else -> emptyList()
                }
                pts.any { pt -> distSq(pt.x, pt.y, pdfX, pdfY) < threshold * threshold }
            }
            is EditAnnotation.LineShape -> pointToSegmentDistSq(pdfX, pdfY, annotation.startPoint, annotation.endPoint) < threshold * threshold
            is EditAnnotation.ArrowShape -> pointToSegmentDistSq(pdfX, pdfY, annotation.startPoint, annotation.endPoint) < threshold * threshold
            is EditAnnotation.ZigzagShape -> RectF(annotation.bounds).apply { inset(-threshold, -threshold) }.contains(pdfX, pdfY)
            else -> false
        }
    }

    /** Check if annotation is an annotate-tab type (not Mark or FillSign). */
    private fun isAnnotateType(ann: EditAnnotation): Boolean = ann is EditAnnotation.FreehandStroke ||
        ann is EditAnnotation.OvalShape || ann is EditAnnotation.RectShape ||
        ann is EditAnnotation.LineShape || ann is EditAnnotation.ArrowShape ||
        ann is EditAnnotation.ZigzagShape || ann is EditAnnotation.TriangleShape ||
        ann is EditAnnotation.PolygonShape

    private fun distSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2; val dy = y1 - y2; return dx * dx + dy * dy
    }

    /** Squared distance from point (px,py) to line segment (a→b). */
    private fun pointToSegmentDistSq(px: Float, py: Float, a: PointF, b: PointF): Float {
        val abx = b.x - a.x; val aby = b.y - a.y
        val lenSq = abx * abx + aby * aby
        if (lenSq < 0.001f) return distSq(px, py, a.x, a.y)
        val t = ((px - a.x) * abx + (py - a.y) * aby) / lenSq
        val ct = t.coerceIn(0f, 1f)
        return distSq(px, py, a.x + ct * abx, a.y + ct * aby)
    }

    private fun clearDrawingState() {
        _currentPoints.value = emptyList()
        _shapeStartPoint.value = null
        _shapeCurrentPoint.value = null
    }

    private fun computeBoundsFromPoints(points: List<PointF>): RectF = RectF(
        points.minOf { it.x }, points.minOf { it.y },
        points.maxOf { it.x }, points.maxOf { it.y }
    )

    private fun rectFromTwoPoints(a: PointF, b: PointF): RectF =
        RectF(minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y))
}
