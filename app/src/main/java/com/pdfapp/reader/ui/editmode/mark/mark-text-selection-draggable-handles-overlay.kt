package com.pdfapp.reader.ui.editmode.mark

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Google blue handle color matching Android native selection. */
private val HandleColor = Color(0xFF4285F4)

private val HANDLE_SIZE = 24.dp
private val TOUCH_TARGET = 48.dp

/**
 * Overlay composable rendering two teardrop selection handles for Mark tab.
 * Matches Android native text selection handle appearance.
 */
@Composable
fun MarkSelectionHandles(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    onStartHandleDrag: (deltaX: Float, deltaY: Float) -> Unit,
    onEndHandleDrag: (deltaX: Float, deltaY: Float) -> Unit,
    onDragStart: () -> Unit = {},
    onDragMove: (Offset) -> Unit = {},
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val handleSizePx = with(density) { HANDLE_SIZE.toPx() }
    val touchTargetPx = with(density) { TOUCH_TARGET.toPx() }
    val halfTouch = touchTargetPx / 2f

    Box(modifier = modifier) {
        // Start handle (teardrop points to top-right, circle goes bottom-left)
        TeardropHandle(
            anchorX = startX,
            anchorY = startY,
            handleSizePx = handleSizePx,
            isStartHandle = true,
            touchOffset = IntOffset(
                (startX - halfTouch).roundToInt(),
                startY.roundToInt()
            ),
            onDrag = onStartHandleDrag,
            onDragStart = onDragStart,
            onDragMove = onDragMove,
            onDragEnd = onDragEnd
        )
        // End handle (teardrop points to top-left, circle goes bottom-right)
        TeardropHandle(
            anchorX = endX,
            anchorY = endY,
            handleSizePx = handleSizePx,
            isStartHandle = false,
            touchOffset = IntOffset(
                (endX - halfTouch).roundToInt(),
                endY.roundToInt()
            ),
            onDrag = onEndHandleDrag,
            onDragStart = onDragStart,
            onDragMove = onDragMove,
            onDragEnd = onDragEnd
        )
    }
}

@Composable
private fun TeardropHandle(
    anchorX: Float,
    anchorY: Float,
    handleSizePx: Float,
    isStartHandle: Boolean,
    touchOffset: IntOffset,
    onDrag: (Float, Float) -> Unit,
    onDragStart: () -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset { touchOffset }
            .size(TOUCH_TARGET)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onDragStart()
                        onDragMove(
                            Offset(
                                touchOffset.x.toFloat() + offset.x,
                                touchOffset.y.toFloat() + offset.y
                            )
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                        onDragMove(
                            Offset(
                                touchOffset.x.toFloat() + change.position.x,
                                touchOffset.y.toFloat() + change.position.y
                            )
                        )
                    },
                    onDragEnd = onDragEnd
                )
            }
    ) {
        Canvas(modifier = Modifier.size(TOUCH_TARGET)) {
            val localAnchorX = size.width / 2f
            val localAnchorY = 0f
            drawTeardropHandle(localAnchorX, localAnchorY, handleSizePx, isStartHandle)
        }
    }
}

/**
 * Draw teardrop handle matching Android's native CursorController shape.
 * Ported from viewer's SelectionHandleLayer.
 */
private fun DrawScope.drawTeardropHandle(
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
