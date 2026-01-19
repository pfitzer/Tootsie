package com.pfitzer.tootsie.presentation.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Notification
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import com.pfitzer.tootsie.domain.usecase.notification.ClearAllNotificationsUseCase
import com.pfitzer.tootsie.domain.usecase.notification.DismissNotificationUseCase
import com.pfitzer.tootsie.domain.usecase.notification.GetNotificationsUseCase
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Notifications Screen.
 * Manages notification data and user actions.
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val dismissNotificationUseCase: DismissNotificationUseCase,
    private val clearAllNotificationsUseCase: ClearAllNotificationsUseCase,
    private val accountManagementRepository: AccountManagementRepository
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
    val notificationsPagingData: Flow<PagingData<Notification>> = _notificationsPagingData.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        getNotificationsUseCase()
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
            NotificationsAction.Refresh -> loadNotifications()
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
                        it.copy(errorMessage = "Failed to dismiss notification")
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
                            errorMessage = "Failed to clear notifications"
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
 * UI state for Notifications Screen.
 */
data class NotificationsUiState(
    val isClearingAll: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for Notifications Screen.
 */
sealed class NotificationsAction {
    data class DismissNotification(val notificationId: String) : NotificationsAction()
    data object ClearAll : NotificationsAction()
    data object Refresh : NotificationsAction()
    data object DismissError : NotificationsAction()
}
