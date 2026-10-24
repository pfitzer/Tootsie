package com.pfitzer.tootsie.presentation.screens.blockedaccounts

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.domain.usecase.account.UnblockAccountUseCase
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
 * ViewModel for Blocked Accounts Screen.
 * Manages the list of blocked accounts with unblock functionality.
 */
@HiltViewModel
class BlockedAccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val unblockAccountUseCase: UnblockAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedAccountsUiState())
    val uiState: StateFlow<BlockedAccountsUiState> = _uiState.asStateFlow()

    /**
     * Accounts unblocked on this screen. They are filtered out of the paged list, so the
     * item count (and with it the empty state) stays correct without reloading.
     */
    private val unblockedAccountIds = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Paging data flow for blocked accounts.
     */
    val blockedAccountsPagingData: Flow<PagingData<Account>> =
        accountRepository.getBlockedAccounts()
            .cachedIn(viewModelScope)
            .combine(unblockedAccountIds) { pagingData, removedIds ->
                pagingData.filter { it.id !in removedIds }
            }

    /**
     * Handle user actions.
     */
    fun onAction(action: BlockedAccountsAction) {
        when (action) {
            is BlockedAccountsAction.UnblockAccount -> unblockAccount(action.account)
            is BlockedAccountsAction.DismissError -> dismissError()
        }
    }

    /**
     * Unblock an account.
     */
    private fun unblockAccount(account: Account) {
        viewModelScope.launch {
            if (account.id in _uiState.value.processingAccountIds) return@launch
            _uiState.update { it.copy(processingAccountIds = it.processingAccountIds + account.id) }

            when (val result = unblockAccountUseCase(account.id)) {
                is Result.Success -> {
                    unblockedAccountIds.update { it + account.id }
                    _uiState.update { it.copy(processingAccountIds = it.processingAccountIds - account.id) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            processingAccountIds = it.processingAccountIds - account.id,
                            errorMessage = UiText.of(R.string.error_unblock_account)
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
 * UI state for Blocked Accounts Screen.
 */
data class BlockedAccountsUiState(
    val processingAccountIds: Set<String> = emptySet(),
    val errorMessage: UiText? = null
)

/**
 * User actions for Blocked Accounts Screen.
 */
sealed class BlockedAccountsAction {
    data class UnblockAccount(val account: Account) : BlockedAccountsAction()
    data object DismissError : BlockedAccountsAction()
}
