package com.pdfapp.reader.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pdfapp.reader.data.repository.PdfFileRepository
import com.pdfapp.reader.domain.model.PdfFileInfo
import com.pdfapp.reader.domain.model.SortOption
import com.pdfapp.reader.domain.model.ViewMode
import com.pdfapp.reader.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val files: List<PdfFileInfo> = emptyList(),
    val filteredFiles: Map<String, List<PdfFileInfo>> = emptyMap(),
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE_NEWEST,
    val viewMode: ViewMode = ViewMode.GRID,
    val isLoading: Boolean = false,
    val isSearchActive: Boolean = false,
    val selectedFiles: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val errorMessage: String? = null,
    val showFavoritesOnly: Boolean = false
)

class HomeViewModel(
    private val pdfFileRepository: PdfFileRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        // Collect cached files from DB first, then trigger a fresh scan
        viewModelScope.launch {
            pdfFileRepository.getAllFiles().collect { cached ->
                val sorted = applySort(cached, _uiState.value.sortOption)
                _uiState.update { state ->
                    state.copy(
                        files = sorted,
                        filteredFiles = buildFilteredFiles(sorted, state.searchQuery, state.showFavoritesOnly, state.sortOption)
                    )
                }
            }
        }
        scanFiles()
    }

    /** Scan device storage, cache results, and refresh UI state. */
    fun scanFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                pdfFileRepository.scanAndCacheDevicePdfs()
                // Flow collector above will update files automatically after scan
                // Generate missing cover thumbnails in background (IO dispatcher)
                launch(Dispatchers.IO) {
                    pdfFileRepository.generateMissingThumbnails()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Filter displayed files by name query. */
    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredFiles = buildFilteredFiles(state.files, query, state.showFavoritesOnly, state.sortOption)
            )
        }
    }

    /** Apply a sort option and re-group files. */
    fun setSortOption(option: SortOption) {
        _uiState.update { state ->
            state.copy(
                sortOption = option,
                filteredFiles = buildFilteredFiles(state.files, state.searchQuery, state.showFavoritesOnly, option)
            )
        }
    }

    /** Toggle the favorites-only filter chip. */
    fun toggleFavoritesFilter() {
        _uiState.update { state ->
            val next = !state.showFavoritesOnly
            state.copy(
                showFavoritesOnly = next,
                filteredFiles = buildFilteredFiles(state.files, state.searchQuery, next, state.sortOption)
            )
        }
    }

    /** Persist isFavorite toggle for a file. */
    fun toggleFavorite(uri: String) {
        viewModelScope.launch {
            try {
                pdfFileRepository.toggleFavorite(uri)
                // DB flow collector will refresh `files` automatically
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Favorite failed: ${e.message}") }
            }
        }
    }

    /** Toggle between LIST and GRID view modes. */
    fun toggleViewMode() {
        _uiState.update { state ->
            state.copy(viewMode = if (state.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
        }
    }

    /** Show or hide the search bar. */
    fun toggleSearchActive() {
        _uiState.update { state ->
            val active = !state.isSearchActive
            // Clear query when closing search
            if (!active) {
                state.copy(
                    isSearchActive = false,
                    searchQuery = "",
                    filteredFiles = buildFilteredFiles(state.files, "", state.showFavoritesOnly, state.sortOption)
                )
            } else {
                state.copy(isSearchActive = true)
            }
        }
    }

    /** Toggle selection state for a file URI (enters multi-select mode on first selection). */
    fun toggleFileSelection(uri: String) {
        _uiState.update { state ->
            val updated = if (uri in state.selectedFiles) state.selectedFiles - uri
            else state.selectedFiles + uri
            state.copy(
                selectedFiles = updated,
                isMultiSelectMode = updated.isNotEmpty()
            )
        }
    }

    /** Clear all selected files and exit multi-select mode. */
    fun clearSelection() {
        _uiState.update { it.copy(selectedFiles = emptySet(), isMultiSelectMode = false) }
    }

    /** Select all currently displayed files. */
    fun selectAll() {
        _uiState.update { state ->
            val allUris = state.files.map { it.uri }.toSet()
            state.copy(selectedFiles = allUris, isMultiSelectMode = allUris.isNotEmpty())
        }
    }

    /** Delete all selected files and exit multi-select mode. */
    fun deleteSelectedFiles() {
        viewModelScope.launch {
            val toDelete = _uiState.value.selectedFiles.toList()
            toDelete.forEach { uri ->
                try { pdfFileRepository.deleteByUri(uri) } catch (_: Exception) {}
            }
            clearSelection()
        }
    }

    /** Rename a file by URI using SAF DocumentsContract. */
    fun renameFile(uri: String, newName: String) {
        viewModelScope.launch {
            try {
                val parsedUri = android.net.Uri.parse(uri)
                android.provider.DocumentsContract.renameDocument(context.contentResolver, parsedUri, newName)
                scanFiles()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Rename failed: ${e.message}") }
            }
        }
    }

    /** Delete a single file by URI. */
    fun deleteFile(uri: String) {
        viewModelScope.launch {
            try {
                pdfFileRepository.deleteByUri(uri)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    // --- Helpers ---

    private fun buildFilteredFiles(
        files: List<PdfFileInfo>,
        query: String,
        favoritesOnly: Boolean,
        sortOption: SortOption
    ): Map<String, List<PdfFileInfo>> {
        var result = files
        if (query.isNotBlank()) result = result.filter { it.name.contains(query, ignoreCase = true) }
        if (favoritesOnly) result = result.filter { it.isFavorite }
        return groupByFolder(applySort(result, sortOption))
    }

    private fun applySort(files: List<PdfFileInfo>, option: SortOption): List<PdfFileInfo> =
        when (option) {
            SortOption.NAME_ASC      -> files.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC     -> files.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_NEWEST   -> files.sortedByDescending { it.lastModified }
            SortOption.DATE_OLDEST   -> files.sortedBy { it.lastModified }
            SortOption.SIZE_LARGEST  -> files.sortedByDescending { it.size }
            SortOption.SIZE_SMALLEST -> files.sortedBy { it.size }
        }

    private fun groupByFolder(files: List<PdfFileInfo>): Map<String, List<PdfFileInfo>> =
        files.groupBy { FileUtils.getFolderName(it.path) }

    class Factory(
        private val repository: PdfFileRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository, context.applicationContext) as T
    }
}
