package com.pdfapp.reader.ui.editmode.fillsign

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import com.pdfapp.reader.domain.model.EditAnnotation
import java.io.File

/**
 * Renders placed Fill & Sign elements (signatures, images) onto a native Android Canvas.
 * Uses LruCache to avoid redundant bitmap file I/O.
 */
object FillSignCanvasRenderer {

    /** 10 MB bitmap cache for loaded signature/image files. */
    private val bitmapCache = object : LruCache<String, Bitmap>(10 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val drawPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    // Cached paint objects to avoid per-frame allocation during drag/resize
    private val textPaint = TextPaint().apply { isAntiAlias = true }
    private val decorationPaint = Paint().apply {
        strokeWidth = 1.5f; style = Paint.Style.STROKE
    }

    /**
     * Draw all Fill & Sign annotations (SignatureElement, ImageElement) onto the canvas.
     * Canvas should already be scaled to PDF coordinate space.
     */
    fun drawPlacedElements(canvas: Canvas, annotations: List<EditAnnotation>) {
        annotations.forEach { ann ->
            when (ann) {
                is EditAnnotation.SignatureElement -> drawSignature(canvas, ann)
                is EditAnnotation.ImageElement -> drawImage(canvas, ann)
                is EditAnnotation.TextElement -> drawTextElement(canvas, ann)
                else -> { /* Skip non-fillsign types */ }
            }
        }
    }

    private fun drawSignature(canvas: Canvas, sig: EditAnnotation.SignatureElement) {
        val bitmap = loadBitmap(sig.imagePath) ?: return

        canvas.save()
        if (sig.rotation != 0f) {
            canvas.rotate(sig.rotation, sig.bounds.centerX(), sig.bounds.centerY())
        }
        canvas.drawBitmap(bitmap, null, sig.bounds, drawPaint)
        canvas.restore()
    }

    private fun drawImage(canvas: Canvas, img: EditAnnotation.ImageElement) {
        val bitmap = loadBitmap(img.imagePath) ?: return

        canvas.save()
        if (img.rotation != 0f) {
            canvas.rotate(img.rotation, img.bounds.centerX(), img.bounds.centerY())
        }
        canvas.drawBitmap(bitmap, null, img.bounds, drawPaint)
        canvas.restore()
    }

    /** Render a TextElement with full formatting (bold/italic/underline/strikethrough). */
    private fun drawTextElement(canvas: Canvas, element: EditAnnotation.TextElement) {
        if (element.text.isEmpty()) return
        val boundsW = element.bounds.width()
        if (boundsW <= 0f || element.bounds.height() <= 0f) return

        // Reuse cached paint objects — update properties per element
        textPaint.color = element.color
        textPaint.textSize = element.fontSize
        textPaint.typeface = resolveTypeface(element.fontFamily, element.isBold, element.isItalic)

        val width = boundsW.toInt().coerceAtLeast(1)
        val layout = StaticLayout.Builder
            .obtain(element.text, 0, element.text.length, textPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .build()

        canvas.save()
        if (element.rotation != 0f) {
            canvas.rotate(element.rotation, element.bounds.centerX(), element.bounds.centerY())
        }
        canvas.translate(element.bounds.left, element.bounds.top)
        layout.draw(canvas)

        // Draw underline/strikethrough decorations using cached paint
        if (element.isUnderline || element.isStrikethrough) {
            decorationPaint.color = element.color
            for (i in 0 until layout.lineCount) {
                val lineLeft = layout.getLineLeft(i)
                val lineRight = layout.getLineRight(i)
                if (element.isUnderline) {
                    val lineBottom = layout.getLineBottom(i).toFloat()
                    canvas.drawLine(lineLeft, lineBottom, lineRight, lineBottom, decorationPaint)
                }
                if (element.isStrikethrough) {
                    val lineTop = layout.getLineTop(i).toFloat()
                    val lineBottom = layout.getLineBottom(i).toFloat()
                    val midY = (lineTop + lineBottom) / 2f
                    canvas.drawLine(lineLeft, midY, lineRight, midY, decorationPaint)
                }
            }
        }
        canvas.restore()
    }

    /** Resolve Android Typeface from font family name and bold/italic flags. */
    private fun resolveTypeface(fontFamily: String, bold: Boolean, italic: Boolean): Typeface {
        val base = when (fontFamily) {
            "Courier" -> Typeface.MONOSPACE
            "Times New Roman" -> Typeface.SERIF
            else -> Typeface.SANS_SERIF
        }
        val style = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        return Typeface.create(base, style)
    }

    private fun loadBitmap(path: String): Bitmap? {
        bitmapCache.get(path)?.let { return it }
        if (!File(path).exists()) return null
        val bmp = BitmapFactory.decodeFile(path) ?: return null
        bitmapCache.put(path, bmp)
        return bmp
    }
}
