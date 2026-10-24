package com.pfitzer.tootsie.presentation.screens.hashtagtimeline

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import com.pfitzer.tootsie.domain.usecase.followedtags.FollowTagUseCase
import com.pfitzer.tootsie.domain.usecase.followedtags.GetFollowedTagsUseCase
import com.pfitzer.tootsie.domain.usecase.followedtags.UnfollowTagUseCase
import com.pfitzer.tootsie.domain.usecase.timeline.GetHashtagTimelineUseCase
import com.pfitzer.tootsie.domain.usecase.status.*
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.presentation.util.applyInteractions

/**
 * ViewModel for Hashtag Timeline Screen.
 * Manages hashtag timeline with pagination and status interactions.
 */
@HiltViewModel
class HashtagTimelineViewModel @Inject constructor(
    private val statusInteractionStore: StatusInteractionStore,
    private val getHashtagTimelineUseCase: GetHashtagTimelineUseCase,
    private val favoriteStatusUseCase: FavoriteStatusUseCase,
    private val unfavoriteStatusUseCase: UnfavoriteStatusUseCase,
    private val reblogStatusUseCase: ReblogStatusUseCase,
    private val unreblogStatusUseCase: UnreblogStatusUseCase,
    private val bookmarkStatusUseCase: BookmarkStatusUseCase,
    private val unbookmarkStatusUseCase: UnbookmarkStatusUseCase,
    private val voteOnPollUseCase: VoteOnPollUseCase,
    private val followTagUseCase: FollowTagUseCase,
    private val unfollowTagUseCase: UnfollowTagUseCase,
    private val getFollowedTagsUseCase: GetFollowedTagsUseCase,
    private val accountManagementRepository: AccountManagementRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val hashtag: String = savedStateHandle.get<String>("hashtag")
        ?: throw IllegalArgumentException("hashtag is required")

    private val _uiState = MutableStateFlow(HashtagTimelineUiState())
    val uiState: StateFlow<HashtagTimelineUiState> = _uiState.asStateFlow()

    /**
     * Observe the currently active account.
     */
    val activeAccount: StateFlow<Account?> = accountManagementRepository.observeActiveAccount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        checkFollowStatus()
    }

    private fun checkFollowStatus() {
        viewModelScope.launch {
            when (val result = getFollowedTagsUseCase()) {
                is Result.Success -> {
                    val isFollowing = result.data.any { it.name.equals(hashtag, ignoreCase = true) }
                    _uiState.update { it.copy(isFollowing = isFollowing) }
                }
                else -> { /* Ignore errors for follow status check */ }
            }
        }
    }

    /**
     * Paging data flow for hashtag timeline statuses.
     */
    val hashtagTimelinePagingData: Flow<PagingData<Status>> = getHashtagTimelineUseCase(hashtag)
        .cachedIn(viewModelScope)
        .applyInteractions(statusInteractionStore)

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
            is HashtagTimelineAction.VoteOnPoll -> voteOnPoll(action.pollId, action.choices)
            is HashtagTimelineAction.DismissError -> dismissError()
            is HashtagTimelineAction.FollowTag -> followTag()
            is HashtagTimelineAction.UnfollowTag -> unfollowTag()
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
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_vote)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Follow the current hashtag.
     */
    private fun followTag() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingFollow = true) }
            when (followTagUseCase(hashtag)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isFollowing = true, isProcessingFollow = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = UiText.of(R.string.error_follow_hashtag, hashtag),
                            isProcessingFollow = false
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unfollow the current hashtag.
     */
    private fun unfollowTag() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingFollow = true) }
            when (unfollowTagUseCase(hashtag)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isFollowing = false, isProcessingFollow = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = UiText.of(R.string.error_unfollow_hashtag, hashtag),
                            isProcessingFollow = false
                        )
                    }
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
                        it.copy(errorMessage = UiText.of(R.string.error_favorite))
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
                    // Reflected via StatusInteractionStore
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = UiText.of(R.string.error_unfavorite))
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
                    // Reflected via StatusInteractionStore
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = UiText.of(R.string.error_boost))
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
                    // Reflected via StatusInteractionStore
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = UiText.of(R.string.error_unboost))
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
                    // Reflected via StatusInteractionStore
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = UiText.of(R.string.error_bookmark))
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
                    // Reflected via StatusInteractionStore
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = UiText.of(R.string.error_unbookmark))
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
    val isFollowing: Boolean = false,
    val isProcessingFollow: Boolean = false,
    val errorMessage: UiText? = null
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
    data class VoteOnPoll(val pollId: String, val choices: List<Int>) : HashtagTimelineAction()
    data object FollowTag : HashtagTimelineAction()
    data object UnfollowTag : HashtagTimelineAction()
    data object DismissError : HashtagTimelineAction()
}
