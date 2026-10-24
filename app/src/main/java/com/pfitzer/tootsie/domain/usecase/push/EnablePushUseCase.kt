package com.pfitzer.tootsie.domain.usecase.push

import android.content.Context
import com.pfitzer.tootsie.domain.model.*
import com.pfitzer.tootsie.domain.repository.PushRepository
import com.pfitzer.tootsie.push.UnifiedPushManager
import com.pfitzer.tootsie.util.Result
import com.pfitzer.tootsie.util.VapidKeyGenerator
import com.pfitzer.tootsie.worker.NotificationPollWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Use case for enabling push notifications.
 * Generates VAPID keys, saves initial config, and registers with UnifiedPush.
 */
class EnablePushUseCase @Inject constructor(
    private val pushRepository: PushRepository,
    private val unifiedPushManager: UnifiedPushManager,
    @ApplicationContext private val context: Context
) {
    /**
     * Enable push notifications for an account.
     *
     * Flow:
     * 1. Check if a UnifiedPush distributor is installed; if not, enable polling instead
     * 2. Generate VAPID keys and auth secret
     * 3. Save initial config to DataStore
     * 4. Register with UnifiedPush (triggers onNewEndpoint callback)
     *
     * @param accountId Account identifier
     * @param alerts Alert preferences
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(accountId: String, alerts: PushAlerts): Result<Unit> {
        // Without a distributor, fall back to polling every 15 minutes
        if (!unifiedPushManager.isDistributorAvailable()) {
            pushRepository.savePushConfig(
                AccountPushConfig(
                    accountId = accountId,
                    enabled = true,
                    endpoint = null,
                    vapidPublicKey = null,
                    vapidPrivateKey = null,
                    authSecret = null,
                    alerts = alerts,
                    status = PushStatus.POLLING
                )
            )
            NotificationPollWorker.schedule(context)
            return Result.Success(Unit)
        }

        // Generate VAPID keys
        val keyPair = VapidKeyGenerator.generateKeyPair()
        val authSecret = VapidKeyGenerator.generateAuthSecret()

        // Save initial config
        val config = AccountPushConfig(
            accountId = accountId,
            enabled = true,
            endpoint = null, // Will be set by UnifiedPush callback
            vapidPublicKey = keyPair.publicKey,
            vapidPrivateKey = keyPair.privateKey,
            authSecret = authSecret,
            alerts = alerts,
            status = PushStatus.DISABLED
        )
        pushRepository.savePushConfig(config)

        // Register with UnifiedPush (will trigger onNewEndpoint callback)
        unifiedPushManager.registerForPush(accountId)

        return Result.Success(Unit)
    }
}
