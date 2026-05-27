package com.pdfapp.reader.ui.permission

import android.os.Build
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.data.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PermissionUiState(
    val isGranted: Boolean = false,
    val isDenied: Boolean = false,
    val isComplete: Boolean = false
)

class PermissionViewModel(private val preferences: AppPreferences?) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState

    /** Called after ActivityResultLauncher returns with the permission result. */
    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(isGranted = granted, isDenied = !granted) }
        if (granted) complete()
    }

    /** User skipped optional permission (camera). */
    fun skip() {
        complete()
    }

    private fun complete() {
        viewModelScope.launch {
            // Mark first-launch done only after camera step (preferences != null means camera screen)
            preferences?.setFirstLaunch(false)
            _uiState.update { it.copy(isComplete = true) }
        }
    }

    /** API 30+ requires MANAGE_EXTERNAL_STORAGE for PDF access. */
    fun needsManageStorage(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** Returns the storage permission string for API < 30. */
    fun getStoragePermission(): String = android.Manifest.permission.READ_EXTERNAL_STORAGE

    /** Check if manage storage is already granted (API 30+). */
    fun isManageStorageGranted(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

    /** Auto-grants storage step if MANAGE_EXTERNAL_STORAGE is already enabled. */
    fun autoGrantIfNotNeeded() {
        if (needsManageStorage() && isManageStorageGranted()) {
            complete()
        }
    }

    class Factory(private val preferences: AppPreferences?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PermissionViewModel(preferences) as T
    }
}
