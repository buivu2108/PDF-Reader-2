package com.pdfapp.reader.domain.model

import android.graphics.RectF
import java.util.UUID

/** Types of quick annotations available from the text selection popup. */
enum class AnnotationType { HIGHLIGHT, UNDERLINE, STRIKETHROUGH }

/** A quick annotation applied from reader mode (persisted in memory until save). */
data class QuickAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val type: AnnotationType,
    val lineBounds: List<RectF>,
    val selectedText: String = "",
    val color: Int = DEFAULT_HIGHLIGHT_COLOR
) {
    /** Union of all line bounds for hit-testing and positioning. */
    val bounds: RectF
        get() {
            if (lineBounds.isEmpty()) return RectF()
            val result = RectF(lineBounds.first())
            lineBounds.forEach { result.union(it) }
            return result
        }

    companion object {
        const val DEFAULT_HIGHLIGHT_COLOR = 0x60FFFF00.toInt()
        const val UNDERLINE_COLOR = 0xFFD32F2F.toInt()
        const val STRIKETHROUGH_COLOR = 0xFFD32F2F.toInt()

        const val COLOR_YELLOW = 0x60FFFF00.toInt()
        const val COLOR_RED = 0x60FF0000.toInt()
        const val COLOR_GREEN = 0x6000FF00.toInt()
        const val COLOR_BLUE = 0x600000FF.toInt()
        const val COLOR_PINK = 0x60FF69B4.toInt()
        const val COLOR_ORANGE = 0x60FF8C00.toInt()
        const val COLOR_PURPLE = 0x608A2BE2.toInt()

        val COLORS = listOf(
            COLOR_YELLOW, COLOR_RED, COLOR_GREEN, COLOR_BLUE,
            COLOR_PINK, COLOR_ORANGE, COLOR_PURPLE
        )
    }
}
