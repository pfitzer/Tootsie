package com.pfitzer.tootsie.presentation.screens.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.domain.usecase.filter.CreateFilterUseCase
import com.pfitzer.tootsie.domain.usecase.filter.DeleteFilterUseCase
import com.pfitzer.tootsie.domain.usecase.filter.GetFiltersUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Filters Screen.
 * Manages user's Mastodon keyword filters with create and delete functionality.
 */
@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val getFiltersUseCase: GetFiltersUseCase,
    private val createFilterUseCase: CreateFilterUseCase,
    private val deleteFilterUseCase: DeleteFilterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FiltersUiState())
    val uiState: StateFlow<FiltersUiState> = _uiState.asStateFlow()

    init {
        loadFilters()
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: FiltersAction) {
        when (action) {
            is FiltersAction.LoadFilters -> loadFilters()
            is FiltersAction.CreateFilter -> createFilter(
                action.title,
                action.contexts,
                action.filterAction,
                action.expiresIn,
                action.keywords
            )
            is FiltersAction.DeleteFilter -> deleteFilter(action.id)
            is FiltersAction.DismissError -> dismissError()
        }
    }

    /**
     * Load all user's filters.
     */
    private fun loadFilters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getFiltersUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            filters = result.data,
                            isLoading = false
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
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Create a new filter.
     */
    private fun createFilter(
        title: String,
        contexts: List<FilterContext>,
        filterAction: FilterAction,
        expiresIn: Int?,
        keywords: List<Pair<String, Boolean>>
    ) {
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Filter title cannot be empty") }
            return
        }
        if (contexts.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Select at least one context") }
            return
        }
        if (keywords.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Add at least one keyword") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }

            when (val result = createFilterUseCase(
                title = title,
                context = contexts,
                filterAction = filterAction,
                expiresIn = expiresIn,
                keywords = keywords
            )) {
                is Result.Success -> {
                    // Reload filters to include the new one
                    loadFilters()
                    _uiState.update { it.copy(isCreating = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Delete a filter.
     */
    private fun deleteFilter(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }

            when (val result = deleteFilterUseCase(id)) {
                is Result.Success -> {
                    // Remove the filter from local state
                    _uiState.update {
                        it.copy(filters = it.filters.filter { filter -> filter.id != id })
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
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
 * UI state for Filters Screen.
 */
data class FiltersUiState(
    val filters: List<Filter> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for Filters Screen.
 */
sealed class FiltersAction {
    data object LoadFilters : FiltersAction()
    data class CreateFilter(
        val title: String,
        val contexts: List<FilterContext>,
        val filterAction: FilterAction,
        val expiresIn: Int?,
        val keywords: List<Pair<String, Boolean>> // keyword, wholeWord
    ) : FiltersAction()
    data class DeleteFilter(val id: String) : FiltersAction()
    data object DismissError : FiltersAction()
}
