package com.pdfapp.reader.ui.viewer.render

/** Constants for PDF rendering configuration. */
object PdfRenderConfig {
    /** Default render DPI — reduced from 150 to 120 for better scroll performance.
     *  120 DPI ≈ 992×1404 px per A4 page (~5.5MB) vs 150 DPI ≈ 1240×1754 px (~8.7MB). */
    const val DEFAULT_DPI = 120

    /** Max pages held in bitmap cache before LRU eviction.
     *  Increased to 7 to accommodate +3 look-ahead + current + 1 behind + 2 buffer. */
    const val DEFAULT_CACHE_SIZE = 7

    /** Zoom bounds for pinch-to-zoom gesture. */
    const val MIN_ZOOM = 1.0f
    const val MAX_ZOOM = 5.0f

    /** Double-tap toggles between these two zoom levels. */
    const val DOUBLE_TAP_ZOOM = 2.5f

    /** Standard PDF coordinate unit: 72 points per inch. */
    const val PDF_POINTS_PER_INCH = 72
}
