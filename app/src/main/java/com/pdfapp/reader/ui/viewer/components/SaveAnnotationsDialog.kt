package com.pdfapp.reader.ui.viewer.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vtsoft.pdfapp.reader.R

/**
 * Dialog shown when user presses back with unsaved annotations.
 * Options: Save, Discard, Cancel.
 */
@Composable
fun SaveAnnotationsDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.viewer_save_annotations_title)) },
        text = { Text(stringResource(R.string.viewer_save_annotations_message)) },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.viewer_save_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.viewer_discard_btn))
            }
        }
    )
}
