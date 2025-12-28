package com.pfitzer.tootsie.presentation.screens.trendinglinks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.TrendingLink
import com.pfitzer.tootsie.domain.usecase.trending.GetTrendingLinksUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Trending Links Screen.
 */
@HiltViewModel
class TrendingLinksViewModel @Inject constructor(
    private val getTrendingLinksUseCase: GetTrendingLinksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendingLinksUiState())
    val uiState: StateFlow<TrendingLinksUiState> = _uiState.asStateFlow()

    init {
        loadTrendingLinks()
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: TrendingLinksAction) {
        when (action) {
            is TrendingLinksAction.Refresh -> loadTrendingLinks()
            is TrendingLinksAction.DismissError -> dismissError()
        }
    }

    /**
     * Load trending links from the API.
     */
    private fun loadTrendingLinks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getTrendingLinksUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            trendingLinks = result.data,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Failed to load trending links",
                            isLoading = false
                        )
                    }
                }
                is Result.Loading -> {
                    // Already set loading above
                }
            }
        }
    }

    /**
     * Dismiss error message.
     */
    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for Trending Links Screen.
 */
data class TrendingLinksUiState(
    val trendingLinks: List<TrendingLink> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for Trending Links Screen.
 */
sealed class TrendingLinksAction {
    data object Refresh : TrendingLinksAction()
    data object DismissError : TrendingLinksAction()
}
