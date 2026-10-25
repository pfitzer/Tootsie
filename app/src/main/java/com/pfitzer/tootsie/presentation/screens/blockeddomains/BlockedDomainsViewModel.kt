package com.pfitzer.tootsie.presentation.screens.blockeddomains

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.pfitzer.tootsie.domain.repository.DomainBlockRepository
import com.pfitzer.tootsie.domain.usecase.domainblock.BlockDomainUseCase
import com.pfitzer.tootsie.domain.usecase.domainblock.UnblockDomainUseCase
import com.pfitzer.tootsie.domain.util.DomainName
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
 * ViewModel for Blocked Domains Screen.
 * Manages the list of blocked domains with block and unblock functionality.
 */
@HiltViewModel
class BlockedDomainsViewModel @Inject constructor(
    domainBlockRepository: DomainBlockRepository,
    private val blockDomainUseCase: BlockDomainUseCase,
    private val unblockDomainUseCase: UnblockDomainUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedDomainsUiState())
    val uiState: StateFlow<BlockedDomainsUiState> = _uiState.asStateFlow()

    /**
     * Domains unblocked on this screen. They are filtered out of the paged list, so the
     * item count (and with it the empty state) stays correct without reloading.
     */
    private val unblockedDomains = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Paging data flow for blocked domains.
     */
    val blockedDomainsPagingData: Flow<PagingData<String>> =
        domainBlockRepository.getBlockedDomains()
            .cachedIn(viewModelScope)
            .combine(unblockedDomains) { pagingData, removed ->
                pagingData.filter { it !in removed }
            }

    /**
     * Handle user actions.
     */
    fun onAction(action: BlockedDomainsAction) {
        when (action) {
            is BlockedDomainsAction.UnblockDomain -> unblockDomain(action.domain)
            is BlockedDomainsAction.BlockDomain -> blockDomain(action.input)
            is BlockedDomainsAction.ShowBlockDialog -> {
                _uiState.update { it.copy(showBlockDialog = true) }
            }
            is BlockedDomainsAction.DismissBlockDialog -> {
                _uiState.update { it.copy(showBlockDialog = false) }
            }
            is BlockedDomainsAction.ReloadConsumed -> {
                _uiState.update { it.copy(reload = false) }
            }
            is BlockedDomainsAction.DismissError -> dismissError()
        }
    }

    /**
     * Block the domain entered in the dialog; the list is reloaded to show it.
     */
    private fun blockDomain(input: String) {
        val domain = DomainName.normalize(input) ?: return
        _uiState.update { it.copy(showBlockDialog = false) }

        viewModelScope.launch {
            when (blockDomainUseCase(domain)) {
                is Result.Success -> {
                    unblockedDomains.update { it - domain }
                    _uiState.update { it.copy(reload = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_block_domain)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unblock a domain.
     */
    private fun unblockDomain(domain: String) {
        viewModelScope.launch {
            if (domain in _uiState.value.processingDomains) return@launch
            _uiState.update { it.copy(processingDomains = it.processingDomains + domain) }

            when (unblockDomainUseCase(domain)) {
                is Result.Success -> {
                    unblockedDomains.update { it + domain }
                    _uiState.update { it.copy(processingDomains = it.processingDomains - domain) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            processingDomains = it.processingDomains - domain,
                            errorMessage = UiText.of(R.string.error_unblock_domain)
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
 * UI state for Blocked Domains Screen.
 *
 * @param reload The list has to be reloaded (after blocking a domain)
 */
data class BlockedDomainsUiState(
    val processingDomains: Set<String> = emptySet(),
    val showBlockDialog: Boolean = false,
    val reload: Boolean = false,
    val errorMessage: UiText? = null
)

/**
 * User actions for Blocked Domains Screen.
 */
sealed class BlockedDomainsAction {
    data class UnblockDomain(val domain: String) : BlockedDomainsAction()
    data class BlockDomain(val input: String) : BlockedDomainsAction()
    data object ShowBlockDialog : BlockedDomainsAction()
    data object DismissBlockDialog : BlockedDomainsAction()
    data object ReloadConsumed : BlockedDomainsAction()
    data object DismissError : BlockedDomainsAction()
}
