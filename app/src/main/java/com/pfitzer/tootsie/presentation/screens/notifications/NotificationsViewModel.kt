package com.pfitzer.tootsie.presentation.screens.notifications

import com.pfitzer.tootsie.R
import com.pfitzer.tootsie.presentation.util.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Notification
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.usecase.notification.ClearAllNotificationsUseCase
import com.pfitzer.tootsie.domain.usecase.notification.DismissNotificationUseCase
import com.pfitzer.tootsie.domain.usecase.notification.GetNotificationsUseCase
import com.pfitzer.tootsie.domain.usecase.status.BookmarkStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.FavoriteStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.ReblogStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.UnbookmarkStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.UnfavoriteStatusUseCase
import com.pfitzer.tootsie.domain.usecase.status.UnreblogStatusUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import com.pfitzer.tootsie.domain.model.withInteractions
import androidx.paging.map
import kotlinx.coroutines.flow.combine

/**
 * ViewModel for Notifications Screen.
 * Manages notification data and user actions.
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val statusInteractionStore: StatusInteractionStore,
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val dismissNotificationUseCase: DismissNotificationUseCase,
    private val clearAllNotificationsUseCase: ClearAllNotificationsUseCase,
    private val accountManagementRepository: AccountManagementRepository,
    private val favoriteStatusUseCase: FavoriteStatusUseCase,
    private val unfavoriteStatusUseCase: UnfavoriteStatusUseCase,
    private val reblogStatusUseCase: ReblogStatusUseCase,
    private val unreblogStatusUseCase: UnreblogStatusUseCase,
    private val bookmarkStatusUseCase: BookmarkStatusUseCase,
    private val unbookmarkStatusUseCase: UnbookmarkStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    /**
     * Observe the currently active account.
     */
    val activeAccount: StateFlow<Account?> = accountManagementRepository.observeActiveAccount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _notificationsPagingData = MutableStateFlow<PagingData<Notification>>(PagingData.empty())
    val notificationsPagingData: Flow<PagingData<Notification>> =
        combine(_notificationsPagingData, statusInteractionStore.interactions) { pagingData, interactions ->
            pagingData.map { notification ->
                notification.copy(status = notification.status?.withInteractions(interactions))
            }
        }

    private var notificationsJob: Job? = null

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        // Cancel previous collection to avoid multiple competing collectors
        notificationsJob?.cancel()
        notificationsJob = getNotificationsUseCase()
            .cachedIn(viewModelScope)
            .onEach { _notificationsPagingData.value = it }
            .launchIn(viewModelScope)
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: NotificationsAction) {
        when (action) {
            is NotificationsAction.DismissNotification -> dismissNotification(action.notificationId)
            is NotificationsAction.ClearAll -> clearAllNotifications()
            is NotificationsAction.DismissError -> dismissError()
            is NotificationsAction.Refresh -> loadNotifications()
            is NotificationsAction.FavoriteStatus -> favoriteStatus(action.status)
            is NotificationsAction.UnfavoriteStatus -> unfavoriteStatus(action.status)
            is NotificationsAction.BoostStatus -> boostStatus(action.status)
            is NotificationsAction.UnboostStatus -> unboostStatus(action.status)
            is NotificationsAction.BookmarkStatus -> bookmarkStatus(action.status)
            is NotificationsAction.UnbookmarkStatus -> unbookmarkStatus(action.status)
        }
    }

    /**
     * Dismiss a single notification.
     */
    private fun dismissNotification(notificationId: String) {
        viewModelScope.launch {
            when (val result = dismissNotificationUseCase(notificationId)) {
                is Result.Success -> {
                    // Success - refresh the paging data to remove the dismissed notification
                    loadNotifications()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = UiText.of(R.string.error_dismiss_notification))
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    /**
     * Clear all notifications.
     */
    private fun clearAllNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingAll = true) }

            when (val result = clearAllNotificationsUseCase()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isClearingAll = false) }
                    // Paging will refresh automatically
                    loadNotifications()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isClearingAll = false,
                            errorMessage = UiText.of(R.string.error_clear_notifications)
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

    /**
     * Favorite a status.
     */
    private fun favoriteStatus(status: Status) {
        viewModelScope.launch {
            when (val result = favoriteStatusUseCase(status.id)) {
                is Result.Success -> loadNotifications()
                is Result.Error -> _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_favorite)) }
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
                is Result.Success -> loadNotifications()
                is Result.Error -> _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_unfavorite)) }
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
                is Result.Success -> loadNotifications()
                is Result.Error -> _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_boost)) }
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
                is Result.Success -> loadNotifications()
                is Result.Error -> _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_unboost)) }
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
                is Result.Success -> loadNotifications()
                is Result.Error -> _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_bookmark)) }
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
                is Result.Success -> loadNotifications()
                is Result.Error -> _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_unbookmark)) }
                is Result.Loading -> Unit
            }
        }
    }
}

/**
 * UI state for Notifications Screen.
 */
data class NotificationsUiState(
    val isClearingAll: Boolean = false,
    val errorMessage: UiText? = null
)

/**
 * User actions for Notifications Screen.
 */
sealed class NotificationsAction {
    data class DismissNotification(val notificationId: String) : NotificationsAction()
    data object ClearAll : NotificationsAction()
    data object Refresh : NotificationsAction()
    data object DismissError : NotificationsAction()
    data class FavoriteStatus(val status: Status) : NotificationsAction()
    data class UnfavoriteStatus(val status: Status) : NotificationsAction()
    data class BoostStatus(val status: Status) : NotificationsAction()
    data class UnboostStatus(val status: Status) : NotificationsAction()
    data class BookmarkStatus(val status: Status) : NotificationsAction()
    data class UnbookmarkStatus(val status: Status) : NotificationsAction()
}
