package com.pdfapp.reader.ui.viewer.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Lightweight shimmer placeholder shown while a PDF page bitmap is rendering.
 * Uses animated linear gradient for visual loading feedback without Canvas overhead.
 * Automatically adapts to night mode via MaterialTheme color tokens.
 */
@Composable
fun PdfPageShimmerPlaceholder(
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface

    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(shimmerOffset - 500f, 0f),
        end = Offset(shimmerOffset, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(shimmerBrush),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = "Page ${pageIndex + 1}",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
