package com.pdfapp.reader.ui.viewer.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.pdfapp.reader.domain.model.SearchResult

/** Orange highlight for the current active search match. */
private val CurrentMatchColor = Color(0x80FF9800)

/** Yellow highlight for other (non-active) search matches. */
private val OtherMatchColor = Color(0x60FFEB3B)

/**
 * Canvas layer that draws search result highlights on a PDF page.
 *
 * Each [SearchResult] has per-line bounds in PDF coordinates.
 * The current match is drawn in orange; other matches in yellow.
 */
@Composable
fun SearchHighlightLayer(
    pageSearchResults: List<SearchResult>,
    currentSearchIndex: Int,
    globalIndexOffset: Int,
    pdfPageWidth: Float,
    pdfPageHeight: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (pdfPageWidth <= 0f || pdfPageHeight <= 0f) return@Canvas
        val scaleX = size.width / pdfPageWidth
        val scaleY = size.height / pdfPageHeight

        pageSearchResults.forEachIndexed { localIndex, result ->
            val globalIndex = globalIndexOffset + localIndex
            val color = if (globalIndex == currentSearchIndex) CurrentMatchColor else OtherMatchColor

            result.lineBounds.forEach { pdfRect ->
                drawRect(
                    color = color,
                    topLeft = Offset(pdfRect.left * scaleX, pdfRect.top * scaleY),
                    size = Size(
                        (pdfRect.right - pdfRect.left) * scaleX,
                        (pdfRect.bottom - pdfRect.top) * scaleY
                    )
                )
            }
        }
    }
}
