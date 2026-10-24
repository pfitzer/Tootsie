package com.pfitzer.tootsie.presentation.screens.trending

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.TrendingTag
import com.pfitzer.tootsie.domain.usecase.trending.GetTrendingHashtagsUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Trending Hashtags Screen.
 */
@HiltViewModel
class TrendingHashtagsViewModel @Inject constructor(
    private val getTrendingHashtagsUseCase: GetTrendingHashtagsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendingHashtagsUiState())
    val uiState: StateFlow<TrendingHashtagsUiState> = _uiState.asStateFlow()

    init {
        loadTrendingHashtags()
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: TrendingHashtagsAction) {
        when (action) {
            is TrendingHashtagsAction.Refresh -> loadTrendingHashtags()
            is TrendingHashtagsAction.DismissError -> dismissError()
        }
    }

    /**
     * Load trending hashtags from the API.
     */
    private fun loadTrendingHashtags() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getTrendingHashtagsUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            trendingTags = result.data,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = UiText.of(R.string.error_load_trending_hashtags),
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
 * UI state for Trending Hashtags Screen.
 */
data class TrendingHashtagsUiState(
    val trendingTags: List<TrendingTag> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null
)

/**
 * User actions for Trending Hashtags Screen.
 */
sealed class TrendingHashtagsAction {
    data object Refresh : TrendingHashtagsAction()
    data object DismissError : TrendingHashtagsAction()
}
