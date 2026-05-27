package com.pdfapp.reader.ui.editmode.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/** Default annotation colors: Yellow, Orange, Green, Blue, Red, Black. */
val defaultAnnotationColors = listOf(
    0xFFFFEB3B.toInt(), // Yellow
    0xFFFF9800.toInt(), // Orange
    0xFF4CAF50.toInt(), // Green
    0xFF2196F3.toInt(), // Blue
    0xFFF44336.toInt(), // Red
    0xFF000000.toInt()  // Black
)

/** Row of color swatches. Selected swatch shows a checkmark. */
@Composable
fun ColorPickerRow(
    selectedColor: Int,
    colors: List<Int> = defaultAnnotationColors,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { color ->
            val isSelected = color == selectedColor
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .then(
                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier
                    )
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color == 0xFF000000.toInt()) Color.White else Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
