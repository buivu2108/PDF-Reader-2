package com.pdfapp.reader.ui.splash

import android.os.Build
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.data.preferences.AppPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SplashUiState(
    val isLoading: Boolean = true,
    val navigateTo: SplashDestination = SplashDestination.NONE
)

enum class SplashDestination { NONE, LANGUAGE, HOME, PERMISSION_STORAGE }

class SplashViewModel(private val preferences: AppPreferences) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState

    init {
        viewModelScope.launch {
            delay(2_000L)
            val isFirstLaunch = preferences.isFirstLaunch.first()
            val hasStorageAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true // READ_EXTERNAL_STORAGE handled by normal runtime permission
            }

            val destination = when {
                isFirstLaunch -> SplashDestination.LANGUAGE
                !hasStorageAccess -> SplashDestination.PERMISSION_STORAGE
                else -> SplashDestination.HOME
            }
            _uiState.value = SplashUiState(isLoading = false, navigateTo = destination)
        }
    }

    class Factory(private val preferences: AppPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SplashViewModel(preferences) as T
    }
}
