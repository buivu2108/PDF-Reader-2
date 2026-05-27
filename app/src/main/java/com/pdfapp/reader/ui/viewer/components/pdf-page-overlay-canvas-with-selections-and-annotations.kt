package com.pdfapp.reader.ui.viewer.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/** Semi-transparent pink for active selection highlights. */
private val SelectionHighlightColor = Color(0x60E91E63)

/**
 * Compose Canvas layer that draws per-line active selection highlight rectangles.
 *
 * Persistent annotations are rendered by [AnnotationRenderLayer] on a separate layer.
 *
 * Coordinates arrive in PDF space; scaling to display space uses the ratio
 * of the Canvas size to the PDF page dimensions.
 */
@Composable
fun SelectionHighlightLayer(
    selectionLineBounds: List<RectF>,
    pdfPageWidth: Float,
    pdfPageHeight: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (pdfPageWidth <= 0f || pdfPageHeight <= 0f) return@Canvas
        val scaleX = size.width / pdfPageWidth
        val scaleY = size.height / pdfPageHeight

        selectionLineBounds.forEach { pdfRect ->
            val left = pdfRect.left * scaleX
            val top = pdfRect.top * scaleY
            val width = (pdfRect.right - pdfRect.left) * scaleX
            val height = (pdfRect.bottom - pdfRect.top) * scaleY
            drawRect(
                color = SelectionHighlightColor,
                topLeft = Offset(left, top),
                size = Size(width, height)
            )
        }
    }
}
