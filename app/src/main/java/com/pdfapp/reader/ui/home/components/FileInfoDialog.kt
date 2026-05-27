package com.pdfapp.reader.ui.home.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pdfapp.reader.domain.model.PdfFileInfo
import com.pdfapp.reader.util.toFormattedDate

/**
 * Dialog showing detailed metadata for a selected PDF file:
 * file name, path, size, page count, last modified, last opened.
 */
@Composable
fun FileInfoDialog(
    file: PdfFileInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(label = "Location", value = file.path)
                InfoRow(
                    label = "Size",
                    value = Formatter.formatFileSize(context, file.size)
                )
                InfoRow(
                    label = "Pages",
                    value = if (file.pageCount > 0) file.pageCount.toString() else "—"
                )
                InfoRow(
                    label = "Last modified",
                    value = file.lastModified.toFormattedDate()
                )
                InfoRow(
                    label = "Last opened",
                    value = file.lastOpened?.toFormattedDate() ?: "Never"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

/** Single label+value row inside the info dialog. */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.62f)
        )
    }
}
