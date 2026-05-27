package com.pdfapp.reader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R

/**
 * Modal dialog showing a spinner with an optional message.
 * Pass a non-null [onDismiss] to show a dismiss/cancel button.
 */
@Composable
fun LoadingDialog(
    message: String,
    onDismiss: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        confirmButton = {},
        dismissButton = if (onDismiss != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        } else null,
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}
