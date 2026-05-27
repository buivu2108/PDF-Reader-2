package com.pdfapp.reader.ui.viewer.selection

import com.pdfapp.reader.domain.model.CharInfo
import kotlin.math.abs

/**
 * Spatial index for O(log n) character lookup by PDF coordinates.
 *
 * Groups characters into lines by Y-proximity, sorts lines by Y and chars by X,
 * then uses binary search for fast position→character mapping.
 *
 * Build once per page load; rebuild when page characters change.
 */
class SpatialCharIndex private constructor(
    private val lines: List<Line>,
    private val allChars: List<CharInfo>
) {
    /** A horizontal line of characters with a Y-range and sorted chars. */
    class Line(
        val minY: Float,
        val maxY: Float,
        val midY: Float,
        val chars: List<IndexedChar>
    )

    /** Character with its original index in the flat allChars list. */
    class IndexedChar(
        val globalIndex: Int,
        val x: Float,
        val right: Float,
        val charInfo: CharInfo
    )

    /** Total characters in the index. */
    val size: Int get() = allChars.size

    /**
     * Find the character at the given PDF position (exact hit or nearest within tolerance).
     * @return global char index in the original chars list, or null if no char nearby.
     */
    fun findCharAt(pdfX: Float, pdfY: Float, tolerance: Float = 50f): Int? {
        if (lines.isEmpty()) return null

        // Binary search for the closest line by Y
        val lineIndex = findClosestLine(pdfY)
        val line = lines[lineIndex]

        // Check if Y is within tolerance of this line
        val yDist = when {
            pdfY < line.minY -> line.minY - pdfY
            pdfY > line.maxY -> pdfY - line.maxY
            else -> 0f
        }
        if (yDist > tolerance) return null

        // Binary search for closest char by X within the line
        return findClosestCharInLine(line, pdfX, tolerance)
    }

    /**
     * Find the nearest character to the given PDF position (always returns closest).
     * @return global char index, or null if index is empty.
     */
    fun findNearestChar(pdfX: Float, pdfY: Float): Int? {
        if (lines.isEmpty()) return null

        val lineIndex = findClosestLine(pdfY)

        // Check this line and adjacent lines for nearest char
        var bestIndex: Int? = null
        var bestDistSq = Float.MAX_VALUE

        val range = (lineIndex - 1).coerceAtLeast(0)..(lineIndex + 1).coerceAtMost(lines.size - 1)
        for (li in range) {
            val line = lines[li]
            for (ic in line.chars) {
                val cx = ic.x + (ic.right - ic.x) / 2
                val cy = line.midY
                val dx = pdfX - cx
                val dy = pdfY - cy
                val distSq = dx * dx + dy * dy
                if (distSq < bestDistSq) {
                    bestDistSq = distSq
                    bestIndex = ic.globalIndex
                }
            }
        }

        // Reject if too far (50 PDF points)
        if (bestDistSq > 50f * 50f) return null
        return bestIndex
    }

    private fun findClosestLine(pdfY: Float): Int {
        var low = 0
        var high = lines.size - 1
        while (low < high) {
            val mid = (low + high) / 2
            if (lines[mid].midY < pdfY) low = mid + 1 else high = mid
        }
        // Check if previous line is closer
        if (low > 0) {
            val distToCurr = abs(pdfY - lines[low].midY)
            val distToPrev = abs(pdfY - lines[low - 1].midY)
            if (distToPrev < distToCurr) return low - 1
        }
        return low
    }

    private fun findClosestCharInLine(line: Line, pdfX: Float, tolerance: Float): Int? {
        val chars = line.chars
        if (chars.isEmpty()) return null

        // Binary search for the char whose X range contains pdfX
        var low = 0
        var high = chars.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            val ic = chars[mid]
            when {
                pdfX < ic.x -> high = mid - 1
                pdfX > ic.right -> low = mid + 1
                else -> return ic.globalIndex // Direct hit
            }
        }

        // No direct hit — find closest among candidates
        var bestIndex: Int? = null
        var bestDist = tolerance

        for (i in (low - 1).coerceAtLeast(0)..(low).coerceAtMost(chars.size - 1)) {
            val ic = chars[i]
            val cx = ic.x + (ic.right - ic.x) / 2
            val dist = abs(pdfX - cx)
            if (dist < bestDist) {
                bestDist = dist
                bestIndex = ic.globalIndex
            }
        }
        return bestIndex
    }

    companion object {
        /**
         * Build a spatial index from a list of CharInfo.
         * Groups by Y-proximity (threshold = fontSize * 0.5), sorts by X within lines.
         */
        fun build(chars: List<CharInfo>): SpatialCharIndex {
            if (chars.isEmpty()) return SpatialCharIndex(emptyList(), chars)

            val sorted = chars.indices.sortedWith(compareBy({ chars[it].y }, { chars[it].x }))

            val lineGroups = mutableListOf<MutableList<Int>>()
            var currentGroup = mutableListOf(sorted[0])

            for (i in 1 until sorted.size) {
                val prevIdx = currentGroup.last()
                val currIdx = sorted[i]
                val yDiff = abs(chars[currIdx].y - chars[prevIdx].y)
                val threshold = chars[prevIdx].height * 0.5f

                if (yDiff < threshold) {
                    currentGroup.add(currIdx)
                } else {
                    lineGroups.add(currentGroup)
                    currentGroup = mutableListOf(currIdx)
                }
            }
            lineGroups.add(currentGroup)

            val lines = lineGroups.map { group ->
                val sortedByX = group.sortedBy { chars[it].x }
                val indexedChars = sortedByX.map { idx ->
                    val c = chars[idx]
                    IndexedChar(
                        globalIndex = idx,
                        x = c.x,
                        right = c.x + c.width,
                        charInfo = c
                    )
                }
                val minY = group.minOf { chars[it].y - chars[it].height }
                val maxY = group.maxOf { chars[it].y }
                Line(minY = minY, maxY = maxY, midY = (minY + maxY) / 2, chars = indexedChars)
            }.sortedBy { it.midY }

            return SpatialCharIndex(lines, chars)
        }
    }
}
