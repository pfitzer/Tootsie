package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.mapper.toDto
import com.pfitzer.tootsie.data.remote.AccountCredentials
import com.pfitzer.tootsie.data.remote.AccountCredentialsProvider
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
    private val preferencesManager: PreferencesManager,
    private val accountCredentialsProvider: AccountCredentialsProvider
) : PushRepository {

    // Push subscriptions belong to an account's token, so every call uses the credentials
    // of the given account instead of the currently active one
    private suspend fun credentialsFor(accountId: String): AccountCredentials? =
        accountCredentialsProvider.forAccount(accountId)

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
                    ),
                    standard = true
                ),
                data = PushDataDto(alerts = alerts.toDto())
            )

            val credentials = credentialsFor(accountId)
                ?: return@withContext Result.Error("Account $accountId is not logged in")
            val response = apiService.subscribeToPush(
                request = request,
                authorization = credentials.authorization,
                instance = credentials.instance
            )
            preferencesManager.setPushStandard(accountId, true)
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
                    ),
                    standard = true
                ),
                data = PushDataDto(alerts = alerts.toDto())
            )

            val credentials = credentialsFor(accountId)
                ?: return@withContext Result.Error("Account $accountId is not logged in")
            val response = apiService.updatePushSubscription(
                request = request,
                authorization = credentials.authorization,
                instance = credentials.instance
            )
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update push alerts")
        }
    }

    override suspend fun unsubscribeFromPush(accountId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val credentials = credentialsFor(accountId)
                    ?: return@withContext Result.Error("Account $accountId is not logged in")
                apiService.deletePushSubscription(
                    authorization = credentials.authorization,
                    instance = credentials.instance
                )
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to unsubscribe from push")
            }
        }

    override suspend fun getPushSubscription(accountId: String): Result<PushSubscription> =
        withContext(Dispatchers.IO) {
            try {
                val credentials = credentialsFor(accountId)
                    ?: return@withContext Result.Error("Account $accountId is not logged in")
                val response = apiService.getPushSubscription(
                    authorization = credentials.authorization,
                    instance = credentials.instance
                )
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

    override suspend fun isStandardSubscription(accountId: String): Boolean =
        withContext(Dispatchers.IO) { preferencesManager.isPushStandard(accountId) }

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
