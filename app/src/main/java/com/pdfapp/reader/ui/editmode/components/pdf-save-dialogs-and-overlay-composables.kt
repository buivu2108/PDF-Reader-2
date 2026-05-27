package com.pdfapp.reader.ui.editmode.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R

/**
 * Bottom sheet presenting Save (overwrite) and Save As (new file) options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveOptionsBottomSheet(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.save_options_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        // Save (overwrite) option
        ListItem(
            headlineContent = { Text(stringResource(R.string.save_overwrite)) },
            supportingContent = { Text(stringResource(R.string.save_overwrite_desc)) },
            leadingContent = { Icon(Icons.Default.Save, contentDescription = null) },
            modifier = Modifier.clickable { onSave() }
        )
        // Save As option
        ListItem(
            headlineContent = { Text(stringResource(R.string.save_as)) },
            supportingContent = { Text(stringResource(R.string.save_as_desc)) },
            leadingContent = { Icon(Icons.Default.SaveAs, contentDescription = null) },
            modifier = Modifier.clickable { onSaveAs() }
        )
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Confirmation dialog before overwriting the original file.
 */
@Composable
fun OverwriteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.overwrite_confirm_title)) },
        text = { Text(stringResource(R.string.overwrite_confirm_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.overwrite_confirm_btn), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Dialog for entering a new file name when using Save As.
 */
@Composable
fun SaveAsNameDialog(
    defaultName: String,
    onConfirm: (fileName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    val isValid = name.isNotBlank() && !name.contains(Regex("[/\\\\:*?\"<>|]"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_as_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.save_as_dialog_hint)) },
                    singleLine = true,
                    suffix = { Text(".pdf") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = isValid) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Fullscreen overlay with progress indicator, blocks all interaction during save.
 */
@Composable
fun SavingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { /* consume all touches */ } },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxSize()
        ) {}
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.edit_saving_pdf),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}
