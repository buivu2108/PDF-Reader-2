package com.pdfapp.reader.ui.editmode.mark

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.NoteAdd
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

/**
 * Mark tab toolbar: horizontally scrollable tool buttons + fixed color swatch on the right.
 */
@Composable
fun MarkToolbar(
    activeTool: MarkTool,
    markColor: Int,
    onToolSelected: (MarkTool) -> Unit,
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MarkToolButton(Icons.Default.Highlight, stringResource(R.string.tool_highlight),
                activeTool == MarkTool.HIGHLIGHT) { onToolSelected(MarkTool.HIGHLIGHT) }
            MarkToolButton(Icons.Default.FormatUnderlined, stringResource(R.string.tool_underline),
                activeTool == MarkTool.UNDERLINE) { onToolSelected(MarkTool.UNDERLINE) }
            MarkToolButton(Icons.Default.FormatStrikethrough, stringResource(R.string.tool_strikethrough),
                activeTool == MarkTool.STRIKETHROUGH) { onToolSelected(MarkTool.STRIKETHROUGH) }
            MarkToolButton(Icons.Default.NoteAdd, stringResource(R.string.tool_note),
                activeTool == MarkTool.NOTE) { onToolSelected(MarkTool.NOTE) }
            // Color swatch
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(markColor))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    .clickable { onSettingsRequested() }
            )
        }
    }
}

@Composable
private fun MarkToolButton(
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
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onToolClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor, maxLines = 1)
    }
}
