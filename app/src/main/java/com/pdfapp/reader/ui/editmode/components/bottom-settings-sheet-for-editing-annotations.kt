package com.pdfapp.reader.ui.editmode.components

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

/**
 * Reusable bottom sheet for adjusting annotation properties: color, opacity, stroke width.
 * Each section is optional -- pass null to hide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSettingsSheet(
    onDismiss: () -> Unit,
    selectedColor: Int? = null,
    onColorSelected: ((Int) -> Unit)? = null,
    opacity: Float? = null,
    onOpacityChange: ((Float) -> Unit)? = null,
    strokeWidth: Float? = null,
    onStrokeWidthChange: ((Float) -> Unit)? = null
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
            // Color picker
            if (selectedColor != null && onColorSelected != null) {
                Text(
                    text = stringResource(R.string.cd_color),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
                ColorPickerRow(
                    selectedColor = selectedColor,
                    onColorSelected = onColorSelected
                )
                Spacer(Modifier.height(16.dp))
            }

            // Opacity slider
            if (opacity != null && onOpacityChange != null) {
                Text(
                    text = stringResource(R.string.edit_opacity),
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
            }

            // Stroke width slider
            if (strokeWidth != null && onStrokeWidthChange != null) {
                Text(
                    text = stringResource(R.string.edit_stroke_width),
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = strokeWidth,
                    onValueChange = onStrokeWidthChange,
                    valueRange = 1f..20f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
