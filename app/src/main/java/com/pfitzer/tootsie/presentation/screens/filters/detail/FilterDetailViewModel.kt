package com.pfitzer.tootsie.presentation.screens.filters.detail

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.domain.usecase.filter.AddKeywordUseCase
import com.pfitzer.tootsie.domain.usecase.filter.DeleteKeywordUseCase
import com.pfitzer.tootsie.domain.usecase.filter.GetFilterUseCase
import com.pfitzer.tootsie.domain.usecase.filter.UpdateFilterUseCase
import com.pfitzer.tootsie.presentation.navigation.TootsieDestinations
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilterDetailViewModel @Inject constructor(
    private val getFilterUseCase: GetFilterUseCase,
    private val updateFilterUseCase: UpdateFilterUseCase,
    private val addKeywordUseCase: AddKeywordUseCase,
    private val deleteKeywordUseCase: DeleteKeywordUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val filterId: String = checkNotNull(savedStateHandle[TootsieDestinations.FilterDetail.ARG_FILTER_ID])

    private val _uiState = MutableStateFlow(FilterDetailUiState())
    val uiState: StateFlow<FilterDetailUiState> = _uiState.asStateFlow()

    init {
        loadFilter()
    }

    fun onAction(action: FilterDetailAction) {
        when (action) {
            is FilterDetailAction.Refresh -> loadFilter()
            is FilterDetailAction.UpdateTitle -> updateTitle(action.title)
            is FilterDetailAction.UpdateContexts -> updateContexts(action.contexts)
            is FilterDetailAction.UpdateFilterAction -> updateFilterAction(action.filterAction)
            is FilterDetailAction.UpdateExpiresIn -> updateExpiresIn(action.expiresIn)
            is FilterDetailAction.AddKeyword -> addKeyword(action.keyword, action.wholeWord)
            is FilterDetailAction.DeleteKeyword -> deleteKeyword(action.keywordId)
            is FilterDetailAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadFilter() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getFilterUseCase(filterId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            filter = result.data,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.of(R.string.error_load_filter)
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun updateTitle(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            when (val result = updateFilterUseCase(id = filterId, title = title)) {
                is Result.Success -> {
                    _uiState.update { it.copy(filter = result.data, isUpdating = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isUpdating = false, errorMessage = UiText.of(R.string.error_update_filter)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun updateContexts(contexts: List<FilterContext>) {
        if (contexts.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            when (val result = updateFilterUseCase(id = filterId, context = contexts)) {
                is Result.Success -> {
                    _uiState.update { it.copy(filter = result.data, isUpdating = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isUpdating = false, errorMessage = UiText.of(R.string.error_update_filter)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun updateFilterAction(filterAction: FilterAction) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            when (val result = updateFilterUseCase(id = filterId, filterAction = filterAction)) {
                is Result.Success -> {
                    _uiState.update { it.copy(filter = result.data, isUpdating = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isUpdating = false, errorMessage = UiText.of(R.string.error_update_filter)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun updateExpiresIn(expiresIn: Int?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            when (val result = updateFilterUseCase(id = filterId, expiresIn = expiresIn)) {
                is Result.Success -> {
                    _uiState.update { it.copy(filter = result.data, isUpdating = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isUpdating = false, errorMessage = UiText.of(R.string.error_update_filter)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun addKeyword(keyword: String, wholeWord: Boolean) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            when (val result = addKeywordUseCase(filterId, keyword, wholeWord)) {
                is Result.Success -> {
                    // Refresh filter to get updated keyword list
                    loadFilter()
                    _uiState.update { it.copy(isUpdating = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isUpdating = false, errorMessage = UiText.of(R.string.error_add_keyword)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun deleteKeyword(keywordId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            when (val result = deleteKeywordUseCase(keywordId)) {
                is Result.Success -> {
                    // Update local state by removing the keyword
                    _uiState.update { state ->
                        state.copy(
                            filter = state.filter?.let { filter ->
                                filter.copy(keywords = filter.keywords.filter { it.id != keywordId })
                            },
                            isUpdating = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isUpdating = false, errorMessage = UiText.of(R.string.error_delete_keyword)) }
                }
                is Result.Loading -> Unit
            }
        }
    }
}

data class FilterDetailUiState(
    val filter: Filter? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: UiText? = null
)

sealed class FilterDetailAction {
    data object Refresh : FilterDetailAction()
    data class UpdateTitle(val title: String) : FilterDetailAction()
    data class UpdateContexts(val contexts: List<FilterContext>) : FilterDetailAction()
    data class UpdateFilterAction(val filterAction: FilterAction) : FilterDetailAction()
    data class UpdateExpiresIn(val expiresIn: Int?) : FilterDetailAction()
    data class AddKeyword(val keyword: String, val wholeWord: Boolean) : FilterDetailAction()
    data class DeleteKeyword(val keywordId: String) : FilterDetailAction()
    data object DismissError : FilterDetailAction()
}
