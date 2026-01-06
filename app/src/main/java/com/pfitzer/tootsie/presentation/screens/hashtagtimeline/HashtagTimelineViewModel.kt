package com.pfitzer.tootsie.presentation.screens.hashtagtimeline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.usecase.timeline.GetHashtagTimelineUseCase
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
 * ViewModel for Hashtag Timeline Screen.
 * Manages hashtag timeline with pagination and status interactions.
 */
@HiltViewModel
class HashtagTimelineViewModel @Inject constructor(
    private val getHashtagTimelineUseCase: GetHashtagTimelineUseCase,
    private val favoriteStatusUseCase: FavoriteStatusUseCase,
    private val unfavoriteStatusUseCase: UnfavoriteStatusUseCase,
    private val reblogStatusUseCase: ReblogStatusUseCase,
    private val unreblogStatusUseCase: UnreblogStatusUseCase,
    private val bookmarkStatusUseCase: BookmarkStatusUseCase,
    private val unbookmarkStatusUseCase: UnbookmarkStatusUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val hashtag: String = savedStateHandle.get<String>("hashtag")
        ?: throw IllegalArgumentException("hashtag is required")

    private val _uiState = MutableStateFlow(HashtagTimelineUiState())
    val uiState: StateFlow<HashtagTimelineUiState> = _uiState.asStateFlow()

    /**
     * Paging data flow for hashtag timeline statuses.
     */
    val hashtagTimelinePagingData: Flow<PagingData<Status>> = getHashtagTimelineUseCase(hashtag)
        .cachedIn(viewModelScope)

    /**
     * Handle user actions.
     */
    fun onAction(action: HashtagTimelineAction) {
        when (action) {
            is HashtagTimelineAction.FavoriteStatus -> favoriteStatus(action.status)
            is HashtagTimelineAction.UnfavoriteStatus -> unfavoriteStatus(action.status)
            is HashtagTimelineAction.BoostStatus -> boostStatus(action.status)
            is HashtagTimelineAction.UnboostStatus -> unboostStatus(action.status)
            is HashtagTimelineAction.BookmarkStatus -> bookmarkStatus(action.status)
            is HashtagTimelineAction.UnbookmarkStatus -> unbookmarkStatus(action.status)
            is HashtagTimelineAction.DismissError -> dismissError()
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
                is Result.Loading -> {
                    // Loading state
                }
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
                is Result.Loading -> {
                    // Loading state
                }
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
                is Result.Loading -> {
                    // Loading state
                }
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
                is Result.Loading -> {
                    // Loading state
                }
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
                is Result.Loading -> {
                    // Loading state
                }
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
                is Result.Loading -> {
                    // Loading state
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
 * UI state for Hashtag Timeline Screen.
 */
data class HashtagTimelineUiState(
    val errorMessage: String? = null
)

/**
 * User actions for Hashtag Timeline Screen.
 */
sealed class HashtagTimelineAction {
    data class FavoriteStatus(val status: Status) : HashtagTimelineAction()
    data class UnfavoriteStatus(val status: Status) : HashtagTimelineAction()
    data class BoostStatus(val status: Status) : HashtagTimelineAction()
    data class UnboostStatus(val status: Status) : HashtagTimelineAction()
    data class BookmarkStatus(val status: Status) : HashtagTimelineAction()
    data class UnbookmarkStatus(val status: Status) : HashtagTimelineAction()
    data object DismissError : HashtagTimelineAction()
}
