package com.pfitzer.tootsie.domain.usecase.push

import com.pfitzer.tootsie.domain.model.PushAlerts
import com.pfitzer.tootsie.domain.model.PushStatus
import com.pfitzer.tootsie.domain.repository.PushRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for updating push notification alert preferences.
 */
class UpdatePushAlertsUseCase @Inject constructor(
    private val pushRepository: PushRepository
) {
    /**
     * Update push alert preferences for an account.
     *
     * Flow:
     * 1. Save the alerts locally (used by the polling fallback and for new subscriptions)
     * 2. Update the server subscription, if one exists (push via UnifiedPush is active)
     *
     * @param accountId Account identifier
     * @param alerts Updated alert preferences
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(accountId: String, alerts: PushAlerts): Result<Unit> {
        val config = when (val configResult = pushRepository.getPushConfig(accountId)) {
            is Result.Success -> configResult.data
            is Result.Error -> return Result.Error(configResult.message)
            is Result.Loading -> return Result.Loading
        }

        // Without a server subscription (push off, polling, or the distributor hasn't
        // delivered an endpoint yet) there is nothing to update on the server
        if (config.status == PushStatus.ACTIVE && config.endpoint != null) {
            val serverResult = pushRepository.updatePushAlerts(accountId, alerts)
            if (serverResult is Result.Error) return Result.Error(serverResult.message)
        }

        return pushRepository.savePushConfig(config.copy(alerts = alerts))
    }
}
