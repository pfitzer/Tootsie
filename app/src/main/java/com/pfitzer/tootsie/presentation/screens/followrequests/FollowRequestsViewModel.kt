package com.pfitzer.tootsie.presentation.screens.followrequests

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.usecase.followrequests.AuthorizeFollowRequestUseCase
import com.pfitzer.tootsie.domain.usecase.followrequests.GetFollowRequestsUseCase
import com.pfitzer.tootsie.domain.usecase.followrequests.RejectFollowRequestUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Follow Requests Screen.
 * Manages follow request data and user actions.
 */
@HiltViewModel
class FollowRequestsViewModel @Inject constructor(
    private val getFollowRequestsUseCase: GetFollowRequestsUseCase,
    private val authorizeFollowRequestUseCase: AuthorizeFollowRequestUseCase,
    private val rejectFollowRequestUseCase: RejectFollowRequestUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowRequestsUiState())
    val uiState: StateFlow<FollowRequestsUiState> = _uiState.asStateFlow()

    // Set of account IDs that have been processed (authorized or rejected)
    private val _processedAccountIds = MutableStateFlow<Set<String>>(emptySet())
    val processedAccountIds: StateFlow<Set<String>> = _processedAccountIds.asStateFlow()

    val followRequestsPagingData: Flow<PagingData<Account>> = getFollowRequestsUseCase()
        .cachedIn(viewModelScope)

    /**
     * Handle user actions.
     */
    fun onAction(action: FollowRequestsAction) {
        when (action) {
            is FollowRequestsAction.AuthorizeRequest -> authorizeRequest(action.account)
            is FollowRequestsAction.RejectRequest -> rejectRequest(action.account)
            is FollowRequestsAction.DismissError -> dismissError()
        }
    }

    /**
     * Authorize a follow request.
     */
    private fun authorizeRequest(account: Account) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingAccountId = account.id) }

            when (authorizeFollowRequestUseCase(account.id)) {
                is Result.Success -> {
                    _processedAccountIds.update { it + account.id }
                    _uiState.update { it.copy(processingAccountId = null) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            processingAccountId = null,
                            errorMessage = UiText.of(R.string.error_authorize_follow_request)
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Reject a follow request.
     */
    private fun rejectRequest(account: Account) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingAccountId = account.id) }

            when (rejectFollowRequestUseCase(account.id)) {
                is Result.Success -> {
                    _processedAccountIds.update { it + account.id }
                    _uiState.update { it.copy(processingAccountId = null) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            processingAccountId = null,
                            errorMessage = UiText.of(R.string.error_reject_follow_request)
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
 * UI state for Follow Requests Screen.
 */
data class FollowRequestsUiState(
    val processingAccountId: String? = null,
    val errorMessage: UiText? = null
)

/**
 * User actions for Follow Requests Screen.
 */
sealed class FollowRequestsAction {
    data class AuthorizeRequest(val account: Account) : FollowRequestsAction()
    data class RejectRequest(val account: Account) : FollowRequestsAction()
    data object DismissError : FollowRequestsAction()
}
