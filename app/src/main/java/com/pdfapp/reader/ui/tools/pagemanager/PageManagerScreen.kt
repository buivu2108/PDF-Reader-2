package com.pdfapp.reader.ui.tools.pagemanager

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RotateRight
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
fun PageManagerScreen(
    viewModel: PageManagerViewModel,
    adManager: AdManager,
    onBack: () -> Unit,
    onOpenPdf: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.selectPdf(context, it) }
    }

    LaunchedEffect(uiState.resultUri) {
        uiState.resultUri?.let { uri ->
            Toast.makeText(context, "PDF saved successfully", Toast.LENGTH_SHORT).show()
            val onComplete = {
                viewModel.clearResult()
                onOpenPdf(uri.toString())
            }
            (context as? Activity)?.let { adManager.showInterstitial(it, onComplete) } ?: onComplete()
        }
    }

    if (uiState.isProcessing) {
        LoadingDialog(message = "Applying changes…")
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Page Manager", fontWeight = FontWeight.Bold) },
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
                actions = {
                    if (uiState.selectedPageIndex != null) {
                        IconButton(
                            onClick = { uiState.selectedPageIndex?.let { viewModel.rotatePage(it) } },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { uiState.selectedPageIndex?.let { viewModel.deletePage(it) } },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.width(8.dp))
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
                                    text = "${uiState.pages.size} pages",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.Edit, "Change PDF", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(visible = uiState.selectedUri != null, enter = slideInVertically() + fadeIn(), exit = fadeOut(), modifier = Modifier.weight(1f)) {
                    if (uiState.selectedUri != null) {
                        PageThumbnailGrid(
                            pages = uiState.pages,
                            sourceUri = uiState.selectedUri!!,
                            selectedIndex = uiState.selectedPageIndex,
                            onPageClick = { viewModel.selectPage(it) },
                            modifier = Modifier.fillMaxSize()
                        )
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

                Spacer(Modifier.height(16.dp))

                val isButtonEnabled = uiState.selectedUri != null && !uiState.isProcessing
                Button(
                    onClick = { viewModel.applyChanges(context) },
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
                        text = "Apply Changes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
