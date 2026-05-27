package com.pdfapp.reader.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        private val KEY_LANGUAGE = stringPreferencesKey("selected_language")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_READER_MODE = stringPreferencesKey("default_reader_mode")
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_FIRST_LAUNCH] ?: true }

    val selectedLanguage: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_LANGUAGE] ?: "en" }

    val themeMode: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_THEME_MODE] ?: "system" }

    val defaultReaderMode: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_READER_MODE] ?: "continuous" }

    val keepScreenOn: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_KEEP_SCREEN_ON] ?: false }

    suspend fun setFirstLaunch(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_FIRST_LAUNCH] = value }
    }

    suspend fun setLanguage(value: String) {
        context.dataStore.edit { prefs -> prefs[KEY_LANGUAGE] = value }
    }

    suspend fun setThemeMode(value: String) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = value }
    }

    suspend fun setReaderMode(value: String) {
        context.dataStore.edit { prefs -> prefs[KEY_READER_MODE] = value }
    }

    suspend fun setKeepScreenOn(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_KEEP_SCREEN_ON] = value }
    }
}
