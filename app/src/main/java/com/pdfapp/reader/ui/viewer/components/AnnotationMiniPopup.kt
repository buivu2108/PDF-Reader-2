package com.pdfapp.reader.ui.viewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.model.QuickAnnotation

/**
 * Small popup shown when tapping an existing annotation.
 * Shows color circles for changing annotation color and a delete button.
 */
@Composable
fun AnnotationMiniPopup(
    annotationColor: Int,
    screenX: Int,
    screenY: Int,
    onColorChange: (Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(screenX, screenY - 80),
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xCC333333),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickAnnotation.COLORS.forEach { color ->
                    val isSelected = (color == annotationColor)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(color or (0xFF shl 24)))
                            .then(
                                if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .clickable { onColorChange(color) }
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
