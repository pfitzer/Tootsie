package com.pfitzer.tootsie.presentation.screens.trendingposts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.usecase.status.BookmarkStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.FavoriteStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.ReblogStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.UnbookmarkStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.UnfavoriteStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.UnreblogStatusUseCase
import com.pfitzer.tootsie.domain.usecase.trending.GetTrendingPostsUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Trending Posts Screen.
 */
@HiltViewModel
class TrendingPostsViewModel @Inject constructor(
    private val getTrendingPostsUseCase: GetTrendingPostsUseCase,
    private val favoriteStatusUseCase: FavoriteStatusUseCase,
    private val unfavoriteStatusUseCase: UnfavoriteStatusUseCase,
    private val reblogStatusUseCase: ReblogStatusUseCase,
    private val unreblogStatusUseCase: UnreblogStatusUseCase,
    private val bookmarkStatusUseCase: BookmarkStatusUseCase,
    private val unbookmarkStatusUseCase: UnbookmarkStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendingPostsUiState())
    val uiState: StateFlow<TrendingPostsUiState> = _uiState.asStateFlow()

    init {
        loadTrendingPosts()
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: TrendingPostsAction) {
        when (action) {
            is TrendingPostsAction.Refresh -> loadTrendingPosts()
            is TrendingPostsAction.FavoriteStatus -> favoriteStatus(action.status)
            is TrendingPostsAction.UnfavoriteStatus -> unfavoriteStatus(action.status)
            is TrendingPostsAction.BoostStatus -> boostStatus(action.status)
            is TrendingPostsAction.UnboostStatus -> unboostStatus(action.status)
            is TrendingPostsAction.BookmarkStatus -> bookmarkStatus(action.status)
            is TrendingPostsAction.UnbookmarkStatus -> unbookmarkStatus(action.status)
            is TrendingPostsAction.DismissError -> dismissError()
        }
    }

    /**
     * Load trending posts from the API.
     */
    private fun loadTrendingPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getTrendingPostsUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            trendingPosts = result.data,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Failed to load trending posts",
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
     * Favorite a status.
     */
    private fun favoriteStatus(status: Status) {
        viewModelScope.launch {
            when (favoriteStatusUseCase(status.id)) {
                is Result.Success -> {
                    // Success - update local state
                    _uiState.update { state ->
                        state.copy(
                            trendingPosts = state.trendingPosts.map {
                                if (it.id == status.id) it.copy(favourited = true, favouritesCount = it.favouritesCount + 1)
                                else it
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to favorite post") }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unfavorite a status.
     */
    private fun unfavoriteStatus(status: Status) {
        viewModelScope.launch {
            when (unfavoriteStatusUseCase(status.id)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            trendingPosts = state.trendingPosts.map {
                                if (it.id == status.id) it.copy(favourited = false, favouritesCount = maxOf(0, it.favouritesCount - 1))
                                else it
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to unfavorite post") }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Boost (reblog) a status.
     */
    private fun boostStatus(status: Status) {
        viewModelScope.launch {
            when (reblogStatusUseCase(status.id)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            trendingPosts = state.trendingPosts.map {
                                if (it.id == status.id) it.copy(reblogged = true, reblogsCount = it.reblogsCount + 1)
                                else it
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to boost post") }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unboost (unreblog) a status.
     */
    private fun unboostStatus(status: Status) {
        viewModelScope.launch {
            when (unreblogStatusUseCase(status.id)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            trendingPosts = state.trendingPosts.map {
                                if (it.id == status.id) it.copy(reblogged = false, reblogsCount = maxOf(0, it.reblogsCount - 1))
                                else it
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to unboost post") }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Bookmark a status.
     */
    private fun bookmarkStatus(status: Status) {
        viewModelScope.launch {
            when (bookmarkStatusUseCase(status.id)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            trendingPosts = state.trendingPosts.map {
                                if (it.id == status.id) it.copy(bookmarked = true)
                                else it
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to bookmark post") }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unbookmark a status.
     */
    private fun unbookmarkStatus(status: Status) {
        viewModelScope.launch {
            when (unbookmarkStatusUseCase(status.id)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            trendingPosts = state.trendingPosts.map {
                                if (it.id == status.id) it.copy(bookmarked = false)
                                else it
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to unbookmark post") }
                }
                is Result.Loading -> Unit
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
 * UI state for Trending Posts Screen.
 */
data class TrendingPostsUiState(
    val trendingPosts: List<Status> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for Trending Posts Screen.
 */
sealed class TrendingPostsAction {
    data object Refresh : TrendingPostsAction()
    data class FavoriteStatus(val status: Status) : TrendingPostsAction()
    data class UnfavoriteStatus(val status: Status) : TrendingPostsAction()
    data class BoostStatus(val status: Status) : TrendingPostsAction()
    data class UnboostStatus(val status: Status) : TrendingPostsAction()
    data class BookmarkStatus(val status: Status) : TrendingPostsAction()
    data class UnbookmarkStatus(val status: Status) : TrendingPostsAction()
    data object DismissError : TrendingPostsAction()
}
