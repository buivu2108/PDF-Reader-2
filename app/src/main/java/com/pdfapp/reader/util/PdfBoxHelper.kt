package com.pdfapp.reader.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import com.pdfapp.reader.domain.model.SearchResult
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object PdfBoxHelper {

    /**
     * Search for [query] across all pages of the PDF at [uri].
     * Returns a list of [SearchResult] with page indices and per-line bounding rects.
     *
     * Extracts character positions via PDFTextStripper to compute actual highlight bounds.
     * Opens the document once for all pages.
     */
    suspend fun searchText(
        context: Context,
        uri: Uri,
        query: String
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val results = mutableListOf<SearchResult>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                try {
                    val lowerQuery = query.lowercase()
                    for (pageIndex in 0 until document.numberOfPages) {
                        ensureActive()
                        try {
                            val chars = extractPageCharPositions(document, pageIndex)
                            if (chars.isEmpty()) continue

                            val pageText = chars.map { it.char }.joinToString("").lowercase()
                            var startIndex = 0
                            while (true) {
                                val foundIndex = pageText.indexOf(lowerQuery, startIndex)
                                if (foundIndex == -1) break
                                val endIndex = foundIndex + query.length - 1
                                val matchText = chars.subList(foundIndex, endIndex + 1)
                                    .map { it.char }.joinToString("")
                                val lineBounds = computeMatchLineBounds(chars, foundIndex, endIndex)
                                results.add(SearchResult(pageIndex, matchText, lineBounds))
                                startIndex = foundIndex + 1
                            }
                        } catch (_: Exception) {
                            // Skip pages that fail extraction (corrupted, encrypted, etc.)
                        }
                    }
                } finally {
                    document.close()
                }
            }
        } catch (_: Exception) { }
        results
    }

    /** Lightweight char position data for search bound computation. */
    private class CharPos(val char: Char, val x: Float, val y: Float, val width: Float, val height: Float)

    /** Extract character positions for a single page using PDFTextStripper. */
    private fun extractPageCharPositions(document: PDDocument, pageIndex: Int): List<CharPos> {
        val chars = mutableListOf<CharPos>()
        val stripper = object : PDFTextStripper() {
            override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
                textPositions?.forEach { tp ->
                    val c = tp.unicode
                    if (c.isNullOrEmpty() || (c == " " && tp.widthOfSpace < 0.1f)) return@forEach
                    chars.add(CharPos(c.first(), tp.xDirAdj, tp.yDirAdj, tp.widthDirAdj, tp.heightDir))
                }
            }
        }
        stripper.startPage = pageIndex + 1
        stripper.endPage = pageIndex + 1
        stripper.sortByPosition = true
        stripper.getText(document)
        return chars
    }

    /** Compute per-line RectF bounds for a match range within char positions. */
    private fun computeMatchLineBounds(chars: List<CharPos>, startIdx: Int, endIdx: Int): List<RectF> {
        if (startIdx > endIdx || startIdx < 0 || endIdx >= chars.size) return emptyList()
        val selected = chars.subList(startIdx, endIdx + 1)
        if (selected.isEmpty()) return emptyList()

        val lines = mutableListOf<MutableList<CharPos>>()
        var currentLine = mutableListOf(selected.first())

        for (i in 1 until selected.size) {
            val prev = currentLine.last()
            val curr = selected[i]
            val yDiff = kotlin.math.abs(curr.y - prev.y)
            if (yDiff < prev.height * 0.5f) {
                currentLine.add(curr)
            } else {
                lines.add(currentLine)
                currentLine = mutableListOf(curr)
            }
        }
        lines.add(currentLine)

        return lines.map { line ->
            RectF(
                line.minOf { it.x },
                line.minOf { it.y - it.height },
                line.maxOf { it.x + it.width },
                line.maxOf { it.y }
            )
        }
    }

    /** Get PDF page dimensions in points (width, height) for all pages. */
    suspend fun getPageDimensions(context: Context, uri: Uri): List<Pair<Float, Float>> =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val document = PDDocument.load(inputStream)
                    try {
                        (0 until document.numberOfPages).map { i ->
                            val mediaBox = document.getPage(i).mediaBox
                            Pair(mediaBox.width, mediaBox.height)
                        }
                    } finally {
                        document.close()
                    }
                } ?: emptyList()
            } catch (_: Exception) { emptyList() }
        }

    /** Get total page count for a PDF. */
    suspend fun getPageCount(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                try {
                    document.numberOfPages
                } finally {
                    document.close()
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }
}

