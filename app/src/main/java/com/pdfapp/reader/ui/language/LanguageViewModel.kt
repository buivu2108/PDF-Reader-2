package com.pdfapp.reader.ui.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.data.preferences.AppPreferences
import com.pdfapp.reader.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class LanguageUiState(
    val languages: List<AppLanguage> = AppLanguage.entries,
    val selectedLanguage: AppLanguage = AppLanguage.EN,
    val confirmed: Boolean = false
)

class LanguageViewModel(private val preferences: AppPreferences) : ViewModel() {

    private val _uiState = MutableStateFlow(LanguageUiState())
    val uiState: StateFlow<LanguageUiState> = _uiState

    init {
        // Auto-detect device locale and pre-select matching language
        val deviceLocale = Locale.getDefault().language
        val detected = AppLanguage.fromLocaleCode(deviceLocale)
        _uiState.update { it.copy(selectedLanguage = detected) }
    }

    fun selectLanguage(language: AppLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun confirmSelection() {
        viewModelScope.launch {
            val code = _uiState.value.selectedLanguage.localeCode
            preferences.setLanguage(code)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
            _uiState.update { it.copy(confirmed = true) }
        }
    }

    class Factory(private val preferences: AppPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LanguageViewModel(preferences) as T
    }
}
