package com.pdfapp.reader.ui.editmode

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.pdfapp.reader.domain.model.AnnotateTool
import com.pdfapp.reader.domain.model.EditAnnotation
import android.graphics.RectF
import com.pdfapp.reader.ui.editmode.annotate.AnnotateCanvasRenderer
import com.pdfapp.reader.ui.editmode.annotate.AnnotateGroupSelectionOverlay
import com.pdfapp.reader.ui.editmode.annotate.AnnotateSelectionOverlay
import com.pdfapp.reader.ui.editmode.annotate.annotateDrawingGestures
import com.pdfapp.reader.ui.editmode.fillsign.FillSignCanvasRenderer
import com.pdfapp.reader.ui.editmode.fillsign.FillSignSelectionOverlay
import com.pdfapp.reader.ui.editmode.fillsign.FillSignTool
import com.pdfapp.reader.ui.editmode.mark.MarkSelectionApplyPopup
import com.pdfapp.reader.ui.editmode.mark.MarkSelectionHandles
import com.pdfapp.reader.ui.editmode.mark.MarkSelectionMagnifierOverlay
import com.pdfapp.reader.ui.editmode.mark.MarkAnnotationPopup
import com.pdfapp.reader.ui.editmode.mark.SelectionResult
import com.pdfapp.reader.ui.editmode.mark.drawMarkAnnotations
import com.pdfapp.reader.ui.editmode.mark.drawSelectionOverlay

/**
 * Renders a single PDF page with annotation layers in Edit Mode.
 *
 * Layer stack:
 * 1. PDF page bitmap
 * 2. Mark annotations (highlights, underlines, strikethroughs)
 * 3. Annotate elements (strokes, shapes) -- Phase 3
 * 4. Fill & Sign elements (signatures, images, dates) -- Phase 4
 * 5. Selection UI (handles, dashed borders)
 * 6. Active drawing preview -- Phase 3
 */
