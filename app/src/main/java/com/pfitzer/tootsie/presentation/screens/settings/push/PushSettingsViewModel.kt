package com.pfitzer.tootsie.presentation.screens.settings.push

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.domain.model.PushAlerts
import com.pfitzer.tootsie.domain.model.PushStatus
import com.pfitzer.tootsie.domain.usecase.push.*
import com.pfitzer.tootsie.push.UnifiedPushManager
import com.pfitzer.tootsie.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for push settings screen.
 */
data class PushSettingsUiState(
    val enabled: Boolean = false,
    val alerts: PushAlerts = PushAlerts(),
    val status: PushStatus = PushStatus.DISABLED,
    val distributorName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User actions for push settings.
 */
sealed interface PushSettingsAction {
    data class TogglePush(val enabled: Boolean) : PushSettingsAction
    data class UpdateAlerts(val alerts: PushAlerts) : PushSettingsAction
    data object DismissError : PushSettingsAction
}

/**
 * ViewModel for push notification settings.
 * Manages push configuration and alert preferences.
 */
@HiltViewModel
class PushSettingsViewModel @Inject constructor(
    private val enablePushUseCase: EnablePushUseCase,
    private val updatePushAlertsUseCase: UpdatePushAlertsUseCase,
    private val getPushConfigUseCase: GetPushConfigUseCase,
    private val unifiedPushManager: UnifiedPushManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PushSettingsUiState())
    val uiState: StateFlow<PushSettingsUiState> = _uiState.asStateFlow()

    init {
        loadPushConfig()
        loadDistributorInfo()
    }

    /**
     * Load current push configuration.
     */
    private fun loadPushConfig() {
        viewModelScope.launch {
            val accountId = preferencesManager.getActiveAccountId() ?: return@launch

            when (val result = getPushConfigUseCase(accountId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            enabled = result.data.enabled,
                            alerts = result.data.alerts,
                            status = result.data.status,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.message,
                            isLoading = false
                        )
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Load UnifiedPush distributor information.
     */
    private fun loadDistributorInfo() {
        val distributor = unifiedPushManager.getDistributor()
        val distributorName = if (distributor.isNotEmpty()) {
            // Extract app name from package (e.g., "org.unifiedpush.distributor.nextpush" -> "NextPush")
            distributor.substringAfterLast(".")
                .replaceFirstChar { it.uppercase() }
        } else {
            "None"
        }

        _uiState.update { it.copy(distributorName = distributorName) }
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: PushSettingsAction) {
        when (action) {
            is PushSettingsAction.TogglePush -> togglePush(action.enabled)
            is PushSettingsAction.UpdateAlerts -> updateAlerts(action.alerts)
            is PushSettingsAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    /**
     * Enable or disable push notifications.
     */
    private fun togglePush(enabled: Boolean) {
        viewModelScope.launch {
            val accountId = preferencesManager.getActiveAccountId() ?: return@launch

            _uiState.update { it.copy(isLoading = true) }

            if (enabled) {
                // Enable push
                when (val result = enablePushUseCase(accountId, _uiState.value.alerts)) {
                    is Result.Success -> {
                        loadPushConfig()
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                errorMessage = result.message,
                                isLoading = false
                            )
                        }
                    }
                    else -> {}
                }
            } else {
                // Disable push (unregister from UnifiedPush)
                unifiedPushManager.unregisterFromPush(accountId)
                _uiState.update {
                    it.copy(
                        enabled = false,
                        status = PushStatus.DISABLED,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Update push alert preferences.
     */
    private fun updateAlerts(alerts: PushAlerts) {
        viewModelScope.launch {
            val accountId = preferencesManager.getActiveAccountId() ?: return@launch

            when (val result = updatePushAlertsUseCase(accountId, alerts)) {
                is Result.Success -> {
                    _uiState.update { it.copy(alerts = alerts) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }
}
