package com.pdfapp.reader.ui.editmode.mark

import android.graphics.PointF
import android.graphics.RectF
import com.pdfapp.reader.domain.model.CharInfo
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure utility functions for text selection in Mark tab.
 * Operates on CharInfo list from ExtractTextBlocksUseCase.
 * Coordinates use top-left origin (xDirAdj/yDirAdj) -- same as CharInfo.
 */
object MarkTextSelectionEngine {

    /**
     * Find word at given point. Returns start/end char indices or null if too far.
     * Uses distance threshold: 2x average char height.
     */
    fun findWordAtPoint(chars: List<CharInfo>, pdfX: Float, pdfY: Float): Pair<Int, Int>? {
        if (chars.isEmpty()) return null

        val avgHeight = chars.map { it.height }.average().toFloat()
        val maxDist = avgHeight * 2f

        // Find nearest char by center distance
        var nearestIdx = -1
        var nearestDist = Float.MAX_VALUE
        chars.forEachIndexed { idx, c ->
            val cx = c.x + c.width / 2f
            val cy = c.y - c.height / 2f
            val dist = sqrt((cx - pdfX) * (cx - pdfX) + (cy - pdfY) * (cy - pdfY))
            if (dist < nearestDist) {
                nearestDist = dist
                nearestIdx = idx
            }
        }

        if (nearestIdx < 0 || nearestDist > maxDist) return null

        // Expand to word boundaries (whitespace/punctuation)
        val start = expandWordBoundary(chars, nearestIdx, forward = false)
        val end = expandWordBoundary(chars, nearestIdx, forward = true)
        return start to end
    }

    /**
     * Compute per-line quads and handle positions for a char range.
     * Groups chars by line (Y proximity), returns one RectF per line.
     */
    fun expandSelectionToHandles(
        chars: List<CharInfo>,
        startIdx: Int,
        endIdx: Int
    ): SelectionResult {
        val safeStart = startIdx.coerceIn(0, chars.lastIndex)
        val safeEnd = endIdx.coerceIn(safeStart, chars.lastIndex)

        val quads = getPerLineQuads(chars, safeStart, safeEnd)
        if (quads.isEmpty()) {
            val c = chars[safeStart]
            val fallback = RectF(c.x, c.y - c.height, c.x + c.width, c.y)
            return SelectionResult(
                quads = listOf(fallback),
                startHandle = PointF(fallback.left, fallback.bottom),
                endHandle = PointF(fallback.right, fallback.bottom),
                startCharIndex = safeStart,
                endCharIndex = safeEnd
            )
        }

        val firstQuad = quads.first()
        val lastQuad = quads.last()
        return SelectionResult(
            quads = quads,
            startHandle = PointF(firstQuad.left, firstQuad.bottom),
            endHandle = PointF(lastQuad.right, lastQuad.bottom),
            startCharIndex = safeStart,
            endCharIndex = safeEnd
        )
    }

    /**
     * Group selected chars by line (Y proximity < 0.5 * charHeight).
     * Returns bounding RectF per line.
     */
    fun getPerLineQuads(chars: List<CharInfo>, startIdx: Int, endIdx: Int): List<RectF> {
        if (startIdx > endIdx || chars.isEmpty()) return emptyList()
        val selected = chars.subList(startIdx, (endIdx + 1).coerceAtMost(chars.size))
        if (selected.isEmpty()) return emptyList()

        val lines = mutableListOf<MutableList<CharInfo>>()
        var currentLine = mutableListOf(selected.first())

        for (i in 1 until selected.size) {
            val prev = currentLine.last()
            val curr = selected[i]
            val yDiff = abs(curr.y - prev.y)
            if (yDiff < prev.height * 0.5f) {
                currentLine.add(curr)
            } else {
                lines.add(currentLine)
                currentLine = mutableListOf(curr)
            }
        }
        lines.add(currentLine)

        return lines.map { line ->
            val minX = line.minOf { it.x }
            val minY = line.minOf { it.y - it.height }
            val maxX = line.maxOf { it.x + it.width }
            val maxY = line.maxOf { it.y }
            RectF(minX, minY, maxX, maxY)
        }
    }

    /**
     * Find char index nearest to the given point. Used for handle dragging.
     */
    fun findCharIndexAtPoint(chars: List<CharInfo>, pdfX: Float, pdfY: Float): Int {
        if (chars.isEmpty()) return 0
        var nearestIdx = 0
        var nearestDist = Float.MAX_VALUE
        chars.forEachIndexed { idx, c ->
            val cx = c.x + c.width / 2f
            val cy = c.y - c.height / 2f
            val dist = sqrt((cx - pdfX) * (cx - pdfX) + (cy - pdfY) * (cy - pdfY))
            if (dist < nearestDist) {
                nearestDist = dist
                nearestIdx = idx
            }
        }
        return nearestIdx
    }

    /** Compute bounding rect encompassing all quads. */
    fun boundingRect(quads: List<RectF>): RectF {
        if (quads.isEmpty()) return RectF()
        return RectF(
            quads.minOf { it.left },
            quads.minOf { it.top },
            quads.maxOf { it.right },
            quads.maxOf { it.bottom }
        )
    }

    /**
     * Snap a char index to the nearest word boundary.
     * Used during handle drag for word-level selection.
     *
     * @param isStart if true, snap to word start (expand backward); if false, snap to word end (expand forward)
     */
    fun snapToWordBoundary(chars: List<CharInfo>, charIndex: Int, isStart: Boolean): Int {
        if (chars.isEmpty()) return charIndex
        val safeIdx = charIndex.coerceIn(0, chars.lastIndex)
        return if (isStart) {
            expandWordBoundary(chars, safeIdx, forward = false)
        } else {
            expandWordBoundary(chars, safeIdx, forward = true)
        }
    }

    private const val MAX_WORD_LENGTH = 30

    internal fun expandWordBoundary(chars: List<CharInfo>, idx: Int, forward: Boolean): Int {
        val sameLineThreshold = chars[idx].height * 0.5f
        var i = idx
        if (forward) {
            while (i < chars.lastIndex && (i - idx) < MAX_WORD_LENGTH) {
                val next = chars[i + 1]
                if (abs(next.y - chars[idx].y) > sameLineThreshold) break
                if (next.char.isWhitespace() || next.char in ".,;:!?()[]{}\"'") break
                i++
            }
        } else {
            while (i > 0 && (idx - i) < MAX_WORD_LENGTH) {
                val prev = chars[i - 1]
                if (abs(prev.y - chars[idx].y) > sameLineThreshold) break
                if (prev.char.isWhitespace() || prev.char in ".,;:!?()[]{}\"'") break
                i--
            }
        }
        return i
    }
}

/** Result of text selection: per-line quads + handle positions + char range. */
data class SelectionResult(
    val quads: List<RectF>,
    val startHandle: PointF,
    val endHandle: PointF,
    val startCharIndex: Int,
    val endCharIndex: Int
)
