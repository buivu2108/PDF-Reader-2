package com.pdfapp.reader.ui.editmode.mark

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.ui.editmode.components.ColorPickerRow

/**
 * Floating popup near a tapped annotation with Color and Delete actions.
 * Color button opens an inline ColorPickerRow.
 *
 * @param annotationColor current color of the selected annotation
 * @param screenX X position in screen coordinates where popup should appear
 * @param screenY Y position in screen coordinates where popup should appear
 * @param onColorChange callback when a new color is selected
 * @param onDelete callback when delete is pressed
 * @param onDismiss callback when popup should close
 */
@Composable
fun MarkAnnotationPopup(
    annotationColor: Int,
    screenX: Int,
    screenY: Int,
    onColorChange: (Int) -> Unit,
    onCopy: (() -> Unit)? = null,
    onNote: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(screenX, screenY - 120),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color button with current color indicator
                    IconButton(onClick = { showColorPicker = !showColorPicker }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = stringResource(R.string.cd_color),
                                modifier = Modifier.size(24.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(annotationColor))
                            )
                        }
                    }
                    // Copy button (hidden for StickyNote)
                    if (onCopy != null) {
                        IconButton(onClick = { onCopy(); onDismiss() }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.cd_copy),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    // Note button
                    IconButton(onClick = { onNote(); onDismiss() }) {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = stringResource(R.string.cd_note),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Delete button
                    IconButton(onClick = { onDelete(); onDismiss() }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.edit_discard_confirm),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                // Inline color picker (toggleable)
                if (showColorPicker) {
                    ColorPickerRow(
                        selectedColor = annotationColor,
                        onColorSelected = { color ->
                            onColorChange(color)
                            showColorPicker = false
                        }
                    )
                }
            }
        }
    }
}
