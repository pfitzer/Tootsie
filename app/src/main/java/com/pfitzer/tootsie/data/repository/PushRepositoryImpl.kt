package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.mapper.toDto
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.*
import com.pfitzer.tootsie.domain.model.*
import com.pfitzer.tootsie.domain.repository.PushRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of PushRepository.
 * Handles push subscription with Mastodon API and local configuration storage.
 */
class PushRepositoryImpl @Inject constructor(
    private val apiService: MastodonApiService,
    private val preferencesManager: PreferencesManager
) : PushRepository {

    override suspend fun subscribeToPush(
        accountId: String,
        endpoint: String,
        vapidPublicKey: String,
        authSecret: String,
        alerts: PushAlerts
    ): Result<PushSubscription> = withContext(Dispatchers.IO) {
        try {
            val request = PushSubscriptionRequestDto(
                subscription = SubscriptionKeysDto(
                    endpoint = endpoint,
                    keys = KeysDto(
                        p256dh = vapidPublicKey,
                        auth = authSecret
                    )
                ),
                data = alerts.toDto()
            )

            val response = apiService.subscribeToPush(request)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to subscribe to push")
        }
    }

    override suspend fun updatePushAlerts(
        accountId: String,
        alerts: PushAlerts
    ): Result<PushSubscription> = withContext(Dispatchers.IO) {
        try {
            val config = preferencesManager.getPushConfig(accountId)

            val request = PushSubscriptionRequestDto(
                subscription = SubscriptionKeysDto(
                    endpoint = config.endpoint ?: return@withContext Result.Error("No endpoint"),
                    keys = KeysDto(
                        p256dh = config.vapidPublicKey ?: return@withContext Result.Error("No key"),
                        auth = config.authSecret ?: return@withContext Result.Error("No auth")
                    )
                ),
                data = alerts.toDto()
            )

            val response = apiService.updatePushSubscription(request)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update push alerts")
        }
    }

    override suspend fun unsubscribeFromPush(accountId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                apiService.deletePushSubscription()
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to unsubscribe from push")
            }
        }

    override suspend fun getPushSubscription(accountId: String): Result<PushSubscription> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPushSubscription()
                Result.Success(response.toDomain())
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to get push subscription")
            }
        }

    override suspend fun savePushConfig(config: AccountPushConfig): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                preferencesManager.savePushConfig(config)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to save push config")
            }
        }

    override suspend fun getPushConfig(accountId: String): Result<AccountPushConfig> =
        withContext(Dispatchers.IO) {
            try {
                val config = preferencesManager.getPushConfig(accountId)
                Result.Success(config)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to get push config")
            }
        }

    override suspend fun deletePushConfig(accountId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                preferencesManager.deletePushConfig(accountId)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to delete push config")
            }
        }
}
