package com.pdfapp.reader.ui.editmode.annotate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Pentagon
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.model.AnnotateTool

/**
 * Annotate tab toolbar: horizontally scrollable tool buttons + fixed color swatch on the right.
 */
@Composable
fun AnnotateToolbar(
    activeTool: AnnotateTool,
    strokeColor: Int,
    onToolSelected: (AnnotateTool) -> Unit,
    onSettingsRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable tool buttons
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnnotateToolButton(Icons.Default.TouchApp, stringResource(R.string.tool_select_region),
                    activeTool == AnnotateTool.SELECT) { onToolSelected(AnnotateTool.SELECT) }
                AnnotateToolButton(Icons.Default.Create, stringResource(R.string.tool_freehand),
                    activeTool == AnnotateTool.DRAW) { onToolSelected(AnnotateTool.DRAW) }
                AnnotateToolButton(Icons.Outlined.Circle, stringResource(R.string.tool_oval),
                    activeTool == AnnotateTool.CIRCLE) { onToolSelected(AnnotateTool.CIRCLE) }
                AnnotateToolButton(Icons.Outlined.CropSquare, stringResource(R.string.tool_rectangle),
                    activeTool == AnnotateTool.RECTANGLE) { onToolSelected(AnnotateTool.RECTANGLE) }
                AnnotateToolButton(Icons.Outlined.Remove, stringResource(R.string.tool_line),
                    activeTool == AnnotateTool.LINE) { onToolSelected(AnnotateTool.LINE) }
                AnnotateToolButton(Icons.Outlined.NorthEast, stringResource(R.string.tool_arrow),
                    activeTool == AnnotateTool.ARROW) { onToolSelected(AnnotateTool.ARROW) }
                AnnotateToolButton(Icons.Outlined.Waves, stringResource(R.string.tool_zigzag),
                    activeTool == AnnotateTool.ZIGZAG) { onToolSelected(AnnotateTool.ZIGZAG) }
                AnnotateToolButton(Icons.Outlined.ChangeHistory, stringResource(R.string.tool_triangle),
                    activeTool == AnnotateTool.TRIANGLE) { onToolSelected(AnnotateTool.TRIANGLE) }
                AnnotateToolButton(Icons.Outlined.Pentagon, stringResource(R.string.tool_polygon),
                    activeTool == AnnotateTool.POLYGON) { onToolSelected(AnnotateTool.POLYGON) }
                AnnotateToolButton(Icons.Outlined.LayersClear, stringResource(R.string.tool_eraser),
                    activeTool == AnnotateTool.ERASER) { onToolSelected(AnnotateTool.ERASER) }
            }
            // Fixed color swatch
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(strokeColor))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    .clickable { onSettingsRequested() }
            )
        }
    }
}

@Composable
private fun AnnotateToolButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onToolClick: () -> Unit
) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
    else Color.Transparent
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onToolClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor, maxLines = 1)
    }
}
