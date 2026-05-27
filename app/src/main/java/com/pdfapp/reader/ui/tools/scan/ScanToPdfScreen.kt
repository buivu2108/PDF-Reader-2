package com.pdfapp.reader.ui.tools.scan

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.domain.usecase.ScanDocumentUseCase
import com.pdfapp.reader.ui.components.LoadingDialog
import com.pdfapp.reader.util.AdManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanToPdfScreen(
    adManager: AdManager,
    onBack: () -> Unit,
    onOpenPdf: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val useCase = remember { ScanDocumentUseCase(context.applicationContext) }

    var isProcessing by remember { mutableStateOf(false) }
    var launched by remember { mutableStateOf(false) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pdfUri = scanResult?.pdf?.uri
            if (pdfUri != null) {
                isProcessing = true
                scope.launch {
                    try {
                        val savedUri = useCase.copyScannedPdf(pdfUri)
                        isProcessing = false
                        Toast.makeText(context, context.getString(R.string.scan_success), Toast.LENGTH_SHORT).show()
                        activity?.let { adManager.showInterstitial(it) { onOpenPdf(savedUri.toString()) } }
                            ?: onOpenPdf(savedUri.toString())
                    } catch (e: Exception) {
                        isProcessing = false
                        Toast.makeText(context, context.getString(R.string.tool_error, e.message ?: ""), Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                onBack()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.scan_cancelled), Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        if (!launched && activity != null) {
            launched = true
            val options = GmsDocumentScannerOptions.Builder()
                .setPageLimit(20)
                .setGalleryImportAllowed(true)
                .setResultFormats(
                    GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
                    GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
                )
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()

            val scanner = GmsDocumentScanning.getClient(options)
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener {
                    Toast.makeText(context, context.getString(R.string.scan_error), Toast.LENGTH_LONG).show()
                    onBack()
                }
        }
    }

    if (isProcessing) {
        LoadingDialog(message = stringResource(R.string.scan_saving))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (!isProcessing) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.scan_launching),
                    modifier = Modifier.padding(top = 80.dp)
                )
            }
        }
    }
}
