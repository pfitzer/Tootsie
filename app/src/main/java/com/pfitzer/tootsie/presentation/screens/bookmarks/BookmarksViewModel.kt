package com.pfitzer.tootsie.presentation.screens.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.usecase.bookmarks.GetBookmarksUseCase
import com.pfitzer.tootsie.domain.usecase.status.BookmarkStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.FavoriteStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.ReblogStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.UnbookmarkStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.UnfavoriteStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.UnreblogStatusUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Bookmarks Screen.
 * Manages bookmarked statuses with pagination and status interactions.
 */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val getBookmarksUseCase: GetBookmarksUseCase,
    private val favoriteStatusUseCase: FavoriteStatusUseCase,
    private val unfavoriteStatusUseCase: UnfavoriteStatusUseCase,
    private val reblogStatusUseCase: ReblogStatusUseCase,
    private val unreblogStatusUseCase: UnreblogStatusUseCase,
    private val bookmarkStatusUseCase: BookmarkStatusUseCase,
    private val unbookmarkStatusUseCase: UnbookmarkStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    /**
     * Paging data flow for bookmarked statuses.
     */
    val bookmarksPagingData: Flow<PagingData<Status>> = getBookmarksUseCase()
        .cachedIn(viewModelScope)

    /**
     * Handle user actions.
     */
    fun onAction(action: BookmarksAction) {
        when (action) {
            is BookmarksAction.FavoriteStatus -> favoriteStatus(action.status)
            is BookmarksAction.UnfavoriteStatus -> unfavoriteStatus(action.status)
            is BookmarksAction.BoostStatus -> boostStatus(action.status)
            is BookmarksAction.UnboostStatus -> unboostStatus(action.status)
            is BookmarksAction.BookmarkStatus -> bookmarkStatus(action.status)
            is BookmarksAction.UnbookmarkStatus -> unbookmarkStatus(action.status)
            is BookmarksAction.DismissError -> dismissError()
        }
    }

    /**
     * Favorite a status.
     */
    private fun favoriteStatus(status: Status) {
        viewModelScope.launch {
            when (favoriteStatusUseCase(status.id)) {
                is Result.Success -> {
                    // Success - status state updated
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to favorite post")
                    }
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
                    // Success - status state updated
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to unfavorite post")
                    }
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
                    // Success - status state updated
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to boost post")
                    }
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
                    // Success - status state updated
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to unboost post")
                    }
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
                    // Success - status state updated
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to bookmark post")
                    }
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
                    // Success - status state updated
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to unbookmark post")
                    }
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
 * UI state for Bookmarks Screen.
 */
data class BookmarksUiState(
    val errorMessage: String? = null
)

/**
 * User actions for Bookmarks Screen.
 */
sealed class BookmarksAction {
    data class FavoriteStatus(val status: Status) : BookmarksAction()
    data class UnfavoriteStatus(val status: Status) : BookmarksAction()
    data class BoostStatus(val status: Status) : BookmarksAction()
    data class UnboostStatus(val status: Status) : BookmarksAction()
    data class BookmarkStatus(val status: Status) : BookmarksAction()
    data class UnbookmarkStatus(val status: Status) : BookmarksAction()
    data object DismissError : BookmarksAction()
}
