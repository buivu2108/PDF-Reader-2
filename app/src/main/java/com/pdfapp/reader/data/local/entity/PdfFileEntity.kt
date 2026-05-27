package com.pdfapp.reader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_files")
data class PdfFileEntity(
    @PrimaryKey val uri: String,
    val name: String,
    val path: String,
    val size: Long,
    val pageCount: Int,
    val lastModified: Long,
    val lastOpened: Long?,
    val thumbnailPath: String?,
    val isFavorite: Boolean = false
)