@Composable
fun EditablePdfPageView(
    pageBitmap: Bitmap?,
    pageIndex: Int,
    annotations: List<EditAnnotation>,
    selectedElementId: String?,
    pdfPageWidth: Float = 0f,
    pdfPageHeight: Float = 0f,
    selectionState: SelectionResult? = null,
    selectionColor: Int = 0xFFFFEB3B.toInt(),
    isDragging: Boolean = false,
    dragTouchPosition: Offset = Offset.Unspecified,
    onLongPress: ((pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit)? = null,
    onTap: ((pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit)? = null,
    onStartHandleDrag: ((deltaX: Float, deltaY: Float) -> Unit)? = null,
    onEndHandleDrag: ((deltaX: Float, deltaY: Float) -> Unit)? = null,
    onHandleDragStart: (() -> Unit)? = null,
    onHandleDragMove: ((Offset) -> Unit)? = null,
    onHandleDragEnd: (() -> Unit)? = null,
    onMarkPageDragStart: ((pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit)? = null,
    onMarkPageDragMove: ((pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit)? = null,
    onMarkPageDragEnd: ((pageIndex: Int) -> Unit)? = null,
    showApplyPopup: Boolean = false,
    activeToolName: String = "",
    activeToolColor: Int = 0,
    onApplyTool: (() -> Unit)? = null,
    onDismissPopup: (() -> Unit)? = null,
    onMarkColorChange: ((id: String, color: Int) -> Unit)? = null,
    onMarkCopy: ((id: String) -> Unit)? = null,
    onMarkNote: ((id: String) -> Unit)? = null,
    onMarkDelete: ((id: String) -> Unit)? = null,
    onMarkDismissPopup: (() -> Unit)? = null,
    // -- Annotate tab parameters --
    isAnnotateTabActive: Boolean = false,
    annotateActiveTool: AnnotateTool = AnnotateTool.NONE,
    currentDrawingPoints: List<PointF> = emptyList(),
    shapeStartPoint: PointF? = null,
    shapeCurrentPoint: PointF? = null,
    drawingStrokeColor: Int = 0xFF000000.toInt(),
    drawingStrokeWidth: Float = 3f,
    onAnnotateDrawStart: ((pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit)? = null,
    onAnnotateDrawMove: ((pdfX: Float, pdfY: Float) -> Unit)? = null,
    onAnnotateDrawEnd: (() -> Unit)? = null,
    onAnnotateTap: ((pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit)? = null,
    onAnnotateRegionSelectStart: ((pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit)? = null,
    onAnnotateRegionSelectMove: ((pdfX: Float, pdfY: Float) -> Unit)? = null,
    onAnnotateRegionSelectEnd: (() -> Unit)? = null,
    annotateRegionSelectRect: RectF? = null,
    selectedAnnotateAnnotation: EditAnnotation? = null,
    // -- Annotate group selection --
    selectedAnnotateGroup: List<EditAnnotation> = emptyList(),
    onAnnotateGroupMove: ((deltaX: Float, deltaY: Float) -> Unit)? = null,
    onAnnotateGroupMoveEnd: ((totalDeltaX: Float, totalDeltaY: Float) -> Unit)? = null,
    onAnnotateGroupDelete: (() -> Unit)? = null,
    onAnnotateMove: ((deltaX: Float, deltaY: Float) -> Unit)? = null,
    onAnnotateResize: ((android.graphics.RectF) -> Unit)? = null,
    onAnnotateDelete: (() -> Unit)? = null,
    // -- Fill & Sign tab parameters --
    isFillSignTabActive: Boolean = false,
    onFillSignTap: ((pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit)? = null,
    selectedFillSignAnnotation: EditAnnotation? = null,
    onFillSignMove: ((deltaX: Float, deltaY: Float) -> Unit)? = null,
    onFillSignResizeAndRotate: ((android.graphics.RectF, Float) -> Unit)? = null,
    onFillSignDelete: (() -> Unit)? = null,
    onFillSignTapBody: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val aspectRatio = when {
        pageBitmap != null -> pageBitmap.width.toFloat() / pageBitmap.height.toFloat()
        pdfPageWidth > 0f && pdfPageHeight > 0f -> pdfPageWidth / pdfPageHeight
        else -> 8.5f / 11f
    }
    val bitmapWidth = pageBitmap?.width?.toFloat() ?: 1f
    val bitmapHeight = pageBitmap?.height?.toFloat() ?: 1f
    // PDF point dimensions for coordinate conversion (CharInfo uses PDF points, not bitmap pixels)
    val pdfW = if (pdfPageWidth > 0f) pdfPageWidth else bitmapWidth
    val pdfH = if (pdfPageHeight > 0f) pdfPageHeight else bitmapHeight

    // Pre-allocate paint objects for mark annotations
    val highlightPaint = remember { Paint().apply { style = Paint.Style.FILL } }
    val linePaint = remember { Paint().apply { style = Paint.Style.STROKE; strokeWidth = 2f } }
    val selectionPaint = remember { Paint().apply { style = Paint.Style.FILL } }

    // Pre-allocate paint objects for annotate annotations
    val annotateStrokePaint = remember { Paint().apply { style = Paint.Style.STROKE; isAntiAlias = true } }
    val annotateFillPaint = remember { Paint().apply { style = Paint.Style.FILL; isAntiAlias = true } }
    val annotatePreviewPaint = remember { Paint().apply { isAntiAlias = true } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
    ) {
        val density = LocalDensity.current
        val boxWidthPx = with(density) { maxWidth.toPx() }
        val boxHeightPx = with(density) { maxHeight.toPx() }
        // Conditional gesture: Annotate uses drag for drawing, Fill & Sign + Mark use tap
        val gestureModifier = when {
            // When group is selected, overlay handles all gestures (move + tap-to-deselect)
            isAnnotateTabActive && selectedAnnotateGroup.isNotEmpty() -> Modifier
            isAnnotateTabActive -> Modifier.annotateDrawingGestures(
                enabled = true, pdfWidth = pdfW, pdfHeight = pdfH,
                pageIndex = pageIndex, activeTool = annotateActiveTool,
                onDrawStart = onAnnotateDrawStart ?: { _, _, _ -> },
                onDrawMove = onAnnotateDrawMove ?: { _, _ -> },
                onDrawEnd = onAnnotateDrawEnd ?: {},
                onTap = onAnnotateTap ?: { _, _, _ -> },
                onRegionSelectStart = onAnnotateRegionSelectStart ?: { _, _, _ -> },
                onRegionSelectMove = onAnnotateRegionSelectMove ?: { _, _ -> },
                onRegionSelectEnd = onAnnotateRegionSelectEnd ?: {}
            )
            isFillSignTabActive -> if (selectedFillSignAnnotation == null) {
                // Only detect page-level taps when no sticker is selected.
                // When a sticker IS selected, FillSignSelectionOverlay handles all gestures
                // (move, resize, rotate, delete, tap-outside-to-deselect, tap-body-to-re-edit).
                Modifier.pointerInput(pageIndex, pdfW, pdfH, onFillSignTap) {
                    detectTapGestures(
                        onTap = { offset ->
                            val px = offset.x / size.width * pdfW
                            val py = offset.y / size.height * pdfH
                            onFillSignTap?.invoke(pageIndex, px, py)
                        }
                    )
                }
            } else {
                Modifier // Overlay handles all interactions for selected stickers
            }
            else -> Modifier.markDragGestures(
                enabled = activeToolName.isNotEmpty(),
                pageIndex = pageIndex,
                pdfW = pdfW,
                pdfH = pdfH,
                onDragStart = onMarkPageDragStart,
                onDragMove = onMarkPageDragMove,
                onDragEnd = onMarkPageDragEnd,
                onLongPress = onLongPress,
                onTap = onTap
            )
        }

        Canvas(
            modifier = Modifier.matchParentSize().then(gestureModifier)
        ) {
            drawIntoCanvas { canvas ->
                val bmpScaleX = size.width / bitmapWidth
                val bmpScaleY = size.height / bitmapHeight
                // Annotation coords are in PDF points, not bitmap pixels
                val annScaleX = size.width / pdfW
                val annScaleY = size.height / pdfH

                // Layer 1: PDF page bitmap (bitmap pixel space)
                if (pageBitmap != null && !pageBitmap.isRecycled) {
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.scale(bmpScaleX, bmpScaleY)
                    canvas.nativeCanvas.drawBitmap(pageBitmap, 0f, 0f, null)
                    canvas.nativeCanvas.restore()
                }

                // Layer 2: Mark annotations (PDF point space)
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.scale(annScaleX, annScaleY)
                drawMarkAnnotations(canvas.nativeCanvas, annotations, highlightPaint, linePaint)
                canvas.nativeCanvas.restore()

                // Layer 2b: Active selection overlay (PDF point space)
                if (selectionState != null) {
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.scale(annScaleX, annScaleY)
                    drawSelectionOverlay(canvas.nativeCanvas, selectionState, selectionColor, selectionPaint)
                    canvas.nativeCanvas.restore()
                }

                // Layer 3: Annotate committed annotations (PDF point space)
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.scale(annScaleX, annScaleY)
                AnnotateCanvasRenderer.drawCommittedAnnotations(
                    canvas.nativeCanvas, annotations, annotateStrokePaint, annotateFillPaint
                )
                canvas.nativeCanvas.restore()

                // Layer 3b: Annotate in-progress preview (PDF point space)
                if (isAnnotateTabActive) {
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.scale(annScaleX, annScaleY)
                    if (currentDrawingPoints.isNotEmpty()) {
                        if (annotateActiveTool == AnnotateTool.POLYGON) {
                            AnnotateCanvasRenderer.drawPolygonPreview(
                                canvas.nativeCanvas, currentDrawingPoints,
                                drawingStrokeColor, drawingStrokeWidth, annotatePreviewPaint
                            )
                        } else {
                            AnnotateCanvasRenderer.drawFreehandPreview(
                                canvas.nativeCanvas, currentDrawingPoints,
                                drawingStrokeColor, drawingStrokeWidth, annotatePreviewPaint
                            )
                        }
                    }
                    if (shapeStartPoint != null && shapeCurrentPoint != null) {
                        AnnotateCanvasRenderer.drawShapePreview(
                            canvas.nativeCanvas, annotateActiveTool,
                            shapeStartPoint, shapeCurrentPoint,
                            drawingStrokeColor, drawingStrokeWidth, annotatePreviewPaint
                        )
                    }
                    canvas.nativeCanvas.restore()
                }

                // Layer 3c: Region selection rectangle preview (PDF point space)
                if (isAnnotateTabActive && annotateRegionSelectRect != null) {
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.scale(annScaleX, annScaleY)
                    val regionPaint = android.graphics.Paint().apply {
                        color = 0x304CAF50 // semi-transparent green fill
                        style = android.graphics.Paint.Style.FILL
                    }
                    canvas.nativeCanvas.drawRect(
                        annotateRegionSelectRect.left, annotateRegionSelectRect.top,
                        annotateRegionSelectRect.right, annotateRegionSelectRect.bottom,
                        regionPaint
                    )
                    regionPaint.apply {
                        color = 0xFF4CAF50.toInt()
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 2f
                        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 6f), 0f)
                    }
                    canvas.nativeCanvas.drawRect(
                        annotateRegionSelectRect.left, annotateRegionSelectRect.top,
                        annotateRegionSelectRect.right, annotateRegionSelectRect.bottom,
                        regionPaint
                    )
                    canvas.nativeCanvas.restore()
                }

                // Layer 4: Fill & Sign elements (PDF point space)
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.scale(annScaleX, annScaleY)
                FillSignCanvasRenderer.drawPlacedElements(canvas.nativeCanvas, annotations)
                canvas.nativeCanvas.restore()
            }
        }

        // Layer 5: Selection handles overlay (screen-space coords)
        // Convert drag deltas from screen pixels -> PDF coordinates before forwarding
        if (selectionState != null && onStartHandleDrag != null && onEndHandleDrag != null) {
            val sx = boxWidthPx / pdfW
            val sy = boxHeightPx / pdfH
            // Track accumulated screen positions for delta->absolute conversion
            var startScreenX by remember(selectionState) { mutableFloatStateOf(selectionState.startHandle.x * sx) }
            var startScreenY by remember(selectionState) { mutableFloatStateOf(selectionState.startHandle.y * sy) }
            var endScreenX by remember(selectionState) { mutableFloatStateOf(selectionState.endHandle.x * sx) }
            var endScreenY by remember(selectionState) { mutableFloatStateOf(selectionState.endHandle.y * sy) }

            MarkSelectionHandles(
                startX = selectionState.startHandle.x * sx,
                startY = selectionState.startHandle.y * sy,
                endX = selectionState.endHandle.x * sx,
                endY = selectionState.endHandle.y * sy,
                onStartHandleDrag = { dx, dy ->
                    startScreenX += dx; startScreenY += dy
                    onStartHandleDrag(startScreenX / sx, startScreenY / sy)
                },
                onEndHandleDrag = { dx, dy ->
                    endScreenX += dx; endScreenY += dy
                    onEndHandleDrag(endScreenX / sx, endScreenY / sy)
                },
                onDragStart = onHandleDragStart ?: {},
                onDragMove = onHandleDragMove ?: {},
                onDragEnd = onHandleDragEnd ?: {},
                modifier = Modifier.matchParentSize()
            )
        }

        // Layer 5b: Annotate selection handles (screen-space)
        if (isAnnotateTabActive && selectedAnnotateAnnotation != null
            && onAnnotateMove != null && onAnnotateResize != null && onAnnotateDelete != null
        ) {
            AnnotateSelectionOverlay(
                annotation = selectedAnnotateAnnotation,
                pdfToScreenX = boxWidthPx / pdfW,
                pdfToScreenY = boxHeightPx / pdfH,
                onMove = onAnnotateMove,
                onResize = onAnnotateResize,
                onDelete = onAnnotateDelete,
                modifier = Modifier.matchParentSize()
            )
        }

        // Layer 5b2: Annotate group selection overlay (screen-space)
        if (isAnnotateTabActive && selectedAnnotateGroup.isNotEmpty()
            && onAnnotateGroupMove != null && onAnnotateGroupMoveEnd != null && onAnnotateGroupDelete != null
        ) {
            AnnotateGroupSelectionOverlay(
                selectedAnnotations = selectedAnnotateGroup,
                pdfToScreenX = boxWidthPx / pdfW,
                pdfToScreenY = boxHeightPx / pdfH,
                onMove = onAnnotateGroupMove,
                onMoveEnd = onAnnotateGroupMoveEnd,
                onDelete = onAnnotateGroupDelete,
                onTapOutside = { onAnnotateTap?.invoke(pageIndex, -1f, -1f) },
                modifier = Modifier.matchParentSize()
            )
        }

        // Layer 5c: Fill & Sign selection overlay (screen-space)
        if (isFillSignTabActive && selectedFillSignAnnotation != null
            && onFillSignMove != null && onFillSignResizeAndRotate != null
            && onFillSignDelete != null
        ) {
            val sigRotation = when (selectedFillSignAnnotation) {
                is EditAnnotation.SignatureElement -> selectedFillSignAnnotation.rotation
                is EditAnnotation.ImageElement -> selectedFillSignAnnotation.rotation
                is EditAnnotation.TextElement -> selectedFillSignAnnotation.rotation
                else -> 0f
            }
            FillSignSelectionOverlay(
                annotation = selectedFillSignAnnotation,
                rotation = sigRotation,
                pdfToScreenX = boxWidthPx / pdfW,
                pdfToScreenY = boxHeightPx / pdfH,
                onMove = onFillSignMove,
                onResizeAndRotate = onFillSignResizeAndRotate,
                onDelete = onFillSignDelete,
                onTapOutside = {
                    // Deselect sticker when tapping outside its bounds
                    onFillSignTap?.invoke(pageIndex, -1f, -1f)
                },
                onTapBody = onFillSignTapBody,
                modifier = Modifier.matchParentSize()
            )
        }

        // Layer 6: Magnifier loupe (shown during handle drag)
        if (selectionState != null) {
            MarkSelectionMagnifierOverlay(
                isVisible = isDragging,
                touchPosition = dragTouchPosition,
                pageBitmap = pageBitmap,
                displayWidth = boxWidthPx,
                displayHeight = boxHeightPx,
                modifier = Modifier.matchParentSize()
            )
        }

        // Layer 7: Floating "Apply [Tool]" popup (shown after selection complete)
        if (showApplyPopup && selectionState != null && onApplyTool != null && onDismissPopup != null) {
            val sx = boxWidthPx / pdfW
            val sy = boxHeightPx / pdfH
            val bounds = selectionState.quads.let { quads ->
                val centerX = quads.map { (it.left + it.right) / 2f }.average().toFloat() * sx
                val topY = quads.minOf { it.top } * sy
                centerX to topY
            }
            MarkSelectionApplyPopup(
                toolName = activeToolName,
                screenX = bounds.first.toInt(),
                screenY = bounds.second.toInt(),
                onApply = onApplyTool,
                onDismiss = onDismissPopup
            )
        }

        // Layer 8: Tapped Mark Annotation Popup (Color, Copy, Note, Delete)
        if (!isAnnotateTabActive && !isFillSignTabActive) {
            val selAnn = annotations.find { it.id == selectedElementId }
            if (selAnn is EditAnnotation.Highlight || selAnn is EditAnnotation.Underline
                || selAnn is EditAnnotation.Strikethrough || selAnn is EditAnnotation.StickyNote
            ) {
                val sx = boxWidthPx / pdfW
                val sy = boxHeightPx / pdfH
                val bounds = selAnn.bounds
                val isStickyNote = selAnn is EditAnnotation.StickyNote
                MarkAnnotationPopup(
                    annotationColor = when (selAnn) {
                        is EditAnnotation.Highlight -> selAnn.color
                        is EditAnnotation.Underline -> selAnn.color
                        is EditAnnotation.Strikethrough -> selAnn.color
                        is EditAnnotation.StickyNote -> selAnn.color
                        else -> 0
                    },
                    screenX = ((bounds.left + bounds.right) / 2f * sx).toInt(),
                    screenY = (bounds.top * sy).toInt(),
                    onColorChange = { color -> onMarkColorChange?.invoke(selAnn.id, color) },
                    onCopy = if (!isStickyNote) { { onMarkCopy?.invoke(selAnn.id) } } else null,
                    onNote = { onMarkNote?.invoke(selAnn.id) },
                    onDelete = { onMarkDelete?.invoke(selAnn.id) },
                    onDismiss = { onMarkDismissPopup?.invoke() }
                )
            }
        }
    }
}

private fun Modifier.markDragGestures(
    enabled: Boolean,
    pageIndex: Int,
    pdfW: Float,
    pdfH: Float,
    onDragStart: ((pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit)?,
    onDragMove: ((pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit)?,
    onDragEnd: ((pageIndex: Int) -> Unit)?,
    onLongPress: ((pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit)?,
    onTap: ((pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit)?
): Modifier = this.pointerInput(enabled, pageIndex, pdfW, pdfH) {
    if (!enabled) {
        detectTapGestures(
            onLongPress = { offset ->
                val px = offset.x / size.width * pdfW
                val py = offset.y / size.height * pdfH
                onLongPress?.invoke(pageIndex, px, py)
            },
            onTap = { offset ->
                val px = offset.x / size.width * pdfW
                val py = offset.y / size.height * pdfH
                onTap?.invoke(pageIndex, px, py)
            }
        )
        return@pointerInput
    }

    awaitEachGesture {
        val down = awaitFirstDown()
        val initialPosition = down.position
        val drag = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
        
        if (drag != null) {
            val startPdfX = initialPosition.x / size.width * pdfW
            val startPdfY = initialPosition.y / size.height * pdfH
            onDragStart?.invoke(pageIndex, startPdfX, startPdfY)
            
            val movePdfX = drag.position.x / size.width * pdfW
            val movePdfY = drag.position.y / size.height * pdfH
            onDragMove?.invoke(pageIndex, movePdfX, movePdfY)
            
            drag(drag.id) { change ->
                change.consume()
                val pdfX = change.position.x / size.width * pdfW
                val pdfY = change.position.y / size.height * pdfH
                onDragMove?.invoke(pageIndex, pdfX, pdfY)
            }
            onDragEnd?.invoke(pageIndex)
        } else {
            val px = initialPosition.x / size.width * pdfW
            val py = initialPosition.y / size.height * pdfH
            onTap?.invoke(pageIndex, px, py)
        }
    }
}

