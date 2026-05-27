package com.pdfapp.reader.ui.editmode.annotate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
 * Overlay for a group of selected annotate annotations.
 * Shows a combined dashed bounding box, supports drag-to-move and delete.
 */
@Composable
fun AnnotateGroupSelectionOverlay(
    selectedAnnotations: List<EditAnnotation>,
    pdfToScreenX: Float,
    pdfToScreenY: Float,
    onMove: (deltaX: Float, deltaY: Float) -> Unit,
    onMoveEnd: (totalDeltaX: Float, totalDeltaY: Float) -> Unit,
    onDelete: () -> Unit,
    onTapOutside: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (selectedAnnotations.isEmpty()) return

    val density = LocalDensity.current

    // Compute combined screen-space bounds directly (cheap math, no allocation)
    val sLeft = selectedAnnotations.minOf { it.bounds.left } * pdfToScreenX
    val sTop = selectedAnnotations.minOf { it.bounds.top } * pdfToScreenY
    val sRight = selectedAnnotations.maxOf { it.bounds.right } * pdfToScreenX
    val sBottom = selectedAnnotations.maxOf { it.bounds.bottom } * pdfToScreenY
    val sMidX = (sLeft + sRight) / 2f

    // Track total drag distance for undo action
    var totalDx by remember { mutableFloatStateOf(0f) }
    var totalDy by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(selectedAnnotations.map { it.id }) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val initial = down.position
                        // Check bounds at drag-start time (not composition time)
                        val curLeft = selectedAnnotations.minOf { it.bounds.left } * pdfToScreenX
                        val curTop = selectedAnnotations.minOf { it.bounds.top } * pdfToScreenY
                        val curRight = selectedAnnotations.maxOf { it.bounds.right } * pdfToScreenX
                        val curBottom = selectedAnnotations.maxOf { it.bounds.bottom } * pdfToScreenY
                        val insideBounds = initial.x in curLeft..curRight && initial.y in curTop..curBottom

                        val dragChange = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                            change.consume()
                        }

                        if (dragChange != null && insideBounds) {
                            // Drag inside bounds → move group
                            totalDx = 0f; totalDy = 0f
                            val dx0 = (dragChange.position.x - initial.x) / pdfToScreenX
                            val dy0 = (dragChange.position.y - initial.y) / pdfToScreenY
                            totalDx += dx0; totalDy += dy0
                            onMove(dx0, dy0)

                            drag(dragChange.id) { change ->
                                change.consume()
                                val dx = (change.position.x - change.previousPosition.x) / pdfToScreenX
                                val dy = (change.position.y - change.previousPosition.y) / pdfToScreenY
                                totalDx += dx; totalDy += dy
                                onMove(dx, dy)
                            }
                            if (totalDx != 0f || totalDy != 0f) onMoveEnd(totalDx, totalDy)
                            totalDx = 0f; totalDy = 0f
                        } else if (dragChange == null) {
                            // Tap — deselect if outside bounds
                            if (!insideBounds) onTapOutside()
                        }
                    }
                }
        ) {
            // Dashed bounding box (green to distinguish from single-select blue)
            drawRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(sLeft, sTop),
                size = Size(sRight - sLeft, sBottom - sTop),
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                )
            )
            // Semi-transparent fill to indicate selection area
            drawRect(
                color = Color(0x154CAF50),
                topLeft = Offset(sLeft, sTop),
                size = Size(sRight - sLeft, sBottom - sTop)
            )
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
