package com.pdfapp.reader.ui.tools.split

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.ui.components.LoadingDialog
import com.pdfapp.reader.util.AdManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPdfScreen(
    viewModel: SplitPdfViewModel,
    adManager: AdManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var shownResultCount by remember { mutableIntStateOf(0) }
    val isDark = isSystemInDarkTheme()

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.selectPdf(it) }
    }

    LaunchedEffect(uiState.resultFiles.size) {
        val resultSize = uiState.resultFiles.size
        if (resultSize > 0 && resultSize != shownResultCount) {
            shownResultCount = resultSize
            val msg = context.getString(R.string.split_success, resultSize)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            (context as? Activity)?.let { activity ->
                adManager.showInterstitial(activity) {}
            }
        }
    }

    if (uiState.isProcessing) {
        LoadingDialog(message = stringResource(R.string.split_processing))
    }

    Scaffold(
        containerColor = Color.Transparent, // Let gradient show through
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.split_title), fontWeight = FontWeight.Bold) },
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

                // Modern Dropzone / File Info Banner
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
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.split_select_pdf),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Rounded.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.fileName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = stringResource(R.string.split_total_pages, uiState.totalPages),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Change PDF",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(visible = uiState.selectedUri != null, enter = slideInVertically() + fadeIn(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text(
                            text = stringResource(R.string.split_method),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Segmented Cards for Methods
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SplitMethod.entries.forEach { method ->
                                val isSelected = uiState.splitMethod == method
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { viewModel.setSplitMethod(method) }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (method) {
                                            SplitMethod.AT_PAGE -> stringResource(R.string.split_at_page)
                                            SplitMethod.EVERY_N -> stringResource(R.string.split_every_n)
                                            SplitMethod.CUSTOM -> stringResource(R.string.split_custom)
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Input Field
                        OutlinedTextField(
                            value = uiState.splitValue,
                            onValueChange = { viewModel.setSplitValue(it) },
                            label = {
                                Text(
                                    when (uiState.splitMethod) {
                                        SplitMethod.AT_PAGE -> stringResource(R.string.split_page_number)
                                        SplitMethod.EVERY_N -> stringResource(R.string.split_num_pages)
                                        SplitMethod.CUSTOM -> stringResource(R.string.split_custom_hint)
                                    }
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (uiState.splitMethod == SplitMethod.CUSTOM) KeyboardType.Text else KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Show success elegantly
                if (uiState.resultFiles.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.split_success, uiState.resultFiles.size),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        uiState.resultFiles.forEach { name ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text(text = name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(24.dp))

                // Create/Execute Pill Button
                val isButtonEnabled = uiState.selectedUri != null && uiState.splitValue.isNotBlank() && !uiState.isProcessing
                Button(
                    onClick = { viewModel.executeSplit() },
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
                        text = stringResource(R.string.split_execute),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
