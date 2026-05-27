package com.pdfapp.reader.ui.editmode.mark

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.vtsoft.pdfapp.reader.R

private val PopupBackground = Color(0xD9000000)
private val ArrowSizeDp = 8.dp

/**
 * Floating popup shown after completing text selection in Mark tab.
 * Shows "Apply [ToolName]" button with arrow pointer.
 * Auto-flips above/below selection based on available space.
 *
 * @param toolName display name of the active tool (e.g., "Highlight")
 * @param screenX popup anchor X in screen pixels
 * @param screenY popup anchor Y in screen pixels
 * @param onApply callback when user taps the apply button
 * @param onDismiss callback when popup is dismissed
 */
@Composable
fun MarkSelectionApplyPopup(
    toolName: String,
    screenX: Int,
    screenY: Int,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val menuHeightPx = with(density) { 44.dp.roundToPx() }
    val arrowHeightPx = with(density) { ArrowSizeDp.roundToPx() }
    val gapPx = with(density) { 4.dp.roundToPx() }
    val totalHeightPx = menuHeightPx + arrowHeightPx
    val menuWidthPx = with(density) { 180.dp.roundToPx() }
    val marginPx = with(density) { 8.dp.roundToPx() }

    val showAbove = screenY > totalHeightPx + gapPx

    val popupOffsetY = if (showAbove) {
        screenY - gapPx - totalHeightPx
    } else {
        screenY + with(density) { 24.dp.roundToPx() } + gapPx
    }

    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    val popupOffsetX = (screenX - menuWidthPx / 2).coerceIn(marginPx, screenWidthPx - menuWidthPx - marginPx)

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(popupOffsetX, maxOf(0, popupOffsetY)),
        onDismissRequest = onDismiss
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!showAbove) {
                PopupArrow(pointingUp = true, anchorScreenX = screenX, menuOffsetX = popupOffsetX)
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PopupBackground,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onApply)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.mark_apply_tool, toolName),
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (showAbove) {
                PopupArrow(pointingUp = false, anchorScreenX = screenX, menuOffsetX = popupOffsetX)
            }
        }
    }
}

@Composable
private fun PopupArrow(pointingUp: Boolean, anchorScreenX: Int, menuOffsetX: Int) {
    val density = LocalDensity.current
    val arrowWidthPx = with(density) { 16.dp.toPx() }
    val arrowHeightPx = with(density) { ArrowSizeDp.toPx() }
    val menuWidthPx = with(density) { 180.dp.toPx() }
    val clampMargin = with(density) { 12.dp.toPx() }
    val localAnchorX = (anchorScreenX - menuOffsetX).toFloat().coerceIn(clampMargin, menuWidthPx - clampMargin)

    Canvas(
        modifier = Modifier
            .width(with(density) { 180.dp })
            .height(ArrowSizeDp)
    ) {
        val path = Path().apply {
            if (pointingUp) {
                moveTo(localAnchorX, 0f)
                lineTo(localAnchorX - arrowWidthPx / 2, arrowHeightPx)
                lineTo(localAnchorX + arrowWidthPx / 2, arrowHeightPx)
            } else {
                moveTo(localAnchorX - arrowWidthPx / 2, 0f)
                lineTo(localAnchorX + arrowWidthPx / 2, 0f)
                lineTo(localAnchorX, arrowHeightPx)
            }
            close()
        }
        drawPath(path, color = PopupBackground)
    }
}
