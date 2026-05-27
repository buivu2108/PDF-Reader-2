package com.pdfapp.reader.ui.editmode.annotate

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.model.EditAnnotation

/**
 * Overlay for selected annotate annotations: dashed bounding box,
 * 8 resize handles, and floating delete button.
 */
@Composable
fun AnnotateSelectionOverlay(
    annotation: EditAnnotation,
    pdfToScreenX: Float,
    pdfToScreenY: Float,
    onMove: (deltaX: Float, deltaY: Float) -> Unit,
    onResize: (newBounds: RectF) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val handleRadiusDp = 5.dp
    val handleRadiusPx = with(density) { handleRadiusDp.toPx() }
    val handleTouchTargetPx = with(density) { 24.dp.toPx() }
    val bounds = annotation.bounds

    // Screen-space bounds
    val sLeft = bounds.left * pdfToScreenX
    val sTop = bounds.top * pdfToScreenY
    val sRight = bounds.right * pdfToScreenX
    val sBottom = bounds.bottom * pdfToScreenY
    val sMidX = (sLeft + sRight) / 2f
    val sMidY = (sTop + sBottom) / 2f

    val handlePositions = remember(sLeft, sTop, sRight, sBottom) {
        listOf(
            "TL" to Offset(sLeft, sTop), "TC" to Offset(sMidX, sTop),
            "TR" to Offset(sRight, sTop), "ML" to Offset(sLeft, sMidY),
            "MR" to Offset(sRight, sMidY), "BL" to Offset(sLeft, sBottom),
            "BC" to Offset(sMidX, sBottom), "BR" to Offset(sRight, sBottom)
        )
    }

    var draggingHandle by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier) {
        // Dashed bounding box + handle circles + body drag
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(annotation.id, pdfToScreenX, pdfToScreenY) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Check if drag started on a handle
                            draggingHandle = handlePositions
                                .firstOrNull { (_, pos) ->
                                    (offset - pos).getDistance() < handleTouchTargetPx
                                }?.first
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val handle = draggingHandle
                            if (handle != null) {
                                // Resize via handle
                                val dx = dragAmount.x / pdfToScreenX
                                val dy = dragAmount.y / pdfToScreenY
                                val b = annotation.bounds
                                val newBounds = computeResizedBounds(handle, b, dx, dy)
                                onResize(newBounds)
                            } else if (isInsideBounds(change.position, sLeft, sTop, sRight, sBottom)) {
                                // Move body
                                val dx = dragAmount.x / pdfToScreenX
                                val dy = dragAmount.y / pdfToScreenY
                                onMove(dx, dy)
                            }
                        },
                        onDragEnd = { draggingHandle = null },
                        onDragCancel = { draggingHandle = null }
                    )
                }
        ) {
            // Dashed bounding box
            drawRect(
                color = Color(0xFF1976D2),
                topLeft = Offset(sLeft, sTop),
                size = androidx.compose.ui.geometry.Size(sRight - sLeft, sBottom - sTop),
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )
            )
            // 8 handle circles
            handlePositions.forEach { (_, pos) ->
                drawCircle(Color.White, radius = handleRadiusPx, center = pos)
                drawCircle(
                    Color(0xFF1976D2), radius = handleRadiusPx, center = pos,
                    style = Stroke(width = 2f)
                )
            }
        }

        // Delete button floating above top-center
        val deleteButtonSize = 32.dp
        val deleteOffsetX = with(density) { sMidX.toDp() - deleteButtonSize / 2 }
        val deleteOffsetY = with(density) { sTop.toDp() - deleteButtonSize - 8.dp }
        if (deleteOffsetY.value >= 0f) {
            FilledIconButton(
                onClick = onDelete,
                modifier = Modifier
                    .offset { IntOffset(deleteOffsetX.roundToPx(), deleteOffsetY.roundToPx()) }
                    .size(deleteButtonSize),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun computeResizedBounds(handle: String, b: RectF, dx: Float, dy: Float): RectF {
    val minSize = 5f
    var left = b.left; var top = b.top; var right = b.right; var bottom = b.bottom
    when (handle) {
        "TL" -> { left += dx; top += dy }
        "TC" -> { top += dy }
        "TR" -> { right += dx; top += dy }
        "ML" -> { left += dx }
        "MR" -> { right += dx }
        "BL" -> { left += dx; bottom += dy }
        "BC" -> { bottom += dy }
        "BR" -> { right += dx; bottom += dy }
    }
    // Enforce minimum size
    if (right - left < minSize) { if (dx > 0) left = right - minSize else right = left + minSize }
    if (bottom - top < minSize) { if (dy > 0) top = bottom - minSize else bottom = top + minSize }
    return RectF(left, top, right, bottom)
}

private fun isInsideBounds(pos: Offset, l: Float, t: Float, r: Float, b: Float): Boolean =
    pos.x in l..r && pos.y in t..b
