package com.pfitzer.tootsie.presentation.screens.edithistory

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.StatusEditVersion
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the edit history of a status.
 */
@HiltViewModel
class EditHistoryViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val statusId: String = checkNotNull(savedStateHandle["statusId"])

    private val _uiState = MutableStateFlow(EditHistoryUiState())
    val uiState: StateFlow<EditHistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onAction(action: EditHistoryAction) {
        when (action) {
            EditHistoryAction.Retry -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = statusRepository.getStatusHistory(statusId)) {
                is Result.Success -> _uiState.update { it.copy(versions = result.data, isLoading = false) }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = UiText.of(R.string.error_load_edit_history))
                }
                is Result.Loading -> Unit
            }
        }
    }
}

/**
 * UI state for the edit history.
 *
 * @param versions Newest first; the last one is the original
 */
data class EditHistoryUiState(
    val versions: List<StatusEditVersion> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null
)

sealed class EditHistoryAction {
    data object Retry : EditHistoryAction()
}
