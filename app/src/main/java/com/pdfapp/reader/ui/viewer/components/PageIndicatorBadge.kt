package com.pdfapp.reader.ui.viewer.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Small badge at the top-right of the viewer showing "current / total" pages. */
@Composable
fun PageIndicatorBadge(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 0) return

    Surface(
        modifier = modifier.padding(bottom = 16.dp, start = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shadowElevation = 2.dp
    ) {
        Text(
            text = "$currentPage / $totalPages",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
