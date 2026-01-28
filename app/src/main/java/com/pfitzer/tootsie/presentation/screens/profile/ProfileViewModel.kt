package com.pfitzer.tootsie.presentation.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.usecase.account.*
import com.pfitzer.tootsie.domain.usecase.status.*
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Profile Screen.
 * Manages account information, relationship status, and user timeline.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getAccountUseCase: GetAccountUseCase,
    private val getRelationshipUseCase: GetRelationshipUseCase,
    private val followAccountUseCase: FollowAccountUseCase,
    private val unfollowAccountUseCase: UnfollowAccountUseCase,
    private val getAccountStatusesUseCase: GetAccountStatusesUseCase,
    private val observeActiveAccountUseCase: ObserveActiveAccountUseCase,
    private val favoriteStatusUseCase: FavoriteStatusUseCase,
    private val unfavoriteStatusUseCase: UnfavoriteStatusUseCase,
    private val reblogStatusUseCase: ReblogStatusUseCase,
    private val unreblogStatusUseCase: UnreblogStatusUseCase,
    private val bookmarkStatusUseCase: BookmarkStatusUseCase,
    private val unbookmarkStatusUseCase: UnbookmarkStatusUseCase,
    private val pinStatusUseCase: PinStatusUseCase,
    private val unpinStatusUseCase: UnpinStatusUseCase,
    private val voteOnPollUseCase: VoteOnPollUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String? = savedStateHandle["accountId"]

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Observe the currently active account.
     */
    val activeAccount: StateFlow<Account?> = observeActiveAccountUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Paging data flow for account timeline.
     */
    val timelinePagingData: Flow<PagingData<Status>>? = accountId?.let { id ->
        getAccountStatusesUseCase(
            accountId = id,
            excludeReplies = false,
            excludeReblogs = false
        ).cachedIn(viewModelScope)
    }

    init {
        loadProfile()
    }

    /**
     * Load profile information.
     * If accountId is null, load current user's profile.
     */
    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Load account info
            val accountResult = getAccountUseCase(accountId)

            when (accountResult) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            account = accountResult.data,
                            isOwnProfile = accountId == null
                        )
                    }

                    // Load relationship if viewing another user's profile
                    if (accountId != null) {
                        loadRelationship(accountId)
                    }

                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load profile: ${accountResult.message}"
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Load relationship status with the account.
     */
    private suspend fun loadRelationship(accountId: String) {
        when (val result = getRelationshipUseCase(accountId)) {
            is Result.Success -> {
                _uiState.update { it.copy(relationship = result.data) }
            }
            is Result.Error -> {
                // Don't show error for relationship - it's not critical
                _uiState.update { it.copy(relationship = null) }
            }
            is Result.Loading -> Unit
        }
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.FollowAccount -> followAccount()
            is ProfileAction.UnfollowAccount -> unfollowAccount()
            is ProfileAction.Refresh -> loadProfile()
            is ProfileAction.DismissError -> dismissError()
            is ProfileAction.FavoriteStatus -> favoriteStatus(action.status)
            is ProfileAction.UnfavoriteStatus -> unfavoriteStatus(action.status)
            is ProfileAction.BoostStatus -> boostStatus(action.status)
            is ProfileAction.UnboostStatus -> unboostStatus(action.status)
            is ProfileAction.BookmarkStatus -> bookmarkStatus(action.status)
            is ProfileAction.UnbookmarkStatus -> unbookmarkStatus(action.status)
            is ProfileAction.PinStatus -> pinStatus(action.status)
            is ProfileAction.UnpinStatus -> unpinStatus(action.status)
            is ProfileAction.VoteOnPoll -> voteOnPoll(action.pollId, action.choices)
        }
    }

    /**
     * Favorite a status.
     */
    private fun favoriteStatus(status: Status) {
        viewModelScope.launch {
            when (val result = favoriteStatusUseCase(status.id)) {
                is Result.Success -> {
                    // Paging will automatically update if using a reactive Room source,
                    // otherwise we might need to manually refresh or update the paging data.
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to favorite status") }
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
            when (val result = unfavoriteStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to unfavorite status") }
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
            when (val result = reblogStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to boost status") }
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
            when (val result = unreblogStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to unboost status") }
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
            when (val result = bookmarkStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to bookmark status") }
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
            when (val result = unbookmarkStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to unbookmark status") }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Pin a status to profile.
     */
    private fun pinStatus(status: Status) {
        viewModelScope.launch {
            when (val result = pinStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to pin status") }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unpin a status from profile.
     */
    private fun unpinStatus(status: Status) {
        viewModelScope.launch {
            when (val result = unpinStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to unpin status") }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Vote on a poll.
     */
    private fun voteOnPoll(pollId: String, choices: List<Int>) {
        viewModelScope.launch {
            when (val result = voteOnPollUseCase(pollId, choices)) {
                is Result.Success -> {
                    // For polls, we often want to refresh to see updated results
                    loadProfile()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Failed to vote: ${result.message}") }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Follow the account.
     */
    private fun followAccount() {
        val currentAccountId = accountId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingFollow = true) }

            when (val result = followAccountUseCase(currentAccountId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            relationship = result.data,
                            isProcessingFollow = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingFollow = false,
                            errorMessage = "Failed to follow account"
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unfollow the account.
     */
    private fun unfollowAccount() {
        val currentAccountId = accountId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingFollow = true) }

            when (val result = unfollowAccountUseCase(currentAccountId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            relationship = result.data,
                            isProcessingFollow = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingFollow = false,
                            errorMessage = "Failed to unfollow account"
                        )
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
 * UI state for Profile Screen.
 */
data class ProfileUiState(
    val account: Account? = null,
    val relationship: Relationship? = null,
    val isOwnProfile: Boolean = false,
    val isLoading: Boolean = false,
    val isProcessingFollow: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for Profile Screen.
 */
sealed class ProfileAction {
    data object FollowAccount : ProfileAction()
    data object UnfollowAccount : ProfileAction()
    data object Refresh : ProfileAction()
    data object DismissError : ProfileAction()
    data class FavoriteStatus(val status: Status) : ProfileAction()
    data class UnfavoriteStatus(val status: Status) : ProfileAction()
    data class BoostStatus(val status: Status) : ProfileAction()
    data class UnboostStatus(val status: Status) : ProfileAction()
    data class BookmarkStatus(val status: Status) : ProfileAction()
    data class UnbookmarkStatus(val status: Status) : ProfileAction()
    data class PinStatus(val status: Status) : ProfileAction()
    data class UnpinStatus(val status: Status) : ProfileAction()
    data class VoteOnPoll(val pollId: String, val choices: List<Int>) : ProfileAction()
}
