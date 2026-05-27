package com.pdfapp.reader.ui.viewer.render

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Color inversion filter for night/dark reading mode.
 * Creates Paint instances per-call to ensure thread safety
 * (android.graphics.Paint is not thread-safe).
 */
object NightModeColorMatrix {

    /** Inverts RGB channels while preserving alpha. */
    private val invertMatrix = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    /** Returns a new Paint with night mode filter. Thread-safe — new instance per call. */
    fun createNightPaint(): Paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(invertMatrix)
    }

    /** Returns a new Paint without any filter. Thread-safe — new instance per call. */
    fun createNormalPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG)
}
