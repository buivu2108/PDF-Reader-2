package com.pdfapp.reader.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vtsoft.pdfapp.reader.R

/**
 * TopAppBar shown when the user is in multi-select mode.
 * Displays selection count and actions: close, select-all, share, delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.file_selected_count, selectedCount),
                style = MaterialTheme.typography.titleMedium
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_close_selection)
                )
            }
        },
        actions = {
            // Select all
            IconButton(onClick = onSelectAll) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = stringResource(R.string.cd_select_all)
                )
            }
            // Share selected
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.share)
                )
            }
            // Delete selected
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
