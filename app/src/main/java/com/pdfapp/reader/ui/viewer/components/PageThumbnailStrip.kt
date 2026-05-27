package com.pdfapp.reader.ui.viewer.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pdfapp.reader.util.ThumbnailGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Scrollable horizontal strip of page thumbnails shown at the bottom of the viewer. */
@Composable
fun PageThumbnailStrip(
    visible: Boolean,
    totalPages: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    uri: Uri? = null,
    context: Context? = null
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentPage) {
        scope.launch {
            listState.animateScrollToItem(currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0)))
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(totalPages) { pageIndex ->
                    PageThumbnailItem(
                        pageIndex = pageIndex,
                        isActive = pageIndex == currentPage,
                        onClick = { onPageSelected(pageIndex) },
                        uri = uri,
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
private fun PageThumbnailItem(
    pageIndex: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    uri: Uri? = null,
    context: Context? = null
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    // Load thumbnail lazily when both uri and context are available
    if (uri != null && context != null) {
        LaunchedEffect(pageIndex) {
            bitmap = withContext(Dispatchers.IO) {
                ThumbnailGenerator.generatePageThumbnail(context, uri, pageIndex)
            }
        }
    }

    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(2.dp, borderColor, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = "${pageIndex + 1}",
                color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
        }
        // Page number overlay once bitmap is loaded
        if (bmp != null) {
            Text(
                text = "${pageIndex + 1}",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
