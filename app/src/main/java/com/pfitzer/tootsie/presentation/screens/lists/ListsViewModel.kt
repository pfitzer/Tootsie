package com.pfitzer.tootsie.presentation.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.MastodonList
import com.pfitzer.tootsie.domain.model.RepliesPolicy
import com.pfitzer.tootsie.domain.usecase.lists.CreateListUseCase
import com.pfitzer.tootsie.domain.usecase.lists.DeleteListUseCase
import com.pfitzer.tootsie.domain.usecase.lists.GetListsUseCase
import com.pfitzer.tootsie.domain.usecase.lists.UpdateListUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Lists Screen.
 * Manages user's Mastodon lists with create, update, and delete functionality.
 */
@HiltViewModel
class ListsViewModel @Inject constructor(
    private val getListsUseCase: GetListsUseCase,
    private val createListUseCase: CreateListUseCase,
    private val updateListUseCase: UpdateListUseCase,
    private val deleteListUseCase: DeleteListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    init {
        loadLists()
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: ListsAction) {
        when (action) {
            is ListsAction.LoadLists -> loadLists()
            is ListsAction.CreateList -> createList(action.title, action.repliesPolicy)
            is ListsAction.UpdateList -> updateList(action.id, action.title, action.repliesPolicy)
            is ListsAction.DeleteList -> deleteList(action.id)
            is ListsAction.DismissError -> dismissError()
        }
    }

    /**
     * Load all user's lists.
     */
    private fun loadLists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getListsUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            lists = result.data,
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
     * Create a new list.
     */
    private fun createList(title: String, repliesPolicy: RepliesPolicy) {
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "List title cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }

            when (val result = createListUseCase(title, repliesPolicy)) {
                is Result.Success -> {
                    // Reload lists to include the new one
                    loadLists()
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
     * Update an existing list.
     */
    private fun updateList(id: String, title: String?, repliesPolicy: RepliesPolicy?) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }

            when (val result = updateListUseCase(id, title, repliesPolicy)) {
                is Result.Success -> {
                    // Reload lists to reflect the update
                    loadLists()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Delete a list.
     */
    private fun deleteList(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }

            when (val result = deleteListUseCase(id)) {
                is Result.Success -> {
                    // Remove the list from local state
                    _uiState.update {
                        it.copy(lists = it.lists.filter { list -> list.id != id })
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
 * UI state for Lists Screen.
 */
data class ListsUiState(
    val lists: List<MastodonList> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for Lists Screen.
 */
sealed class ListsAction {
    data object LoadLists : ListsAction()
    data class CreateList(val title: String, val repliesPolicy: RepliesPolicy) : ListsAction()
    data class UpdateList(val id: String, val title: String?, val repliesPolicy: RepliesPolicy?) : ListsAction()
    data class DeleteList(val id: String) : ListsAction()
    data object DismissError : ListsAction()
}
