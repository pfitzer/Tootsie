package com.pfitzer.tootsie.presentation.screens.drafts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.data.local.entities.DraftEntity
import com.pfitzer.tootsie.domain.repository.DraftRepository
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Drafts Screen.
 * Manages draft list and user actions.
 */
@HiltViewModel
class DraftsViewModel @Inject constructor(
    private val draftRepository: DraftRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DraftsUiState())
    val uiState: StateFlow<DraftsUiState> = _uiState.asStateFlow()

    /**
     * Flow of all drafts from the database.
     */
    val drafts: Flow<List<DraftEntity>> = draftRepository.getAllDrafts()

    /**
     * Flow of draft count.
     */
    val draftCount: Flow<Int> = draftRepository.getDraftCount()

    /**
     * Handle user actions.
     */
    fun onAction(action: DraftsAction) {
        when (action) {
            is DraftsAction.DeleteDraft -> deleteDraft(action.draftId)
            is DraftsAction.DeleteAllDrafts -> deleteAllDrafts()
            is DraftsAction.DismissError -> dismissError()
        }
    }

    /**
     * Delete a single draft.
     */
    private fun deleteDraft(draftId: Long) {
        viewModelScope.launch {
            when (val result = draftRepository.deleteDraft(draftId)) {
                is Result.Success -> {
                    // Success - Flow will update automatically
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to delete draft")
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Delete all drafts.
     */
    private fun deleteAllDrafts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAll = true) }

            when (val result = draftRepository.deleteAllDrafts()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isDeletingAll = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isDeletingAll = false,
                            errorMessage = "Failed to delete all drafts"
                        )
                    }
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
 * UI state for Drafts Screen.
 */
data class DraftsUiState(
    val isDeletingAll: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for Drafts Screen.
 */
sealed class DraftsAction {
    data class DeleteDraft(val draftId: Long) : DraftsAction()
    data object DeleteAllDrafts : DraftsAction()
    data object DismissError : DraftsAction()
}
