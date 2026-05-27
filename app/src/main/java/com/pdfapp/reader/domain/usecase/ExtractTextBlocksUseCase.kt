package com.pdfapp.reader.domain.usecase

import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import com.pdfapp.reader.domain.model.CharInfo
import com.pdfapp.reader.domain.model.TextBlock
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private const val TAG = "TextSelection"

/**
 * Extracts text blocks from PDF pages using PdfBox PDFTextStripper.
 * Groups characters into lines, then lines into blocks based on proximity.
 */
class ExtractTextBlocksUseCase(private val context: Context) {

    private val charCache = mutableMapOf<String, List<CharInfo>>()

    suspend fun extractPage(uri: Uri, pageIndex: Int): List<TextBlock> =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val document = PDDocument.load(stream)
                    try {
                        if (pageIndex >= document.numberOfPages) return@use emptyList()
                        val chars = extractCharacters(document, pageIndex)
                        groupIntoTextBlocks(chars, pageIndex)
                    } finally {
                        document.close()
                    }
                } ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    /** Public character extraction with caching for text selection. */
    suspend fun extractCharactersForPage(uri: Uri, pageIndex: Int): List<CharInfo> =
        withContext(Dispatchers.IO) {
            val cacheKey = "${uri}:$pageIndex"
            charCache[cacheKey]?.let {
                Log.d(TAG, "extractCharactersForPage: cache HIT for page $pageIndex, ${it.size} chars")
                return@withContext it
            }
            Log.d(TAG, "extractCharactersForPage: cache MISS for page $pageIndex, uri=$uri")
            try {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) {
                    Log.e(TAG, "extractCharactersForPage: openInputStream RETURNED NULL for uri=$uri")
                    return@withContext emptyList()
                }
                stream.use {
                    val document = PDDocument.load(it)
                    try {
                        Log.d(TAG, "extractCharactersForPage: document loaded, " +
                            "pages=${document.numberOfPages} requested=$pageIndex")
                        if (pageIndex >= document.numberOfPages) {
                            Log.w(TAG, "extractCharactersForPage: pageIndex=$pageIndex >= " +
                                "numberOfPages=${document.numberOfPages}")
                            return@use emptyList()
                        }
                        val chars = extractCharacters(document, pageIndex)
                        Log.d(TAG, "extractCharactersForPage: extracted ${chars.size} chars " +
                            "for page $pageIndex")
                        charCache[cacheKey] = chars
                        chars
                    } finally {
                        document.close()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "extractCharactersForPage: EXCEPTION for page $pageIndex: ${e.message}", e)
                emptyList()
            }
        }

    private fun extractCharacters(document: PDDocument, pageIndex: Int): List<CharInfo> {
        val chars = mutableListOf<CharInfo>()
        val stripper = object : PDFTextStripper() {
            override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
                textPositions?.forEach { tp ->
                    val c = tp.unicode
                    if (c.isNullOrEmpty() || c == " " && tp.widthOfSpace < 0.1f) return@forEach
                    val fontName = tp.font?.name ?: "unknown"
                    val isBold = fontName.contains("Bold", ignoreCase = true) ||
                            (tp.font?.fontDescriptor?.isForceBold == true)

                    val fontColor = try {
                        val gs = graphicsState
                        val color = gs?.nonStrokingColor
                        val components = color?.components
                        if (components != null && components.size >= 3) {
                            Color.rgb(
                                (components[0] * 255).toInt().coerceIn(0, 255),
                                (components[1] * 255).toInt().coerceIn(0, 255),
                                (components[2] * 255).toInt().coerceIn(0, 255)
                            )
                        } else Color.BLACK
                    } catch (_: Exception) { Color.BLACK }

                    chars.add(
                        CharInfo(
                            char = c.first(),
                            x = tp.xDirAdj,
                            y = tp.yDirAdj,
                            width = tp.widthDirAdj,
                            height = tp.heightDir,
                            fontSize = tp.fontSize,
                            fontName = fontName,
                            fontColor = fontColor,
                            isBold = isBold,
                            pageIndex = pageIndex
                        )
                    )
                }
            }
        }
        stripper.startPage = pageIndex + 1
        stripper.endPage = pageIndex + 1
        stripper.sortByPosition = true
        stripper.getText(document)
        return chars
    }

    private fun groupIntoTextBlocks(chars: List<CharInfo>, pageIndex: Int): List<TextBlock> {
        if (chars.isEmpty()) return emptyList()
        val lines = groupIntoLines(chars)
        // Each line is its own selectable text block
        return lines.map { line -> buildTextBlock(listOf(line), pageIndex) }
    }

    private fun groupIntoLines(chars: List<CharInfo>): List<List<CharInfo>> {
        val sorted = chars.sortedWith(compareBy({ it.y }, { it.x }))
        val lines = mutableListOf<MutableList<CharInfo>>()
        var currentLine = mutableListOf(sorted.first())

        for (i in 1 until sorted.size) {
            val prev = currentLine.last()
            val curr = sorted[i]
            val yDiff = kotlin.math.abs(curr.y - prev.y)
            if (yDiff < prev.height * 0.5f) {
                currentLine.add(curr)
            } else {
                lines.add(currentLine)
                currentLine = mutableListOf(curr)
            }
        }
        lines.add(currentLine)
        return lines
    }

    private fun buildTextBlock(lines: List<List<CharInfo>>, pageIndex: Int): TextBlock {
        val allChars = lines.flatten()
        val text = lines.joinToString("\n") { line ->
            line.sortedBy { it.x }.map { it.char }.joinToString("")
        }
        val minX = allChars.minOf { it.x }
        val minY = allChars.minOf { it.y - it.height }
        val maxX = allChars.maxOf { it.x + it.width }
        val maxY = allChars.maxOf { it.y }
        val fontSize = allChars.map { it.fontSize }.groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key ?: 12f
        val fontName = allChars.map { it.fontName }.groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key ?: "Helvetica"
        val fontColor = allChars.first().fontColor
        val isBold = allChars.count { it.isBold } > allChars.size / 2

        return TextBlock(
            pageIndex = pageIndex,
            pdfBounds = RectF(minX, minY, maxX, maxY),
            originalText = text,
            currentText = text,
            fontSize = fontSize,
            fontFamily = fontName,
            fontColor = fontColor,
            isBold = isBold,
            baselineOffset = allChars.last().height * 0.2f
        )
    }
}
