package com.pdfapp.reader.ui.tools.compress

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdfapp.reader.ui.components.LoadingDialog
import com.pdfapp.reader.util.AdManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressPdfScreen(
    viewModel: CompressPdfViewModel,
    adManager: AdManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.selectPdf(it) }
    }

    LaunchedEffect(uiState.resultUri) {
        uiState.resultUri?.let {
            Toast.makeText(context, "Compressed successfully", Toast.LENGTH_SHORT).show()
            val onComplete = { viewModel.clearResult() }
            (context as? Activity)?.let { act ->
                adManager.showInterstitial(act, onComplete)
            } ?: onComplete()
        }
    }

    if (uiState.isCompressing) {
        LoadingDialog(message = "Compressing...")
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Compress PDF", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isDark) {
                        Brush.verticalGradient(listOf(Color(0xFF40226D), Color(0xFF40226D)))
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFDFCAFF),
                                Color(0xFFF2EAFC),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                val strokeColor = MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            if (uiState.selectedUri == null) {
                                drawRoundRect(
                                    color = strokeColor,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 4f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                                    ),
                                    cornerRadius = CornerRadius(16.dp.toPx())
                                )
                            }
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (uiState.selectedUri == null) Color.Transparent else MaterialTheme.colorScheme.surface)
                        .clickable { pdfPicker.launch(arrayOf("application/pdf")) }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.selectedUri == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            }
                            Text(
                                text = "Select PDF",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.fileName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Size: ${formatSize(uiState.originalSize)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.Edit, "Change PDF", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(visible = uiState.selectedUri != null, enter = slideInVertically() + fadeIn(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Compression Quality: ${uiState.quality}%",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Slider(
                                value = uiState.quality.toFloat(),
                                onValueChange = { viewModel.setQuality(it.toInt()) },
                                valueRange = 1f..100f,
                                steps = 98,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        if (uiState.compressedSize > 0 && uiState.originalSize > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val savings = ((uiState.originalSize - uiState.compressedSize) * 100f / uiState.originalSize)
                                        .coerceAtLeast(0f)
                                    Text(
                                        text = "Compression Result",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Original:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text(formatSize(uiState.originalSize), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Compressed:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text(formatSize(uiState.compressedSize), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Savings:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text("${"%.1f".format(savings)}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                uiState.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(24.dp))

                val isButtonEnabled = uiState.selectedUri != null && !uiState.isCompressing
                Button(
                    onClick = { viewModel.compressPdf() },
                    enabled = isButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(if (isButtonEnabled) 8.dp else 0.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        text = "Compress",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
        bytes >= 1_024 -> "${"%.1f".format(bytes / 1_024.0)} KB"
        else -> "$bytes B"
    }
}
