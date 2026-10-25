package com.pfitzer.tootsie.presentation.screens.followlist

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.FollowListEntry
import com.pfitzer.tootsie.domain.model.FollowListType
import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.domain.usecase.account.FollowAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.GetAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.ObserveActiveAccountUseCase
import com.pfitzer.tootsie.domain.usecase.account.UnfollowAccountUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the followers and following lists of an account.
 * Each entry can be followed or unfollowed.
 */
@HiltViewModel
class FollowListViewModel @Inject constructor(
    accountRepository: AccountRepository,
    private val getAccountUseCase: GetAccountUseCase,
    private val followAccountUseCase: FollowAccountUseCase,
    private val unfollowAccountUseCase: UnfollowAccountUseCase,
    private val observeActiveAccountUseCase: ObserveActiveAccountUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String = checkNotNull(savedStateHandle["accountId"])
    val type: FollowListType = FollowListType.valueOf(checkNotNull(savedStateHandle["type"]))

    private val _uiState = MutableStateFlow(FollowListUiState())
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    /**
     * Relationships changed on this screen; they replace the loaded ones, so the buttons
     * update without reloading the list.
     */
    private val changedRelationships = MutableStateFlow<Map<String, Relationship>>(emptyMap())

    /**
     * Paging data flow for the list.
     */
    val entries: Flow<PagingData<FollowListEntry>> =
        when (type) {
            FollowListType.FOLLOWERS -> accountRepository.getFollowers(accountId)
            FollowListType.FOLLOWING -> accountRepository.getFollowing(accountId)
        }
            .cachedIn(viewModelScope)
            .combine(changedRelationships) { pagingData, changed ->
                pagingData.map { entry ->
                    changed[entry.account.id]?.let { entry.copy(relationship = it) } ?: entry
                }
            }

    init {
        viewModelScope.launch {
            val ownAccountId = observeActiveAccountUseCase().first()?.id
            _uiState.update { it.copy(ownAccountId = ownAccountId) }
        }
        viewModelScope.launch {
            // For the title and the hint about lists of other servers
            (getAccountUseCase(accountId) as? Result.Success)?.let { result ->
                _uiState.update { it.copy(account = result.data) }
            }
        }
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: FollowListAction) {
        when (action) {
            is FollowListAction.ToggleFollow -> toggleFollow(action.entry)
            is FollowListAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    /**
     * Follow the account, or unfollow it (also withdraws a pending follow request).
     */
    private fun toggleFollow(entry: FollowListEntry) {
        val id = entry.account.id
        if (id in _uiState.value.processingIds) return
        val relationship = entry.relationship ?: return
        val unfollow = relationship.following || relationship.requested

        _uiState.update { it.copy(processingIds = it.processingIds + id) }
        viewModelScope.launch {
            val result = if (unfollow) unfollowAccountUseCase(id) else followAccountUseCase(id)
            when (result) {
                is Result.Success -> changedRelationships.update { it + (id to result.data) }
                is Result.Error -> _uiState.update {
                    it.copy(
                        errorMessage = UiText.of(
                            if (unfollow) R.string.error_unfollow_account else R.string.error_follow_account
                        )
                    )
                }
                is Result.Loading -> Unit
            }
            _uiState.update { it.copy(processingIds = it.processingIds - id) }
        }
    }
}

/**
 * UI state for the follow list screen.
 *
 * @param account Whose list is shown (null until loaded)
 * @param ownAccountId The logged-in account, which gets no follow button
 */
data class FollowListUiState(
    val account: Account? = null,
    val ownAccountId: String? = null,
    val processingIds: Set<String> = emptySet(),
    val errorMessage: UiText? = null
) {
    /** Lists of accounts on other servers only contain what this server knows. */
    val isRemoteAccount: Boolean get() = account?.acct?.contains('@') == true
}

/**
 * User actions for the follow list screen.
 */
sealed class FollowListAction {
    data class ToggleFollow(val entry: FollowListEntry) : FollowListAction()
    data object DismissError : FollowListAction()
}
