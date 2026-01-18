package com.pfitzer.tootsie.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Main ViewModel for app-wide state.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val accountManagementRepository: AccountManagementRepository
) : ViewModel() {

    /**
     * Observe the currently active account.
     */
    val activeAccount: StateFlow<Account?> = accountManagementRepository.observeActiveAccount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
