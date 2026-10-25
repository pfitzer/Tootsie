package com.pfitzer.tootsie.presentation.screens.profile

import com.pfitzer.tootsie.domain.model.remoteDomain
import com.pfitzer.tootsie.domain.usecase.domainblock.BlockDomainUseCase
import com.pfitzer.tootsie.domain.usecase.domainblock.UnblockDomainUseCase
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.usecase.account.*
import com.pfitzer.tootsie.domain.usecase.report.ReportUseCase
import com.pfitzer.tootsie.domain.usecase.status.*
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.presentation.util.applyInteractions

/**
 * ViewModel for Profile Screen.
 * Manages account information, relationship status, and user timeline.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val statusInteractionStore: StatusInteractionStore,
    private val getAccountUseCase: GetAccountUseCase,
    private val getRelationshipUseCase: GetRelationshipUseCase,
    private val followAccountUseCase: FollowAccountUseCase,
    private val unfollowAccountUseCase: UnfollowAccountUseCase,
    private val muteAccountUseCase: MuteAccountUseCase,
    private val unmuteAccountUseCase: UnmuteAccountUseCase,
    private val blockAccountUseCase: BlockAccountUseCase,
    private val unblockAccountUseCase: UnblockAccountUseCase,
    private val blockDomainUseCase: BlockDomainUseCase,
    private val unblockDomainUseCase: UnblockDomainUseCase,
    private val getAccountStatusesUseCase: GetAccountStatusesUseCase,
    private val observeActiveAccountUseCase: ObserveActiveAccountUseCase,
    private val favoriteStatusUseCase: FavoriteStatusUseCase,
    private val unfavoriteStatusUseCase: UnfavoriteStatusUseCase,
    private val reblogStatusUseCase: ReblogStatusUseCase,
    private val unreblogStatusUseCase: UnreblogStatusUseCase,
    private val bookmarkStatusUseCase: BookmarkStatusUseCase,
    private val unbookmarkStatusUseCase: UnbookmarkStatusUseCase,
    private val pinStatusUseCase: PinStatusUseCase,
    private val unpinStatusUseCase: UnpinStatusUseCase,
    private val voteOnPollUseCase: VoteOnPollUseCase,
    private val reportUseCase: ReportUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String? = savedStateHandle["accountId"]

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

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
     * Resolved account ID - set after profile loads.
     * For own profile, this is populated from the loaded account since SavedStateHandle accountId is null.
     */
    private val _resolvedAccountId = MutableStateFlow(accountId)

    /**
     * Map of profile tab to its paging data flow.
     * Derived from _resolvedAccountId so it works for both own profile and other profiles.
     */
    val tabPagingData: StateFlow<Map<ProfileTab, Flow<PagingData<Status>>>> =
        _resolvedAccountId.filterNotNull().map { id ->
            ProfileTab.entries.associateWith { tab ->
                when (tab) {
                    ProfileTab.POSTS -> getAccountStatusesUseCase(
                        accountId = id,
                        excludeReplies = true
                    )
                    ProfileTab.POSTS_AND_REPLIES -> getAccountStatusesUseCase(
                        accountId = id,
                        excludeReplies = false
                    )
                    ProfileTab.MEDIA -> getAccountStatusesUseCase(
                        accountId = id,
                        onlyMedia = true
                    )
                    ProfileTab.FEATURED -> getAccountStatusesUseCase(
                        accountId = id,
                        pinned = true
                    )
                }.cachedIn(viewModelScope)
                    .applyInteractions(statusInteractionStore, viewModelScope)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

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
            val accountResult = getAccountUseCase(accountId)

            when (accountResult) {
                is Result.Success -> {
                    // Read the active account directly: activeAccount is a WhileSubscribed
                    // StateFlow whose value is still null at this point
                    val ownAccountId = observeActiveAccountUseCase().first()?.id
                    val isOwnProfile = accountId == null ||
                        (ownAccountId != null && accountResult.data.id == ownAccountId)
                    _uiState.update {
                        it.copy(
                            account = accountResult.data,
                            isOwnProfile = isOwnProfile
                        )
                    }

                    // Set resolved account ID (fixes own-profile having no timeline)
                    _resolvedAccountId.value = accountId ?: accountResult.data.id

                    // Load relationship if viewing another user's profile
                    if (accountId != null && !isOwnProfile) {
                        loadRelationship(accountId)
                    }

                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.of(R.string.error_load_profile)
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
            is ProfileAction.MuteAccount -> muteAccount()
            is ProfileAction.UnmuteAccount -> unmuteAccount()
            is ProfileAction.BlockAccount -> blockAccount()
            is ProfileAction.UnblockAccount -> unblockAccount()
            is ProfileAction.BlockDomain -> setDomainBlocked(true)
            is ProfileAction.UnblockDomain -> setDomainBlocked(false)
            is ProfileAction.Refresh -> loadProfile()
            is ProfileAction.DismissError -> dismissError()
            is ProfileAction.FavoriteStatus -> favoriteStatus(action.status)
            is ProfileAction.UnfavoriteStatus -> unfavoriteStatus(action.status)
            is ProfileAction.BoostStatus -> boostStatus(action.status)
            is ProfileAction.UnboostStatus -> unboostStatus(action.status)
            is ProfileAction.BookmarkStatus -> bookmarkStatus(action.status)
            is ProfileAction.UnbookmarkStatus -> unbookmarkStatus(action.status)
            is ProfileAction.PinStatus -> pinStatus(action.status)
            is ProfileAction.UnpinStatus -> unpinStatus(action.status)
            is ProfileAction.VoteOnPoll -> voteOnPoll(action.pollId, action.choices)
            is ProfileAction.ReportAccount -> reportAccount(action.category, action.comment, action.forward)
            is ProfileAction.ReportStatus -> reportStatus(action.status, action.category, action.comment, action.forward)
            is ProfileAction.DismissSuccess -> dismissSuccess()
        }
    }

    /**
     * Favorite a status.
     */
    private fun favoriteStatus(status: Status) {
        viewModelScope.launch {
            when (val result = favoriteStatusUseCase(status.id)) {
                is Result.Success -> {
                    // Paging will automatically update if using a reactive Room source,
                    // otherwise we might need to manually refresh or update the paging data.
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_favorite)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unfavorite a status.
     */
    private fun unfavoriteStatus(status: Status) {
        viewModelScope.launch {
            when (val result = unfavoriteStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_unfavorite)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Boost a status.
     */
    private fun boostStatus(status: Status) {
        viewModelScope.launch {
            when (val result = reblogStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_boost)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unboost a status.
     */
    private fun unboostStatus(status: Status) {
        viewModelScope.launch {
            when (val result = unreblogStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_unboost)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Bookmark a status.
     */
    private fun bookmarkStatus(status: Status) {
        viewModelScope.launch {
            when (val result = bookmarkStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_bookmark)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unbookmark a status.
     */
    private fun unbookmarkStatus(status: Status) {
        viewModelScope.launch {
            when (val result = unbookmarkStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_unbookmark)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Pin a status to profile.
     */
    private fun pinStatus(status: Status) {
        viewModelScope.launch {
            when (val result = pinStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_pin)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unpin a status from profile.
     */
    private fun unpinStatus(status: Status) {
        viewModelScope.launch {
            when (val result = unpinStatusUseCase(status.id)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_unpin)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Vote on a poll.
     */
    private fun voteOnPoll(pollId: String, choices: List<Int>) {
        viewModelScope.launch {
            when (val result = voteOnPollUseCase(pollId, choices)) {
                is Result.Success -> {
                    // For polls, we often want to refresh to see updated results
                    loadProfile()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_vote)) }
                }
                is Result.Loading -> Unit
            }
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
                            errorMessage = UiText.of(R.string.error_follow_account)
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
                            errorMessage = UiText.of(R.string.error_unfollow_account)
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Mute the account.
     */
    private fun muteAccount() {
        val currentAccountId = accountId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingMute = true) }

            when (val result = muteAccountUseCase(currentAccountId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            relationship = result.data,
                            isProcessingMute = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingMute = false,
                            errorMessage = UiText.of(R.string.error_mute_account)
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unmute the account.
     */
    private fun unmuteAccount() {
        val currentAccountId = accountId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingMute = true) }

            when (val result = unmuteAccountUseCase(currentAccountId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            relationship = result.data,
                            isProcessingMute = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingMute = false,
                            errorMessage = UiText.of(R.string.error_unmute_account)
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Block the account.
     */
    private fun blockAccount() {
        val currentAccountId = accountId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBlock = true) }

            when (val result = blockAccountUseCase(currentAccountId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            relationship = result.data,
                            isProcessingBlock = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingBlock = false,
                            errorMessage = UiText.of(R.string.error_block_account)
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Unblock the account.
     */
    private fun unblockAccount() {
        val currentAccountId = accountId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBlock = true) }

            when (val result = unblockAccountUseCase(currentAccountId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            relationship = result.data,
                            isProcessingBlock = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingBlock = false,
                            errorMessage = UiText.of(R.string.error_unblock_account)
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Block or unblock the server of the (remote) account.
     */
    private fun setDomainBlocked(blocked: Boolean) {
        val currentAccountId = accountId ?: return
        val domain = _uiState.value.account?.remoteDomain ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingDomainBlock = true) }

            val result = if (blocked) blockDomainUseCase(domain) else unblockDomainUseCase(domain)
            when (result) {
                is Result.Success -> {
                    // Blocking a domain also removes follows, so reload the whole relationship
                    loadRelationship(currentAccountId)
                    _uiState.update {
                        it.copy(
                            isProcessingDomainBlock = false,
                            successMessage = UiText.of(
                                if (blocked) R.string.success_block_domain else R.string.success_unblock_domain,
                                domain
                            )
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingDomainBlock = false,
                            errorMessage = UiText.of(
                                if (blocked) R.string.error_block_domain else R.string.error_unblock_domain
                            )
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Report the account.
     */
    private fun reportAccount(category: String, comment: String?, forward: Boolean) {
        val currentAccountId = _resolvedAccountId.value ?: return
        viewModelScope.launch {
            when (reportUseCase(currentAccountId, null, comment, forward, category)) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = UiText.of(R.string.success_report)) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_report)) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Report a status from this profile's timeline.
     */
    private fun reportStatus(status: Status, category: String, comment: String?, forward: Boolean) {
        viewModelScope.launch {
            when (reportUseCase(status.account.id, listOf(status.id), comment, forward, category)) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = UiText.of(R.string.success_report)) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_report)) }
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

    /**
     * Dismiss success message.
     */
    private fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
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
    val isProcessingMute: Boolean = false,
    val isProcessingBlock: Boolean = false,
    val isProcessingDomainBlock: Boolean = false,
    val errorMessage: UiText? = null,
    val successMessage: UiText? = null
)

/**
 * User actions for Profile Screen.
 */
sealed class ProfileAction {
    data object FollowAccount : ProfileAction()
    data object UnfollowAccount : ProfileAction()
    data object MuteAccount : ProfileAction()
    data object UnmuteAccount : ProfileAction()
    data object BlockAccount : ProfileAction()
    data object UnblockAccount : ProfileAction()
    data object BlockDomain : ProfileAction()
    data object UnblockDomain : ProfileAction()
    data object Refresh : ProfileAction()
    data object DismissError : ProfileAction()
    data class FavoriteStatus(val status: Status) : ProfileAction()
    data class UnfavoriteStatus(val status: Status) : ProfileAction()
    data class BoostStatus(val status: Status) : ProfileAction()
    data class UnboostStatus(val status: Status) : ProfileAction()
    data class BookmarkStatus(val status: Status) : ProfileAction()
    data class UnbookmarkStatus(val status: Status) : ProfileAction()
    data class PinStatus(val status: Status) : ProfileAction()
    data class UnpinStatus(val status: Status) : ProfileAction()
    data class VoteOnPoll(val pollId: String, val choices: List<Int>) : ProfileAction()
    data class ReportAccount(val category: String, val comment: String?, val forward: Boolean) : ProfileAction()
    data class ReportStatus(val status: Status, val category: String, val comment: String?, val forward: Boolean) : ProfileAction()
    data object DismissSuccess : ProfileAction()
}

/**
 * Profile tabs for filtering account statuses.
 */
enum class ProfileTab(
    @StringRes val titleResId: Int,
    @StringRes val emptyResId: Int
) {
    POSTS(R.string.profile_tab_posts, R.string.empty_profile_posts),
    POSTS_AND_REPLIES(R.string.profile_tab_posts_replies, R.string.empty_profile_posts_replies),
    MEDIA(R.string.profile_tab_media, R.string.empty_profile_media),
    FEATURED(R.string.profile_tab_featured, R.string.empty_profile_featured)
}
