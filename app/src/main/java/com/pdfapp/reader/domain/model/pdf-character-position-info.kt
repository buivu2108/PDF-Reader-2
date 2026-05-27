package com.pdfapp.reader.domain.model

/**
 * Per-character position data extracted from PDF via PdfBox PDFTextStripper.
 * Coordinates use xDirAdj/yDirAdj (top-left origin, Y downward).
 * Used by SelectionEngine and ExtractTextBlocksUseCase for character-level lookups.
 */
data class CharInfo(
    val char: Char,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val fontSize: Float,
    val fontName: String,
    val fontColor: Int,
    val isBold: Boolean,
    val pageIndex: Int
)
