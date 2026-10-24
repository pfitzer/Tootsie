package com.pfitzer.tootsie.presentation.screens.filters

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Filter
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
 * Lists the user's Mastodon keyword filters and deletes them; the screen reloads the
 * list whenever it is shown (e.g. after the editor). Creating and editing: FilterEditViewModel.
 */
@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val getFiltersUseCase: GetFiltersUseCase,
    private val deleteFilterUseCase: DeleteFilterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FiltersUiState())
    val uiState: StateFlow<FiltersUiState> = _uiState.asStateFlow()

    /**
     * Handle user actions.
     */
    fun onAction(action: FiltersAction) {
        when (action) {
            is FiltersAction.LoadFilters -> loadFilters()
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
                            errorMessage = UiText.of(R.string.error_load_filters)
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
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_delete_filter)) }
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
    val errorMessage: UiText? = null
)

/**
 * User actions for Filters Screen.
 */
sealed class FiltersAction {
    data object LoadFilters : FiltersAction()
    data class DeleteFilter(val id: String) : FiltersAction()
    data object DismissError : FiltersAction()
}
