package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.AccountPushConfig
import com.pfitzer.tootsie.domain.model.PushAlerts
import com.pfitzer.tootsie.domain.model.PushSubscription
import com.pfitzer.tootsie.util.Result

/**
 * Repository interface for push notification operations.
 * Handles Web Push subscriptions with Mastodon API and local push configuration.
 */
interface PushRepository {
    /**
     * Subscribe to push notifications on the Mastodon server.
     *
     * @param accountId Account identifier
     * @param endpoint UnifiedPush endpoint URL
     * @param vapidPublicKey VAPID public key (base64url)
     * @param authSecret Auth secret (base64url)
     * @param alerts Alert preferences
     * @return Result with PushSubscription on success
     */
    suspend fun subscribeToPush(
        accountId: String,
        endpoint: String,
        vapidPublicKey: String,
        authSecret: String,
        alerts: PushAlerts
    ): Result<PushSubscription>

    /**
     * Update push alert preferences.
     *
     * @param accountId Account identifier
     * @param alerts Updated alert preferences
     * @return Result with updated PushSubscription
     */
    suspend fun updatePushAlerts(accountId: String, alerts: PushAlerts): Result<PushSubscription>

    /**
     * Unsubscribe from push notifications on the Mastodon server.
     *
     * @param accountId Account identifier
     * @return Result indicating success or failure
     */
    suspend fun unsubscribeFromPush(accountId: String): Result<Unit>

    /**
     * Get current push subscription from the Mastodon server.
     *
     * @param accountId Account identifier
     * @return Result with PushSubscription if exists
     */
    suspend fun getPushSubscription(accountId: String): Result<PushSubscription>

    /**
     * Save push configuration locally (DataStore).
     *
     * @param config Push configuration to save
     * @return Result indicating success or failure
     */
    suspend fun savePushConfig(config: AccountPushConfig): Result<Unit>

    /**
     * Get push configuration from local storage (DataStore).
     *
     * @param accountId Account identifier
     * @return Result with AccountPushConfig
     */
    suspend fun getPushConfig(accountId: String): Result<AccountPushConfig>

    /**
     * Delete push configuration from local storage (DataStore).
     *
     * @param accountId Account identifier
     * @return Result indicating success or failure
     */
    suspend fun deletePushConfig(accountId: String): Result<Unit>
}
