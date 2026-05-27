package com.pdfapp.reader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pdfapp.reader.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE pdfUri = :pdfUri ORDER BY pageIndex ASC")
    fun getBookmarksForPdf(pdfUri: String): Flow<List<BookmarkEntity>>

    @Insert
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE pdfUri = :pdfUri AND pageIndex = :pageIndex")
    suspend fun delete(pdfUri: String, pageIndex: Int)

    @Query("SELECT COUNT(*) > 0 FROM bookmarks WHERE pdfUri = :pdfUri AND pageIndex = :pageIndex")
    suspend fun isBookmarked(pdfUri: String, pageIndex: Int): Boolean
}
