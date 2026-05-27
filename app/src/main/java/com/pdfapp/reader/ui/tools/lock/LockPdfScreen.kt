package com.pdfapp.reader.ui.tools.lock

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.ui.components.LoadingDialog
import com.pdfapp.reader.util.AdManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockPdfScreen(
    viewModel: LockPdfViewModel,
    adManager: AdManager,
    onBack: () -> Unit,
    onOpenPdf: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.selectPdf(it) }
    }

    LaunchedEffect(uiState.resultUri) {
        if (uiState.resultUri != null) {
            showSuccessDialog = true
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                viewModel.resetForNewLock()
            },
            title = { Text(stringResource(R.string.lock_title)) },
            text = { Text(stringResource(R.string.lock_success)) },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    viewModel.resetForNewLock()
                }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (uiState.isProcessing) {
        LoadingDialog(message = stringResource(R.string.lock_processing))
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lock_title), fontWeight = FontWeight.Bold) },
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
                                text = stringResource(R.string.lock_select_pdf),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = uiState.fileName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.Edit, "Change PDF", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(visible = uiState.selectedUri != null, enter = slideInVertically() + fadeIn(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = { viewModel.setPassword(it) },
                            label = { Text(stringResource(R.string.lock_password)) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
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

                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = { viewModel.setConfirmPassword(it) },
                            label = { Text(stringResource(R.string.lock_confirm_password)) },
                            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                    Icon(if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
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

                val isButtonEnabled = uiState.password.isNotBlank() && uiState.confirmPassword.isNotBlank() && !uiState.isProcessing && uiState.selectedUri != null
                Button(
                    onClick = { viewModel.lockPdf() },
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
                        text = stringResource(R.string.lock_execute),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