/**
 * Detects the dominant background color around a text block by sampling
 * pixels at the edges outside the block bounds on the rendered page bitmap.
 */
object BackgroundColorDetector {

    private const val EDGE_MARGIN = 4
    private const val SAMPLE_INTERVAL = 3
    private const val QUANTIZE_STEP = 16

    /** Detect dominant background color around [blockBounds] on [pageBitmap]. */
    fun detectBackgroundColor(pageBitmap: Bitmap, blockBounds: RectF): Int {
        val w = pageBitmap.width
        val h = pageBitmap.height
        val pixels = mutableListOf<Int>()

        val left = max(0, (blockBounds.left - EDGE_MARGIN).toInt())
        val top = max(0, (blockBounds.top - EDGE_MARGIN).toInt())
        val right = min(w - 1, (blockBounds.right + EDGE_MARGIN).toInt())
        val bottom = min(h - 1, (blockBounds.bottom + EDGE_MARGIN).toInt())

        // Left edge: vertical samples
        if (left > 0) {
            val x = max(0, left - 1)
            var y = top
            while (y <= bottom) {
                pixels.add(pageBitmap.getPixel(x, min(y, h - 1)))
                y += SAMPLE_INTERVAL
            }
        }
        // Right edge: vertical samples
        if (right < w - 1) {
            val x = min(w - 1, right + 1)
            var y = top
            while (y <= bottom) {
                pixels.add(pageBitmap.getPixel(x, min(y, h - 1)))
                y += SAMPLE_INTERVAL
            }
        }
        // Top edge: horizontal samples
        if (top > 0) {
            val y = max(0, top - 1)
            var x = left
            while (x <= right) {
                pixels.add(pageBitmap.getPixel(min(x, w - 1), y))
                x += SAMPLE_INTERVAL
            }
        }
        // Bottom edge: horizontal samples
        if (bottom < h - 1) {
            val y = min(h - 1, bottom + 1)
            var x = left
            while (x <= right) {
                pixels.add(pageBitmap.getPixel(min(x, w - 1), y))
                x += SAMPLE_INTERVAL
            }
        }

        return if (pixels.isEmpty()) Color.WHITE else findDominantColor(pixels)
    }

    private fun findDominantColor(pixels: List<Int>): Int {
        val groups = mutableMapOf<Int, MutableList<Int>>()
        for (pixel in pixels) {
            val qR = (Color.red(pixel) / QUANTIZE_STEP) * QUANTIZE_STEP
            val qG = (Color.green(pixel) / QUANTIZE_STEP) * QUANTIZE_STEP
            val qB = (Color.blue(pixel) / QUANTIZE_STEP) * QUANTIZE_STEP
            val key = Color.rgb(qR, qG, qB)
            groups.getOrPut(key) { mutableListOf() }.add(pixel)
        }
        val largestGroup = groups.values.maxByOrNull { it.size } ?: return Color.WHITE
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        for (pixel in largestGroup) {
            totalR += Color.red(pixel)
            totalG += Color.green(pixel)
            totalB += Color.blue(pixel)
        }
        val count = largestGroup.size
        return Color.rgb(
            (totalR / count).toInt(),
            (totalG / count).toInt(),
            (totalB / count).toInt()
        )
    }
}
