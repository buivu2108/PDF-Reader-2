package com.pdfapp.reader.domain.model

import com.pdfapp.reader.data.local.entity.PdfFileEntity

/** Domain model for a PDF file (UI-facing, decoupled from Room entity). */
data class PdfFileInfo(
    val uri: String,
    val name: String,
    val path: String,
    val size: Long,
    val pageCount: Int,
    val lastModified: Long,
    val lastOpened: Long?,
    val thumbnailPath: String?,
    val isFavorite: Boolean
)

fun PdfFileEntity.toDomain(): PdfFileInfo = PdfFileInfo(
    uri = uri,
    name = name,
    path = path,
    size = size,
    pageCount = pageCount,
    lastModified = lastModified,
    lastOpened = lastOpened,
    thumbnailPath = thumbnailPath,
    isFavorite = isFavorite
)

fun PdfFileInfo.toEntity(): PdfFileEntity = PdfFileEntity(
    uri = uri,
    name = name,
    path = path,
    size = size,
    pageCount = pageCount,
    lastModified = lastModified,
    lastOpened = lastOpened,
    thumbnailPath = thumbnailPath,
    isFavorite = isFavorite
)
