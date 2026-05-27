package com.pdfapp.reader.ui.editmode

import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.model.AnnotateTool
import com.pdfapp.reader.domain.model.EditAnnotation
import com.pdfapp.reader.domain.model.EditTab
import com.pdfapp.reader.domain.usecase.ExtractTextBlocksUseCase
import com.pdfapp.reader.domain.usecase.SaveAnnotatedPdfUseCase
import com.pdfapp.reader.domain.usecase.SaveOutputMode
import com.pdfapp.reader.data.repository.SignatureRepository
import com.pdfapp.reader.ui.editmode.components.SaveOptionsBottomSheet
import com.pdfapp.reader.ui.editmode.components.OverwriteConfirmDialog
import com.pdfapp.reader.ui.editmode.components.SaveAsNameDialog
import com.pdfapp.reader.ui.editmode.components.SavingOverlay
import com.pdfapp.reader.util.FileUtils
import com.pdfapp.reader.ui.editmode.annotate.AnnotateTabViewModel
import kotlinx.coroutines.launch
import com.pdfapp.reader.ui.editmode.annotate.AnnotateToolbar
import com.pdfapp.reader.ui.editmode.components.PageNavigationBar
import com.pdfapp.reader.ui.editmode.fillsign.AddPageDialog
import com.pdfapp.reader.ui.editmode.fillsign.FillSignTabViewModel
import com.pdfapp.reader.ui.editmode.fillsign.FillSignTool
import com.pdfapp.reader.ui.editmode.fillsign.FillSignToolbar
import com.pdfapp.reader.ui.editmode.fillsign.SignatureCreationDialog
import com.pdfapp.reader.ui.editmode.fillsign.TextCreationBottomSheet
import com.pdfapp.reader.ui.editmode.mark.MarkTabViewModel
import com.pdfapp.reader.ui.editmode.mark.MarkTool
import com.pdfapp.reader.ui.editmode.mark.MarkToolbar

/**
 * Main Edit Mode screen with 3-tab layout (Mark, Annotate, Fill & Sign).
 * Renders a single PDF page in fixed-fit mode with page navigation bar.
 */
