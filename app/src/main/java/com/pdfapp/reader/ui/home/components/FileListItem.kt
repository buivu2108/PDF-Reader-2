package com.pdfapp.reader.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.model.PdfFileInfo
import com.pdfapp.reader.util.toFormattedDate
import com.pdfapp.reader.util.toReadableSize

import com.pdfapp.reader.ui.theme.getDynamicItemColor

/**
 * List-style row item displaying a PDF file thumbnail, name, metadata,
 * and a context-menu trigger. Supports long-press for multi-select.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: PdfFileInfo,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onMoreClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dynamicColor = getDynamicItemColor(file.name)
    val selectedBackground = MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) selectedBackground else dynamicColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or placeholder icon
            if (file.thumbnailPath != null) {
            AsyncImage(
                model = java.io.File(file.thumbnailPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // File name and metadata
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (file.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFFFFB300),
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${file.lastModified.toFormattedDate()} · ${file.size.toReadableSize()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // More options button
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
}
