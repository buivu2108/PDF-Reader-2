package com.pdfapp.reader.ui.editmode.annotate

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.ui.editmode.components.ColorPickerRow

/**
 * Bottom sheet for freehand stroke settings: color picker, stroke width slider,
 * and live preview line showing current width + color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreehandStrokeSettingsSheet(
    strokeColor: Int,
    strokeWidth: Float,
    onColorSelected: (Int) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Color picker
            Text(stringResource(R.string.cd_color), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            ColorPickerRow(selectedColor = strokeColor, onColorSelected = onColorSelected)
            Spacer(Modifier.height(16.dp))

            // Stroke width slider
            Text(stringResource(R.string.edit_stroke_width), style = MaterialTheme.typography.labelLarge)
            Slider(
                value = strokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = 1f..10f,
                modifier = Modifier.fillMaxWidth()
            )

            // Live preview line
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 16.dp)
            ) {
                drawLine(
                    color = Color(strokeColor),
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = strokeWidth.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
