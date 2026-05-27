package com.pdfapp.reader.ui.viewer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdfapp.reader.data.local.entity.BookmarkEntity

/** Bottom sheet listing bookmarks for the current PDF. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkSheet(
    bookmarks: List<BookmarkEntity>,
    onNavigate: (pageIndex: Int) -> Unit,
    onDelete: (BookmarkEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No bookmarks yet")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(bottom = 16.dp)) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(bookmark.pageIndex) }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page ${bookmark.pageIndex + 1}",
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(bookmark) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete bookmark")
                        }
                    }
                }
            }
        }
    }
}
