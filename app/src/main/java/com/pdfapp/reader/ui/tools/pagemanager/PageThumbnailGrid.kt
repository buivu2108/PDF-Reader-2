package com.pdfapp.reader.ui.tools.pagemanager

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfapp.reader.util.ThumbnailGenerator

/**
 * 2-column grid of PDF page thumbnails.
 * Handles async thumbnail loading, rotation indicator, delete overlay, and selection highlight.
 */
@Composable
fun PageThumbnailGrid(
    pages: List<PageInfo>,
    sourceUri: Uri,
    selectedIndex: Int?,
    onPageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(pages) { listIndex, pageInfo ->
            PageThumbnailItem(
                pageInfo = pageInfo,
                listIndex = listIndex,
                sourceUri = sourceUri,
                isSelected = selectedIndex == listIndex,
                onClick = { onPageClick(listIndex) }
            )
        }
    }
}

@Composable
private fun PageThumbnailItem(
    pageInfo: PageInfo,
    listIndex: Int,
    sourceUri: Uri,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(pageInfo.index, sourceUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageInfo.index, sourceUri) {
        bitmap = ThumbnailGenerator.generatePageThumbnail(
            context, sourceUri, pageInfo.index, width = 120, height = 160
        )
    }

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        pageInfo.deleted -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.small
            )
            .clickable { onClick() },
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail image with rotation applied visually
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Page ${listIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = pageInfo.rotation.toFloat() }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }

            // Delete overlay
            if (pageInfo.deleted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DELETED",
                        color = Color.White,
                        fontSize = 11.sp,
                        textDecoration = TextDecoration.LineThrough,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Rotation badge
            if (pageInfo.rotation != 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = "+${pageInfo.rotation}°",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 9.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Page number label at bottom
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Text(
                    text = "Page ${listIndex + 1}",
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
