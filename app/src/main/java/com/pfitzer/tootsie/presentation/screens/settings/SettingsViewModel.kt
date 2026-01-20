package com.pfitzer.tootsie.presentation.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.usecase.account.*
import com.pfitzer.tootsie.domain.usecase.auth.LogoutUseCase
import com.pfitzer.tootsie.domain.usecase.settings.UpdateThemeUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

/**
 * ViewModel for Settings Screen.
 * Manages account list, switching, and removal using MVI pattern.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeActiveAccountUseCase: ObserveActiveAccountUseCase,
    private val getActiveAccountUseCase: GetActiveAccountUseCase,
    private val preferencesManager: com.pfitzer.tootsie.data.local.datastore.PreferencesManager,
    private val getAllAccountsUseCase: GetAllAccountsUseCase,
    private val switchAccountUseCase: SwitchAccountUseCase,
    private val removeAccountUseCase: RemoveAccountUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val updateThemeUseCase: UpdateThemeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)

    /**
     * Observe the currently active account.
     */
    val activeAccount: StateFlow<Account?> = observeActiveAccountUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Combined UI state observing both accounts and theme.
     */
    val combinedUiState: StateFlow<SettingsUiState> = combine(
        _uiState,
        preferencesManager.theme
    ) { state, theme ->
        if (state is SettingsUiState.Success) {
            state.copy(theme = theme)
        } else {
            state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState.Loading
    )

    val uiState: StateFlow<SettingsUiState> = combinedUiState

    init {
        loadAccounts()
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SwitchAccount -> switchAccount(action.accountId)
            is SettingsAction.RemoveAccount -> removeAccount(action.accountId)
            is SettingsAction.ConfirmRemoveAccount -> confirmRemoveAccount()
            is SettingsAction.CancelRemoveAccount -> cancelRemoveAccount()
            is SettingsAction.Refresh -> loadAccounts()
            is SettingsAction.DismissError -> dismissError()
            is SettingsAction.ChangeTheme -> changeTheme(action.theme)
        }
    }

    /**
     * Load all logged-in accounts.
     */
    private fun loadAccounts() {
        viewModelScope.launch {
            _uiState.update { SettingsUiState.Loading }
            Log.d(TAG, "Loading all accounts...")

            try {
                val accounts = getAllAccountsUseCase()
                val activeAccount = getActiveAccountUseCase()

                Log.d(TAG, "Loaded ${accounts.size} accounts, active account: ${activeAccount?.acct}")

                _uiState.update {
                    SettingsUiState.Success(
                        accounts = accounts,
                        activeAccountId = activeAccount?.id
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load accounts", e)
                _uiState.update {
                    SettingsUiState.Error("Failed to load accounts: ${e.message}")
                }
            }
        }
    }

    /**
     * Switch to a different account.
     */
    private fun switchAccount(accountId: String) {
        val currentState = _uiState.value
        if (currentState !is SettingsUiState.Success) return

        // Don't switch if already active
        if (currentState.activeAccountId == accountId) {
            Log.d(TAG, "Account $accountId is already active")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                if (it is SettingsUiState.Success) {
                    it.copy(isSwitching = true)
                } else it
            }

            Log.d(TAG, "Switching to account: $accountId")

            try {
                switchAccountUseCase(accountId)
                Log.d(TAG, "Account switched successfully")

                // Reload accounts to update active state
                loadAccounts()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch account", e)
                _uiState.update {
                    if (it is SettingsUiState.Success) {
                        it.copy(
                            isSwitching = false,
                            errorMessage = "Failed to switch account: ${e.message}"
                        )
                    } else it
                }
            }
        }
    }

    /**
     * Show confirmation dialog before removing account.
     */
    private fun removeAccount(accountId: String) {
        _uiState.update {
            if (it is SettingsUiState.Success) {
                it.copy(accountToRemove = accountId)
            } else it
        }
    }

    /**
     * Confirm and execute account removal.
     */
    private fun confirmRemoveAccount() {
        val currentState = _uiState.value
        if (currentState !is SettingsUiState.Success) return

        val accountToRemove = currentState.accountToRemove ?: return

        viewModelScope.launch {
            _uiState.update {
                if (it is SettingsUiState.Success) {
                    it.copy(isRemoving = true, accountToRemove = null)
                } else it
            }

            Log.d(TAG, "Removing account: $accountToRemove")

            try {
                val needsLogout = removeAccountUseCase(accountToRemove)
                Log.d(TAG, "Account removed, needsLogout=$needsLogout")

                if (needsLogout) {
                    // Last account was removed, perform full logout
                    when (logoutUseCase()) {
                        is Result.Success -> {
                            Log.d(TAG, "Logout successful")
                            _uiState.update { SettingsUiState.LoggedOut }
                        }
                        is Result.Error -> {
                            Log.e(TAG, "Logout failed after account removal")
                            _uiState.update { SettingsUiState.LoggedOut } // Still logged out
                        }
                        is Result.Loading -> Unit
                    }
                } else {
                    // Reload accounts to show updated list
                    loadAccounts()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove account", e)
                _uiState.update {
                    if (it is SettingsUiState.Success) {
                        it.copy(
                            isRemoving = false,
                            errorMessage = "Failed to remove account: ${e.message}"
                        )
                    } else it
                }
            }
        }
    }

    /**
     * Cancel account removal.
     */
    private fun cancelRemoveAccount() {
        _uiState.update {
            if (it is SettingsUiState.Success) {
                it.copy(accountToRemove = null)
            } else it
        }
    }

    /**
     * Dismiss error message.
     */
    private fun dismissError() {
        _uiState.update {
            if (it is SettingsUiState.Success) {
                it.copy(errorMessage = null)
            } else it
        }
    }

    /**
     * Change app theme.
     */
    private fun changeTheme(theme: String) {
        viewModelScope.launch {
            updateThemeUseCase(theme)
        }
    }
}

/**
 * UI State for Settings Screen (MVI pattern).
 */
sealed class SettingsUiState {
    data object Loading : SettingsUiState()

    data class Success(
        val accounts: List<Account>,
        val activeAccountId: String?,
        val isSwitching: Boolean = false,
        val isRemoving: Boolean = false,
        val accountToRemove: String? = null,
        val errorMessage: String? = null,
        val theme: String = "system"
    ) : SettingsUiState()

    data class Error(val message: String) : SettingsUiState()

    data object LoggedOut : SettingsUiState()
}

/**
 * User actions for Settings Screen (MVI pattern).
 */
sealed class SettingsAction {
    data class SwitchAccount(val accountId: String) : SettingsAction()
    data class RemoveAccount(val accountId: String) : SettingsAction()
    data object ConfirmRemoveAccount : SettingsAction()
    data object CancelRemoveAccount : SettingsAction()
    data object Refresh : SettingsAction()
    data object DismissError : SettingsAction()
    data class ChangeTheme(val theme: String) : SettingsAction()
}
