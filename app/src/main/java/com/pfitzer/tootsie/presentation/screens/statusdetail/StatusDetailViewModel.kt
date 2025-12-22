package com.pfitzer.tootsie.presentation.screens.statusdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.domain.usecase.status.GetStatusContextUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Status Detail Screen.
 * Manages loading and displaying a status with its conversation context.
 */
@HiltViewModel
class StatusDetailViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val getStatusContextUseCase: GetStatusContextUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val statusId: String = checkNotNull(savedStateHandle["statusId"])

    private val _uiState = MutableStateFlow(StatusDetailUiState())
    val uiState: StateFlow<StatusDetailUiState> = _uiState.asStateFlow()

    init {
        loadStatusAndContext()
    }

    /**
     * Load the status and its conversation context.
     */
    private fun loadStatusAndContext() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Load the main status
            when (val statusResult = statusRepository.getStatus(statusId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(status = statusResult.data) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load status: ${statusResult.message}"
                        )
                    }
                    return@launch
                }
                is Result.Loading -> Unit
            }

            // Load conversation context
            when (val contextResult = getStatusContextUseCase(statusId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            ancestors = contextResult.data.ancestors,
                            descendants = contextResult.data.descendants,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load conversation: ${contextResult.message}"
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Refresh the status and context.
     */
    fun onAction(action: StatusDetailAction) {
        when (action) {
            is StatusDetailAction.Refresh -> loadStatusAndContext()
            is StatusDetailAction.DismissError -> dismissError()
        }
    }

    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for Status Detail Screen.
 */
data class StatusDetailUiState(
    val status: Status? = null,
    val ancestors: List<Status> = emptyList(),
    val descendants: List<Status> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for Status Detail Screen.
 */
sealed class StatusDetailAction {
    data object Refresh : StatusDetailAction()
    data object DismissError : StatusDetailAction()
}
