package com.pdfapp.reader.ui.viewer.components

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pdfapp.reader.domain.model.QuickAnnotation
import com.pdfapp.reader.domain.model.SearchResult
import com.pdfapp.reader.ui.viewer.render.NightModeColorMatrix

/**
 * Displays a single PDF page with layered rendering:
 * L1: BitmapLayer (Canvas drawImage with night mode)
 * L2: AnnotationRenderLayer (persistent highlights, underlines, strikethroughs)
 * L3: SelectionHighlightLayer (active selection highlights)
 * L4: SelectionHandleLayer (teardrop handles)
 * L5: SelectionGestureLayer (long press, handle drag, tap outside)
 *
 * Shows a shimmer placeholder when [bitmap] is null.
 */
@Composable
fun PdfPageView(
    pageIndex: Int,
    bitmap: Bitmap?,
    aspectRatio: Float,
    nightMode: Boolean,
    modifier: Modifier = Modifier,
    // Selection state
    selectionPageIndex: Int = -1,
    selectionLineBounds: List<RectF> = emptyList(),
    hasSelection: Boolean = false,
    annotations: List<QuickAnnotation> = emptyList(),
    // Search highlight state — layer only composed when search is globally active
    isSearchActive: Boolean = false,
    pageSearchResults: List<SearchResult> = emptyList(),
    currentSearchIndex: Int = 0,
    searchGlobalIndexOffset: Int = 0,
    pdfPageWidth: Float = 0f,
    pdfPageHeight: Float = 0f,
    // Error state for this page (corrupted/password-protected)
    pageError: String? = null,
    // Gesture callbacks
    onLongPress: ((Offset) -> Unit)? = null,
    onStartHandleDrag: ((Offset) -> Unit)? = null,
    onEndHandleDrag: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onTapOutside: (() -> Unit)? = null,
    onSingleTap: (() -> Unit)? = null
) {
    val isSelectedPage = selectionPageIndex == pageIndex
    val activeLineBounds = if (isSelectedPage) selectionLineBounds else emptyList()
    val hasPdfDimensions = pdfPageWidth > 0f && pdfPageHeight > 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio.coerceAtLeast(0.1f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null && !bitmap.isRecycled) {
            // L1: Bitmap layer — always render PDF in original colors regardless of theme
            val bitmapPaint = remember { NightModeColorMatrix.createNormalPaint() }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    val scaleX = size.width / bitmap.width
                    val scaleY = size.height / bitmap.height
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.scale(scaleX, scaleY)
                    canvas.nativeCanvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)
                    canvas.nativeCanvas.restore()
                }
            }

            // L2: Persistent annotations (highlights, underlines, strikethroughs)
            if (hasPdfDimensions && annotations.isNotEmpty()) {
                AnnotationRenderLayer(
                    annotations = annotations,
                    pageIndex = pageIndex,
                    pdfPageWidth = pdfPageWidth,
                    pdfPageHeight = pdfPageHeight
                )
            }

            // L2.5: Search result highlights — skipped entirely when search inactive
            if (isSearchActive && hasPdfDimensions && pageSearchResults.isNotEmpty()) {
                SearchHighlightLayer(
                    pageSearchResults = pageSearchResults,
                    currentSearchIndex = currentSearchIndex,
                    globalIndexOffset = searchGlobalIndexOffset,
                    pdfPageWidth = pdfPageWidth,
                    pdfPageHeight = pdfPageHeight
                )
            }

            // L3: Active selection highlights
            if (hasPdfDimensions && activeLineBounds.isNotEmpty()) {
                SelectionHighlightLayer(
                    selectionLineBounds = activeLineBounds,
                    pdfPageWidth = pdfPageWidth,
                    pdfPageHeight = pdfPageHeight
                )
            }

            // L4: Teardrop selection handles
            if (hasPdfDimensions && isSelectedPage && activeLineBounds.isNotEmpty()) {
                SelectionHandleLayer(
                    selectionLineBounds = activeLineBounds,
                    pdfPageWidth = pdfPageWidth,
                    pdfPageHeight = pdfPageHeight
                )
            }

            // L5: Gesture layer (long press, handle drag, tap outside)
            if (hasPdfDimensions && onLongPress != null) {
                SelectionGestureLayer(
                    hasSelection = isSelectedPage && hasSelection,
                    selectionLineBounds = activeLineBounds,
                    pdfPageWidth = pdfPageWidth,
                    pdfPageHeight = pdfPageHeight,
                    onLongPress = onLongPress,
                    onStartHandleDrag = onStartHandleDrag ?: {},
                    onEndHandleDrag = onEndHandleDrag ?: {},
                    onDragEnd = onDragEnd ?: {},
                    onTapOutside = onTapOutside ?: {},
                    onSingleTap = onSingleTap ?: {}
                )
            }
        } else if (pageError != null) {
            // Error placeholder for corrupted/failed pages
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = pageError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Page ${pageIndex + 1}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            // Shimmer placeholder while bitmap renders — fills page area for visual continuity
            PdfPageShimmerPlaceholder(pageIndex = pageIndex)
        }
    }
}
