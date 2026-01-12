package com.pfitzer.tootsie.domain.usecase.push

import com.pfitzer.tootsie.domain.model.PushAlerts
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
     * 1. Update alerts on Mastodon server
     * 2. Update local config with new alerts
     *
     * @param accountId Account identifier
     * @param alerts Updated alert preferences
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(accountId: String, alerts: PushAlerts): Result<Unit> {
        return when (val result = pushRepository.updatePushAlerts(accountId, alerts)) {
            is Result.Success -> {
                // Update local config
                val configResult = pushRepository.getPushConfig(accountId)
                if (configResult is Result.Success) {
                    val updatedConfig = configResult.data.copy(alerts = alerts)
                    pushRepository.savePushConfig(updatedConfig)
                }
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.message)
            is Result.Loading -> Result.Loading
        }
    }
}
