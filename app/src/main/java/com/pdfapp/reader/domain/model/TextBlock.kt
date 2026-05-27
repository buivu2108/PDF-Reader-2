package com.pdfapp.reader.domain.model

import android.graphics.RectF
import java.util.UUID

/**
 * Represents a text block extracted from a PDF page via PdfBox.
 * pdfBounds uses PDFTextStripper coordinates (top-left origin, Y downward).
 * screenBounds is in bitmap-pixel space (scaled from pdfBounds at extraction time).
 */
data class TextBlock(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val pdfBounds: RectF,
    val screenBounds: RectF = RectF(),
    val originalText: String,
    val currentText: String,
    val fontSize: Float,
    val currentFontSize: Float = fontSize,
    val fontFamily: String,
    val currentFontFamily: String = fontFamily,
    val fontColor: Int,
    val currentFontColor: Int = fontColor,
    val isBold: Boolean,
    val currentIsBold: Boolean = isBold,
    val baselineOffset: Float = 0f,
    val isDeleted: Boolean = false,
    val isModified: Boolean = false
) {
    /** Whether this block has any pending changes. */
    val hasChanges: Boolean
        get() = isDeleted || isModified
}
