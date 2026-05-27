package com.pdfapp.reader.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.pdfapp.reader.data.local.dao.SignatureDao
import com.pdfapp.reader.data.local.entity.SignatureEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class SignatureRepository(
    private val dao: SignatureDao,
    private val context: Context
) {

    fun getAll(): Flow<List<SignatureEntity>> = dao.getAll()

    /** Max number of saved signatures allowed. */
    companion object {
        const val MAX_SIGNATURES = 5
    }

    /** Saves a bitmap signature to internal storage and persists metadata to DB. */
    suspend fun save(
        bitmap: Bitmap,
        name: String = "",
        color: Int = 0xFF000000.toInt()
    ): SignatureEntity? = withContext(Dispatchers.IO) {
        if (dao.getCount() >= MAX_SIGNATURES) return@withContext null
        val id = UUID.randomUUID().toString()
        val sigDir = File(context.filesDir, "signatures").apply { mkdirs() }
        val file = File(sigDir, "$id.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val entity = SignatureEntity(
            id = id,
            imagePath = file.absolutePath,
            name = name,
            color = color,
            createdAt = System.currentTimeMillis()
        )
        dao.insert(entity)
        entity
    }

    /** Check if at max capacity. */
    suspend fun isAtLimit(): Boolean = withContext(Dispatchers.IO) {
        dao.getCount() >= MAX_SIGNATURES
    }

    suspend fun deleteById(id: String) {
        // Remove file from storage
        val file = File(context.filesDir, "signatures/$id.png")
        if (file.exists()) file.delete()
        dao.deleteById(id)
    }
}
