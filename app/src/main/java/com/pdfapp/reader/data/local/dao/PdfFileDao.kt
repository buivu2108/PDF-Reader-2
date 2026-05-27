package com.pdfapp.reader.data.local.dao

import androidx.room.*
import com.pdfapp.reader.data.local.entity.PdfFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfFileDao {

    @Query("SELECT * FROM pdf_files ORDER BY lastModified DESC")
    fun getAll(): Flow<List<PdfFileEntity>>

    @Query("SELECT * FROM pdf_files")
    suspend fun getAllOnce(): List<PdfFileEntity>

    @Query("SELECT * FROM pdf_files WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): PdfFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<PdfFileEntity>)

    @Update
    suspend fun update(file: PdfFileEntity)

    @Query("DELETE FROM pdf_files WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM pdf_files")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(files: List<PdfFileEntity>) {
        deleteAll()
        insertAll(files)
    }

    @Query("SELECT * FROM pdf_files WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<PdfFileEntity>>

    @Query("UPDATE pdf_files SET isFavorite = NOT isFavorite WHERE uri = :uri")
    suspend fun toggleFavorite(uri: String)
}
