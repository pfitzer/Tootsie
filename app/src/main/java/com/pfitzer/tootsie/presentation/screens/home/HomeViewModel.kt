package com.pfitzer.tootsie.presentation.screens.home

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.TimelineFilter
import com.pfitzer.tootsie.domain.model.TimelineType
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.domain.usecase.filter.GetFiltersForContextUseCase
import com.pfitzer.tootsie.domain.usecase.report.ReportUseCase
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
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.presentation.util.applyInteractions

/**
 * ViewModel for Home Screen.
 * Manages timeline data and status interactions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val statusInteractionStore: StatusInteractionStore,
    private val getTimelineUseCase: GetTimelineUseCase,
    private val refreshTimelineUseCase: RefreshTimelineUseCase,
    private val updateTimelineFilterUseCase: UpdateTimelineFilterUseCase,
    private val observeActiveAccountUseCase: ObserveActiveAccountUseCase,
    private val getFiltersForContextUseCase: GetFiltersForContextUseCase,
    private val preferencesManager: PreferencesManager,
    private val favoriteStatusUseCase: FavoriteStatusUseCase,
    private val unfavoriteStatusUseCase: UnfavoriteStatusUseCase,
    private val reblogStatusUseCase: ReblogStatusUseCase,
    private val unreblogStatusUseCase: UnreblogStatusUseCase,
    private val bookmarkStatusUseCase: BookmarkStatusUseCase,
    private val unbookmarkStatusUseCase: UnbookmarkStatusUseCase,
    private val voteOnPollUseCase: VoteOnPollUseCase,
    private val reportUseCase: ReportUseCase
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
     * Keyword filters for HOME context.
     */
    private val homeKeywordFilters: Flow<List<Filter>> = getFiltersForContextUseCase(FilterContext.HOME)

    /**
     * Keyword filters for PUBLIC context.
     */
    private val publicKeywordFilters: Flow<List<Filter>> = getFiltersForContextUseCase(FilterContext.PUBLIC)

    /**
     * Get keyword filters for the given timeline type.
     */
    private fun getKeywordFiltersForType(type: TimelineType): Flow<List<Filter>> {
        return when (type) {
            TimelineType.HOME -> homeKeywordFilters
            TimelineType.PUBLIC_LOCAL, TimelineType.PUBLIC_FEDERATED -> publicKeywordFilters
        }
    }

    /**
     * Paging data flow for timeline with filter applied.
     * Automatically refreshes when either the filter, the active account, timeline type, or keyword filters change.
     */
    val timelinePagingData: Flow<PagingData<Status>> = combine(
        timelineFilter,
        _activeTimelineType,
        activeAccount.map { it?.id }.distinctUntilChanged()
    ) { filter, timelineType, _ ->
        Pair(filter, timelineType)
    }
        .flatMapLatest { (filter, timelineType) ->
            getKeywordFiltersForType(timelineType).flatMapLatest { keywordFilters ->
                getTimelineUseCase(timelineType, filter, keywordFilters)
            }
        }
        .cachedIn(viewModelScope)
        .applyInteractions(statusInteractionStore)

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
            is HomeAction.DismissSuccess -> dismissSuccess()
            is HomeAction.UpdateFilter -> updateFilter(action.filter)
            is HomeAction.ChangeTimelineType -> changeTimelineType(action.timelineType)
            is HomeAction.VoteOnPoll -> voteOnPoll(action.pollId, action.choices)
            is HomeAction.ReportStatus -> reportStatus(action.status, action.category, action.comment, action.forward)
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
                        it.copy(errorMessage = UiText.of(R.string.error_vote))
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
                        it.copy(errorMessage = UiText.of(R.string.error_unfavorite))
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
                        it.copy(errorMessage = UiText.of(R.string.error_boost))
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
                        it.copy(errorMessage = UiText.of(R.string.error_unboost))
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
                        it.copy(errorMessage = UiText.of(R.string.error_bookmark))
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
                        it.copy(errorMessage = UiText.of(R.string.error_unbookmark))
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
     * Report a status.
     */
    private fun reportStatus(status: Status, category: String, comment: String?, forward: Boolean) {
        viewModelScope.launch {
            when (reportUseCase(
                accountId = status.account.id,
                statusIds = listOf(status.id),
                comment = comment,
                forward = forward,
                category = category
            )) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = UiText.of(R.string.success_report)) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_report)) }
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

    /**
     * Dismiss success message.
     */
    private fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
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
    val errorMessage: UiText? = null,
    val successMessage: UiText? = null
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
    data object DismissSuccess : HomeAction()
    data class UpdateFilter(val filter: TimelineFilter) : HomeAction()
    data class ChangeTimelineType(val timelineType: TimelineType) : HomeAction()
    data class VoteOnPoll(val pollId: String, val choices: List<Int>) : HomeAction()
    data class ReportStatus(
        val status: Status,
        val category: String,
        val comment: String?,
        val forward: Boolean
    ) : HomeAction()
}
