package com.pdfapp.reader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pdfapp.reader.data.local.dao.BookmarkDao
import com.pdfapp.reader.data.local.dao.PdfFileDao
import com.pdfapp.reader.data.local.dao.RecentFontDao
import com.pdfapp.reader.data.local.dao.SignatureDao
import com.pdfapp.reader.data.local.entity.BookmarkEntity
import com.pdfapp.reader.data.local.entity.PdfFileEntity
import com.pdfapp.reader.data.local.entity.RecentFontEntity
import com.pdfapp.reader.data.local.entity.SignatureEntity

@Database(
    entities = [PdfFileEntity::class, SignatureEntity::class, RecentFontEntity::class, BookmarkEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pdfFileDao(): PdfFileDao
    abstract fun signatureDao(): SignatureDao
    abstract fun recentFontDao(): RecentFontDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        /** Migration: add name and color columns to signatures table. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE signatures ADD COLUMN name TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE signatures ADD COLUMN color INTEGER NOT NULL DEFAULT ${0xFF000000.toInt()}")
            }
        }
    }
}
