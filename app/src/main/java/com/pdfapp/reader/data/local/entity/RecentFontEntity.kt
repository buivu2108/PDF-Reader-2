package com.pdfapp.reader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_fonts")
data class RecentFontEntity(
    @PrimaryKey val name: String,
    val filePath: String?,
    val lastUsed: Long
)
