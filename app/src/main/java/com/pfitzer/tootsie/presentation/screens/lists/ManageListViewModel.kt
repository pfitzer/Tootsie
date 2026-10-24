package com.pfitzer.tootsie.presentation.screens.lists

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.ListsRepository
import com.pfitzer.tootsie.domain.repository.SearchRepository
import com.pfitzer.tootsie.presentation.navigation.TootsieDestinations
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageListViewModel @Inject constructor(
    private val listsRepository: ListsRepository,
    private val searchRepository: SearchRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle[TootsieDestinations.ManageList.ARG_LIST_ID])
    val listName: String = savedStateHandle[TootsieDestinations.ManageList.ARG_LIST_NAME] ?: "List"

    private val _uiState = MutableStateFlow(ManageListUiState())
    val uiState: StateFlow<ManageListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadMembers()
    }

    fun onAction(action: ManageListAction) {
        when (action) {
            is ManageListAction.LoadMembers -> loadMembers()
            is ManageListAction.RemoveMember -> removeMember(action.accountId)
            is ManageListAction.SearchAccounts -> searchAccounts(action.query)
            is ManageListAction.AddMember -> addMember(action.accountId)
            is ManageListAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMembers = true, errorMessage = null) }
            when (val result = listsRepository.getListAccounts(listId)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(members = result.data, isLoadingMembers = false) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(errorMessage = UiText.of(R.string.error_load_list_members), isLoadingMembers = false) 
                    }
                }
                else -> Unit
            }
        }
    }

    private fun removeMember(accountId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            when (val result = listsRepository.removeAccountsFromList(listId, listOf(accountId))) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(members = state.members.filter { it.id != accountId })
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_remove_list_member)) }
                }
                else -> Unit
            }
        }
    }

    private fun searchAccounts(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            _uiState.update { it.copy(isSearching = true) }
            when (val result = searchRepository.search(query, type = "accounts", following = true)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(searchResults = result.data.accounts, isSearching = false) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(errorMessage = UiText.of(R.string.error_search_accounts), isSearching = false) 
                    }
                }
                else -> Unit
            }
        }
    }

    private fun addMember(accountId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            when (val result = listsRepository.addAccountsToList(listId, listOf(accountId))) {
                is Result.Success -> {
                    // Refresh members
                    loadMembers()
                    // Clear search
                    _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_add_list_member)) }
                }
                else -> Unit
            }
        }
    }
}

data class ManageListUiState(
    val members: List<Account> = emptyList(),
    val isLoadingMembers: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Account> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: UiText? = null
)

sealed interface ManageListAction {
    data object LoadMembers : ManageListAction
    data class RemoveMember(val accountId: String) : ManageListAction
    data class SearchAccounts(val query: String) : ManageListAction
    data class AddMember(val accountId: String) : ManageListAction
    data object DismissError : ManageListAction
}
