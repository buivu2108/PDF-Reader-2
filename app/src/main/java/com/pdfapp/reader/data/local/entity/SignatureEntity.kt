package com.pdfapp.reader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey val id: String, // UUID
    val imagePath: String,
    val name: String = "",
    val color: Int = 0xFF000000.toInt(),
    val createdAt: Long
)
