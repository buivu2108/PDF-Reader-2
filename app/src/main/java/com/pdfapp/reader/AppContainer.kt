package com.pdfapp.reader

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.room.Room
import com.pdfapp.reader.data.local.AppDatabase
import com.pdfapp.reader.data.preferences.AppPreferences
import com.pdfapp.reader.data.repository.FontRepository
import com.pdfapp.reader.data.repository.PdfFileRepository
import com.pdfapp.reader.data.repository.SettingsRepository
import com.pdfapp.reader.data.repository.SignatureRepository
import com.pdfapp.reader.domain.usecase.SaveAnnotatedPdfUseCase
import com.pdfapp.reader.ui.viewer.render.PdfRenderService
import com.pdfapp.reader.util.AdManager

/** Manual dependency injection container — single instance held by [App]. */
class AppContainer(private val context: Context) {

    val database: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "pdf_reader_db"
    ).addMigrations(AppDatabase.MIGRATION_1_2).fallbackToDestructiveMigration().build()

    val pdfFileDao = database.pdfFileDao()
    val signatureDao = database.signatureDao()
    val recentFontDao = database.recentFontDao()
    val bookmarkDao = database.bookmarkDao()

    val preferences = AppPreferences(context)

    val pdfFileRepository = PdfFileRepository(pdfFileDao, context)
    val settingsRepository = SettingsRepository(preferences)
    val signatureRepository = SignatureRepository(signatureDao, context)
    val fontRepository = FontRepository(recentFontDao)

    val saveAnnotatedPdfUseCase = SaveAnnotatedPdfUseCase(context)

    val adManager = AdManager()

    /** Creates a [PdfRenderService] for the given content URI. Caller owns lifecycle. */
    fun createPdfRenderService(uri: Uri): PdfRenderService {
        val fd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Cannot open PDF: $uri")
        return PdfRenderService(fd)
    }
}
