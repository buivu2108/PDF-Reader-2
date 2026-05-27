package com.pdfapp.reader.domain.usecase

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.pdfapp.reader.domain.model.PdfFileInfo
import com.pdfapp.reader.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Scans device storage for all PDF files using MediaStore API. */
class ScanPdfFilesUseCase(private val context: Context) {

    suspend operator fun invoke(): List<PdfFileInfo> = withContext(Dispatchers.IO) {
        val pdfs = mutableListOf<PdfFileInfo>()
        val seenPaths = mutableSetOf<String>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Unknown.pdf"
                val path = cursor.getString(dataCol) ?: ""
                val size = cursor.getLong(sizeCol)
                val dateModified = cursor.getLong(dateCol) * 1000

                // Deduplicate by path (or content URI id if path is empty on API 29+)
                val dedupeKey = path.ifEmpty { id.toString() }
                if (!seenPaths.add(dedupeKey)) continue

                val contentUri = ContentUris.withAppendedId(collection, id)

                pdfs.add(
                    PdfFileInfo(
                        uri = contentUri.toString(),
                        name = name,
                        path = path,
                        size = size,
                        pageCount = 0,
                        lastModified = dateModified,
                        lastOpened = null,
                        thumbnailPath = null,
                        isFavorite = false
                    )
                )
            }
        }
        pdfs
    }
}
