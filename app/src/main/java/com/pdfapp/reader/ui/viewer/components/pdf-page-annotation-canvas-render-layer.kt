package com.pdfapp.reader.ui.viewer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.pdfapp.reader.domain.model.AnnotationType
import com.pdfapp.reader.domain.model.QuickAnnotation

/**
 * Compose Canvas layer dedicated to rendering persistent annotations
 * (highlights, underlines, strikethroughs) on a PDF page.
 *
 * Separated from active selection highlights so each layer has a single
 * responsibility and recomposes independently.
 *
 * Coordinates arrive in PDF space; scaling uses Canvas/PDF dimension ratio.
 */
@Composable
fun AnnotationRenderLayer(
    annotations: List<QuickAnnotation>,
    pageIndex: Int,
    pdfPageWidth: Float,
    pdfPageHeight: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (pdfPageWidth <= 0f || pdfPageHeight <= 0f) return@Canvas
        val scaleX = size.width / pdfPageWidth
        val scaleY = size.height / pdfPageHeight

        annotations.filter { it.pageIndex == pageIndex }.forEach { annotation ->
            annotation.lineBounds.forEach { pdfRect ->
                val left = pdfRect.left * scaleX
                val top = pdfRect.top * scaleY
                val width = (pdfRect.right - pdfRect.left) * scaleX
                val height = (pdfRect.bottom - pdfRect.top) * scaleY

                when (annotation.type) {
                    AnnotationType.HIGHLIGHT -> {
                        drawRect(
                            color = Color(annotation.color),
                            topLeft = Offset(left, top),
                            size = Size(width, height)
                        )
                    }
                    AnnotationType.UNDERLINE -> {
                        val strokeW = (height * 0.1f).coerceAtLeast(2f)
                        drawLine(
                            color = Color(annotation.color or (0xFF shl 24)),
                            start = Offset(left, top + height),
                            end = Offset(left + width, top + height),
                            strokeWidth = strokeW
                        )
                    }
                    AnnotationType.STRIKETHROUGH -> {
                        val strokeW = (height * 0.1f).coerceAtLeast(2f)
                        val centerY = top + height / 2
                        drawLine(
                            color = Color(annotation.color or (0xFF shl 24)),
                            start = Offset(left, centerY),
                            end = Offset(left + width, centerY),
                            strokeWidth = strokeW
                        )
                    }
                }
            }
        }
    }
}
