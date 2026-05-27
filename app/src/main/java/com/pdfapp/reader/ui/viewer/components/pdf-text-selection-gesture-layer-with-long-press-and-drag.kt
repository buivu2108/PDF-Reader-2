package com.pdfapp.reader.ui.viewer.components

import android.graphics.RectF
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp

/**
 * Full-page gesture layer that handles text selection interactions.
 *
 * When no selection exists: long-press triggers word selection.
 * When selection exists: handle drag updates selection, tap outside clears it.
 *
 * All coordinates are reported in screen-space pixels; the ViewModel is
 * responsible for converting to PDF space via CoordinateTransform.
 */
@Composable
fun SelectionGestureLayer(
    hasSelection: Boolean,
    selectionLineBounds: List<RectF>,
    pdfPageWidth: Float,
    pdfPageHeight: Float,
    onLongPress: (Offset) -> Unit,
    onStartHandleDrag: (Offset) -> Unit,
    onEndHandleDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTapOutside: () -> Unit,
    onSingleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    val touchSlop = viewConfiguration.touchSlop
    val hitRadiusPx = with(density) { 48.dp.toPx() }

    val currentHasSelection by rememberUpdatedState(hasSelection)
    val currentBounds by rememberUpdatedState(selectionLineBounds)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnStartDrag by rememberUpdatedState(onStartHandleDrag)
    val currentOnEndDrag by rememberUpdatedState(onEndHandleDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnTapOutside by rememberUpdatedState(onTapOutside)
    val currentOnSingleTap by rememberUpdatedState(onSingleTap)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    if (currentHasSelection) {
                        // --- Selection active: handle drag or tap outside ---
                        down.consume()

                        val displayWidth = size.width.toFloat()
                        val displayHeight = size.height.toFloat()
                        val handlePositions = computeHandleScreenPositions(
                            currentBounds, pdfPageWidth, pdfPageHeight,
                            displayWidth, displayHeight
                        )

                        if (handlePositions == null) {
                            // No valid handle positions — dismiss
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                change.consume()
                                if (!change.pressed) {
                                    currentOnTapOutside()
                                    break
                                }
                            }
                        } else {
                            val (startHandle, endHandle) = handlePositions
                            val distToStart = (down.position - startHandle).getDistance()
                            val distToEnd = (down.position - endHandle).getDistance()

                            // Check if touch is in selection area
                            val scaleY = displayHeight / pdfPageHeight
                            val selTopY = currentBounds.minOf { it.top } * scaleY
                            val selBottomY = currentBounds.maxOf { it.bottom } * scaleY
                            val inSelArea = down.position.y in
                                (selTopY - hitRadiusPx)..(selBottomY + hitRadiusPx)

                            val hitHandle: String? = when {
                                distToStart <= hitRadiusPx && distToEnd <= hitRadiusPx ->
                                    if (distToStart <= distToEnd) "start" else "end"
                                distToStart <= hitRadiusPx -> "start"
                                distToEnd <= hitRadiusPx -> "end"
                                inSelArea -> if (distToStart <= distToEnd) "start" else "end"
                                else -> null
                            }

                            if (hitHandle != null) {
                                // Handle drag
                                val dragCallback = if (hitHandle == "start")
                                    currentOnStartDrag else currentOnEndDrag
                                val dragStartPos = if (hitHandle == "start")
                                    startHandle else endHandle
                                var dragDelta = Offset.Zero
                                var dragStarted = false
                                val minSlop = touchSlop / 4f

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (change.pressed) {
                                        val delta = change.positionChange()
                                        change.consume()
                                        dragDelta += delta
                                        if (!dragStarted && dragDelta.getDistance() > minSlop) {
                                            dragStarted = true
                                        }
                                        if (dragStarted) {
                                            dragCallback(dragStartPos + dragDelta)
                                        }
                                    } else {
                                        change.consume()
                                        if (dragStarted) currentOnDragEnd()
                                        break
                                    }
                                }
                            } else {
                                // Tap outside
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    change.consume()
                                    if (!change.pressed) {
                                        currentOnTapOutside()
                                        break
                                    }
                                }
                            }
                        }
                    } else {
                        // --- No selection: detect long press ---
                        // IMPORTANT: Do NOT consume move events during monitoring.
                        // Consuming them prevents the parent HorizontalPager / LazyColumn
                        // from detecting swipe gestures, making the page un-swipeable
                        // in areas covered by this layer.
                        var longPressDetected = false
                        try {
                            withTimeout(longPressTimeout) {
                                var totalDrag = Offset.Zero
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) {
                                        // Finger lifted before timeout → single tap
                                        change.consume()
                                        currentOnSingleTap()
                                        break
                                    }
                                    totalDrag += change.positionChange()
                                    if (totalDrag.getDistance() > touchSlop) {
                                        // Moved too much → not a long press, let parent handle swipe
                                        break
                                    }
                                    // Don't consume — let events pass to parent for scroll/swipe
                                }
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            // Timeout → long press detected
                            longPressDetected = true
                        }

                        if (longPressDetected) {
                            down.consume()
                            currentOnLongPress(down.position)
                            // Consume remaining events until finger lifts
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                change.consume()
                                if (!change.pressed) break
                            }
                        }
                    }
                }
            }
    )
}
