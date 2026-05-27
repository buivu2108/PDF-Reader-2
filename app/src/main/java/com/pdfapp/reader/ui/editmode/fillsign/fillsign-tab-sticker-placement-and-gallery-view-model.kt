package com.pdfapp.reader.ui.editmode.fillsign

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import com.pdfapp.reader.data.local.entity.SignatureEntity
import com.pdfapp.reader.data.repository.SignatureRepository
import com.pdfapp.reader.domain.model.EditAnnotation
import com.pdfapp.reader.ui.editmode.EditModeCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * ViewModel for Fill & Sign tab. Manages tool selection, saved signature gallery,
 * element placement on PDF, and manipulation (move/resize/rotate/delete).
 */
class FillSignTabViewModel(
    private val coordinator: EditModeCoordinator,
    private val signatureRepository: SignatureRepository,
    private val scope: CoroutineScope
) {
    // -- Tool state --
    private val _activeTool = MutableStateFlow(FillSignTool.NONE)
    val activeTool: StateFlow<FillSignTool> = _activeTool.asStateFlow()

    // -- Saved signatures from Room DB --
    val savedSignatures: StateFlow<List<SignatureEntity>> = signatureRepository.getAll()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    // -- Signature creation dialog visibility --
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    // -- Text bottom sheet state --
    private val _showTextSheet = MutableStateFlow(false)
    val showTextSheet: StateFlow<Boolean> = _showTextSheet.asStateFlow()

    /** When re-editing, holds the ID of the text element being edited. */
    private val _editingTextElementId = MutableStateFlow<String?>(null)
    val editingTextElementId: StateFlow<String?> = _editingTextElementId.asStateFlow()

    // -- Selected placed element on PDF --
    private val _selectedElementId = MutableStateFlow<String?>(null)
    val selectedElementId: StateFlow<String?> = _selectedElementId.asStateFlow()

    /** Deselect the currently selected sticker (e.g. on page change). */
    fun clearSelection() {
        _selectedElementId.value = null
        coordinator.selectElement(null)
    }

    // -- Tool selection (toggle behavior) --

    fun selectTool(tool: FillSignTool) {
        _activeTool.value = if (_activeTool.value == tool) FillSignTool.NONE else tool
        _selectedElementId.value = null
        coordinator.selectElement(null)
    }

    // -- Signature creation dialog --

    fun openCreateDialog() { _showCreateDialog.value = true }
    fun dismissCreateDialog() { _showCreateDialog.value = false }

    // -- Text sheet open/dismiss --

    /** Snapshot of text element before editing, for cancel/restore. */
    private var editingSnapshot: EditAnnotation.TextElement? = null

    fun openTextSheet() { _showTextSheet.value = true }

    fun dismissTextSheet() {
        // Cancel: restore snapshot if editing an existing element
        val snapshot = editingSnapshot
        if (snapshot != null) {
            coordinator.updateAnnotation(snapshot)
        }
        editingSnapshot = null
        _showTextSheet.value = false
        _editingTextElementId.value = null
    }

    fun openTextSheetForEdit(elementId: String) {
        // Save snapshot for cancel/restore
        editingSnapshot = coordinator.annotations.value
            .find { it.id == elementId } as? EditAnnotation.TextElement
        _editingTextElementId.value = elementId
        _showTextSheet.value = true
    }

    // -- Place new text element (centered on page) --

    fun placeText(
        text: String, fontSize: Float, fontFamily: String, color: Int,
        isBold: Boolean, isItalic: Boolean, isUnderline: Boolean, isStrikethrough: Boolean,
        pageIndex: Int, pdfPageWidth: Float, pdfPageHeight: Float
    ) {
        if (text.isBlank()) return
        val placedWidth = pdfPageWidth * 0.4f
        val lineHeight = fontSize * 1.4f
        // Account for both text-wrapping and explicit newlines
        val explicitLines = text.count { it == '\n' } + 1
        val wrappedLines = (text.length * fontSize * 0.6f / placedWidth).coerceAtLeast(1f).toInt()
        val lines = maxOf(explicitLines, wrappedLines)
        val placedHeight = lineHeight * lines + fontSize * 0.5f
        val centerX = pdfPageWidth / 2f
        val centerY = pdfPageHeight / 2f

        val element = EditAnnotation.TextElement(
            pageIndex = pageIndex,
            bounds = RectF(
                centerX - placedWidth / 2f,
                centerY - placedHeight / 2f,
                centerX + placedWidth / 2f,
                centerY + placedHeight / 2f
            ),
            text = text, fontSize = fontSize, fontFamily = fontFamily,
            color = color, isBold = isBold, isItalic = isItalic,
            isUnderline = isUnderline, isStrikethrough = isStrikethrough
        )
        coordinator.addAnnotation(element)
        _selectedElementId.value = element.id
        coordinator.selectElement(element.id)
        _showTextSheet.value = false
    }

    // -- Place date stamp (centered on page, formatted as MM/dd/yyyy) --

    fun placeDateStamp(pageIndex: Int, pdfPageWidth: Float, pdfPageHeight: Float) {
        val dateText = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
        val stampWidth = pdfPageWidth * 0.25f
        val stampHeight = 24f
        val x = (pdfPageWidth - stampWidth) / 2f
        val y = (pdfPageHeight - stampHeight) / 2f
        val annotation = EditAnnotation.DateStamp(
            pageIndex = pageIndex,
            bounds = RectF(x, y, x + stampWidth, y + stampHeight),
            text = dateText,
            fontSize = 14f,
            color = Color.BLACK
        )
        coordinator.addAnnotation(annotation)
        _selectedElementId.value = annotation.id
        coordinator.selectElement(annotation.id)
    }

    // -- Update existing text element (re-edit) --

    /** Live update: apply formatting changes in real-time while sheet is open. */
    fun liveUpdateTextElement(
        id: String, text: String, fontSize: Float, fontFamily: String, color: Int,
        isBold: Boolean, isItalic: Boolean, isUnderline: Boolean, isStrikethrough: Boolean
    ) {
        val ann = coordinator.annotations.value.find { it.id == id } as? EditAnnotation.TextElement ?: return
        // Recompute bounds based on new text/fontSize to keep sticker properly sized
        val snapshot = editingSnapshot ?: return
        val newBounds = recomputeTextBounds(snapshot.bounds, text, fontSize, snapshot.fontSize)
        val updated = ann.copy(
            bounds = newBounds, text = text, fontSize = fontSize, fontFamily = fontFamily,
            color = color, isBold = isBold, isItalic = isItalic,
            isUnderline = isUnderline, isStrikethrough = isStrikethrough
        )
        coordinator.updateAnnotation(updated)
    }

    /** Done: commit changes and close sheet. */
    fun updateTextElement(
        id: String, text: String, fontSize: Float, fontFamily: String, color: Int,
        isBold: Boolean, isItalic: Boolean, isUnderline: Boolean, isStrikethrough: Boolean
    ) {
        val ann = coordinator.annotations.value.find { it.id == id } as? EditAnnotation.TextElement ?: return
        val snapshot = editingSnapshot
        val newBounds = if (snapshot != null) recomputeTextBounds(snapshot.bounds, text, fontSize, snapshot.fontSize) else ann.bounds
        val updated = ann.copy(
            bounds = newBounds, text = text, fontSize = fontSize, fontFamily = fontFamily,
            color = color, isBold = isBold, isItalic = isItalic,
            isUnderline = isUnderline, isStrikethrough = isStrikethrough
        )
        coordinator.updateAnnotation(updated)
        editingSnapshot = null // Clear snapshot — changes committed
        _showTextSheet.value = false
        _editingTextElementId.value = null
    }

    /** Recompute text element bounds when text or fontSize changes during editing. */
    private fun recomputeTextBounds(
        originalBounds: android.graphics.RectF, text: String, newFontSize: Float, originalFontSize: Float
    ): android.graphics.RectF {
        val centerX = originalBounds.centerX()
        val centerY = originalBounds.centerY()
        // Scale width proportionally to font size change
        val sizeRatio = newFontSize / originalFontSize.coerceAtLeast(1f)
        val newWidth = (originalBounds.width() * sizeRatio).coerceIn(40f, 1000f)
        // Compute height from text content
        val lineHeight = newFontSize * 1.4f
        val explicitLines = text.count { it == '\n' } + 1
        val wrappedLines = (text.length * newFontSize * 0.6f / newWidth).coerceAtLeast(1f).toInt()
        val lines = maxOf(explicitLines, wrappedLines)
        val newHeight = (lineHeight * lines + newFontSize * 0.5f).coerceAtLeast(newFontSize * 1.5f)
        return android.graphics.RectF(
            centerX - newWidth / 2f, centerY - newHeight / 2f,
            centerX + newWidth / 2f, centerY + newHeight / 2f
        )
    }

    // -- Signature CRUD --

    /** Emits true when save fails due to max limit. UI layer should show toast. */
    private val _saveFailedMaxReached = MutableStateFlow(false)
    val saveFailedMaxReached: StateFlow<Boolean> = _saveFailedMaxReached.asStateFlow()

    fun consumeSaveFailedEvent() { _saveFailedMaxReached.value = false }

    fun saveSignature(bitmap: Bitmap, name: String, color: Int) {
        scope.launch {
            val entity = signatureRepository.save(bitmap, name, color)
            if (entity != null) {
                _showCreateDialog.value = false
            } else {
                _saveFailedMaxReached.value = true
            }
        }
    }

    fun deleteSignature(id: String) {
        scope.launch { signatureRepository.deleteById(id) }
    }

    // -- Placement: auto-center on viewport at ~25% page width --

    fun placeSignature(entity: SignatureEntity, pageIndex: Int, pdfPageWidth: Float, pdfPageHeight: Float) {
        scope.launch {
            val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                android.graphics.BitmapFactory.decodeFile(entity.imagePath)
            } ?: return@launch
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val placedWidth = pdfPageWidth * 0.25f
            val placedHeight = placedWidth / aspectRatio
            val centerX = pdfPageWidth / 2f
            val centerY = pdfPageHeight / 2f

            val element = EditAnnotation.SignatureElement(
                id = UUID.randomUUID().toString(),
                pageIndex = pageIndex,
                bounds = RectF(
                    centerX - placedWidth / 2f,
                    centerY - placedHeight / 2f,
                    centerX + placedWidth / 2f,
                    centerY + placedHeight / 2f
                ),
                imagePath = entity.imagePath,
                color = entity.color
            )
            coordinator.addAnnotation(element)
            _selectedElementId.value = element.id
            coordinator.selectElement(element.id)
        }
    }

    // -- Image placement: pick from device gallery, copy & resize, place on page --

    /** Max pixel dimension for copied images (width or height). */
    private val MAX_IMAGE_SIZE = 1024

    fun placeImage(context: Context, uri: Uri, pageIndex: Int, pdfPageWidth: Float, pdfPageHeight: Float) {
        scope.launch {
            val imagePath = copyAndResizeImage(context, uri) ?: return@launch
            val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                BitmapFactory.decodeFile(imagePath)
            } ?: return@launch
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val placedWidth = pdfPageWidth * 0.25f
            val placedHeight = placedWidth / aspectRatio
            val centerX = pdfPageWidth / 2f
            val centerY = pdfPageHeight / 2f

            val element = EditAnnotation.ImageElement(
                id = UUID.randomUUID().toString(),
                pageIndex = pageIndex,
                bounds = RectF(
                    centerX - placedWidth / 2f,
                    centerY - placedHeight / 2f,
                    centerX + placedWidth / 2f,
                    centerY + placedHeight / 2f
                ),
                imagePath = imagePath
            )
            coordinator.addAnnotation(element)
            _selectedElementId.value = element.id
            coordinator.selectElement(element.id)
        }
    }

    /** Copy image URI to internal storage, downscale to MAX_IMAGE_SIZE, save as JPEG 85%. */
    private suspend fun copyAndResizeImage(context: Context, uri: Uri): String? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null

                // First pass: decode bounds only to calculate sample size
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, MAX_IMAGE_SIZE)

                // Second pass: decode with sample size
                val stream2 = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val decoded = BitmapFactory.decodeStream(
                    stream2, null,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize }
                )
                stream2.close()
                if (decoded == null) return@withContext null

                // Downscale further if still too large
                val scaled = downscaleIfNeeded(decoded, MAX_IMAGE_SIZE)

                // Save to internal storage
                val dir = java.io.File(context.filesDir, "images").apply { mkdirs() }
                val file = java.io.File(dir, "${UUID.randomUUID()}.jpg")
                file.outputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                if (scaled !== decoded) scaled.recycle()
                decoded.recycle()

                file.absolutePath
            } catch (_: Exception) {
                null
            }
        }

    private fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sample = 1
        var w = width; var h = height
        while (w > maxSize * 2 || h > maxSize * 2) {
            sample *= 2; w /= 2; h /= 2
        }
        return sample
    }

    private fun downscaleIfNeeded(bitmap: Bitmap, maxSize: Int): Bitmap {
        val w = bitmap.width; val h = bitmap.height
        if (w <= maxSize && h <= maxSize) return bitmap
        val scale = maxSize.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    // -- Selection: tap hit-test on placed Fill & Sign elements --

    fun onElementTap(pageIndex: Int, pdfX: Float, pdfY: Float) {
        val annotations = coordinator.getAnnotationsForPage(pageIndex)
        // Hit-test Fill & Sign elements only (reverse order = topmost first)
        // Accounts for rotation by un-rotating tap point around element center,
        // and adds a small tolerance (5pt) for easier selection.
        val tolerance = 5f
        val hit = annotations.asReversed().firstOrNull { ann ->
            if (ann !is EditAnnotation.SignatureElement && ann !is EditAnnotation.ImageElement
                && ann !is EditAnnotation.DateStamp && ann !is EditAnnotation.TextElement) return@firstOrNull false

            val rotation = when (ann) {
                is EditAnnotation.SignatureElement -> ann.rotation
                is EditAnnotation.ImageElement -> ann.rotation
                is EditAnnotation.TextElement -> ann.rotation
                else -> 0f
            }
            val b = ann.bounds
            if (rotation == 0f) {
                // Fast path: no rotation, use expanded bounds
                pdfX >= b.left - tolerance && pdfX <= b.right + tolerance &&
                    pdfY >= b.top - tolerance && pdfY <= b.bottom + tolerance
            } else {
                // Un-rotate tap point around element center before checking bounds
                val cx = b.centerX(); val cy = b.centerY()
                val rad = Math.toRadians(-rotation.toDouble())
                val cos = kotlin.math.cos(rad).toFloat()
                val sin = kotlin.math.sin(rad).toFloat()
                val dx = pdfX - cx; val dy = pdfY - cy
                val localX = cx + dx * cos - dy * sin
                val localY = cy + dx * sin + dy * cos
                localX >= b.left - tolerance && localX <= b.right + tolerance &&
                    localY >= b.top - tolerance && localY <= b.bottom + tolerance
            }
        }
        _selectedElementId.value = hit?.id
        coordinator.selectElement(hit?.id)
    }

    // -- Manipulation --

    fun moveElement(id: String, dx: Float, dy: Float) {
        val ann = coordinator.annotations.value.find { it.id == id } ?: return
        val b = ann.bounds
        val newBounds = clampBoundsToPage(
            RectF(b.left + dx, b.top + dy, b.right + dx, b.bottom + dy),
            ann.pageIndex
        )
        val updated = when (ann) {
            is EditAnnotation.SignatureElement -> ann.copy(bounds = newBounds)
            is EditAnnotation.ImageElement -> ann.copy(bounds = newBounds)
            is EditAnnotation.TextElement -> ann.copy(bounds = newBounds)
            is EditAnnotation.DateStamp -> ann.copy(bounds = newBounds)
            else -> return
        }
        coordinator.updateAnnotation(updated)
    }

    fun resizeElement(id: String, newBounds: RectF) {
        val ann = coordinator.annotations.value.find { it.id == id } ?: return
        val clamped = clampBoundsToPage(newBounds, ann.pageIndex)
        val updated = when (ann) {
            is EditAnnotation.SignatureElement -> ann.copy(bounds = clamped)
            is EditAnnotation.ImageElement -> ann.copy(bounds = clamped)
            is EditAnnotation.TextElement -> return // Text stickers: no resize, use re-edit to change
            is EditAnnotation.DateStamp -> ann.copy(bounds = clamped)
            else -> return
        }
        coordinator.updateAnnotation(updated)
    }

    /** Atomic resize + rotate in a single update to avoid intermediate visual states. */
    fun resizeAndRotateElement(id: String, newBounds: RectF, newRotation: Float) {
        val ann = coordinator.annotations.value.find { it.id == id } ?: return
        val clamped = clampBoundsToPage(newBounds, ann.pageIndex)
        val updated = when (ann) {
            is EditAnnotation.SignatureElement -> ann.copy(bounds = clamped, rotation = newRotation)
            is EditAnnotation.ImageElement -> ann.copy(bounds = clamped, rotation = newRotation)
            is EditAnnotation.TextElement -> {
                // Text stickers: rotation only, no resize (bounds stay fixed)
                ann.copy(rotation = newRotation)
            }
            // DateStamp has no rotation field — only allow repositioning via bounds
            is EditAnnotation.DateStamp -> ann.copy(bounds = clamped)
            else -> return
        }
        coordinator.updateAnnotation(updated)
    }

    /**
     * Compute scale factor from old bounds to new bounds for text font scaling.
     * Uses the average of width and height ratios for balanced scaling.
     */
    private fun computeResizeScale(oldBounds: RectF, newBounds: RectF): Float {
        val oldW = oldBounds.width()
        val oldH = oldBounds.height()
        if (oldW <= 0f || oldH <= 0f) return 1f
        val scaleW = newBounds.width() / oldW
        val scaleH = newBounds.height() / oldH
        return ((scaleW + scaleH) / 2f).coerceIn(0.5f, 3f)
    }

    /**
     * Clamp bounds so sticker stays mostly within the PDF page (PDF point space).
     * Allows up to 30% of sticker width/height to overflow beyond page edges,
     * so stickers can be placed near page borders without being blocked.
     */
    private fun clampBoundsToPage(bounds: RectF, pageIndex: Int): RectF {
        // Use PDF point dimensions (not bitmap pixels) for correct coordinate space
        val dims = coordinator.pageDimensions.value[pageIndex]
        val pageW: Float
        val pageH: Float
        if (dims != null) {
            pageW = dims.first
            pageH = dims.second
        } else {
            val pageBitmap = coordinator.pageBitmaps.value[pageIndex] ?: return bounds
            pageW = pageBitmap.width.toFloat()
            pageH = pageBitmap.height.toFloat()
        }
        val w = bounds.width(); val h = bounds.height()
        // Allow 30% overflow beyond page edges
        val overflowX = w * 0.3f
        val overflowY = h * 0.3f
        // Guard: when sticker exceeds page size (e.g. during rotation with high aspect ratio),
        // coerceIn min > max would crash. Center the sticker on page in that case.
        val minLeft = -overflowX
        val maxLeft = pageW - w + overflowX
        val left = if (minLeft <= maxLeft) bounds.left.coerceIn(minLeft, maxLeft) else (pageW - w) / 2f
        val minTop = -overflowY
        val maxTop = pageH - h + overflowY
        val top = if (minTop <= maxTop) bounds.top.coerceIn(minTop, maxTop) else (pageH - h) / 2f
        return RectF(left, top, left + w, top + h)
    }

    fun rotateElement(id: String, newRotation: Float) {
        val ann = coordinator.annotations.value.find { it.id == id } ?: return
        val updated = when (ann) {
            is EditAnnotation.SignatureElement -> ann.copy(rotation = newRotation)
            is EditAnnotation.ImageElement -> ann.copy(rotation = newRotation)
            is EditAnnotation.TextElement -> ann.copy(rotation = newRotation)
            else -> return
        }
        coordinator.updateAnnotation(updated)
    }

    fun deleteSelectedElement() {
        val id = _selectedElementId.value ?: return
        coordinator.removeAnnotation(id)
        _selectedElementId.value = null
        coordinator.selectElement(null)
    }

    // -- Stamp placement: predefined text stamps (APPROVED, DRAFT, etc.) --

    fun placeStamp(text: String, pageIndex: Int, pdfPageWidth: Float, pdfPageHeight: Float) {
        val fontSize = 32f
        val stampWidth = pdfPageWidth * 0.5f
        val stampHeight = fontSize * 2.5f
        val centerX = pdfPageWidth / 2f
        val centerY = pdfPageHeight / 2f

        val element = EditAnnotation.TextElement(
            pageIndex = pageIndex,
            bounds = RectF(
                centerX - stampWidth / 2f,
                centerY - stampHeight / 2f,
                centerX + stampWidth / 2f,
                centerY + stampHeight / 2f
            ),
            text = text,
            fontSize = fontSize,
            fontFamily = "Helvetica",
            color = 0xFFD32F2F.toInt(),
            isBold = true
        )
        coordinator.addAnnotation(element)
        _selectedElementId.value = element.id
        coordinator.selectElement(element.id)
    }

    // -- Link dialog state --

    private val _showLinkDialog = MutableStateFlow(false)
    val showLinkDialog: StateFlow<Boolean> = _showLinkDialog.asStateFlow()

    fun openLinkDialog() { _showLinkDialog.value = true }
    fun dismissLinkDialog() { _showLinkDialog.value = false }

    /** Place a link as a blue underlined text element. */
    fun placeLink(url: String, pageIndex: Int, pdfPageWidth: Float, pdfPageHeight: Float) {
        if (url.isBlank()) return
        val fontSize = 14f
        val linkWidth = pdfPageWidth * 0.5f
        val linkHeight = fontSize * 1.8f
        val centerX = pdfPageWidth / 2f
        val centerY = pdfPageHeight / 2f

        val element = EditAnnotation.TextElement(
            pageIndex = pageIndex,
            bounds = RectF(
                centerX - linkWidth / 2f,
                centerY - linkHeight / 2f,
                centerX + linkWidth / 2f,
                centerY + linkHeight / 2f
            ),
            text = url,
            fontSize = fontSize,
            fontFamily = "Helvetica",
            color = 0xFF1565C0.toInt(),
            isUnderline = true
        )
        coordinator.addAnnotation(element)
        _selectedElementId.value = element.id
        coordinator.selectElement(element.id)
        _showLinkDialog.value = false
    }

    // -- Insert page (PDF or image) --

    private val _showAddPageDialog = MutableStateFlow(false)
    val showAddPageDialog: StateFlow<Boolean> = _showAddPageDialog.asStateFlow()

    /** Which page index to insert after. -1 means insert at beginning. */
    private val _insertAfterIndex = MutableStateFlow(0)
    val insertAfterIndex: StateFlow<Int> = _insertAfterIndex.asStateFlow()

    fun openAddPageDialog() {
        _insertAfterIndex.value = coordinator.currentPageIndex.value
        _showAddPageDialog.value = true
    }
    fun dismissAddPageDialog() { _showAddPageDialog.value = false }
    fun setInsertAfterIndex(index: Int) { _insertAfterIndex.value = index }

    /** Insert an image as a new page at the selected position. */
    fun insertImagePage(context: Context, uri: Uri) {
        val afterIdx = _insertAfterIndex.value
        _showAddPageDialog.value = false
        scope.launch {
            val result = loadAndSavePageBitmap(context, uri)
            if (result != null) {
                val (bitmap, savedPath, width, height) = result
                coordinator.insertPageAt(
                    afterIndex = afterIdx,
                    bitmap = bitmap,
                    pdfWidth = width,
                    pdfHeight = height,
                    savedPath = savedPath
                )
            }
        }
    }

    /** Insert pages from an external PDF at the selected position. */
    fun insertPdfPages(context: Context, uri: Uri) {
        val afterIdx = _insertAfterIndex.value
        _showAddPageDialog.value = false
        scope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext
                    val renderer = android.graphics.pdf.PdfRenderer(fd)
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val scale = 2f
                        val bmpW = (page.width * scale).toInt()
                        val bmpH = (page.height * scale).toInt()
                        val bitmap = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val pdfW = page.width.toFloat()
                        val pdfH = page.height.toFloat()
                        page.close()

                        val dir = java.io.File(context.filesDir, "inserted_pages").apply { mkdirs() }
                        val file = java.io.File(dir, "${java.util.UUID.randomUUID()}.jpg")
                        file.outputStream().use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                        }

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            coordinator.insertPageAt(
                                afterIndex = afterIdx + i,
                                bitmap = bitmap,
                                pdfWidth = pdfW,
                                pdfHeight = pdfH,
                                savedPath = file.absolutePath
                            )
                        }
                    }
                    renderer.close()
                    fd.close()
                } catch (e: Exception) {
                    android.util.Log.e("FillSignVM", "Failed to insert PDF pages", e)
                }
            }
        }
    }

    /** Load image from URI, resize to fit A4, save to internal storage. Returns (bitmap, path, width, height). */
    private suspend fun loadAndSavePageBitmap(
        context: Context, uri: Uri
    ): PageBitmapResult? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return@withContext null

            // Scale image to fit A4 page (595x842 PDF points) while maintaining aspect ratio
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val pdfW: Float; val pdfH: Float
            if (aspectRatio > 595f / 842f) {
                pdfW = 595f; pdfH = 595f / aspectRatio
            } else {
                pdfH = 842f; pdfW = 842f * aspectRatio
            }

            // Create page bitmap: white background with image centered
            val pageBitmap = android.graphics.Bitmap.createBitmap(
                pdfW.toInt().coerceAtLeast(1), pdfH.toInt().coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(pageBitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.drawBitmap(
                bitmap, null,
                android.graphics.RectF(0f, 0f, pdfW, pdfH),
                null
            )
            bitmap.recycle()

            // Save to internal storage
            val dir = java.io.File(context.filesDir, "inserted_pages").apply { mkdirs() }
            val file = java.io.File(dir, "${java.util.UUID.randomUUID()}.jpg")
            file.outputStream().use { out ->
                pageBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            PageBitmapResult(pageBitmap, file.absolutePath, pdfW, pdfH)
        } catch (e: Exception) {
            android.util.Log.e("FillSignVM", "Failed to load image as page", e)
            null
        }
    }

    private data class PageBitmapResult(
        val bitmap: android.graphics.Bitmap,
        val savedPath: String,
        val width: Float,
        val height: Float
    )
}
