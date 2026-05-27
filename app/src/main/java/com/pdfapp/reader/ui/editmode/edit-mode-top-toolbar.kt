package com.pdfapp.reader.ui.editmode

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Done
import com.vtsoft.pdfapp.reader.R

/** Top toolbar for Edit Mode: Close, Undo, Redo, Save. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditModeTopToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    isSaving: Boolean,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
            }
        },
        title = { },
        actions = {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.cd_undo))
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = stringResource(R.string.cd_redo))
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .background(
                        color = if (isSaving) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Done,
                    contentDescription = stringResource(R.string.cd_save),
                    tint = if (isSaving) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                           else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    )
}