@Composable
fun EditModeScreen(
    uri: Uri,
    coordinator: EditModeCoordinator,
    extractTextBlocksUseCase: ExtractTextBlocksUseCase,
    signatureRepository: SignatureRepository,
    saveAnnotatedPdfUseCase: SaveAnnotatedPdfUseCase,
    onBack: () -> Unit,
    onSaveComplete: (Uri) -> Unit = {},
    initialPageIndex: Int? = null
) {
    val annotations by coordinator.annotations.collectAsStateWithLifecycle()
    val selectedTab by coordinator.selectedTab.collectAsStateWithLifecycle()
    val selectedElementId by coordinator.selectedElementId.collectAsStateWithLifecycle()
    val canUndo by coordinator.undoRedoManager.canUndo.collectAsStateWithLifecycle()
    val canRedo by coordinator.undoRedoManager.canRedo.collectAsStateWithLifecycle()
    val isLoading by coordinator.isLoading.collectAsStateWithLifecycle()
    val isSaving by coordinator.isSaving.collectAsStateWithLifecycle()
    val pageBitmaps by coordinator.pageBitmaps.collectAsStateWithLifecycle()
    val pageDimensions by coordinator.pageDimensions.collectAsStateWithLifecycle()
    val pageCount by coordinator.pageCount.collectAsStateWithLifecycle()
    val currentPageIndex by coordinator.currentPageIndex.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val markViewModel = remember {
        MarkTabViewModel(coordinator, extractTextBlocksUseCase, coroutineScope)
    }
    val activeTool by markViewModel.activeTool.collectAsStateWithLifecycle()
    val markColor by markViewModel.markColor.collectAsStateWithLifecycle()
    val markOpacity by markViewModel.markOpacity.collectAsStateWithLifecycle()
    val selectionState by markViewModel.selectionState.collectAsStateWithLifecycle()
    val selectionPageIndex by markViewModel.selectionPageIndex.collectAsStateWithLifecycle()
    val noTextAvailable by markViewModel.noTextAvailable.collectAsStateWithLifecycle()
    val isDragging by markViewModel.isDragging.collectAsStateWithLifecycle()
    val dragTouchPosition by markViewModel.dragTouchPosition.collectAsStateWithLifecycle()
    val showApplyPopup by markViewModel.showApplyPopup.collectAsStateWithLifecycle()

    // -- Fill & Sign ViewModel & state --
    val fillSignViewModel = remember {
        FillSignTabViewModel(coordinator, signatureRepository, coroutineScope)
    }
    // -- Image picker launcher for Fill & Sign "Add Image" --
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val dims = pageDimensions[currentPageIndex]
            if (dims != null) {
                fillSignViewModel.placeImage(context, uri, currentPageIndex, dims.first, dims.second)
            }
        }
    }

    // -- PDF picker launcher for "Add Page" → Import PDF --
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            fillSignViewModel.insertPdfPages(context, uri)
        }
    }
    // -- Image picker launcher for "Add Page" → Import Image --
    val pageImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fillSignViewModel.insertImagePage(context, uri)
        }
    }

    val fillSignActiveTool by fillSignViewModel.activeTool.collectAsStateWithLifecycle()
    val fillSignSelectedId by fillSignViewModel.selectedElementId.collectAsStateWithLifecycle()
    val showSignatureDialog by fillSignViewModel.showCreateDialog.collectAsStateWithLifecycle()
    val savedSignatures by fillSignViewModel.savedSignatures.collectAsStateWithLifecycle()
    val saveFailedMax by fillSignViewModel.saveFailedMaxReached.collectAsStateWithLifecycle()
    val showTextSheet by fillSignViewModel.showTextSheet.collectAsStateWithLifecycle()
    val editingTextElementId by fillSignViewModel.editingTextElementId.collectAsStateWithLifecycle()
    val showLinkDialog by fillSignViewModel.showLinkDialog.collectAsStateWithLifecycle()
    val showAddPageDialog by fillSignViewModel.showAddPageDialog.collectAsStateWithLifecycle()

    // -- Annotate ViewModel & state --
    val annotateViewModel = remember { AnnotateTabViewModel(coordinator) }
    val annotateActiveTool by annotateViewModel.activeTool.collectAsStateWithLifecycle()
    val annotateStrokeColor by annotateViewModel.strokeColor.collectAsStateWithLifecycle()
    val annotateStrokeWidth by annotateViewModel.strokeWidth.collectAsStateWithLifecycle()
    val annotateFillEnabled by annotateViewModel.fillEnabled.collectAsStateWithLifecycle()
    val annotateFillColor by annotateViewModel.fillColor.collectAsStateWithLifecycle()
    val annotateCurrentPoints by annotateViewModel.currentPoints.collectAsStateWithLifecycle()
    val annotateShapeStart by annotateViewModel.shapeStartPoint.collectAsStateWithLifecycle()
    val annotateShapeCurrent by annotateViewModel.shapeCurrentPoint.collectAsStateWithLifecycle()
    val annotateSelectedId by annotateViewModel.selectedAnnotationId.collectAsStateWithLifecycle()
    val annotateSelectedGroupIds by annotateViewModel.selectedAnnotationIds.collectAsStateWithLifecycle()
    val annotateRegionSelectRect by annotateViewModel.regionSelectRect.collectAsStateWithLifecycle()

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showMarkSettings by remember { mutableStateOf(false) }
    var showAnnotateSettings by remember { mutableStateOf(false) }

    // -- Save flow state --
    var showSaveOptions by remember { mutableStateOf(false) }
    var showOverwriteConfirm by remember { mutableStateOf(false) }
    var showSaveAsName by remember { mutableStateOf(false) }
    var pendingSaveUri by remember { mutableStateOf<Uri?>(null) }

    var showMarkNoteDialog by remember { mutableStateOf(false) }
    var noteEditAnnotationId by remember { mutableStateOf<String?>(null) }
    var currentNoteText by remember { mutableStateOf("") }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // Navigate after save completes — decoupled from save coroutine to avoid lifecycle race
    LaunchedEffect(pendingSaveUri) {
        pendingSaveUri?.let { savedUri -> onSaveComplete(savedUri) }
    }

    /** Execute save with the given output mode. NonCancellable wraps only file I/O. */
    fun executeSave(outputMode: SaveOutputMode) {
        if (isSaving) return // prevent concurrent saves
        coroutineScope.launch {
            coordinator.setSaving(true)
            val bitmapSizes = pageBitmaps.mapValues { (_, bmp) -> bmp.width to bmp.height }
            // NonCancellable only wraps the file operation to prevent PDF corruption
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                saveAnnotatedPdfUseCase.save(
                    sourceUri = uri,
                    annotations = annotations,
                    pageDimensions = pageDimensions,
                    pageBitmapSizes = bitmapSizes,
                    outputMode = outputMode,
                    insertedPages = coordinator.insertedPages.value
                )
            }
            coordinator.setSaving(false)
            result.fold(
                onSuccess = { savedUri ->
                    Toast.makeText(context, R.string.edit_save_success, Toast.LENGTH_SHORT).show()
                    // Set state to trigger navigation via LaunchedEffect — coroutine completes first
                    pendingSaveUri = savedUri
                },
                onFailure = {
                    Toast.makeText(context, R.string.edit_save_error, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Load PDF pages (bitmaps + dimensions) for Edit Mode
    LaunchedEffect(uri) { loadPdfForEditMode(context, uri, coordinator) }

    // Set initial page index
    LaunchedEffect(pageCount, initialPageIndex) {
        if (initialPageIndex != null && initialPageIndex in 0 until pageCount) {
            coordinator.setCurrentPage(initialPageIndex)
        }
    }

    // Clear selection first, then load chars for current page (merged to avoid race)
    // Auto-select default tool when switching tabs for immediate usability
    LaunchedEffect(selectedTab, currentPageIndex) {
        // Clear all selections on page/tab change — stickers stay on their page
        markViewModel.clearSelection()
        coordinator.selectElement(null)
        fillSignViewModel.clearSelection()
        annotateViewModel.clearGroupSelection()
        when (selectedTab) {
            EditTab.MARK -> {
                markViewModel.loadCharsForPage(uri, currentPageIndex)
                if (markViewModel.activeTool.value == MarkTool.NONE) {
                    markViewModel.selectTool(MarkTool.HIGHLIGHT)
                }
                annotateViewModel.selectTool(AnnotateTool.NONE)
                fillSignViewModel.selectTool(FillSignTool.NONE)
            }
            EditTab.ANNOTATE -> {
                if (annotateViewModel.activeTool.value == AnnotateTool.NONE) {
                    annotateViewModel.selectTool(AnnotateTool.RECTANGLE)
                }
                fillSignViewModel.selectTool(FillSignTool.NONE)
            }
            EditTab.FILL_SIGN -> {
                annotateViewModel.selectTool(AnnotateTool.NONE)
            }
        }
    }

    // Show toast when no text available
    LaunchedEffect(noTextAvailable) {
        if (noTextAvailable) {
            Toast.makeText(context, R.string.mark_no_selectable_text, Toast.LENGTH_SHORT).show()
            markViewModel.dismissNoTextMessage()
        }
    }

    // Haptic feedback
    val haptic = LocalHapticFeedback.current
    val wordBoundaryCrossed by markViewModel.wordBoundaryCrossed.collectAsStateWithLifecycle()

    // Haptic on initial word selection (long-press)
    LaunchedEffect(selectionState) {
        if (selectionState != null && !isDragging) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Haptic on word boundary crossing during handle drag
    LaunchedEffect(wordBoundaryCrossed) {
        if (wordBoundaryCrossed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            markViewModel.consumeWordBoundaryCrossed()
        }
    }

    // Haptic on annotate tool selection
    LaunchedEffect(annotateActiveTool) {
        if (annotateActiveTool != AnnotateTool.NONE) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Toast when max signatures reached
    LaunchedEffect(saveFailedMax) {
        if (saveFailedMax) {
            Toast.makeText(
                context,
                context.getString(R.string.fillsign_max_reached, 5),
                Toast.LENGTH_SHORT
            ).show()
            fillSignViewModel.consumeSaveFailedEvent()
        }
    }

    DisposableEffect(Unit) { onDispose { coordinator.dispose() } }

    val handleClose: () -> Unit = {
        if (coordinator.hasChanges()) showDiscardDialog = true
        else onBack()
    }

    BackHandler { handleClose() }

    val currentSelectionColor = markColor

    val isSelectionPage = selectionPageIndex == currentPageIndex

    Column(modifier = Modifier.fillMaxSize()) {
        EditModeTopToolbar(
            canUndo = canUndo, canRedo = canRedo, isSaving = isSaving,
            onClose = handleClose,
            onUndo = coordinator::applyUndo,
            onRedo = coordinator::applyRedo,
            onSave = {
                if (coordinator.hasChanges()) showSaveOptions = true
                else onBack()
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFFE8E8E8)) // Light gray background to contrast white PDF page
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                // -- Pinch-to-zoom state (2-finger only, 1x–3x) --
                var zoomScale by remember { mutableFloatStateOf(1f) }
                var zoomOffsetX by remember { mutableFloatStateOf(0f) }
                var zoomOffsetY by remember { mutableFloatStateOf(0f) }

                // Reset zoom on page change
                LaunchedEffect(currentPageIndex) {
                    zoomScale = 1f; zoomOffsetX = 0f; zoomOffsetY = 0f
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTwoFingerZoomGesture { zoom, pan ->
                                val newScale = (zoomScale * zoom).coerceIn(1f, 3f)
                                zoomScale = newScale
                                if (newScale > 1f) {
                                    val maxX = size.width * (newScale - 1) / 2f
                                    val maxY = size.height * (newScale - 1) / 2f
                                    zoomOffsetX = (zoomOffsetX + pan.x).coerceIn(-maxX, maxX)
                                    zoomOffsetY = (zoomOffsetY + pan.y).coerceIn(-maxY, maxY)
                                } else {
                                    zoomOffsetX = 0f; zoomOffsetY = 0f
                                }
                            }
                        }
                ) {

                val dims = pageDimensions[currentPageIndex]
                EditablePdfPageView(
                    pageBitmap = pageBitmaps[currentPageIndex],
                    pageIndex = currentPageIndex,
                    annotations = annotations.filter { it.pageIndex == currentPageIndex },
                    selectedElementId = selectedElementId,
                    pdfPageWidth = dims?.first ?: 0f,
                    pdfPageHeight = dims?.second ?: 0f,
                    selectionState = if (selectedTab == EditTab.MARK && isSelectionPage) selectionState else null,
                    selectionColor = currentSelectionColor,
                    isDragging = isDragging,
                    dragTouchPosition = dragTouchPosition,
                    onLongPress = if (selectedTab == EditTab.MARK && activeTool != MarkTool.NONE && activeTool != MarkTool.NOTE)
                        { pi, x, y -> markViewModel.onLongPress(pi, x, y) } else null,
                    onTap = if (selectedTab == EditTab.MARK)
                        { pi, x, y -> markViewModel.onPageTap(pi, x, y) } else null,
                    onStartHandleDrag = if (isSelectionPage && selectionState != null)
                        { dx, dy -> markViewModel.onHandleDrag(currentPageIndex, dx, dy, true) } else null,
                    onEndHandleDrag = if (isSelectionPage && selectionState != null)
                        { dx, dy -> markViewModel.onHandleDrag(currentPageIndex, dx, dy, false) } else null,
                    onHandleDragStart = { markViewModel.onDragStart() },
                    onHandleDragMove = { pos -> markViewModel.onDragMove(pos) },
                    onHandleDragEnd = if (isSelectionPage) { { markViewModel.onDragEnd() } } else null,
                    onMarkPageDragStart = if (selectedTab == EditTab.MARK && activeTool != MarkTool.NONE && activeTool != MarkTool.NOTE)
                        { pi, x, y -> markViewModel.onPageDragStart(pi, x, y) } else null,
                    onMarkPageDragMove = if (selectedTab == EditTab.MARK && activeTool != MarkTool.NONE && activeTool != MarkTool.NOTE)
                        { pi, x, y -> markViewModel.onPageDragMove(pi, x, y) } else null,
                    onMarkPageDragEnd = if (selectedTab == EditTab.MARK && activeTool != MarkTool.NONE && activeTool != MarkTool.NOTE)
                        { pi -> markViewModel.onPageDragEnd(pi) } else null,
                    showApplyPopup = showApplyPopup && isSelectionPage,
                    activeToolName = when (activeTool) {
                        MarkTool.HIGHLIGHT -> context.getString(R.string.tool_highlight)
                        MarkTool.UNDERLINE -> context.getString(R.string.tool_underline)
                        MarkTool.STRIKETHROUGH -> context.getString(R.string.tool_strikethrough)
                        MarkTool.NOTE -> "" // No drag gesture for note tool
                        MarkTool.NONE -> ""
                    },
                    activeToolColor = currentSelectionColor,
                    onApplyTool = { markViewModel.applyCurrentTool(currentPageIndex) },
                    onDismissPopup = { markViewModel.dismissPopup() },
                    onMarkColorChange = { id, color -> markViewModel.changeAnnotationColor(id, color) },
                    onMarkCopy = { id ->
                        val text = markViewModel.getAnnotationText(id)
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                    },
                    onMarkNote = { id ->
                        noteEditAnnotationId = id
                        val ann = annotations.find { it.id == id }
                        currentNoteText = when (ann) {
                            is EditAnnotation.Highlight -> ann.note ?: ""
                            is EditAnnotation.Underline -> ann.note ?: ""
                            is EditAnnotation.Strikethrough -> ann.note ?: ""
                            is EditAnnotation.StickyNote -> ann.text
                            else -> ""
                        }
                        showMarkNoteDialog = true
                    },
                    onMarkDelete = { id -> markViewModel.deleteAnnotation(id) },
                    onMarkDismissPopup = {
                        coordinator.selectElement(null)
                        markViewModel.clearSelection()
                    },
                    // -- Annotate tab wiring --
                    isAnnotateTabActive = selectedTab == EditTab.ANNOTATE,
                    annotateActiveTool = annotateActiveTool,
                    currentDrawingPoints = annotateCurrentPoints,
                    shapeStartPoint = annotateShapeStart,
                    shapeCurrentPoint = annotateShapeCurrent,
                    drawingStrokeColor = annotateStrokeColor,
                    drawingStrokeWidth = annotateStrokeWidth,
                    onAnnotateDrawStart = { pi, x, y -> annotateViewModel.onDrawStart(pi, x, y) },
                    onAnnotateDrawMove = { x, y -> annotateViewModel.onDrawMove(x, y) },
                    onAnnotateDrawEnd = { annotateViewModel.onDrawEnd() },
                    onAnnotateTap = { pi, x, y -> annotateViewModel.onAnnotationTap(pi, x, y) },
                    onAnnotateRegionSelectStart = { pi, x, y -> annotateViewModel.onRegionSelectStart(pi, x, y) },
                    onAnnotateRegionSelectMove = { x, y -> annotateViewModel.onRegionSelectMove(x, y) },
                    onAnnotateRegionSelectEnd = { annotateViewModel.onRegionSelectEnd() },
                    annotateRegionSelectRect = annotateRegionSelectRect,
                    selectedAnnotateAnnotation = annotations.find { it.id == annotateSelectedId },
                    selectedAnnotateGroup = annotations.filter { it.id in annotateSelectedGroupIds },
                    onAnnotateGroupMove = { dx, dy -> annotateViewModel.moveSelectedGroup(dx, dy) },
                    onAnnotateGroupMoveEnd = { tdx, tdy -> annotateViewModel.commitGroupMove(tdx, tdy) },
                    onAnnotateGroupDelete = { annotateViewModel.deleteSelectedGroup() },
                    onAnnotateMove = { dx, dy ->
                        annotateSelectedId?.let { annotateViewModel.moveAnnotation(it, dx, dy) }
                    },
                    onAnnotateResize = { newBounds ->
                        annotateSelectedId?.let { annotateViewModel.resizeAnnotation(it, newBounds) }
                    },
                    onAnnotateDelete = { annotateViewModel.deleteSelectedAnnotation() },
                    // -- Fill & Sign tab wiring --
                    isFillSignTabActive = selectedTab == EditTab.FILL_SIGN,
                    onFillSignTap = { pi, x, y -> fillSignViewModel.onElementTap(pi, x, y) },
                    selectedFillSignAnnotation = annotations.find { it.id == fillSignSelectedId },
                    onFillSignMove = { dx, dy ->
                        fillSignSelectedId?.let { fillSignViewModel.moveElement(it, dx, dy) }
                    },
                    onFillSignResizeAndRotate = { newBounds, rotation ->
                        fillSignSelectedId?.let { fillSignViewModel.resizeAndRotateElement(it, newBounds, rotation) }
                    },
                    onFillSignDelete = { fillSignViewModel.deleteSelectedElement() },
                    // Tap on selected sticker body → re-edit for TextElement
                    onFillSignTapBody = {
                        val selId = fillSignSelectedId
                        val ann = if (selId != null) annotations.find { it.id == selId } else null
                        if (ann is EditAnnotation.TextElement) {
                            fillSignViewModel.openTextSheetForEdit(ann.id)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .shadow(4.dp, RoundedCornerShape(2.dp))
                        .background(Color.White, RoundedCornerShape(2.dp))
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = zoomOffsetX
                            translationY = zoomOffsetY
                        }
                )

                } // end zoom Box
            }
        }

        // Page navigation bar (between page content and toolbar)
        if (pageCount > 1) {
            PageNavigationBar(
                currentPage = currentPageIndex,
                pageCount = pageCount,
                onPrevious = coordinator::previousPage,
                onNext = coordinator::nextPage,
                onPageJump = coordinator::setCurrentPage
            )
        }

        // Tab-specific toolbar
        when (selectedTab) {
            EditTab.MARK -> MarkToolbar(
                activeTool = activeTool,
                markColor = markColor,
                onToolSelected = markViewModel::selectTool,
                onSettingsRequested = { showMarkSettings = true }
            )
            EditTab.ANNOTATE -> AnnotateToolbar(
                activeTool = annotateActiveTool,
                strokeColor = annotateStrokeColor,
                onToolSelected = annotateViewModel::selectTool,
                onSettingsRequested = { showAnnotateSettings = true }
            )
            EditTab.FILL_SIGN -> FillSignToolbar(
                activeTool = fillSignActiveTool,
                savedSignatures = savedSignatures,
                onToolSelected = { tool ->
                    fillSignViewModel.selectTool(tool) // Clear selection + set active tool
                    if (tool == FillSignTool.TEXT) fillSignViewModel.openTextSheet()
                },
                onAddSignature = { fillSignViewModel.openCreateDialog() },
                onPlaceSignature = { entity ->
                    val dims = pageDimensions[currentPageIndex]
                    if (dims != null) {
                        fillSignViewModel.placeSignature(
                            entity, currentPageIndex, dims.first, dims.second
                        )
                    }
                },
                onDeleteSignature = { id -> fillSignViewModel.deleteSignature(id) },
                onPickImage = {
                    fillSignViewModel.selectTool(FillSignTool.IMAGE) // Hide signature gallery
                    imagePickerLauncher.launch("image/*")
                },
                onPlaceDateStamp = {
                    fillSignViewModel.selectTool(FillSignTool.DATE_STAMP)
                    val dims = pageDimensions[currentPageIndex]
                    if (dims != null) {
                        fillSignViewModel.placeDateStamp(currentPageIndex, dims.first, dims.second)
                    }
                },
                onAddPage = {
                    fillSignViewModel.openAddPageDialog()
                },
                onPlaceStamp = { stampText ->
                    val dims = pageDimensions[currentPageIndex]
                    if (dims != null) {
                        fillSignViewModel.placeStamp(stampText, currentPageIndex, dims.first, dims.second)
                    }
                },
                onLink = {
                    fillSignViewModel.selectTool(FillSignTool.LINK)
                    fillSignViewModel.openLinkDialog()
                }
            )
        }

        EditModeTabSelector(
            selectedTab = selectedTab,
            onTabSelected = coordinator::selectTab
        )
    }

    // Discard dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.edit_discard_title)) },
            text = { Text(stringResource(R.string.edit_discard_message)) },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onBack() }) {
                    Text(stringResource(R.string.edit_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.edit_discard_cancel))
                }
            }
        )
    }

    // Mark tool settings sheet (shared color + optional opacity for Highlight)
    MarkToolSettingsSheet(
        showSettings = showMarkSettings,
        markColor = markColor,
        markOpacity = markOpacity,
        markViewModel = markViewModel,
        onDismiss = { showMarkSettings = false }
    )

    // Annotate settings sheets
    AnnotateToolSettingsSheets(
        showAnnotateSettings = showAnnotateSettings,
        annotateActiveTool = annotateActiveTool,
        annotateStrokeColor = annotateStrokeColor,
        annotateStrokeWidth = annotateStrokeWidth,
        annotateFillEnabled = annotateFillEnabled,
        annotateFillColor = annotateFillColor,
        annotateViewModel = annotateViewModel,
        onDismiss = { showAnnotateSettings = false }
    )

    // Signature creation dialog
    if (showSignatureDialog) {
        SignatureCreationDialog(
            onDismiss = { fillSignViewModel.dismissCreateDialog() },
            onSave = { bitmap, name, color ->
                fillSignViewModel.saveSignature(bitmap, name, color)
            }
        )
    }

    // Text creation/edit bottom sheet
    if (showTextSheet) {
        val editingElement = editingTextElementId?.let { id ->
            annotations.find { it.id == id } as? EditAnnotation.TextElement
        }
        TextCreationBottomSheet(
            initialText = editingElement?.text ?: "",
            initialFontSize = editingElement?.fontSize ?: 16f,
            initialFontFamily = editingElement?.fontFamily ?: "Helvetica",
            initialColor = editingElement?.color ?: 0xFF000000.toInt(),
            initialBold = editingElement?.isBold ?: false,
            initialItalic = editingElement?.isItalic ?: false,
            initialUnderline = editingElement?.isUnderline ?: false,
            initialStrikethrough = editingElement?.isStrikethrough ?: false,
            onDismiss = { fillSignViewModel.dismissTextSheet() },
            onDone = { text, fontSize, fontFamily, color, bold, italic, underline, strikethrough ->
                if (editingElement != null) {
                    fillSignViewModel.updateTextElement(
                        editingElement.id, text, fontSize, fontFamily, color,
                        bold, italic, underline, strikethrough
                    )
                } else {
                    val dims = pageDimensions[currentPageIndex]
                    if (dims != null) {
                        fillSignViewModel.placeText(
                            text, fontSize, fontFamily, color,
                            bold, italic, underline, strikethrough,
                            currentPageIndex, dims.first, dims.second
                        )
                    }
                }
            },
            // Real-time preview: update sticker on PDF as user changes formatting
            onLiveUpdate = if (editingElement != null) { text, fontSize, fontFamily, color, bold, italic, underline, strikethrough ->
                fillSignViewModel.liveUpdateTextElement(
                    editingElement.id, text, fontSize, fontFamily, color,
                    bold, italic, underline, strikethrough
                )
            } else null
        )
    }

    // Link URL input dialog
    if (showLinkDialog) {
        var linkUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { fillSignViewModel.dismissLinkDialog() },
            title = { Text(stringResource(R.string.fillsign_tool_link)) },
            text = {
                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    label = { Text(stringResource(R.string.fillsign_link_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val dims = pageDimensions[currentPageIndex]
                    if (dims != null) {
                        fillSignViewModel.placeLink(linkUrl, currentPageIndex, dims.first, dims.second)
                    }
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { fillSignViewModel.dismissLinkDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Add Page dialog — choose position and source (image or PDF)
    if (showAddPageDialog) {
        val insertAfterIndex by fillSignViewModel.insertAfterIndex.collectAsStateWithLifecycle()
        AddPageDialog(
            currentPageIndex = currentPageIndex,
            pageCount = pageCount,
            insertAfterIndex = insertAfterIndex,
            onInsertAfterChanged = { fillSignViewModel.setInsertAfterIndex(it) },
            onImportImage = {
                pageImagePickerLauncher.launch("image/*")
            },
            onImportPdf = {
                pdfPickerLauncher.launch(arrayOf("application/pdf"))
            },
            onDismiss = { fillSignViewModel.dismissAddPageDialog() }
        )
    }

    // -- Save flow dialogs --
    if (showSaveOptions) {
        SaveOptionsBottomSheet(
            onDismiss = { showSaveOptions = false },
            onSave = { showSaveOptions = false; showOverwriteConfirm = true },
            onSaveAs = { showSaveOptions = false; showSaveAsName = true }
        )
    }
    if (showOverwriteConfirm) {
        OverwriteConfirmDialog(
            onConfirm = { showOverwriteConfirm = false; executeSave(SaveOutputMode.Overwrite(uri)) },
            onDismiss = { showOverwriteConfirm = false }
        )
    }
    if (showSaveAsName) {
        val defaultName = FileUtils.getFileNameWithoutExtension(context.contentResolver, uri)
            ?.let { "${it}_edited" } ?: "edited"
        SaveAsNameDialog(
            defaultName = defaultName,
            onConfirm = { name ->
                showSaveAsName = false
                executeSave(SaveOutputMode.SaveAs(uri, name))
            },
            onDismiss = { showSaveAsName = false }
        )
    }

    if (showMarkNoteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showMarkNoteDialog = false },
            title = { androidx.compose.material3.Text(stringResource(R.string.note_dialog_title)) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = currentNoteText,
                    onValueChange = { currentNoteText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { androidx.compose.material3.Text(stringResource(R.string.note_dialog_hint)) }
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        noteEditAnnotationId?.let { id ->
                            markViewModel.updateAnnotationNote(id, currentNoteText)
                        }
                        showMarkNoteDialog = false
                    }
                ) {
                    androidx.compose.material3.Text(stringResource(R.string.fillsign_save))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showMarkNoteDialog = false }) {
                    androidx.compose.material3.Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (isSaving) { SavingOverlay() }
}

/**
 * Detect 2-finger pinch-to-zoom gesture only.
 * Single-finger gestures pass through to child composables unaffected.
 */
private suspend fun PointerInputScope.detectTwoFingerZoomGesture(
    onGesture: (zoom: Float, pan: Offset) -> Unit
) {
    awaitEachGesture {
        // Wait for first pointer down
        awaitFirstDown(requireUnconsumed = false)
        // Wait until a second pointer appears or all pointers lift
        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) return@awaitEachGesture // all lifted, no zoom
            if (pressed.size >= 2) break // 2 fingers detected → start zoom tracking
        }

        // Initialize tracking from current 2-finger state
        var prevEvent = awaitPointerEvent()
        var prevPressed = prevEvent.changes.filter { it.pressed }
        if (prevPressed.size < 2) return@awaitEachGesture
        var prevCentroid = centroidOf(prevPressed)
        var prevSpread = spreadOf(prevPressed)
        prevEvent.changes.forEach { if (it.positionChanged()) it.consume() }

        // Track ongoing 2-finger gesture
        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }
            if (pressed.size < 2) break

            val centroid = centroidOf(pressed)
            val spread = spreadOf(pressed)

            if (prevSpread > 1f) {
                onGesture(spread / prevSpread, centroid - prevCentroid)
            }

            event.changes.forEach { if (it.positionChanged()) it.consume() }
            prevCentroid = centroid
            prevSpread = spread
        }
    }
}

/** Centroid (average position) of pressed pointers. */
private fun centroidOf(pointers: List<androidx.compose.ui.input.pointer.PointerInputChange>): Offset {
    val x = pointers.map { it.position.x }.average().toFloat()
    val y = pointers.map { it.position.y }.average().toFloat()
    return Offset(x, y)
}

/** Distance between first two pressed pointers. */
private fun spreadOf(pointers: List<androidx.compose.ui.input.pointer.PointerInputChange>): Float {
    if (pointers.size < 2) return 0f
    return (pointers[0].position - pointers[1].position).getDistance()
}
