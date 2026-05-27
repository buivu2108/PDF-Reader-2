package com.pdfapp.reader.data.local.dao

import androidx.room.*
import com.pdfapp.reader.data.local.entity.RecentFontEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFontDao {

    @Query("SELECT * FROM recent_fonts ORDER BY lastUsed DESC")
    fun getAll(): Flow<List<RecentFontEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(font: RecentFontEntity)

    @Query("SELECT * FROM recent_fonts WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): RecentFontEntity?
}
