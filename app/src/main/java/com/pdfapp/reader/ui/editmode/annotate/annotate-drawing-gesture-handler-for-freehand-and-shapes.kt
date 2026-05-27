package com.pdfapp.reader.ui.editmode.annotate

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.pdfapp.reader.domain.model.AnnotateTool

/**
 * Gesture handler modifier for the Annotate tab.
 * - Tool active: drag gestures for drawing strokes/shapes.
 * - Tool NONE: tap for single selection, drag for region multi-select.
 */
fun Modifier.annotateDrawingGestures(
    enabled: Boolean,
    pdfWidth: Float,
    pdfHeight: Float,
    pageIndex: Int,
    activeTool: AnnotateTool,
    onDrawStart: (pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit,
    onDrawMove: (pdfX: Float, pdfY: Float) -> Unit,
    onDrawEnd: () -> Unit,
    onTap: (pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit,
    onRegionSelectStart: (pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit = { _, _, _ -> },
    onRegionSelectMove: (pdfX: Float, pdfY: Float) -> Unit = { _, _ -> },
    onRegionSelectEnd: () -> Unit = {}
): Modifier = this.pointerInput(enabled, pageIndex, pdfWidth, pdfHeight, activeTool) {
    if (!enabled) return@pointerInput
    // Both modes use awaitEachGesture to distinguish tap vs drag
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        val initialPosition = down.position

        val drag = awaitTouchSlopOrCancellation(down.id) { change, _ ->
            change.consume()
        }

        val isSelectMode = activeTool == AnnotateTool.NONE || activeTool == AnnotateTool.SELECT

        if (drag != null) {
            if (!isSelectMode) {
                // Drawing mode: forward to draw callbacks
                val startPdfX = initialPosition.x / size.width * pdfWidth
                val startPdfY = initialPosition.y / size.height * pdfHeight
                onDrawStart(pageIndex, startPdfX, startPdfY)

                val movePdfX = drag.position.x / size.width * pdfWidth
                val movePdfY = drag.position.y / size.height * pdfHeight
                onDrawMove(movePdfX, movePdfY)

                drag(drag.id) { change ->
                    change.consume()
                    val pdfX = change.position.x / size.width * pdfWidth
                    val pdfY = change.position.y / size.height * pdfHeight
                    onDrawMove(pdfX, pdfY)
                }
                onDrawEnd()
            } else {
                // No tool: drag = region select
                val startPdfX = initialPosition.x / size.width * pdfWidth
                val startPdfY = initialPosition.y / size.height * pdfHeight
                onRegionSelectStart(pageIndex, startPdfX, startPdfY)

                val movePdfX = drag.position.x / size.width * pdfWidth
                val movePdfY = drag.position.y / size.height * pdfHeight
                onRegionSelectMove(movePdfX, movePdfY)

                drag(drag.id) { change ->
                    change.consume()
                    val pdfX = change.position.x / size.width * pdfWidth
                    val pdfY = change.position.y / size.height * pdfHeight
                    onRegionSelectMove(pdfX, pdfY)
                }
                onRegionSelectEnd()
            }
        } else {
            // Tap (no drag detected)
            val pdfX = initialPosition.x / size.width * pdfWidth
            val pdfY = initialPosition.y / size.height * pdfHeight
            onTap(pageIndex, pdfX, pdfY)
        }
    }
}
