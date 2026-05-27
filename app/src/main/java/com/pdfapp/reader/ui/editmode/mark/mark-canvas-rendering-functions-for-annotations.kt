package com.pdfapp.reader.ui.editmode.mark

import android.graphics.Paint
import android.graphics.Path
import com.pdfapp.reader.domain.model.EditAnnotation

/** Draw committed mark annotations (highlights, underlines, strikethroughs). */
fun drawMarkAnnotations(
    canvas: android.graphics.Canvas,
    annotations: List<EditAnnotation>,
    highlightPaint: Paint,
    linePaint: Paint
) {
    annotations.forEach { ann ->
        when (ann) {
            is EditAnnotation.Highlight -> {
                highlightPaint.color = ann.color
                highlightPaint.alpha = (ann.opacity * 255).toInt()
                ann.textQuads.forEach { quad -> canvas.drawRect(quad, highlightPaint) }
            }
            is EditAnnotation.Underline -> {
                linePaint.color = ann.color
                linePaint.alpha = (ann.opacity * 255).toInt()
                linePaint.strokeWidth = 2f
                ann.textQuads.forEach { quad ->
                    canvas.drawLine(quad.left, quad.bottom, quad.right, quad.bottom, linePaint)
                }
            }
            is EditAnnotation.Strikethrough -> {
                linePaint.color = ann.color
                linePaint.alpha = (ann.opacity * 255).toInt()
                linePaint.strokeWidth = 2f
                ann.textQuads.forEach { quad ->
                    val centerY = quad.centerY()
                    canvas.drawLine(quad.left, centerY, quad.right, centerY, linePaint)
                }
            }
            is EditAnnotation.StickyNote -> {
                val b = ann.bounds
                // Filled note body
                highlightPaint.color = ann.color
                highlightPaint.alpha = 230
                highlightPaint.style = Paint.Style.FILL
                canvas.drawRect(b, highlightPaint)
                // Dark border
                linePaint.color = 0xFF000000.toInt()
                linePaint.alpha = 120
                linePaint.strokeWidth = 1.5f
                linePaint.style = Paint.Style.STROKE
                canvas.drawRect(b, linePaint)
                // Folded corner triangle (top-right)
                val foldSize = b.width() * 0.3f
                val foldPath = Path().apply {
                    moveTo(b.right - foldSize, b.top)
                    lineTo(b.right, b.top + foldSize)
                    lineTo(b.right, b.top)
                    close()
                }
                highlightPaint.color = 0x40000000
                highlightPaint.alpha = 60
                canvas.drawPath(foldPath, highlightPaint)
            }
            else -> {}
        }
    }
}

/** Draw active selection overlay (colored quads before annotation is committed). */
fun drawSelectionOverlay(
    canvas: android.graphics.Canvas,
    selection: SelectionResult,
    color: Int,
    paint: Paint
) {
    paint.color = color
    paint.alpha = 80
    paint.style = Paint.Style.FILL
    selection.quads.forEach { quad -> canvas.drawRect(quad, paint) }
}
