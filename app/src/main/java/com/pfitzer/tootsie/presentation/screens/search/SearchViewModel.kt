package com.pfitzer.tootsie.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Hashtag
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import com.pfitzer.tootsie.domain.usecase.search.SearchUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Search Screen.
 * Manages search query, results, and filtering.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUseCase: SearchUseCase,
    private val accountManagementRepository: AccountManagementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * Observe the currently active account.
     */
    val activeAccount: StateFlow<Account?> = accountManagementRepository.observeActiveAccount()
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
                            errorMessage = result.message
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
    val errorMessage: String? = null
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
    object ClearSearch : SearchAction()
    object DismissError : SearchAction()
}
