package com.pdfapp.reader.ui.viewer

import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.model.PageMode
import com.pdfapp.reader.domain.model.QuickAnnotation
import com.pdfapp.reader.domain.model.SearchResult
import com.pdfapp.reader.ui.viewer.components.AnnotationMiniPopup
import com.pdfapp.reader.ui.viewer.components.BookmarkSheet
import com.pdfapp.reader.ui.viewer.components.PageIndicatorBadge
import com.pdfapp.reader.ui.viewer.components.PageThumbnailStrip
import com.pdfapp.reader.ui.viewer.components.PdfPageView
import com.pdfapp.reader.ui.viewer.components.SaveAnnotationsDialog
import com.pdfapp.reader.ui.viewer.components.SearchOverlay
import com.pdfapp.reader.ui.viewer.components.SystemTextSelectionToolbar
import com.pdfapp.reader.ui.viewer.components.ViewerTopBar
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComposePdfViewerScreen(
    uri: Uri,
    fileName: String,
    viewModel: PdfViewerViewModel,
    onBack: () -> Unit,
    onEditMode: ((pageIndex: Int, bounds: RectF?) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pageBitmaps by viewModel.pageBitmaps.collectAsStateWithLifecycle()
    val pageAspectRatios by viewModel.pageAspectRatios.collectAsStateWithLifecycle()
    val scrollToPage by viewModel.scrollToPage.collectAsStateWithLifecycle()
    val pageErrors by viewModel.pageErrors.collectAsStateWithLifecycle()
    val pdfPageDimensions by viewModel.pdfPageDimensions.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val isCurrentPageBookmarked by viewModel.isCurrentPageBookmarked.collectAsStateWithLifecycle()

    // Track selected page's position in root coordinates for toolbar positioning
    var selectionPageRootOffset by remember { mutableStateOf(Offset.Zero) }
    var showBookmarkSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        viewModel.loadPdf(uri)
        viewModel.loadBookmarks(uri.toString())
    }

    BackHandler(enabled = state.hasUnsavedAnnotations) { viewModel.showSaveDialog() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.error != null -> {
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )
            }
            state.totalPages > 0 -> {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    ViewerTopBar(
                        visible = state.showToolbar,
                        fileName = fileName,
                        isNightMode = state.isNightMode,
                        pageMode = state.pageMode,
                        isBookmarked = isCurrentPageBookmarked,
                        isSearchActive = state.isSearchActive,
                        searchQuery = state.searchQuery,
                        searchResultCount = state.searchResults.size,
                        searchCurrentIndex = state.currentSearchIndex,
                        onSearchQueryChange = viewModel::search,
                        onSearchNext = viewModel::nextSearchResult,
                        onSearchPrev = viewModel::previousSearchResult,
                        onSearchClose = viewModel::toggleSearch,
                        onBack = onBack,
                        onSearch = viewModel::toggleSearch,
                        onNightModeToggle = viewModel::toggleNightMode,
                        onPageModeToggle = {
                            val newMode = if (state.pageMode == PageMode.CONTINUOUS) {
                                PageMode.SINGLE
                            } else PageMode.CONTINUOUS
                            viewModel.setPageMode(newMode)
                        },
                        onToggleBookmark = {
                            viewModel.toggleBookmark(uri.toString(), state.currentPage)
                        },
                        onShowBookmarks = { showBookmarkSheet = true }
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (state.pageMode == PageMode.CONTINUOUS) {
                    ContinuousScrollMode(
                        totalPages = state.totalPages,
                        pageBitmaps = pageBitmaps,
                        pageAspectRatios = pageAspectRatios,
                        nightMode = state.isNightMode,
                        selectionPageIndex = state.selectionPageIndex,
                        selectionLineBounds = state.selectionLineBounds,
                        hasSelection = state.isTextSelected,
                        annotations = state.annotations,
                        isSearchActive = state.isSearchActive,
                        searchResults = state.searchResults,
                        currentSearchIndex = state.currentSearchIndex,
                        scrollToPage = scrollToPage,
                        onScrollToPageConsumed = viewModel::onScrollToPageConsumed,
                        pageErrors = pageErrors,
                        pdfPageDimensions = pdfPageDimensions,
                        onPageVisible = { viewModel.setCurrentPage(it) },
                        onLongPress = { pageIndex, offset, displayW, displayH ->
                            viewModel.onLongPressScreen(pageIndex, offset.x, offset.y, displayW, displayH)
                        },
                        onStartHandleDrag = { offset, displayW, displayH ->
                            viewModel.onHandleDragScreen(true, offset.x, offset.y, displayW, displayH)
                        },
                        onEndHandleDrag = { offset, displayW, displayH ->
                            viewModel.onHandleDragScreen(false, offset.x, offset.y, displayW, displayH)
                        },
                        onDragEnd = viewModel::onHandleDragEnd,
                        onTapOutside = viewModel::clearSelection,
                        onSingleTap = viewModel::toggleToolbar,
                        onSelectionPagePositioned = { selectionPageRootOffset = it }
                    )
                } else {
                    SinglePageMode(
                        totalPages = state.totalPages,
                        currentPage = state.currentPage,
                        pageBitmaps = pageBitmaps,
                        pageAspectRatios = pageAspectRatios,
                        nightMode = state.isNightMode,
                        selectionPageIndex = state.selectionPageIndex,
                        selectionLineBounds = state.selectionLineBounds,
                        hasSelection = state.isTextSelected,
                        annotations = state.annotations,
                        isSearchActive = state.isSearchActive,
                        searchResults = state.searchResults,
                        currentSearchIndex = state.currentSearchIndex,
                        pageErrors = pageErrors,
                        pdfPageDimensions = pdfPageDimensions,
                        onPageChanged = { viewModel.setCurrentPage(it) },
                        onLongPress = { pageIndex, offset, displayW, displayH ->
                            viewModel.onLongPressScreen(pageIndex, offset.x, offset.y, displayW, displayH)
                        },
                        onStartHandleDrag = { offset, displayW, displayH ->
                            viewModel.onHandleDragScreen(true, offset.x, offset.y, displayW, displayH)
                        },
                        onEndHandleDrag = { offset, displayW, displayH ->
                            viewModel.onHandleDragScreen(false, offset.x, offset.y, displayW, displayH)
                        },
                        onDragEnd = viewModel::onHandleDragEnd,
                        onTapOutside = viewModel::clearSelection,
                        onSingleTap = viewModel::toggleToolbar,
                        onSelectionPagePositioned = { selectionPageRootOffset = it }
                    )
                }

                PageIndicatorBadge(
                    currentPage = state.currentPage + 1,
                    totalPages = state.totalPages,
                    modifier = Modifier.align(Alignment.BottomStart)
                )

                PageThumbnailStrip(
                    visible = state.showThumbnails,
                    totalPages = state.totalPages,
                    currentPage = state.currentPage,
                    onPageSelected = { viewModel.jumpToPage(it) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                    uri = uri,
                    context = LocalContext.current
                )



                state.tappedAnnotationId?.let { annotationId ->
                    val annotation = state.annotations.find { it.id == annotationId }
                    if (annotation != null) {
                        AnnotationMiniPopup(
                            annotationColor = annotation.color,
                            screenX = state.selectionAnchorScreenX,
                            screenY = state.selectionAnchorScreenY,
                            onColorChange = { viewModel.updateAnnotationColor(annotationId, it) },
                            onDelete = {
                                viewModel.removeAnnotation(annotationId)
                                viewModel.dismissAnnotationPopup()
                            },
                            onDismiss = viewModel::dismissAnnotationPopup
                        )
                    }
                }

                // System floating text selection toolbar (Copy, Select All, Share, Search, etc.)
                SystemTextSelectionToolbar(
                    isVisible = state.isTextSelected && !state.isDraggingHandle,
                    selectedText = state.selectedText,
                    anchorX = state.selectionAnchorScreenX + selectionPageRootOffset.x.toInt(),
                    anchorY = state.selectionAnchorScreenY + selectionPageRootOffset.y.toInt(),
                    onCopy = viewModel::onCopyText,
                    onSelectAll = viewModel::onSelectAll,
                    onShare = viewModel::onShareText,
                    onDismiss = viewModel::clearSelection
                )

                if (onEditMode != null && state.showToolbar) {
                    FloatingActionButton(
                        onClick = { onEditMode(state.currentPage, null) },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit_pdf))
                    }
                }
                    } // Box
                } // Column
            }
        }

        if (showBookmarkSheet) {
            BookmarkSheet(
                bookmarks = bookmarks,
                onNavigate = { pageIndex ->
                    viewModel.jumpToPage(pageIndex)
                    showBookmarkSheet = false
                },
                onDelete = { bookmark ->
                    viewModel.toggleBookmark(bookmark.pdfUri, bookmark.pageIndex)
                },
                onDismiss = { showBookmarkSheet = false }
            )
        }

        if (state.showSaveDialog) {
            SaveAnnotationsDialog(
                onSave = viewModel::saveAnnotations,
                onDiscard = viewModel::discardAnnotations,
                onCancel = viewModel::dismissSaveDialog
            )
        }
    }
}

