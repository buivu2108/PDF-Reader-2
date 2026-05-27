package com.pdfapp.reader.ui.viewer.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Deterministic coordinate transforms between screen pixels and PDF points.
 * Pure functions — no mutable state.
 */
object CoordinateTransform {

    /** Converts a screen-space point to PDF-space coordinates. */
    fun screenToPdf(
        screenPoint: Offset,
        scale: Float,
        offset: Offset,
        displaySize: Size,
        pdfPageSize: Size
    ): Offset {
        val unscaled = (screenPoint - offset) / scale
        return Offset(
            unscaled.x / displaySize.width * pdfPageSize.width,
            unscaled.y / displaySize.height * pdfPageSize.height
        )
    }

    /** Converts a PDF-space point to screen-space coordinates. */
    fun pdfToScreen(
        pdfPoint: Offset,
        scale: Float,
        offset: Offset,
        displaySize: Size,
        pdfPageSize: Size
    ): Offset {
        val normalized = Offset(
            pdfPoint.x / pdfPageSize.width * displaySize.width,
            pdfPoint.y / pdfPageSize.height * displaySize.height
        )
        return normalized * scale + offset
    }

    /** Converts a PDF-space rectangle to screen-space. */
    fun pdfRectToScreenRect(
        pdfRect: Rect,
        scale: Float,
        offset: Offset,
        displaySize: Size,
        pdfPageSize: Size
    ): Rect {
        val topLeft = pdfToScreen(pdfRect.topLeft, scale, offset, displaySize, pdfPageSize)
        val bottomRight = pdfToScreen(pdfRect.bottomRight, scale, offset, displaySize, pdfPageSize)
        return Rect(topLeft, bottomRight)
    }
}
