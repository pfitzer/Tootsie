package com.pfitzer.tootsie.domain.usecase.push

import com.pfitzer.tootsie.domain.model.*
import com.pfitzer.tootsie.domain.repository.PushRepository
import com.pfitzer.tootsie.push.UnifiedPushManager
import com.pfitzer.tootsie.util.Result
import com.pfitzer.tootsie.util.VapidKeyGenerator
import javax.inject.Inject

/**
 * Use case for enabling push notifications.
 * Generates VAPID keys, saves initial config, and registers with UnifiedPush.
 */
class EnablePushUseCase @Inject constructor(
    private val pushRepository: PushRepository,
    private val unifiedPushManager: UnifiedPushManager
) {
    /**
     * Enable push notifications for an account.
     *
     * Flow:
     * 1. Check if UnifiedPush distributor is available
     * 2. Generate VAPID keys and auth secret
     * 3. Save initial config to DataStore
     * 4. Register with UnifiedPush (triggers onNewEndpoint callback)
     *
     * @param accountId Account identifier
     * @param alerts Alert preferences
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(accountId: String, alerts: PushAlerts): Result<Unit> {
        // Check if distributor is available
        if (!unifiedPushManager.isDistributorAvailable()) {
            return Result.Error("No UnifiedPush distributor available")
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
