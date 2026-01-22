package com.pfitzer.tootsie.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.TimelineFilter
import com.pfitzer.tootsie.domain.model.TimelineType
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.domain.usecase.status.*
import com.pfitzer.tootsie.domain.usecase.timeline.GetTimelineUseCase
import com.pfitzer.tootsie.domain.usecase.timeline.RefreshTimelineUseCase
import com.pfitzer.tootsie.domain.usecase.timeline.UpdateTimelineFilterUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home Screen.
 * Manages timeline data and status interactions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTimelineUseCase: GetTimelineUseCase,
    private val refreshTimelineUseCase: RefreshTimelineUseCase,
    private val updateTimelineFilterUseCase: UpdateTimelineFilterUseCase,
    private val observeActiveAccountUseCase: ObserveActiveAccountUseCase,
    private val preferencesManager: PreferencesManager,
    private val favoriteStatusUseCase: FavoriteStatusUseCase,
    private val unfavoriteStatusUseCase: UnfavoriteStatusUseCase,
    private val reblogStatusUseCase: ReblogStatusUseCase,
    private val unreblogStatusUseCase: UnreblogStatusUseCase,
    private val bookmarkStatusUseCase: BookmarkStatusUseCase,
    private val unbookmarkStatusUseCase: UnbookmarkStatusUseCase,
    private val voteOnPollUseCase: VoteOnPollUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Observe the currently active account.
     */
    val activeAccount: StateFlow<Account?> = observeActiveAccountUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _activeTimelineType = MutableStateFlow(TimelineType.HOME)
    val activeTimelineType: StateFlow<TimelineType> = _activeTimelineType.asStateFlow()

    /**
     * Timeline filter from preferences.
     */
    val timelineFilter: StateFlow<TimelineFilter> = preferencesManager.timelineFilter
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TimelineFilter.DEFAULT
        )

    /**
     * Paging data flow for timeline with filter applied.
     * Automatically refreshes when either the filter, the active account OR the timeline type changes.
     */
    val timelinePagingData: Flow<PagingData<Status>> = combine(
        timelineFilter,
        _activeTimelineType,
        activeAccount.map { it?.id }.distinctUntilChanged()
    ) { filter, timelineType, _ ->
        Triple(filter, timelineType, null)
    }
        .flatMapLatest { (filter, timelineType, _) ->
            getTimelineUseCase(timelineType, filter)
        }
        .cachedIn(viewModelScope)

    /**
     * Handle user actions.
     */
    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.FavoriteStatus -> favoriteStatus(action.status)
            is HomeAction.UnfavoriteStatus -> unfavoriteStatus(action.status)
            is HomeAction.BoostStatus -> boostStatus(action.status)
            is HomeAction.UnboostStatus -> unboostStatus(action.status)
            is HomeAction.BookmarkStatus -> bookmarkStatus(action.status)
            is HomeAction.UnbookmarkStatus -> unbookmarkStatus(action.status)
            is HomeAction.RefreshTimeline -> refreshTimeline()
            is HomeAction.DismissError -> dismissError()
            is HomeAction.UpdateFilter -> updateFilter(action.filter)
            is HomeAction.ChangeTimelineType -> changeTimelineType(action.timelineType)
            is HomeAction.VoteOnPoll -> voteOnPoll(action.pollId, action.choices)
        }
    }

    /**
     * Vote on a poll.
     */
    private fun voteOnPoll(pollId: String, choices: List<Int>) {
        viewModelScope.launch {
            when (voteOnPollUseCase(pollId, choices)) {
                is Result.Success -> {
                    // Success - Poll will refresh automatically via Room Flow if it's cached.
                    // If not cached, we might need a manual refresh or observe poll directly.
                    // For now, we assume it's part of a status and will refresh when the status updates.
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

    /**
     * Favorite a status.
     */
    private fun favoriteStatus(status: Status) {
        viewModelScope.launch {
            when (favoriteStatusUseCase(status.id)) {
                is Result.Success -> {
                    // Success - Paging will refresh automatically via Room Flow
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
                    // Success - Paging will refresh automatically via Room Flow
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
                    // Success - Paging will refresh automatically via Room Flow
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
                    // Success - Paging will refresh automatically via Room Flow
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
                    // Success - Paging will refresh automatically via Room Flow
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
                    // Success - Paging will refresh automatically via Room Flow
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
     * Refresh the timeline.
     */
    private fun refreshTimeline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                refreshTimelineUseCase(_activeTimelineType.value)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Change active timeline type.
     */
    private fun changeTimelineType(timelineType: TimelineType) {
        _activeTimelineType.value = timelineType
    }

    /**
     * Dismiss error message.
     */
    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Update timeline filter.
     */
    private fun updateFilter(filter: TimelineFilter) {
        viewModelScope.launch {
            updateTimelineFilterUseCase(filter)
        }
    }
}

/**
 * UI state for Home Screen.
 */
data class HomeUiState(
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for Home Screen.
 */
sealed class HomeAction {
    data class FavoriteStatus(val status: Status) : HomeAction()
    data class UnfavoriteStatus(val status: Status) : HomeAction()
    data class BoostStatus(val status: Status) : HomeAction()
    data class UnboostStatus(val status: Status) : HomeAction()
    data class BookmarkStatus(val status: Status) : HomeAction()
    data class UnbookmarkStatus(val status: Status) : HomeAction()
    data object RefreshTimeline : HomeAction()
    data object DismissError : HomeAction()
    data class UpdateFilter(val filter: TimelineFilter) : HomeAction()
    data class ChangeTimelineType(val timelineType: TimelineType) : HomeAction()
    data class VoteOnPoll(val pollId: String, val choices: List<Int>) : HomeAction()
}
