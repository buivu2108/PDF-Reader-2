package com.pdfapp.reader.ui.editmode.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vtsoft.pdfapp.reader.R

/**
 * Dialog with number input for jumping to a specific page.
 * Shows validation error if page number is out of range.
 *
 * @param currentPage 0-indexed current page (pre-filled)
 * @param pageCount total page count
 * @param onPageSelected callback with 0-indexed page when user confirms
 * @param onDismiss callback when dialog is dismissed
 */
@Composable
fun PageNumberInputDialog(
    currentPage: Int,
    pageCount: Int,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf((currentPage + 1).toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    fun validate(): Int? {
        val num = text.trim().toIntOrNull()
        if (num == null || num < 1 || num > pageCount) {
            error = "Enter a number between 1 and $pageCount"
            return null
        }
        error = null
        return num - 1 // convert to 0-indexed
    }

    fun submit() {
        val idx = validate() ?: return
        onPageSelected(idx)
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_go_to_page)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    label = { Text(stringResource(R.string.edit_page_number_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }) {
                Text(stringResource(R.string.edit_go))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
