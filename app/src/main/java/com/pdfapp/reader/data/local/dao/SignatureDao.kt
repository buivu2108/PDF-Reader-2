package com.pdfapp.reader.data.local.dao

import androidx.room.*
import com.pdfapp.reader.data.local.entity.SignatureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SignatureDao {

    @Query("SELECT * FROM signatures ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SignatureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(signature: SignatureEntity)

    @Query("DELETE FROM signatures WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM signatures")
    suspend fun getCount(): Int
}