@Composable
private fun ContinuousScrollMode(
    totalPages: Int,
    pageBitmaps: Map<Int, android.graphics.Bitmap>,
    pageAspectRatios: Map<Int, Float>,
    nightMode: Boolean,
    selectionPageIndex: Int,
    selectionLineBounds: List<RectF>,
    hasSelection: Boolean,
    annotations: List<QuickAnnotation>,
    isSearchActive: Boolean,
    searchResults: List<SearchResult>,
    currentSearchIndex: Int,
    scrollToPage: Int,
    onScrollToPageConsumed: () -> Unit,
    pageErrors: Map<Int, String>,
    pdfPageDimensions: Map<Int, Pair<Float, Float>>,
    onPageVisible: (Int) -> Unit,
    onLongPress: (pageIndex: Int, screenOffset: Offset, displayW: Float, displayH: Float) -> Unit,
    onStartHandleDrag: (screenOffset: Offset, displayW: Float, displayH: Float) -> Unit,
    onEndHandleDrag: (screenOffset: Offset, displayW: Float, displayH: Float) -> Unit,
    onDragEnd: () -> Unit,
    onTapOutside: () -> Unit,
    onSingleTap: () -> Unit,
    onSelectionPagePositioned: (Offset) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val currentPage by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex }
    }

    // Pre-compute per-page search result groups and global index offsets
    val searchByPage = remember(searchResults) {
        searchResults.groupBy { it.pageIndex }
    }
    val searchGlobalOffsets = remember(searchResults) {
        val offsets = mutableMapOf<Int, Int>()
        searchResults.forEachIndexed { idx, result ->
            offsets.putIfAbsent(result.pageIndex, idx)
        }
        offsets
    }
    // Pre-filter annotations by page to avoid per-item filtering during scroll
    val annotationsByPage = remember(annotations) {
        annotations.groupBy { it.pageIndex }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { currentPage }
            .distinctUntilChanged()
            .collect { onPageVisible(it) }
    }

    // Auto-scroll to page when search navigates
    LaunchedEffect(scrollToPage) {
        if (scrollToPage >= 0) {
            lazyListState.animateScrollToItem(scrollToPage)
            onScrollToPageConsumed()
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(totalPages, key = { it }) { pageIndex ->
            val dims = pdfPageDimensions[pageIndex]
            var displaySize by remember { mutableStateOf(IntSize.Zero) }
            val pageResults = searchByPage[pageIndex] ?: emptyList()
            val globalOffset = searchGlobalOffsets[pageIndex] ?: 0

            PdfPageView(
                pageIndex = pageIndex,
                bitmap = pageBitmaps[pageIndex],
                aspectRatio = pageAspectRatios[pageIndex] ?: (8.5f / 11f),
                nightMode = nightMode,
                selectionPageIndex = selectionPageIndex,
                selectionLineBounds = selectionLineBounds,
                hasSelection = hasSelection,
                annotations = annotationsByPage[pageIndex] ?: emptyList(),
                isSearchActive = isSearchActive,
                pageSearchResults = pageResults,
                currentSearchIndex = currentSearchIndex,
                searchGlobalIndexOffset = globalOffset,
                pdfPageWidth = dims?.first ?: 0f,
                pdfPageHeight = dims?.second ?: 0f,
                pageError = pageErrors[pageIndex],
                onLongPress = { offset ->
                    if (displaySize.width > 0) {
                        onLongPress(pageIndex, offset,
                            displaySize.width.toFloat(), displaySize.height.toFloat())
                    }
                },
                onStartHandleDrag = { offset ->
                    if (displaySize.width > 0) onStartHandleDrag(offset,
                        displaySize.width.toFloat(), displaySize.height.toFloat())
                },
                onEndHandleDrag = { offset ->
                    if (displaySize.width > 0) onEndHandleDrag(offset,
                        displaySize.width.toFloat(), displaySize.height.toFloat())
                },
                onDragEnd = onDragEnd,
                onTapOutside = onTapOutside,
                onSingleTap = onSingleTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { displaySize = it }
                    .let { mod ->
                        if (pageIndex == selectionPageIndex && hasSelection) {
                            mod.onGloballyPositioned { coords ->
                                onSelectionPagePositioned(coords.positionInRoot())
                            }
                        } else mod
                    }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SinglePageMode(
    totalPages: Int,
    currentPage: Int,
    pageBitmaps: Map<Int, android.graphics.Bitmap>,
    pageAspectRatios: Map<Int, Float>,
    nightMode: Boolean,
    selectionPageIndex: Int,
    selectionLineBounds: List<RectF>,
    hasSelection: Boolean,
    annotations: List<QuickAnnotation>,
    isSearchActive: Boolean,
    searchResults: List<SearchResult>,
    currentSearchIndex: Int,
    pageErrors: Map<Int, String>,
    pdfPageDimensions: Map<Int, Pair<Float, Float>>,
    onPageChanged: (Int) -> Unit,
    onLongPress: (pageIndex: Int, screenOffset: Offset, displayW: Float, displayH: Float) -> Unit,
    onStartHandleDrag: (screenOffset: Offset, displayW: Float, displayH: Float) -> Unit,
    onEndHandleDrag: (screenOffset: Offset, displayW: Float, displayH: Float) -> Unit,
    onDragEnd: () -> Unit,
    onTapOutside: () -> Unit,
    onSingleTap: () -> Unit,
    onSelectionPagePositioned: (Offset) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = currentPage) { totalPages }

    // Pre-compute per-page search data
    val searchByPage = remember(searchResults) {
        searchResults.groupBy { it.pageIndex }
    }
    val searchGlobalOffsets = remember(searchResults) {
        val offsets = mutableMapOf<Int, Int>()
        searchResults.forEachIndexed { idx, result ->
            offsets.putIfAbsent(result.pageIndex, idx)
        }
        offsets
    }
    val annotationsByPage = remember(annotations) {
        annotations.groupBy { it.pageIndex }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { onPageChanged(it) }
    }

    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage) {
            pagerState.animateScrollToPage(currentPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { pageIndex ->
        val dims = pdfPageDimensions[pageIndex]
        var displaySize by remember { mutableStateOf(IntSize.Zero) }
        val pageResults = searchByPage[pageIndex] ?: emptyList()
        val globalOffset = searchGlobalOffsets[pageIndex] ?: 0

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            PdfPageView(
                pageIndex = pageIndex,
                bitmap = pageBitmaps[pageIndex],
                aspectRatio = pageAspectRatios[pageIndex] ?: (8.5f / 11f),
                nightMode = nightMode,
                selectionPageIndex = selectionPageIndex,
                selectionLineBounds = selectionLineBounds,
                hasSelection = hasSelection,
                annotations = annotationsByPage[pageIndex] ?: emptyList(),
                isSearchActive = isSearchActive,
                pageSearchResults = pageResults,
                currentSearchIndex = currentSearchIndex,
                searchGlobalIndexOffset = globalOffset,
                pdfPageWidth = dims?.first ?: 0f,
                pdfPageHeight = dims?.second ?: 0f,
                pageError = pageErrors[pageIndex],
                onLongPress = { offset ->
                    if (displaySize.width > 0) {
                        onLongPress(pageIndex, offset,
                            displaySize.width.toFloat(), displaySize.height.toFloat())
                    }
                },
                onStartHandleDrag = { offset ->
                    if (displaySize.width > 0) onStartHandleDrag(offset,
                        displaySize.width.toFloat(), displaySize.height.toFloat())
                },
                onEndHandleDrag = { offset ->
                    if (displaySize.width > 0) onEndHandleDrag(offset,
                        displaySize.width.toFloat(), displaySize.height.toFloat())
                },
                onDragEnd = onDragEnd,
                onTapOutside = onTapOutside,
                onSingleTap = onSingleTap,
                modifier = Modifier
                    .onSizeChanged { displaySize = it }
                    .let { mod ->
                        if (pageIndex == selectionPageIndex && hasSelection) {
                            mod.onGloballyPositioned { coords ->
                                onSelectionPagePositioned(coords.positionInRoot())
                            }
                        } else mod
                    }
            )
        }
    }
}
