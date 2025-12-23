package com.pfitzer.tootsie.presentation.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.usecase.account.FollowAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.GetAccountStatusesUseCase
import com.pfitzer.tootsie.domain.usecase.account.GetAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.GetRelationshipUseCase
import com.pfitzer.tootsie.domain.usecase.account.UnfollowAccountUseCase
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
 * ViewModel for Profile Screen.
 * Manages account information, relationship status, and user timeline.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getAccountUseCase: GetAccountUseCase,
    private val getRelationshipUseCase: GetRelationshipUseCase,
    private val followAccountUseCase: FollowAccountUseCase,
    private val unfollowAccountUseCase: UnfollowAccountUseCase,
    private val getAccountStatusesUseCase: GetAccountStatusesUseCase,
    private val accountRepository: com.pfitzer.tootsie.domain.repository.AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String? = savedStateHandle["accountId"]

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Paging data flow for account timeline.
     */
    val timelinePagingData: Flow<PagingData<Status>>? = accountId?.let { id ->
        getAccountStatusesUseCase(
            accountId = id,
            excludeReplies = false,
            excludeReblogs = false
        ).cachedIn(viewModelScope)
    }

    init {
        loadProfile()
    }

    /**
     * Load profile information.
     * If accountId is null, load current user's profile.
     */
    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Load account info
            val accountResult = if (accountId != null) {
                getAccountUseCase(accountId)
            } else {
                accountRepository.getCurrentAccount()
            }

            when (accountResult) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            account = accountResult.data,
                            isOwnProfile = accountId == null
                        )
                    }

                    // Load relationship if viewing another user's profile
                    if (accountId != null) {
                        loadRelationship(accountId)
                    }

                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load profile: ${accountResult.message}"
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Load relationship status with the account.
     */
    private suspend fun loadRelationship(accountId: String) {
        when (val result = getRelationshipUseCase(accountId)) {
            is Result.Success -> {
                _uiState.update { it.copy(relationship = result.data) }
            }
            is Result.Error -> {
                // Don't show error for relationship - it's not critical
                _uiState.update { it.copy(relationship = null) }
            }
            is Result.Loading -> Unit
        }
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.FollowAccount -> followAccount()
            is ProfileAction.UnfollowAccount -> unfollowAccount()
            is ProfileAction.Refresh -> loadProfile()
            is ProfileAction.DismissError -> dismissError()
        }
    }

    /**
     * Follow the account.
     */
    private fun followAccount() {
        val currentAccountId = accountId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingFollow = true) }

            when (val result = followAccountUseCase(currentAccountId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            relationship = result.data,
                            isProcessingFollow = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingFollow = false,
                            errorMessage = "Failed to follow account"
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unfollow the account.
     */
    private fun unfollowAccount() {
        val currentAccountId = accountId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingFollow = true) }

            when (val result = unfollowAccountUseCase(currentAccountId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            relationship = result.data,
                            isProcessingFollow = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingFollow = false,
                            errorMessage = "Failed to unfollow account"
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
 * UI state for Profile Screen.
 */
data class ProfileUiState(
    val account: Account? = null,
    val relationship: Relationship? = null,
    val isOwnProfile: Boolean = false,
    val isLoading: Boolean = false,
    val isProcessingFollow: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for Profile Screen.
 */
sealed class ProfileAction {
    data object FollowAccount : ProfileAction()
    data object UnfollowAccount : ProfileAction()
    data object Refresh : ProfileAction()
    data object DismissError : ProfileAction()
}
