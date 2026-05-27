package com.pdfapp.reader.util

import android.graphics.RectF

/**
 * Converts between PDF coordinate space (72 DPI, origin at bottom-left)
 * and screen coordinate space (variable DPI, origin at top-left).
 */
object PdfCoordinateConverter {

    /** Convert screen coordinates to PDF points for a given page. */
    fun screenToPdf(
        screenX: Float, screenY: Float,
        pageWidth: Float, pageHeight: Float,
        viewWidth: Float, viewHeight: Float,
        pdfPageWidth: Float, pdfPageHeight: Float
    ): Pair<Float, Float> {
        val scaleX = pdfPageWidth / viewWidth
        val scaleY = pdfPageHeight / viewHeight
        val pdfX = screenX * scaleX
        // PDF Y is inverted (bottom-left origin)
        val pdfY = pdfPageHeight - (screenY * scaleY)
        return Pair(pdfX, pdfY)
    }

    /** Convert PDF points to screen coordinates. */
    fun pdfToScreen(
        pdfX: Float, pdfY: Float,
        viewWidth: Float, viewHeight: Float,
        pdfPageWidth: Float, pdfPageHeight: Float
    ): Pair<Float, Float> {
        val scaleX = viewWidth / pdfPageWidth
        val scaleY = viewHeight / pdfPageHeight
        val screenX = pdfX * scaleX
        val screenY = (pdfPageHeight - pdfY) * scaleY
        return Pair(screenX, screenY)
    }

    /** Convert a screen RectF to PDF RectF. */
    fun screenRectToPdf(
        screenRect: RectF,
        viewWidth: Float, viewHeight: Float,
        pdfPageWidth: Float, pdfPageHeight: Float
    ): RectF {
        val scaleX = pdfPageWidth / viewWidth
        val scaleY = pdfPageHeight / viewHeight
        return RectF(
            screenRect.left * scaleX,
            pdfPageHeight - (screenRect.bottom * scaleY), // PDF bottom-left origin
            screenRect.right * scaleX,
            pdfPageHeight - (screenRect.top * scaleY)
        )
    }

    /** Convert a PDF RectF to screen RectF. */
    fun pdfRectToScreen(
        pdfRect: RectF,
        viewWidth: Float, viewHeight: Float,
        pdfPageWidth: Float, pdfPageHeight: Float
    ): RectF {
        val scaleX = viewWidth / pdfPageWidth
        val scaleY = viewHeight / pdfPageHeight
        return RectF(
            pdfRect.left * scaleX,
            (pdfPageHeight - pdfRect.top) * scaleY,
            pdfRect.right * scaleX,
            (pdfPageHeight - pdfRect.bottom) * scaleY
        )
    }
}
