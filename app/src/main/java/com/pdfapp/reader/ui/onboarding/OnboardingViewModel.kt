package com.pdfapp.reader.ui.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class OnboardingUiState(
    val currentPage: Int = 0,
    val finished: Boolean = false
)

private const val TOTAL_PAGES = 3

class OnboardingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    fun nextPage() {
        val next = _uiState.value.currentPage + 1
        if (next >= TOTAL_PAGES) {
            finish()
        } else {
            _uiState.update { it.copy(currentPage = next) }
        }
    }

    fun setPage(page: Int) {
        _uiState.update { it.copy(currentPage = page.coerceIn(0, TOTAL_PAGES - 1)) }
    }

    fun skip() = finish()

    private fun finish() {
        _uiState.update { it.copy(finished = true) }
    }
}
