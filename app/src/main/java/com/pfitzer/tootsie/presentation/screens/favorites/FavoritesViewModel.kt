package com.pfitzer.tootsie.presentation.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.usecase.favorites.GetFavoritesUseCase
import com.pfitzer.tootsie.domain.usecase.status.*
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.presentation.util.applyInteractions

/**
 * ViewModel for Favorites Screen.
 * Manages favorited statuses with pagination and status interactions.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val statusInteractionStore: StatusInteractionStore,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val favoriteStatusUseCase: FavoriteStatusUseCase,
    private val unfavoriteStatusUseCase: UnfavoriteStatusUseCase,
    private val reblogStatusUseCase: ReblogStatusUseCase,
    private val unreblogStatusUseCase: UnreblogStatusUseCase,
    private val bookmarkStatusUseCase: BookmarkStatusUseCase,
    private val unbookmarkStatusUseCase: UnbookmarkStatusUseCase,
    private val voteOnPollUseCase: VoteOnPollUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    /**
     * Paging data flow for favorited statuses.
     */
    val favoritesPagingData: Flow<PagingData<Status>> = getFavoritesUseCase()
        .cachedIn(viewModelScope)
        .applyInteractions(statusInteractionStore)

    /**
     * Handle user actions.
     */
    fun onAction(action: FavoritesAction) {
        when (action) {
            is FavoritesAction.FavoriteStatus -> favoriteStatus(action.status)
            is FavoritesAction.UnfavoriteStatus -> unfavoriteStatus(action.status)
            is FavoritesAction.BoostStatus -> boostStatus(action.status)
            is FavoritesAction.UnboostStatus -> unboostStatus(action.status)
            is FavoritesAction.BookmarkStatus -> bookmarkStatus(action.status)
            is FavoritesAction.UnbookmarkStatus -> unbookmarkStatus(action.status)
            is FavoritesAction.VoteOnPoll -> voteOnPoll(action.pollId, action.choices)
            is FavoritesAction.DismissError -> dismissError()
        }
    }

    /**
     * Vote on a poll.
     */
    private fun voteOnPoll(pollId: String, choices: List<Int>) {
        viewModelScope.launch {
            when (val result = voteOnPollUseCase(pollId, choices)) {
                is Result.Success -> {
                    // Paging is reactive
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to vote: ${result.message}") }
                }
                is Result.Loading -> Unit
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
                    // Reflected via StatusInteractionStore
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
                    // Reflected via StatusInteractionStore
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
                    // Reflected via StatusInteractionStore
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
                    // Reflected via StatusInteractionStore
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
                    // Reflected via StatusInteractionStore
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
                    // Reflected via StatusInteractionStore
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
 * UI state for Favorites Screen.
 */
data class FavoritesUiState(
    val errorMessage: String? = null
)

/**
 * User actions for Favorites Screen.
 */
sealed class FavoritesAction {
    data class FavoriteStatus(val status: Status) : FavoritesAction()
    data class UnfavoriteStatus(val status: Status) : FavoritesAction()
    data class BoostStatus(val status: Status) : FavoritesAction()
    data class UnboostStatus(val status: Status) : FavoritesAction()
    data class BookmarkStatus(val status: Status) : FavoritesAction()
    data class UnbookmarkStatus(val status: Status) : FavoritesAction()
    data class VoteOnPoll(val pollId: String, val choices: List<Int>) : FavoritesAction()
    data object DismissError : FavoritesAction()
}
