package com.pfitzer.tootsie.presentation.screens.statusdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.domain.usecase.status.GetStatusContextUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.domain.model.withInteractions
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for Status Detail Screen.
 * Manages loading and displaying a status with its conversation context.
 */
@HiltViewModel
class StatusDetailViewModel @Inject constructor(
    private val statusInteractionStore: StatusInteractionStore,
    private val statusRepository: StatusRepository,
    private val getStatusContextUseCase: GetStatusContextUseCase,
    private val favoriteStatusUseCase: com.pfitzer.tootsie.domain.usecase.status.FavoriteStatusUseCase,
    private val unfavoriteStatusUseCase: com.pfitzer.tootsie.domain.usecase.status.UnfavoriteStatusUseCase,
    private val reblogStatusUseCase: com.pfitzer.tootsie.domain.usecase.status.ReblogStatusUseCase,
    private val unreblogStatusUseCase: com.pfitzer.tootsie.domain.usecase.status.UnreblogStatusUseCase,
    private val bookmarkStatusUseCase: com.pfitzer.tootsie.domain.usecase.status.BookmarkStatusUseCase,
    private val unbookmarkStatusUseCase: com.pfitzer.tootsie.domain.usecase.status.UnbookmarkStatusUseCase,
    private val voteOnPollUseCase: com.pfitzer.tootsie.domain.usecase.status.VoteOnPollUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val statusId: String = checkNotNull(savedStateHandle["statusId"])

    private val _uiState = MutableStateFlow(StatusDetailUiState())
    val uiState: StateFlow<StatusDetailUiState> =
        combine(_uiState, statusInteractionStore.interactions) { state, interactions ->
            state.copy(
                status = state.status?.withInteractions(interactions),
                ancestors = state.ancestors.map { it.withInteractions(interactions) },
                descendants = state.descendants.map { it.withInteractions(interactions) }
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value)

    init {
        loadStatusAndContext()
    }

    /**
     * Load the status and its conversation context.
     */
    private fun loadStatusAndContext() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Load the main status
            when (val statusResult = statusRepository.getStatus(statusId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(status = statusResult.data) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load status: ${statusResult.message}"
                        )
                    }
                    return@launch
                }
                is Result.Loading -> Unit
            }

            // Load conversation context
            when (val contextResult = getStatusContextUseCase(statusId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            ancestors = contextResult.data.ancestors,
                            descendants = contextResult.data.descendants,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load conversation: ${contextResult.message}"
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: StatusDetailAction) {
        when (action) {
            is StatusDetailAction.Refresh -> loadStatusAndContext()
            is StatusDetailAction.DismissError -> dismissError()
            is StatusDetailAction.FavoriteStatus -> favoriteStatus(action.status)
            is StatusDetailAction.UnfavoriteStatus -> unfavoriteStatus(action.status)
            is StatusDetailAction.BoostStatus -> boostStatus(action.status)
            is StatusDetailAction.UnboostStatus -> unboostStatus(action.status)
            is StatusDetailAction.BookmarkStatus -> bookmarkStatus(action.status)
            is StatusDetailAction.UnbookmarkStatus -> unbookmarkStatus(action.status)
            is StatusDetailAction.VoteOnPoll -> voteOnPoll(action.pollId, action.choices)
            is StatusDetailAction.ShareStatus -> { /* Handled in UI */ }
            is StatusDetailAction.CopyLink -> { /* Handled in UI */ }
            is StatusDetailAction.CopyText -> { /* Handled in UI */ }
        }
    }

    /**
     * Vote on a poll.
     */
    private fun voteOnPoll(pollId: String, choices: List<Int>) {
        viewModelScope.launch {
            when (val result = voteOnPollUseCase(pollId, choices)) {
                is Result.Success -> {
                    // Refresh status to show updated poll results
                    loadStatusAndContext()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to vote on poll")
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Favorite a status.
     */
    private fun favoriteStatus(status: Status) {
        viewModelScope.launch {
            when (favoriteStatusUseCase(status.id)) {
                is Result.Success -> Unit // Reflected via StatusInteractionStore
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
                is Result.Success -> Unit // Reflected via StatusInteractionStore
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
     * Boost a status.
     */
    private fun boostStatus(status: Status) {
        viewModelScope.launch {
            when (reblogStatusUseCase(status.id)) {
                is Result.Success -> Unit // Reflected via StatusInteractionStore
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
     * Unboost a status.
     */
    private fun unboostStatus(status: Status) {
        viewModelScope.launch {
            when (unreblogStatusUseCase(status.id)) {
                is Result.Success -> Unit // Reflected via StatusInteractionStore
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
                is Result.Success -> Unit // Reflected via StatusInteractionStore
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
                is Result.Success -> Unit // Reflected via StatusInteractionStore
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to unbookmark post")
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }
}

/**
 * UI state for Status Detail Screen.
 */
data class StatusDetailUiState(
    val status: Status? = null,
    val ancestors: List<Status> = emptyList(),
    val descendants: List<Status> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for Status Detail Screen.
 */
sealed class StatusDetailAction {
    data object Refresh : StatusDetailAction()
    data object DismissError : StatusDetailAction()
    data class FavoriteStatus(val status: Status) : StatusDetailAction()
    data class UnfavoriteStatus(val status: Status) : StatusDetailAction()
    data class BoostStatus(val status: Status) : StatusDetailAction()
    data class UnboostStatus(val status: Status) : StatusDetailAction()
    data class BookmarkStatus(val status: Status) : StatusDetailAction()
    data class UnbookmarkStatus(val status: Status) : StatusDetailAction()
    data class VoteOnPoll(val pollId: String, val choices: List<Int>) : StatusDetailAction()
    data class ShareStatus(val status: Status) : StatusDetailAction()
    data class CopyLink(val status: Status) : StatusDetailAction()
    data class CopyText(val status: Status) : StatusDetailAction()
}
