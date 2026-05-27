package com.pdfapp.reader.ui.editmode.mark

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.ui.editmode.components.ColorPickerRow

/**
 * Settings sheet for highlight tool: opacity slider + color picker.
 * For underline/strikethrough, use the generic BottomSettingsSheet (color only).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightSettingsSheet(
    highlightColor: Int,
    highlightOpacity: Float,
    onColorSelected: (Int) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Color section
            Text(
                text = stringResource(R.string.cd_color),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(8.dp))
            ColorPickerRow(
                selectedColor = highlightColor,
                onColorSelected = onColorSelected
            )
            Spacer(Modifier.height(16.dp))

            // Opacity section
            Text(
                text = stringResource(R.string.edit_opacity),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "${(highlightOpacity * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = highlightOpacity,
                onValueChange = onOpacityChange,
                valueRange = 0.1f..1.0f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
