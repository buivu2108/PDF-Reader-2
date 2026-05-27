package com.pdfapp.reader.ui.tools.merge

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
fun MergePdfScreen(
    viewModel: MergePdfViewModel,
    adManager: AdManager,
    onBack: () -> Unit,
    onOpenPdf: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> uris.forEach { viewModel.addPdf(it) } }

    LaunchedEffect(uiState.resultUri) {
        uiState.resultUri?.let { uri ->
            Toast.makeText(context, "PDFs merged successfully", Toast.LENGTH_SHORT).show()
            val onComplete = { viewModel.clearResult(); onOpenPdf(uri.toString()) }
            (context as? Activity)?.let { adManager.showInterstitial(it, onComplete) } ?: onComplete()
        }
    }

    if (uiState.isMerging) {
        LoadingDialog(message = "Merging PDFs…")
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Merge PDF", fontWeight = FontWeight.Bold) },
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
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                val strokeColor = MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRoundRect(
                                color = strokeColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 4f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                                ),
                                cornerRadius = CornerRadius(16.dp.toPx())
                            )
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { pdfPicker.launch(arrayOf("application/pdf")) }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                        Text(
                            text = "Add PDF Files",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(visible = uiState.selectedFiles.isNotEmpty(), enter = slideInVertically() + fadeIn(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                        Text(
                            text = "${uiState.selectedFiles.size} files, ${uiState.selectedFiles.sumOf { it.pageCount }} pages total",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxHeight(0.7f)
                        ) {
                            itemsIndexed(uiState.selectedFiles, key = { i, item -> "${item.uri}_$i" }) { index, item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            IconButton(
                                                onClick = { viewModel.movePdf(index, index - 1) },
                                                enabled = index > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) { Icon(Icons.Default.KeyboardArrowUp, null, tint = if (index > 0) MaterialTheme.colorScheme.primary else Color.Gray) }
                                            IconButton(
                                                onClick = { viewModel.movePdf(index, index + 1) },
                                                enabled = index < uiState.selectedFiles.size - 1,
                                                modifier = Modifier.size(28.dp)
                                            ) { Icon(Icons.Default.KeyboardArrowDown, null, tint = if (index < uiState.selectedFiles.size - 1) MaterialTheme.colorScheme.primary else Color.Gray) }
                                        }
                                        
                                        Spacer(Modifier.width(12.dp))
                                        Icon(Icons.Rounded.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                        Spacer(Modifier.width(12.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                                            Text("${item.pageCount} pages", style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        
                                        IconButton(onClick = { viewModel.removePdf(index) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                        }
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

                val isButtonEnabled = uiState.selectedFiles.size >= 2 && !uiState.isMerging
                Button(
                    onClick = { viewModel.mergePdfs() },
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
                        text = "Merge PDFs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
