package com.pdfapp.reader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Persisted bookmark: links a PDF URI + page index with an optional label. */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pdfUri: String,
    val pageIndex: Int,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
