package com.pdfapp.reader.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.data.repository.SettingsRepository
import com.pdfapp.reader.domain.model.AppLanguage
import com.pdfapp.reader.domain.model.PageMode
import com.pdfapp.reader.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.EN,
    val readerMode: PageMode = PageMode.CONTINUOUS,
    val keepScreenOn: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        collectSettings()
    }

    private fun collectSettings() {
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsRepository.selectedLanguage.collect { lang ->
                _uiState.update { it.copy(language = lang) }
            }
        }
        viewModelScope.launch {
            settingsRepository.defaultReaderMode.collect { mode ->
                _uiState.update { it.copy(readerMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsRepository.keepScreenOn.collect { keep ->
                _uiState.update { it.copy(keepScreenOn = keep) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.localeCode))
        }
    }

    fun setReaderMode(mode: PageMode) {
        viewModelScope.launch {
            settingsRepository.setReaderMode(mode)
        }
    }

    fun setKeepScreenOn(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(value)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settingsRepository) as T
    }
}
