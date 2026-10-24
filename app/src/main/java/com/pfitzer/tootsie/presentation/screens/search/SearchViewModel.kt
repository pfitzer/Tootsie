package com.pfitzer.tootsie.presentation.screens.search

import androidx.annotation.StringRes
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Hashtag
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.domain.usecase.followedtags.FollowTagUseCase
import com.pfitzer.tootsie.domain.usecase.followedtags.UnfollowTagUseCase
import com.pfitzer.tootsie.domain.usecase.search.SearchUseCase
import com.pfitzer.tootsie.domain.usecase.status.*
import com.pfitzer.tootsie.domain.usecase.trending.GetTrendingHashtagsUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.domain.model.withInteractions

/**
 * ViewModel for Search Screen.
 * Manages search query, results, paging per result type, recent searches and suggestions.
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
    private val voteOnPollUseCase: VoteOnPollUseCase,
    private val followTagUseCase: FollowTagUseCase,
    private val unfollowTagUseCase: UnfollowTagUseCase,
    private val getTrendingHashtagsUseCase: GetTrendingHashtagsUseCase,
    private val preferencesManager: PreferencesManager
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

    // The running search; a new search cancels it, so an older response can't
    // overwrite the results of a newer query
    private var searchJob: Job? = null

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

        // Recent searches belong to the active account
        viewModelScope.launch {
            observeActiveAccountUseCase()
                .map { it?.id }
                .distinctUntilChanged()
                .collect { accountId ->
                    val recent = accountId?.let { preferencesManager.getRecentSearches(it) }.orEmpty()
                    _uiState.update { it.copy(recentSearches = recent) }
                }
        }

        loadTrendingHashtags()
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.UpdateQuery -> updateQuery(action.query)
            is SearchAction.Submit -> submit()
            is SearchAction.SelectFilter -> selectFilter(action.filter)
            is SearchAction.LoadMore -> loadMore()
            is SearchAction.ClearSearch -> clearSearch()
            is SearchAction.DismissError -> dismissError()
            is SearchAction.RememberQuery -> rememberQuery(_uiState.value.query)
            is SearchAction.RemoveRecentSearch -> removeRecentSearch(action.query)
            is SearchAction.ClearRecentSearches -> saveRecentSearches(emptyList())
            is SearchAction.ToggleFollowHashtag -> toggleFollowHashtag(action.hashtag)
            is SearchAction.FavoriteStatus -> interact(R.string.error_favorite) { favoriteStatusUseCase(action.status.id) }
            is SearchAction.UnfavoriteStatus -> interact(R.string.error_unfavorite) { unfavoriteStatusUseCase(action.status.id) }
            is SearchAction.BoostStatus -> interact(R.string.error_boost) { reblogStatusUseCase(action.status.id) }
            is SearchAction.UnboostStatus -> interact(R.string.error_unboost) { unreblogStatusUseCase(action.status.id) }
            is SearchAction.BookmarkStatus -> interact(R.string.error_bookmark) { bookmarkStatusUseCase(action.status.id) }
            is SearchAction.UnbookmarkStatus -> interact(R.string.error_unbookmark) { unbookmarkStatusUseCase(action.status.id) }
            is SearchAction.VoteOnPoll -> interact(R.string.error_vote) { voteOnPollUseCase(action.pollId, action.choices) }
        }
    }

    /**
     * Run a status interaction. The confirmed state reaches the results through
     * StatusInteractionStore, so the search doesn't have to be repeated.
     */
    private fun interact(@StringRes errorRes: Int, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            if (block() is Result.Error) {
                _uiState.update { it.copy(errorMessage = UiText.of(errorRes)) }
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
            searchJob?.cancel()
            clearResults()
        }
    }

    /**
     * Search action of the keyboard: search right away, also resolving remote accounts and
     * posts, and remember the query.
     */
    private fun submit() {
        val query = _uiState.value.query
        if (query.isBlank()) return
        rememberQuery(query)
        performSearch(query, forceResolve = true)
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
        searchJob?.cancel()
        _uiState.update {
            SearchUiState(recentSearches = it.recentSearches, trendingHashtags = it.trendingHashtags)
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
                isLoadingMore = false,
                endReached = false,
                errorMessage = null
            )
        }
    }

    /**
     * Perform a search for the first page.
     */
    private fun performSearch(query: String, forceResolve: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isLoadingMore = false, errorMessage = null) }

            val filter = _uiState.value.selectedFilter
            val result = searchUseCase(
                query = query,
                type = filter.apiType,
                resolve = forceResolve || shouldResolve(query),
                limit = filter.pageSize
            )

            when (result) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update {
                        it.copy(
                            accounts = data.accounts,
                            statuses = data.statuses,
                            hashtags = data.hashtags,
                            isLoading = false,
                            // "All" shows a preview only; the tabs page further
                            endReached = filter == SearchFilter.ALL || filter.resultCount(data) < filter.pageSize,
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
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Load the next page of the selected result type.
     */
    private fun loadMore() {
        val state = _uiState.value
        val filter = state.selectedFilter
        if (filter == SearchFilter.ALL || state.isLoading || state.isLoadingMore || state.endReached) return
        if (state.query.isBlank()) return

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val offset = when (filter) {
                SearchFilter.ACCOUNTS -> state.accounts.size
                SearchFilter.STATUSES -> state.statuses.size
                SearchFilter.HASHTAGS -> state.hashtags.size
                SearchFilter.ALL -> 0
            }
            val result = searchUseCase(
                query = state.query,
                type = filter.apiType,
                resolve = shouldResolve(state.query),
                limit = filter.pageSize,
                offset = offset
            )
            when (result) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update {
                        it.copy(
                            accounts = (it.accounts + data.accounts).distinctBy { a -> a.id },
                            statuses = (it.statuses + data.statuses).distinctBy { s -> s.id },
                            hashtags = (it.hashtags + data.hashtags).distinctBy { h -> h.name.lowercase() },
                            isLoadingMore = false,
                            endReached = filter.resultCount(data) < filter.pageSize
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoadingMore = false, errorMessage = UiText.of(R.string.error_search))
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Follow or unfollow a hashtag from the results.
     */
    private fun toggleFollowHashtag(hashtag: Hashtag) {
        val follow = hashtag.following != true
        viewModelScope.launch {
            val result = if (follow) followTagUseCase(hashtag.name) else unfollowTagUseCase(hashtag.name)
            when (result) {
                is Result.Success -> _uiState.update { state ->
                    state.copy(
                        hashtags = state.hashtags.map {
                            if (it.name.equals(hashtag.name, ignoreCase = true)) it.copy(following = follow) else it
                        }
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        errorMessage = UiText.of(
                            if (follow) R.string.error_follow_hashtag else R.string.error_unfollow_hashtag,
                            hashtag.name
                        )
                    )
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Trending hashtags, shown as suggestions before searching.
     */
    private fun loadTrendingHashtags() {
        viewModelScope.launch {
            val result = getTrendingHashtagsUseCase(limit = TRENDING_LIMIT)
            if (result is Result.Success) {
                _uiState.update { it.copy(trendingHashtags = result.data.map { tag -> tag.name }) }
            }
        }
    }

    /**
     * Add a query to the recent searches (newest first, without duplicates).
     */
    private fun rememberQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val updated = (listOf(trimmed) + _uiState.value.recentSearches.filterNot { it.equals(trimmed, ignoreCase = true) })
            .take(MAX_RECENT_SEARCHES)
        saveRecentSearches(updated)
    }

    private fun removeRecentSearch(query: String) {
        saveRecentSearches(_uiState.value.recentSearches - query)
    }

    private fun saveRecentSearches(queries: List<String>) {
        _uiState.update { it.copy(recentSearches = queries) }
        viewModelScope.launch {
            val accountId = activeAccount.value?.id ?: observeActiveAccountUseCase().first()?.id ?: return@launch
            preferencesManager.saveRecentSearches(accountId, queries)
        }
    }

    /**
     * Dismiss error message.
     */
    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val MAX_RECENT_SEARCHES = 10
        private const val TRENDING_LIMIT = 8

        // `@user@domain` (or `user@domain`) and URLs refer to a specific remote account or
        // post; the server only knows it if it is asked to resolve it
        private val HANDLE = Regex("""^@?[\w.]+@[\w-]+(\.[\w-]+)+$""")
        private val URL = Regex("""^https?://\S+$""", RegexOption.IGNORE_CASE)

        /**
         * Whether a query should be resolved via WebFinger / ActivityPub.
         */
        fun shouldResolve(query: String): Boolean {
            val q = query.trim()
            return HANDLE.matches(q) || URL.matches(q)
        }
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
    val isLoadingMore: Boolean = false,
    /** No more pages for the selected tab. */
    val endReached: Boolean = false,
    val errorMessage: UiText? = null,
    val recentSearches: List<String> = emptyList(),
    val trendingHashtags: List<String> = emptyList()
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
 *
 * @param apiType `type` parameter of the search API; null searches all types
 * @param pageSize Results per request; "All" shows a few per type as a preview
 */
enum class SearchFilter(@StringRes val labelRes: Int, val apiType: String?, val pageSize: Int) {
    ALL(R.string.search_filter_all, null, 5),
    ACCOUNTS(R.string.search_filter_accounts, "accounts", 20),
    STATUSES(R.string.search_filter_posts, "statuses", 20),
    HASHTAGS(R.string.search_filter_hashtags, "hashtags", 20);

    fun resultCount(results: com.pfitzer.tootsie.domain.model.SearchResults): Int = when (this) {
        ALL -> results.accounts.size + results.statuses.size + results.hashtags.size
        ACCOUNTS -> results.accounts.size
        STATUSES -> results.statuses.size
        HASHTAGS -> results.hashtags.size
    }
}

/**
 * User actions for Search screen.
 */
sealed class SearchAction {
    data class UpdateQuery(val query: String) : SearchAction()
    /** Search action of the keyboard. */
    data object Submit : SearchAction()
    data class SelectFilter(val filter: SearchFilter) : SearchAction()
    data object LoadMore : SearchAction()
    data object ClearSearch : SearchAction()
    data object DismissError : SearchAction()
    /** A result was opened: keep the query in the recent searches. */
    data object RememberQuery : SearchAction()
    data class RemoveRecentSearch(val query: String) : SearchAction()
    data object ClearRecentSearches : SearchAction()
    data class ToggleFollowHashtag(val hashtag: Hashtag) : SearchAction()
    data class FavoriteStatus(val status: Status) : SearchAction()
    data class UnfavoriteStatus(val status: Status) : SearchAction()
    data class BoostStatus(val status: Status) : SearchAction()
    data class UnboostStatus(val status: Status) : SearchAction()
    data class BookmarkStatus(val status: Status) : SearchAction()
    data class UnbookmarkStatus(val status: Status) : SearchAction()
    data class VoteOnPoll(val pollId: String, val choices: List<Int>) : SearchAction()
}
