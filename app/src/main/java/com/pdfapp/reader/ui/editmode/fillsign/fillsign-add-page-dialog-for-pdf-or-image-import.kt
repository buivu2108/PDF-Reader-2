package com.pdfapp.reader.ui.editmode.fillsign

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R

/**
 * Dialog for inserting external PDF or image as new pages into the current PDF.
 * Shows a position picker (insert after which page) and source type buttons (Image / PDF).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPageDialog(
    currentPageIndex: Int,
    pageCount: Int,
    insertAfterIndex: Int,
    onInsertAfterChanged: (Int) -> Unit,
    onImportImage: () -> Unit,
    onImportPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    val atBeginningLabel = stringResource(R.string.add_page_at_beginning)
    val insertAfterLabel = stringResource(R.string.add_page_insert_position)

    // Position options: -1 = at beginning, 0..pageCount-1 = after page N
    val positionOptions = buildList {
        add(-1 to atBeginningLabel)
        for (i in 0 until pageCount) {
            add(i to "$insertAfterLabel ${i + 1}")
        }
    }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_page_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Position selector dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    val selectedLabel = positionOptions.find { it.first == insertAfterIndex }?.second
                        ?: "$insertAfterLabel ${insertAfterIndex + 1}"
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(insertAfterLabel) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        positionOptions.forEach { (index, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onInsertAfterChanged(index)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Source type buttons: Import Image / Import PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onImportImage,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_page_import_image))
                    }
                    OutlinedButton(
                        onClick = onImportPdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_page_import_pdf))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
