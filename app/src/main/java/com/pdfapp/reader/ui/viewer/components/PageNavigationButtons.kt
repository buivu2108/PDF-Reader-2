package com.pdfapp.reader.ui.viewer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Vertical column of FABs for navigating to the previous or next page. */
@Composable
fun PageNavigationButtons(
    onPageUp: () -> Unit,
    onPageDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingActionButton(
            onClick = onPageUp,
            modifier = Modifier.size(44.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Previous page",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        FloatingActionButton(
            onClick = onPageDown,
            modifier = Modifier
                .padding(top = 8.dp)
                .size(44.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Next page",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
