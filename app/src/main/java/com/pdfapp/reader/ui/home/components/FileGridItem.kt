package com.pdfapp.reader.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pdfapp.reader.domain.model.PdfFileInfo
import com.pdfapp.reader.util.toReadableSize

import com.pdfapp.reader.ui.theme.getDynamicItemColor

/**
 * Card-based grid item for displaying a PDF file.
 * Shows thumbnail (or PDF icon fallback), file name, and file size.
 * Intended for a 2-column LazyVerticalGrid layout.
 * Supports long-press for multi-select mode.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    file: PdfFileInfo,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onLongPress: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dynamicColor = getDynamicItemColor(file.name)
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else dynamicColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(24.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Thumbnail area with PDF icon fallback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (file.thumbnailPath != null) {
                    AsyncImage(
                        model = java.io.File(file.thumbnailPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                // Favorite toggle button
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(36.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (file.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (file.isFavorite) androidx.compose.ui.graphics.Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // File name and size
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = file.size.toReadableSize(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
