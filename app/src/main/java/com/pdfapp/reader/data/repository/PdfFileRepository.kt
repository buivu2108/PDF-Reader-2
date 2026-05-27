package com.pdfapp.reader.data.repository

import android.content.Context
import android.net.Uri
import com.pdfapp.reader.data.local.dao.PdfFileDao
import com.pdfapp.reader.domain.model.PdfFileInfo
import com.pdfapp.reader.domain.model.toDomain
import com.pdfapp.reader.domain.model.toEntity
import com.pdfapp.reader.domain.usecase.ScanPdfFilesUseCase
import com.pdfapp.reader.util.ThumbnailGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PdfFileRepository(
    private val dao: PdfFileDao,
    private val context: Context
) {
    private val scanUseCase = ScanPdfFilesUseCase(context)

    fun getAllFiles(): Flow<List<PdfFileInfo>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    suspend fun getByUri(uri: String): PdfFileInfo? =
        dao.getByUri(uri)?.toDomain()

    suspend fun insertAll(files: List<PdfFileInfo>) =
        dao.insertAll(files.map { it.toEntity() })

    suspend fun updateFile(file: PdfFileInfo) =
        dao.update(file.toEntity())

    suspend fun deleteByUri(uri: String) =
        dao.deleteByUri(uri)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun toggleFavorite(uri: String) = dao.toggleFavorite(uri)

    fun searchFiles(query: String): Flow<List<PdfFileInfo>> =
        dao.searchByName(query).map { list -> list.map { it.toDomain() } }

    /** Scan device storage for PDF files via MediaStore and cache in Room. */
    suspend fun scanAndCacheDevicePdfs(): List<PdfFileInfo> {
        val scanned = scanUseCase()
        val existingByUri = dao.getAllOnce().associateBy { it.uri }
        val merged = scanned.map { newFile ->
            val existing = existingByUri[newFile.uri]
            if (existing != null) {
                newFile.copy(
                    isFavorite = existing.isFavorite,
                    lastOpened = existing.lastOpened ?: newFile.lastOpened,
                    thumbnailPath = existing.thumbnailPath ?: newFile.thumbnailPath
                )
            } else {
                newFile
            }
        }
        dao.replaceAll(merged.map { it.toEntity() })
        return merged
    }

    /**
     * Generate cover thumbnails for files that don't yet have one.
     * Runs on the calling coroutine's dispatcher (caller should use IO).
     */
    suspend fun generateMissingThumbnails() {
        dao.getAllOnce()
            .filter { it.thumbnailPath == null }
            .forEach { entity ->
                try {
                    val path = ThumbnailGenerator.generateThumbnail(context, Uri.parse(entity.uri))
                    if (path != null) {
                        dao.update(entity.copy(thumbnailPath = path))
                    }
                } catch (_: Exception) { /* skip failed thumbnails */ }
            }
    }
}
