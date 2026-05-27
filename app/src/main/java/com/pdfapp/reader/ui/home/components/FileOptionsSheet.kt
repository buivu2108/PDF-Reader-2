package com.pdfapp.reader.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.model.PdfFileInfo

/**
 * Modal bottom sheet displaying contextual actions for a selected PDF file.
 * Options: Open, Share, Rename, Delete, File Info.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileOptionsSheet(
    file: PdfFileInfo,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onFavorite: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            // Sheet header — file name
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))

            OptionRow(
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                label = stringResource(R.string.file_open),
                onClick = { onOpen(); onDismiss() }
            )
            OptionRow(
                icon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                label = stringResource(R.string.share),
                onClick = { onShare(); onDismiss() }
            )
            OptionRow(
                icon = {
                    Icon(
                        imageVector = if (file.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = if (file.isFavorite) androidx.compose.ui.graphics.Color(0xFFFFB300)
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = if (file.isFavorite) "Remove from Favorites" else "Add to Favorites",
                onClick = { onFavorite(); onDismiss() }
            )
            OptionRow(
                icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                label = stringResource(R.string.rename),
                onClick = { onRename(); onDismiss() }
            )
            OptionRow(
                icon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                label = stringResource(R.string.delete),
                labelColor = MaterialTheme.colorScheme.error,
                onClick = { onDelete(); onDismiss() }
            )
            OptionRow(
                icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                label = stringResource(R.string.info),
                onClick = { onInfo(); onDismiss() }
            )
        }
    }
}

/** Reusable single option row with icon and label inside the sheet. */
@Composable
private fun OptionRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            icon()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor
        )
    }
}
