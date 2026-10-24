package com.pfitzer.tootsie.presentation.screens.mutedaccounts

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.domain.usecase.account.UnmuteAccountUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Muted Accounts Screen.
 * Manages the list of muted accounts with unmute functionality.
 */
@HiltViewModel
class MutedAccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val unmuteAccountUseCase: UnmuteAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MutedAccountsUiState())
    val uiState: StateFlow<MutedAccountsUiState> = _uiState.asStateFlow()

    /**
     * Accounts unmuted on this screen. They are filtered out of the paged list, so the
     * item count (and with it the empty state) stays correct without reloading.
     */
    private val unmutedAccountIds = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Paging data flow for muted accounts.
     */
    val mutedAccountsPagingData: Flow<PagingData<Account>> =
        accountRepository.getMutedAccounts()
            .cachedIn(viewModelScope)
            .combine(unmutedAccountIds) { pagingData, removedIds ->
                pagingData.filter { it.id !in removedIds }
            }

    /**
     * Handle user actions.
     */
    fun onAction(action: MutedAccountsAction) {
        when (action) {
            is MutedAccountsAction.UnmuteAccount -> unmuteAccount(action.account)
            is MutedAccountsAction.DismissError -> dismissError()
        }
    }

    /**
     * Unmute an account.
     */
    private fun unmuteAccount(account: Account) {
        viewModelScope.launch {
            if (account.id in _uiState.value.processingAccountIds) return@launch
            _uiState.update { it.copy(processingAccountIds = it.processingAccountIds + account.id) }

            when (val result = unmuteAccountUseCase(account.id)) {
                is Result.Success -> {
                    unmutedAccountIds.update { it + account.id }
                    _uiState.update { it.copy(processingAccountIds = it.processingAccountIds - account.id) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            processingAccountIds = it.processingAccountIds - account.id,
                            errorMessage = UiText.of(R.string.error_unmute_account)
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
 * UI state for Muted Accounts Screen.
 */
data class MutedAccountsUiState(
    val processingAccountIds: Set<String> = emptySet(),
    val errorMessage: UiText? = null
)

/**
 * User actions for Muted Accounts Screen.
 */
sealed class MutedAccountsAction {
    data class UnmuteAccount(val account: Account) : MutedAccountsAction()
    data object DismissError : MutedAccountsAction()
}
