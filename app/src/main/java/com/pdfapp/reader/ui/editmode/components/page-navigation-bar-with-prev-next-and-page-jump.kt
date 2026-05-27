package com.pdfapp.reader.ui.editmode.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R

/**
 * Navigation bar with prev/next arrows and tappable "Page X of Y" label.
 * Tapping the label opens a page number input dialog for direct jump.
 *
 * @param currentPage 0-indexed current page
 * @param pageCount total page count
 * @param onPrevious navigate to previous page
 * @param onNext navigate to next page
 * @param onPageJump jump to specific page (0-indexed)
 */
@Composable
fun PageNavigationBar(
    currentPage: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPageJump: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, enabled = currentPage > 0) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous page"
            )
        }

        Text(
            text = stringResource(R.string.edit_page_of, currentPage + 1, pageCount),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable { showDialog = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        IconButton(onClick = onNext, enabled = currentPage < pageCount - 1) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next page"
            )
        }
    }

    if (showDialog) {
        PageNumberInputDialog(
            currentPage = currentPage,
            pageCount = pageCount,
            onPageSelected = { onPageJump(it); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}
