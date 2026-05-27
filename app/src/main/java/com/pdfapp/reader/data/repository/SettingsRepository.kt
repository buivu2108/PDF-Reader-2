package com.pdfapp.reader.data.repository

import com.pdfapp.reader.data.preferences.AppPreferences
import com.pdfapp.reader.domain.model.AppLanguage
import com.pdfapp.reader.domain.model.PageMode
import com.pdfapp.reader.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val prefs: AppPreferences) {

    val isFirstLaunch: Flow<Boolean> = prefs.isFirstLaunch

    val selectedLanguage: Flow<AppLanguage> = prefs.selectedLanguage
        .map { code -> AppLanguage.fromLocaleCode(code) }

    val themeMode: Flow<ThemeMode> = prefs.themeMode
        .map { value -> ThemeMode.fromString(value) }

    val defaultReaderMode: Flow<PageMode> = prefs.defaultReaderMode
        .map { value -> PageMode.fromString(value) }

    val keepScreenOn: Flow<Boolean> = prefs.keepScreenOn

    suspend fun setFirstLaunch(value: Boolean) = prefs.setFirstLaunch(value)

    suspend fun setLanguage(language: AppLanguage) = prefs.setLanguage(language.localeCode)

    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode.name.lowercase())

    suspend fun setReaderMode(mode: PageMode) = prefs.setReaderMode(mode.name.lowercase())

    suspend fun setKeepScreenOn(value: Boolean) = prefs.setKeepScreenOn(value)
}
