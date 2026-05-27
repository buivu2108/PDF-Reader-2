package com.pdfapp.reader.ui.editmode.fillsign

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.model.EditAnnotation

private val STICKER_RED = Color(0xFFE53935)

/** Padding between sticker content and dashed selection border (in dp). */
private val SELECTION_PADDING_DP = 5.dp

/** Button diameter for delete and resize handles. */
private val HANDLE_SIZE_DP = 26.dp

/**
 * Selection overlay for placed Fill & Sign stickers.
 * 2 handles: X delete (top-left), resize+rotate (bottom-right).
 * Drag body to move. Drag bottom-right handle to resize. Rotation via atan2 on handle drag.
 *
 * Touch handling: resize handle has its own pointerInput for direct drag capture,
 * ensuring accurate touch detection regardless of touch slop offset.
 */
@Composable
fun FillSignSelectionOverlay(
    annotation: EditAnnotation,
    rotation: Float = 0f,
    pdfToScreenX: Float,
    pdfToScreenY: Float,
    onMove: (deltaX: Float, deltaY: Float) -> Unit,
    onResizeAndRotate: (newBounds: RectF, newRotation: Float) -> Unit,
    onDelete: () -> Unit,
    onTapOutside: () -> Unit,
    onTapBody: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val handleSizePx = with(density) { HANDLE_SIZE_DP.toPx() }
    val handleTouchPx = with(density) { 48.dp.toPx() } // generous touch target for fallback
    val paddingPx = with(density) { SELECTION_PADDING_DP.toPx() }

    // Dash pattern in pixels (6dp dash, 4dp gap)
    val dashOnPx = with(density) { 6.dp.toPx() }
    val dashOffPx = with(density) { 4.dp.toPx() }
    val borderWidthPx = with(density) { 1.5.dp.toPx() }

    // Fresh values for the pointerInput coroutine (avoids stale closure captures)
    val latestAnnotation by rememberUpdatedState(annotation)
    val latestRotation by rememberUpdatedState(rotation)
    val latestScaleX by rememberUpdatedState(pdfToScreenX)
    val latestScaleY by rememberUpdatedState(pdfToScreenY)
    val latestOnMove by rememberUpdatedState(onMove)
    val latestOnResizeAndRotate by rememberUpdatedState(onResizeAndRotate)
    val latestOnTapOutside by rememberUpdatedState(onTapOutside)
    val latestOnTapBody by rememberUpdatedState(onTapBody)

    // Screen-space bounds for sticker content (recomputed each recomposition)
    val bounds = annotation.bounds
    val sLeft = bounds.left * pdfToScreenX
    val sTop = bounds.top * pdfToScreenY
    val sRight = bounds.right * pdfToScreenX
    val sBottom = bounds.bottom * pdfToScreenY
    val sCenterX = (sLeft + sRight) / 2f
    val sCenterY = (sTop + sBottom) / 2f

    // Padded border corners (expanded outward from sticker content)
    val bLeft = sLeft - paddingPx
    val bTop = sTop - paddingPx
    val bRight = sRight + paddingPx
    val bBottom = sBottom + paddingPx

    var draggingHandle by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier) {
        // Canvas: dashed border + drag gestures for body move only
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    // Tap outside sticker body and handles → deselect
                    detectTapGestures { offset ->
                        val ann = latestAnnotation
                        val rot = latestRotation
                        val scX = latestScaleX; val scY = latestScaleY
                        val b = ann.bounds
                        val sl = b.left * scX; val st = b.top * scY
                        val sr = b.right * scX; val sb = b.bottom * scY
                        val cx = (sl + sr) / 2f; val cy = (st + sb) / 2f
                        val pad = paddingPx

                        // Exclude handle areas from "tap outside" detection
                        val brCorner = Offset(sr + pad, sb + pad)
                        val rotatedBR = rotatePoint(brCorner, Offset(cx, cy), rot)
                        if ((offset - rotatedBR).getDistance() < handleTouchPx) return@detectTapGestures

                        val tlCorner = Offset(sl - pad, st - pad)
                        val rotatedTL = rotatePoint(tlCorner, Offset(cx, cy), rot)
                        if ((offset - rotatedTL).getDistance() < handleTouchPx) return@detectTapGestures

                        val local = rotatePoint(offset, Offset(cx, cy), -rot)
                        if (local.x !in sl..sr || local.y !in st..sb) {
                            latestOnTapOutside()
                        } else {
                            latestOnTapBody?.invoke()
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val ann = latestAnnotation
                            val rot = latestRotation
                            val scX = latestScaleX
                            val scY = latestScaleY
                            val pad = paddingPx
                            val b = ann.bounds
                            val sl = b.left * scX; val st = b.top * scY
                            val sr = b.right * scX; val sb = b.bottom * scY
                            val cx = (sl + sr) / 2f; val cy = (st + sb) / 2f

                            // Bottom-right corner for resize/rotate handle (fallback detection)
                            val brCorner = Offset(sr + pad, sb + pad)
                            val rotatedBR = rotatePoint(brCorner, Offset(cx, cy), rot)
                            if ((offset - rotatedBR).getDistance() < handleTouchPx) {
                                draggingHandle = "EXPAND"
                                return@detectDragGestures
                            }

                            // Check body (un-rotate touch to test axis-aligned bounds)
                            val local = rotatePoint(offset, Offset(cx, cy), -rot)
                            if (local.x in sl..sr && local.y in st..sb) {
                                draggingHandle = "BODY"
                            } else {
                                draggingHandle = null
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val scX = latestScaleX
                            val scY = latestScaleY
                            when (draggingHandle) {
                                "BODY" -> {
                                    latestOnMove(dragAmount.x / scX, dragAmount.y / scY)
                                }
                                "EXPAND" -> {
                                    handleResizeAndRotate(
                                        change.position,
                                        latestAnnotation, latestRotation,
                                        latestScaleX, latestScaleY, paddingPx,
                                        latestOnResizeAndRotate
                                    )
                                }
                                null -> {
                                    // Drag started outside, re-check body
                                    val ann = latestAnnotation
                                    val rot = latestRotation
                                    val b = ann.bounds
                                    val sl = b.left * scX; val st = b.top * scY
                                    val sr = b.right * scX; val sb = b.bottom * scY
                                    val cx = (sl + sr) / 2f; val cy = (st + sb) / 2f
                                    val local = rotatePoint(change.position, Offset(cx, cy), -rot)
                                    if (local.x in sl..sr && local.y in st..sb) {
                                        draggingHandle = "BODY"
                                        latestOnMove(dragAmount.x / scX, dragAmount.y / scY)
                                    }
                                }
                            }
                        },
                        onDragEnd = { draggingHandle = null },
                        onDragCancel = { draggingHandle = null }
                    )
                }
        ) {
            rotate(degrees = rotation, pivot = Offset(sCenterX, sCenterY)) {
                drawRect(
                    color = STICKER_RED,
                    topLeft = Offset(bLeft, bTop),
                    size = androidx.compose.ui.geometry.Size(bRight - bLeft, bBottom - bTop),
                    style = Stroke(
                        width = borderWidthPx,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOnPx, dashOffPx))
                    )
                )
            }
        }

        // X delete button — top-left corner
        val tlRotated = rotatePoint(Offset(bLeft, bTop), Offset(sCenterX, sCenterY), rotation)
        val deleteX = with(density) { (tlRotated.x - handleSizePx / 2).toDp() }
        val deleteY = with(density) { (tlRotated.y - handleSizePx / 2).toDp() }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(deleteX.roundToPx(), deleteY.roundToPx()) }
                .size(HANDLE_SIZE_DP)
                .background(STICKER_RED, CircleShape)
                .clickable { onDelete() }
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_delete),
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }

        // Resize handle — bottom-right corner. Has own drag handler for direct touch capture.
        val brRotated = rotatePoint(Offset(bRight, bBottom), Offset(sCenterX, sCenterY), rotation)
        val expandX = with(density) { (brRotated.x - handleSizePx / 2).toDp() }
        val expandY = with(density) { (brRotated.y - handleSizePx / 2).toDp() }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(expandX.roundToPx(), expandY.roundToPx()) }
                .size(HANDLE_SIZE_DP)
                .background(STICKER_RED, CircleShape)
                .pointerInput(Unit) {
                    // Direct drag detection on the handle — no hit-test math needed
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            // Compute handle page position dynamically from latest state
                            val ann = latestAnnotation
                            val rot = latestRotation
                            val scX = latestScaleX; val scY = latestScaleY
                            val b = ann.bounds
                            val sr = b.right * scX; val sb = b.bottom * scY
                            val cx = (b.left * scX + sr) / 2f
                            val cy = (b.top * scY + sb) / 2f
                            val brPage = Offset(sr + paddingPx, sb + paddingPx)
                            val rotatedBR = rotatePoint(brPage, Offset(cx, cy), rot)
                            val handleLeftPx = rotatedBR.x - handleSizePx / 2
                            val handleTopPx = rotatedBR.y - handleSizePx / 2
                            val pageTouchPos = Offset(
                                handleLeftPx + change.position.x,
                                handleTopPx + change.position.y
                            )
                            handleResizeAndRotate(
                                pageTouchPos,
                                ann, rot, scX, scY, paddingPx,
                                latestOnResizeAndRotate
                            )
                        }
                    )
                }
        ) {
            Icon(
                Icons.Default.OpenInFull,
                contentDescription = stringResource(R.string.fillsign_resize),
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

/**
 * Center-anchored resize + rotation: sticker expands/contracts from center.
 * Keeps rotation pivot (center) stable — prevents visual jumping on rotated stickers.
 * Resize and rotation are emitted as a single atomic callback.
 */
private fun handleResizeAndRotate(
    pageTouchPos: Offset,
    annotation: EditAnnotation,
    rotation: Float,
    scaleX: Float,
    scaleY: Float,
    paddingPx: Float,
    onResizeAndRotate: (RectF, Float) -> Unit
) {
    val b = annotation.bounds
    val sl = b.left * scaleX; val st = b.top * scaleY
    val sr = b.right * scaleX; val sb = b.bottom * scaleY
    val cx = (sl + sr) / 2f; val cy = (st + sb) / 2f

    // Un-rotate finger position to axis-aligned space, then convert to PDF coords.
    // Subtract padding because handle is at padded border, not content edge.
    val local = rotatePoint(pageTouchPos, Offset(cx, cy), -rotation)
    val targetRight = (local.x - paddingPx) / scaleX
    val targetBottom = (local.y - paddingPx) / scaleY

    // Center-anchored: compute half-size from center to finger position
    val minHalfSize = 20f
    val maxHalfSize = 500f // Cap maximum size to prevent excessive growth during rotation
    val w = b.width(); val h = b.height()
    if (w <= 0f || h <= 0f) return
    val aspectRatio = w / h
    val centerPdfX = b.centerX()
    val centerPdfY = b.centerY()

    val rawHalfW = (targetRight - centerPdfX).coerceIn(minHalfSize, maxHalfSize)
    val rawHalfH = (targetBottom - centerPdfY).coerceIn(minHalfSize, maxHalfSize)
    // Aspect-locked: pick dominant axis, maintain aspect ratio
    var newHalfW: Float
    var newHalfH: Float
    if (rawHalfW / aspectRatio >= rawHalfH) {
        newHalfW = rawHalfW
        newHalfH = newHalfW / aspectRatio
    } else {
        newHalfH = rawHalfH
        newHalfW = newHalfH * aspectRatio
    }
    // Ensure neither dimension exceeds max (high aspect ratios can amplify one axis)
    if (newHalfW > maxHalfSize) { newHalfW = maxHalfSize; newHalfH = newHalfW / aspectRatio }
    if (newHalfH > maxHalfSize) { newHalfH = maxHalfSize; newHalfW = newHalfH * aspectRatio }
    val newBounds = RectF(
        centerPdfX - newHalfW, centerPdfY - newHalfH,
        centerPdfX + newHalfW, centerPdfY + newHalfH
    )

    // Rotation from absolute touch angle relative to sticker center
    val angle = Math.toDegrees(
        kotlin.math.atan2(
            (pageTouchPos.y - cy).toDouble(),
            (pageTouchPos.x - cx).toDouble()
        )
    ).toFloat() - 45f // offset: BR handle is at ~45 degrees

    onResizeAndRotate(newBounds, angle)
}

/** Rotate a point around a pivot by angle degrees. */
private fun rotatePoint(point: Offset, pivot: Offset, angleDeg: Float): Offset {
    val rad = Math.toRadians(angleDeg.toDouble())
    val cos = kotlin.math.cos(rad).toFloat()
    val sin = kotlin.math.sin(rad).toFloat()
    val dx = point.x - pivot.x
    val dy = point.y - pivot.y
    return Offset(
        pivot.x + dx * cos - dy * sin,
        pivot.y + dx * sin + dy * cos
    )
}
