package com.pdfapp.reader.ui.viewer.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Google blue handle color matching Android native selection. */
private val HandleColor = Color(0xFF4285F4)

/**
 * Renders two teardrop selection handles at the start and end of the selection.
 *
 * Handle positions are computed from selectionLineBounds (PDF coords) and
 * scaled to display space. Start handle sits at bottom-left of the first line,
 * end handle at bottom-right of the last line.
 *
 * @param selectionLineBounds per-line selection bounds in PDF coordinates
 * @param pdfPageWidth PDF page width in points
 * @param pdfPageHeight PDF page height in points
 */
@Composable
fun SelectionHandleLayer(
    selectionLineBounds: List<RectF>,
    pdfPageWidth: Float,
    pdfPageHeight: Float,
    modifier: Modifier = Modifier
) {
    if (selectionLineBounds.isEmpty()) return

    val density = LocalDensity.current
    val handleSizePx = with(density) { 24.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        val firstLine = selectionLineBounds.first()
        val lastLine = selectionLineBounds.last()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = size.width / pdfPageWidth
            val scaleY = size.height / pdfPageHeight

            val startX = firstLine.left * scaleX
            val startY = firstLine.bottom * scaleY
            val endX = lastLine.right * scaleX
            val endY = lastLine.bottom * scaleY

            drawTeardropHandle(startX, startY, handleSizePx, isStartHandle = true)
            drawTeardropHandle(endX, endY, handleSizePx, isStartHandle = false)
        }
    }
}

/**
 * Compute screen positions of handles for gesture hit-testing.
 * Returns Pair(startHandleScreen, endHandleScreen) or null if no selection.
 */
fun computeHandleScreenPositions(
    selectionLineBounds: List<RectF>,
    pdfPageWidth: Float,
    pdfPageHeight: Float,
    displayWidth: Float,
    displayHeight: Float
): Pair<Offset, Offset>? {
    if (selectionLineBounds.isEmpty()) return null
    val scaleX = displayWidth / pdfPageWidth
    val scaleY = displayHeight / pdfPageHeight
    val firstLine = selectionLineBounds.first()
    val lastLine = selectionLineBounds.last()
    return Pair(
        Offset(firstLine.left * scaleX, firstLine.bottom * scaleY),
        Offset(lastLine.right * scaleX, lastLine.bottom * scaleY)
    )
}

/**
 * Draw teardrop handle at the given screen position.
 * Matches Android's native CursorController handle shape.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTeardropHandle(
    anchorX: Float,
    anchorY: Float,
    handleSize: Float,
    isStartHandle: Boolean
) {
    val r = handleSize / 2f
    val tipH = r * 0.7f

    val cx = if (isStartHandle) anchorX - r else anchorX + r
    val cy = anchorY + tipH + r

    // Filled circle body
    drawCircle(color = HandleColor, radius = r, center = Offset(cx, cy))

    // Triangular tip connecting anchor point to circle
    val tipPath = Path().apply {
        if (isStartHandle) {
            moveTo(anchorX, anchorY)
            lineTo(cx - r * 0.3f, cy - r * 0.85f)
            lineTo(cx + r * 0.85f, cy - r * 0.3f)
            close()
        } else {
            moveTo(anchorX, anchorY)
            lineTo(cx + r * 0.3f, cy - r * 0.85f)
            lineTo(cx - r * 0.85f, cy - r * 0.3f)
            close()
        }
    }
    drawPath(tipPath, HandleColor, style = Fill)
}
