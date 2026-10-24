package com.pfitzer.tootsie.presentation.screens.search

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Hashtag
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.domain.usecase.search.SearchUseCase
import com.pfitzer.tootsie.domain.usecase.status.*
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.domain.model.withInteractions
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for Search Screen.
 * Manages search query, results, and filtering.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val statusInteractionStore: StatusInteractionStore,
    private val searchUseCase: SearchUseCase,
    private val observeActiveAccountUseCase: ObserveActiveAccountUseCase,
    private val favoriteStatusUseCase: FavoriteStatusUseCase,
    private val unfavoriteStatusUseCase: UnfavoriteStatusUseCase,
    private val reblogStatusUseCase: ReblogStatusUseCase,
    private val unreblogStatusUseCase: UnreblogStatusUseCase,
    private val bookmarkStatusUseCase: BookmarkStatusUseCase,
    private val unbookmarkStatusUseCase: UnbookmarkStatusUseCase,
    private val voteOnPollUseCase: VoteOnPollUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> =
        combine(_uiState, statusInteractionStore.interactions) { state, interactions ->
            state.copy(statuses = state.statuses.map { it.withInteractions(interactions) })
        }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value)

    /**
     * Observe the currently active account.
     */
    val activeAccount: StateFlow<Account?> = observeActiveAccountUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _searchQuery = MutableStateFlow("")

    init {
        // Set up debounced search
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Wait 300ms after user stops typing
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.UpdateQuery -> updateQuery(action.query)
            is SearchAction.SelectFilter -> selectFilter(action.filter)
            is SearchAction.ClearSearch -> clearSearch()
            is SearchAction.DismissError -> dismissError()
            is SearchAction.FavoriteStatus -> favoriteStatus(action.status)
            is SearchAction.UnfavoriteStatus -> unfavoriteStatus(action.status)
            is SearchAction.BoostStatus -> boostStatus(action.status)
            is SearchAction.UnboostStatus -> unboostStatus(action.status)
            is SearchAction.BookmarkStatus -> bookmarkStatus(action.status)
            is SearchAction.UnbookmarkStatus -> unbookmarkStatus(action.status)
            is SearchAction.VoteOnPoll -> voteOnPoll(action.pollId, action.choices)
        }
    }

    /**
     * Favorite a status.
     */
    private fun favoriteStatus(status: Status) {
        viewModelScope.launch {
            when (val result = favoriteStatusUseCase(status.id)) {
                is Result.Success -> {
                    // Refresh results to show update
                    performSearch(_uiState.value.query)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_favorite)) }
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
                is Result.Success -> {
                    performSearch(_uiState.value.query)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_unfavorite)) }
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
                is Result.Success -> {
                    performSearch(_uiState.value.query)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_boost)) }
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
                is Result.Success -> {
                    performSearch(_uiState.value.query)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_unboost)) }
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
                is Result.Success -> {
                    performSearch(_uiState.value.query)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_bookmark)) }
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
                is Result.Success -> {
                    performSearch(_uiState.value.query)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_unbookmark)) }
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
                    performSearch(_uiState.value.query)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_vote)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Update search query and trigger search.
     */
    private fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        _searchQuery.value = query

        if (query.isBlank()) {
            clearResults()
        }
    }

    /**
     * Select a result type filter.
     */
    private fun selectFilter(filter: SearchFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }

        // Re-run search with the new filter if we have a query
        if (_uiState.value.query.isNotBlank()) {
            performSearch(_uiState.value.query)
        }
    }

    /**
     * Clear search query and results.
     */
    private fun clearSearch() {
        _uiState.update {
            SearchUiState()
        }
        _searchQuery.value = ""
    }

    /**
     * Clear search results only.
     */
    private fun clearResults() {
        _uiState.update {
            it.copy(
                accounts = emptyList(),
                statuses = emptyList(),
                hashtags = emptyList(),
                isLoading = false,
                errorMessage = null
            )
        }
    }

    /**
     * Perform search with current query.
     */
    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Determine search type based on filter
            val searchType = when (_uiState.value.selectedFilter) {
                SearchFilter.ALL -> null
                SearchFilter.ACCOUNTS -> "accounts"
                SearchFilter.STATUSES -> "statuses"
                SearchFilter.HASHTAGS -> "hashtags"
            }

            when (val result = searchUseCase(query = query, type = searchType)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            accounts = result.data.accounts,
                            statuses = result.data.statuses,
                            hashtags = result.data.hashtags,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.of(R.string.error_search)
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
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
 * UI state for Search screen.
 */
data class SearchUiState(
    val query: String = "",
    val selectedFilter: SearchFilter = SearchFilter.ALL,
    val accounts: List<Account> = emptyList(),
    val statuses: List<Status> = emptyList(),
    val hashtags: List<Hashtag> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null
) {
    /**
     * Check if there are any results.
     */
    val hasResults: Boolean
        get() = accounts.isNotEmpty() || statuses.isNotEmpty() || hashtags.isNotEmpty()

    /**
     * Check if results are empty (after search was performed).
     */
    val isEmpty: Boolean
        get() = !isLoading && query.isNotBlank() && !hasResults
}

/**
 * Search result filters.
 */
enum class SearchFilter(val displayName: String) {
    ALL("All"),
    ACCOUNTS("Accounts"),
    STATUSES("Posts"),
    HASHTAGS("Hashtags")
}

/**
 * User actions for Search screen.
 */
sealed class SearchAction {
    data class UpdateQuery(val query: String) : SearchAction()
    data class SelectFilter(val filter: SearchFilter) : SearchAction()
    data object ClearSearch : SearchAction()
    data object DismissError : SearchAction()
    data class FavoriteStatus(val status: Status) : SearchAction()
    data class UnfavoriteStatus(val status: Status) : SearchAction()
    data class BoostStatus(val status: Status) : SearchAction()
    data class UnboostStatus(val status: Status) : SearchAction()
    data class BookmarkStatus(val status: Status) : SearchAction()
    data class UnbookmarkStatus(val status: Status) : SearchAction()
    data class VoteOnPoll(val pollId: String, val choices: List<Int>) : SearchAction()
}
