package com.pdfapp.reader.ui.tools.imagetopdf

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.ui.components.LoadingDialog
import com.pdfapp.reader.util.AdManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    viewModel: ImageToPdfViewModel,
    adManager: AdManager,
    onBack: () -> Unit,
    onOpenPdf: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addImages(uris)
    }

    LaunchedEffect(uiState.resultUri) {
        uiState.resultUri?.let { uri ->
            Toast.makeText(context, context.getString(R.string.image_to_pdf_success), Toast.LENGTH_SHORT).show()
            val onComplete = {
                viewModel.clearResult()
                onOpenPdf(uri.toString())
            }
            (context as? Activity)?.let { adManager.showInterstitial(it, onComplete) } ?: onComplete()
        }
    }

    if (uiState.isCreating) {
        LoadingDialog(message = stringResource(R.string.image_to_pdf_creating))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) {
                    SolidColor(Color(0xFF40226D))
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFDFCAFF), // Vibrant pastel purple
                            Color(0xFFF2EAFC), // Mid light purple
                            MaterialTheme.colorScheme.surface
                        )
                    )
                }
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    title = { 
                        Text(
                            text = stringResource(R.string.image_to_pdf_title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // Dropzone area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .dashedBorder(
                            color = MaterialTheme.colorScheme.primary,
                            width = 2.dp,
                            radius = 24.dp
                        )
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .clickable {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
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
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.image_to_pdf_select),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.image_to_pdf_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }

                // Image preview strip
                if (uiState.selectedImages.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.image_to_pdf_selected_count, uiState.selectedImages.size),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(uiState.selectedImages) { index, uri ->
                                Box(modifier = Modifier.aspectRatio(1f)) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = stringResource(R.string.cd_image_number, index + 1),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 8.dp, end = 8.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .shadow(8.dp, RoundedCornerShape(16.dp))
                                    )
                                    // Close button badge
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .clickable { viewModel.removeImage(index) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.cd_remove),
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                // Error message
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Create PDF button
                Button(
                    onClick = { viewModel.createPdf() },
                    enabled = uiState.selectedImages.isNotEmpty() && !uiState.isCreating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = if (uiState.selectedImages.isNotEmpty()) 12.dp else 0.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = stringResource(R.string.image_to_pdf_create),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun Modifier.dashedBorder(color: Color, width: Dp, radius: Dp) = this.drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
        ),
        cornerRadius = CornerRadius(radius.toPx())
    )
}
