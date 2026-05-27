package com.pdfapp.reader.ui.editmode.mark

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Circular magnifier overlay shown while dragging selection handles in edit mode.
 * Crops and zooms the page bitmap at 1.5x around the touch point.
 * Ported from viewer's SelectionMagnifierOverlay with identical behavior.
 *
 * @param isVisible Whether the magnifier should be shown
 * @param touchPosition Screen-space touch coordinates (relative to page)
 * @param pageBitmap Current page bitmap to sample from
 * @param displayWidth Page display width in pixels
 * @param displayHeight Page display height in pixels
 */
@Composable
fun MarkSelectionMagnifierOverlay(
    isVisible: Boolean,
    touchPosition: Offset,
    pageBitmap: Bitmap?,
    displayWidth: Float,
    displayHeight: Float,
    modifier: Modifier = Modifier
) {
    if (!isVisible || pageBitmap == null || touchPosition == Offset.Unspecified) return
    if (displayWidth <= 0f || displayHeight <= 0f) return

    val density = LocalDensity.current
    val diameterPx = with(density) { 100.dp.toPx() }
    val offsetYPx = with(density) { 60.dp.toPx() }
    val borderPx = with(density) { 2.dp.toPx() }
    val zoom = 1.5f
    val radius = diameterPx / 2f

    // Cache ImageBitmap to avoid per-frame allocation
    val imageBitmap = remember(pageBitmap) { pageBitmap.asImageBitmap() }

    // Position magnifier above touch, or below if near top
    val centerX = touchPosition.x.coerceIn(radius, displayWidth - radius)
    val aboveY = touchPosition.y - offsetYPx - radius
    val centerY = if (aboveY - radius > 0f) aboveY else touchPosition.y + offsetYPx + radius

    Canvas(modifier = modifier.fillMaxSize()) {
        // Map touch position to bitmap coordinates
        val bitmapX = (touchPosition.x / displayWidth * pageBitmap.width).toInt()
        val bitmapY = (touchPosition.y / displayHeight * pageBitmap.height).toInt()
        val cropRadiusBitmap = (radius / zoom / displayWidth * pageBitmap.width).toInt()

        if (cropRadiusBitmap <= 0) return@Canvas

        // Clamp crop rect to bitmap bounds
        val cropLeft = (bitmapX - cropRadiusBitmap).coerceAtLeast(0)
        val cropTop = (bitmapY - cropRadiusBitmap).coerceAtLeast(0)
        val cropRight = (bitmapX + cropRadiusBitmap).coerceAtMost(pageBitmap.width)
        val cropBottom = (bitmapY + cropRadiusBitmap).coerceAtMost(pageBitmap.height)

        val cropW = cropRight - cropLeft
        val cropH = cropBottom - cropTop
        if (cropW <= 0 || cropH <= 0) return@Canvas

        // Clip to circle and draw zoomed bitmap
        val circlePath = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    centerX - radius, centerY - radius,
                    centerX + radius, centerY + radius
                )
            )
        }

        // Draw shadow behind magnifier
        drawCircle(
            color = Color.Black.copy(alpha = 0.15f),
            radius = radius + borderPx,
            center = Offset(centerX + 2f, centerY + 2f),
            style = Stroke(width = borderPx * 2)
        )

        clipPath(circlePath) {
            drawImage(
                image = imageBitmap,
                srcOffset = IntOffset(cropLeft, cropTop),
                srcSize = IntSize(cropW, cropH),
                dstOffset = IntOffset(
                    (centerX - radius).toInt(),
                    (centerY - radius).toInt()
                ),
                dstSize = IntSize(diameterPx.toInt(), diameterPx.toInt())
            )
        }

        // Draw border
        drawCircle(
            color = Color.Gray.copy(alpha = 0.6f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = borderPx)
        )
    }
}
