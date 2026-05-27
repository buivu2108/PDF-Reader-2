package com.pdfapp.reader.ui.viewer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.vtsoft.pdfapp.reader.R

private val MenuBackground = Color(0xD9000000)
private val ArrowSizeDp = 8.dp
private val MenuCornerRadius = 12.dp

/**
 * 2-row floating context menu with arrow pointer for text selection.
 *
 * Row 1: Copy | Highlight | Underline | Strikethrough
 * Row 2: Edit Text (full-width)
 *
 * Auto-flips above/below selection anchor. Arrow points toward selected text.
 */
@Composable
fun TextSelectionContextMenu(
    screenX: Int,
    screenY: Int,
    onCopy: () -> Unit,
    onHighlight: () -> Unit,
    onUnderline: () -> Unit,
    onStrikethrough: () -> Unit,
    onEditText: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val menuHeightPx = with(density) { 100.dp.roundToPx() }
    val arrowHeightPx = with(density) { ArrowSizeDp.roundToPx() }
    val gapPx = with(density) { 4.dp.roundToPx() }
    val totalHeightPx = menuHeightPx + arrowHeightPx
    val menuWidthPx = with(density) { 260.dp.roundToPx() }
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
                MenuArrow(pointingUp = true, anchorScreenX = screenX, menuOffsetX = popupOffsetX)
            }

            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(MenuCornerRadius),
                color = MenuBackground,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContextMenuActionButton(
                            icon = Icons.Default.ContentCopy,
                            label = stringResource(R.string.viewer_copy),
                            onClick = onCopy
                        )
                        ContextMenuDivider()
                        ContextMenuActionButton(
                            icon = Icons.Default.BorderColor,
                            label = stringResource(R.string.viewer_highlight),
                            onClick = onHighlight
                        )
                        ContextMenuDivider()
                        ContextMenuActionButton(
                            icon = Icons.Default.FormatUnderlined,
                            label = stringResource(R.string.viewer_underline),
                            onClick = onUnderline
                        )
                        ContextMenuDivider()
                        ContextMenuActionButton(
                            icon = Icons.Default.FormatStrikethrough,
                            label = stringResource(R.string.viewer_strikethrough),
                            onClick = onStrikethrough
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onEditText)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.viewer_edit_text),
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            if (showAbove) {
                MenuArrow(pointingUp = false, anchorScreenX = screenX, menuOffsetX = popupOffsetX)
            }
        }
    }
}

/**
 * Small triangle arrow pointing toward the selected text.
 */
@Composable
private fun MenuArrow(pointingUp: Boolean, anchorScreenX: Int, menuOffsetX: Int) {
    val density = LocalDensity.current
    val arrowWidthPx = with(density) { 16.dp.toPx() }
    val arrowHeightPx = with(density) { ArrowSizeDp.toPx() }
    val menuWidthPx = with(density) { 260.dp.toPx() }
    val clampMargin = with(density) { 12.dp.toPx() }
    val localAnchorX = (anchorScreenX - menuOffsetX).toFloat().coerceIn(clampMargin, menuWidthPx - clampMargin)

    Canvas(
        modifier = Modifier
            .width(with(density) { 260.dp })
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
        drawPath(path, color = MenuBackground)
    }
}

@Composable
private fun ContextMenuDivider() {
    VerticalDivider(
        modifier = Modifier.height(36.dp).width(1.dp),
        color = Color.White.copy(alpha = 0.2f)
    )
}

@Composable
private fun ContextMenuActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
