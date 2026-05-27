package com.pdfapp.reader.ui.viewer.state

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import com.pdfapp.reader.ui.viewer.render.PdfRenderConfig

/**
 * Compose state holder for zoom + pan gestures on a PDF page.
 *
 * Manages scale, offset, pinch-to-zoom, double-tap toggle,
 * and viewport bound clamping.
 */
class ZoomPanState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var isZooming by mutableStateOf(false)
        private set

    /** Container size in pixels — set by the composable measuring its bounds. */
    var containerSize by mutableStateOf(Size.Zero)

    /** Updates scale and offset from a transform gesture. */
    fun onTransform(centroid: Offset, pan: Offset, zoom: Float) {
        val newScale = (scale * zoom).coerceIn(PdfRenderConfig.MIN_ZOOM, PdfRenderConfig.MAX_ZOOM)
        val newOffset = offset + pan + (centroid - offset) * (1 - zoom)
        scale = newScale
        offset = clampOffset(newOffset, newScale)
        isZooming = newScale != 1f
    }

    /** Double-tap toggles between 1x and [PdfRenderConfig.DOUBLE_TAP_ZOOM]. */
    fun onDoubleTap(tapPosition: Offset) {
        if (scale > 1.01f) {
            scale = 1f
            offset = Offset.Zero
            isZooming = false
        } else {
            val targetScale = PdfRenderConfig.DOUBLE_TAP_ZOOM
            val newOffset = tapPosition * (1 - targetScale)
            scale = targetScale
            offset = clampOffset(newOffset, targetScale)
            isZooming = true
        }
    }

    /** Resets zoom and pan to default. */
    fun reset() {
        scale = 1f
        offset = Offset.Zero
        isZooming = false
    }

    private fun clampOffset(proposedOffset: Offset, currentScale: Float): Offset {
        if (containerSize == Size.Zero) return proposedOffset
        val maxX = (containerSize.width * (currentScale - 1)) / 2
        val maxY = (containerSize.height * (currentScale - 1)) / 2
        return Offset(
            proposedOffset.x.coerceIn(-maxX, maxX),
            proposedOffset.y.coerceIn(-maxY, maxY)
        )
    }
}

@Composable
fun rememberZoomPanState(): ZoomPanState = remember { ZoomPanState() }

/**
 * Applies pinch-to-zoom, pan, and double-tap gestures using [state].
 */
fun Modifier.zoomPanGesture(state: ZoomPanState): Modifier = this
    .pointerInput(Unit) {
        detectTransformGestures { centroid, pan, zoom, _ ->
            state.onTransform(centroid, pan, zoom)
        }
    }
    .pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = { state.onDoubleTap(it) }
        )
    }
