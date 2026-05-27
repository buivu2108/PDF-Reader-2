package com.pdfapp.reader.data.repository

import com.pdfapp.reader.data.local.dao.RecentFontDao
import com.pdfapp.reader.data.local.entity.RecentFontEntity
import kotlinx.coroutines.flow.Flow

class FontRepository(private val dao: RecentFontDao) {

    fun getAll(): Flow<List<RecentFontEntity>> = dao.getAll()

    suspend fun addRecent(name: String, filePath: String?) {
        dao.insert(
            RecentFontEntity(
                name = name,
                filePath = filePath,
                lastUsed = System.currentTimeMillis()
            )
        )
    }

    suspend fun getByName(name: String): RecentFontEntity? = dao.getByName(name)
}
