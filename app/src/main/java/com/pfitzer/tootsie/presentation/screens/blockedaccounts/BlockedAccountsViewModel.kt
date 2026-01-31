package com.pfitzer.tootsie.presentation.screens.blockedaccounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.domain.usecase.account.UnblockAccountUseCase
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
     * Paging data flow for blocked accounts.
     */
    val blockedAccountsPagingData: Flow<PagingData<Account>> =
        accountRepository.getBlockedAccounts().cachedIn(viewModelScope)

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
            _uiState.update { it.copy(processingAccountId = account.id) }

            when (val result = unblockAccountUseCase(account.id)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            processingAccountId = null,
                            unblockedAccountId = account.id
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            processingAccountId = null,
                            errorMessage = result.message ?: "Failed to unblock account"
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
    val processingAccountId: String? = null,
    val unblockedAccountId: String? = null,
    val errorMessage: String? = null
)

/**
 * User actions for Blocked Accounts Screen.
 */
sealed class BlockedAccountsAction {
    data class UnblockAccount(val account: Account) : BlockedAccountsAction()
    data object DismissError : BlockedAccountsAction()
}
