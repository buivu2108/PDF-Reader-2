package com.pdfapp.reader.ui.viewer.selection

import android.graphics.RectF
import android.net.Uri
import com.pdfapp.reader.domain.model.CharInfo
import com.pdfapp.reader.domain.usecase.ExtractTextBlocksUseCase
import kotlin.math.abs

/**
 * Immutable selection state: page, char range, text, and per-line bounds in PDF coords.
 */
data class SelectionState(
    val pageIndex: Int,
    val startIndex: Int,
    val endIndex: Int,
    val selectedText: String,
    val lineBounds: List<RectF>
)

/**
 * Core text selection logic: word detection, handle dragging, line bounds computation.
 *
 * Uses [SpatialCharIndex] for O(log n) character lookup instead of linear scan.
 * Uses [ExtractTextBlocksUseCase] for character extraction with caching.
 */
class SelectionEngine(
    private val extractUseCase: ExtractTextBlocksUseCase,
    private val maxCachedPages: Int = 10
) {

    private val pageChars = LinkedHashMap<Int, List<CharInfo>>(maxCachedPages + 1, 0.75f, true)
    private val pageIndices = LinkedHashMap<Int, SpatialCharIndex>(maxCachedPages + 1, 0.75f, true)
    private var currentSelection: SelectionState? = null

    val selection: SelectionState? get() = currentSelection

    /** Load characters and build spatial index for a page (LRU-cached, max [maxCachedPages]). */
    suspend fun loadCharsForPage(uri: Uri, pageIndex: Int): List<CharInfo> {
        pageChars[pageIndex]?.let { return it }
        val chars = extractUseCase.extractCharactersForPage(uri, pageIndex)
        pageChars[pageIndex] = chars
        pageIndices[pageIndex] = SpatialCharIndex.build(chars)
        trimCache()
        return chars
    }

    private fun trimCache() {
        while (pageChars.size > maxCachedPages) {
            val eldest = pageChars.keys.first()
            pageChars.remove(eldest)
            pageIndices.remove(eldest)
        }
    }

    /**
     * Select the word at the given PDF coordinates.
     * @return SelectionState or null if no text found at position.
     */
    suspend fun selectWordAt(uri: Uri, pageIndex: Int, pdfX: Float, pdfY: Float): SelectionState? {
        val chars = loadCharsForPage(uri, pageIndex)
        if (chars.isEmpty()) return null

        val index = getOrBuildIndex(pageIndex, chars)
        val hitIndex = index.findCharAt(pdfX, pdfY) ?: index.findNearestChar(pdfX, pdfY) ?: return null

        // Expand to word boundaries
        var start = hitIndex
        var end = hitIndex
        while (start > 0 && !isWordBoundary(chars[start - 1].char)) start--
        while (end < chars.size - 1 && !isWordBoundary(chars[end + 1].char)) end++

        val text = chars.subList(start, end + 1).map { it.char }.joinToString("")
        if (text.isBlank()) return null

        val lineBounds = computeLineBounds(chars, start, end)
        currentSelection = SelectionState(pageIndex, start, end, text, lineBounds)
        return currentSelection
    }

    /**
     * Update selection when a handle is dragged.
     * @param isStartHandle true = start handle being dragged, false = end handle
     */
    suspend fun updateSelection(
        uri: Uri, isStartHandle: Boolean, pdfX: Float, pdfY: Float
    ): SelectionState? {
        val sel = currentSelection ?: return null
        val chars = loadCharsForPage(uri, sel.pageIndex)
        if (chars.isEmpty()) return null

        val index = getOrBuildIndex(sel.pageIndex, chars)
        val hitIndex = index.findNearestChar(pdfX, pdfY) ?: return sel

        val newStart: Int
        val newEnd: Int
        if (isStartHandle) {
            newStart = hitIndex.coerceAtMost(sel.endIndex)
            newEnd = sel.endIndex
        } else {
            newStart = sel.startIndex
            newEnd = hitIndex.coerceAtLeast(sel.startIndex)
        }
        if (newStart > newEnd) return sel

        val text = chars.subList(newStart, newEnd + 1).map { it.char }.joinToString("")
        val lineBounds = computeLineBounds(chars, newStart, newEnd)
        currentSelection = SelectionState(sel.pageIndex, newStart, newEnd, text, lineBounds)
        return currentSelection
    }

    /**
     * Select all text on a given page.
     * @return SelectionState or null if page has no extractable text.
     */
    suspend fun selectAllOnPage(uri: Uri, pageIndex: Int): SelectionState? {
        val chars = loadCharsForPage(uri, pageIndex)
        if (chars.isEmpty()) return null

        val text = chars.map { it.char }.joinToString("")
        if (text.isBlank()) return null

        val lineBounds = computeLineBounds(chars, 0, chars.size - 1)
        currentSelection = SelectionState(pageIndex, 0, chars.size - 1, text, lineBounds)
        return currentSelection
    }

    fun clearSelection() {
        currentSelection = null
    }

    /** Returns char list for page (must call loadCharsForPage first). */
    fun getCharsForPage(pageIndex: Int): List<CharInfo> = pageChars[pageIndex] ?: emptyList()

    private fun getOrBuildIndex(pageIndex: Int, chars: List<CharInfo>): SpatialCharIndex {
        return pageIndices.getOrPut(pageIndex) { SpatialCharIndex.build(chars) }
    }

    /**
     * Compute per-line RectF bounds for the selected range.
     * Groups characters into lines by Y-proximity.
     */
    private fun computeLineBounds(chars: List<CharInfo>, startIdx: Int, endIdx: Int): List<RectF> {
        if (startIdx > endIdx || startIdx < 0 || endIdx >= chars.size) return emptyList()
        val selected = chars.subList(startIdx, endIdx + 1)
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
            val maxX = line.maxOf { it.x + it.width }
            val minY = line.minOf { it.y - it.height }
            val maxY = line.maxOf { it.y }
            RectF(minX, minY, maxX, maxY)
        }
    }

    private fun isWordBoundary(c: Char): Boolean =
        c.isWhitespace() || c in setOf(
            ',', '.', ';', ':', '!', '?', '(', ')', '[', ']', '{', '}', '"', '\''
        )
}
