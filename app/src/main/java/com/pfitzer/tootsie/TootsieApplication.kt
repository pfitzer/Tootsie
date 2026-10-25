package com.pfitzer.tootsie

import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pfitzer.tootsie.data.repository.AuthRepositoryImpl
import com.pfitzer.tootsie.domain.model.AccountPushConfig
import com.pfitzer.tootsie.domain.model.PushStatus
import com.pfitzer.tootsie.domain.repository.PushRepository
import com.pfitzer.tootsie.push.NotificationHelper
import com.pfitzer.tootsie.push.PushDecryptor
import com.pfitzer.tootsie.push.UnifiedPushHandler
import com.pfitzer.tootsie.util.Result
import com.pfitzer.tootsie.worker.NotificationPollWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TootsieApplication"

/**
 * Application class implementing UnifiedPush callbacks.
 * Handles push notification registration and message processing.
 */
@HiltAndroidApp
class TootsieApplication : Application(), UnifiedPushHandler, Configuration.Provider {

    @Inject lateinit var authRepository: AuthRepositoryImpl
    @Inject lateinit var pushRepository: PushRepository
    @Inject lateinit var pushDecryptor: PushDecryptor
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Handle new UnifiedPush endpoint registration.
     * Called when UnifiedPush distributor provides a push endpoint.
     *
     * Flow:
     * 1. Get saved config with VAPID keys
     * 2. Subscribe to Mastodon push API with endpoint
     * 3. Update config with endpoint and active status
     * 4. Cancel WorkManager polling since push is active
     */
    override fun handleNewEndpoint(endpoint: String, accountId: String) {
        applicationScope.launch {
            // Push callbacks may start a cold process without the splash screen
            authRepository.ensureInitialized()

            // Get saved config with VAPID keys
            val configResult = pushRepository.getPushConfig(accountId)
            if (configResult is Result.Success) {
                val config = configResult.data

                // Subscribe to Mastodon push API
                val subscribeResult = pushRepository.subscribeToPush(
                    accountId = accountId,
                    endpoint = endpoint,
                    vapidPublicKey = config.vapidPublicKey ?: return@launch,
                    authSecret = config.authSecret ?: return@launch,
                    alerts = config.alerts
                )

                if (subscribeResult is Result.Success) {
                    // Update config with endpoint and active status
                    val updatedConfig = config.copy(
                        endpoint = endpoint,
                        status = PushStatus.ACTIVE
                    )
                    pushRepository.savePushConfig(updatedConfig)

                    // Polling stops for this account; the worker cancels itself once no
                    // account needs it anymore
                }
            }
        }
    }

    /**
     * Handle incoming push message.
     * Called when UnifiedPush receives a notification.
     *
     * Flow:
     * 1. Get saved config with decryption keys
     * 2. Decrypt message payload
     * 3. Display notification
     */
    override fun handleMessage(message: ByteArray, accountId: String) {
        applicationScope.launch {
            val configResult = pushRepository.getPushConfig(accountId)
            if (configResult is Result.Success) {
                val config = configResult.data

                // Decrypt message (RFC 8291 aes128gcm)
                val payload = pushDecryptor.decrypt(
                    message,
                    config.vapidPublicKey,
                    config.vapidPrivateKey,
                    config.authSecret
                )

                if (payload != null) {
                    notificationHelper.showPushNotification(payload, accountId)
                    // If polling takes over later, it starts after this notification
                    payload.notification_id?.let { preferencesManager.saveLastNotificationId(accountId, it) }
                } else if (!pushRepository.isStandardSubscription(accountId)) {
                    // Subscriptions created before standard encryption was requested use the
                    // legacy scheme, which can't be decrypted via UnifiedPush. Subscribe again
                    // once with the existing endpoint and keys to switch to aes128gcm.
                    resubscribeWithStandardEncryption(config)
                }
            }
        }
    }

    private suspend fun resubscribeWithStandardEncryption(config: AccountPushConfig) {
        val endpoint = config.endpoint ?: return
        Log.i(TAG, "Re-subscribing account ${config.accountId} with standard Web Push encryption")
        authRepository.ensureInitialized()
        pushRepository.subscribeToPush(
            accountId = config.accountId,
            endpoint = endpoint,
            vapidPublicKey = config.vapidPublicKey ?: return,
            authSecret = config.authSecret ?: return,
            alerts = config.alerts
        )
    }

    /**
     * Handle UnifiedPush registration failure.
     * Called when no distributor is available or registration fails.
     *
     * Flow:
     * 1. Update config status to POLLING
     * 2. Start WorkManager 15-minute polling as fallback
     */
    override fun handleRegistrationFailed(accountId: String) {
        applicationScope.launch {
            // Fall back to WorkManager polling
            val configResult = pushRepository.getPushConfig(accountId)
            if (configResult is Result.Success) {
                val config = configResult.data.copy(status = PushStatus.POLLING)
                pushRepository.savePushConfig(config)

                // Start polling
                NotificationPollWorker.schedule(this@TootsieApplication)
            }
        }
    }

    /**
     * Handle UnifiedPush unregistration.
     * Called when user disables push or distributor is uninstalled.
     *
     * Flow:
     * 1. Unsubscribe from Mastodon push API
     * 2. Delete local config
     */
    override fun handleUnregistered(accountId: String) {
        applicationScope.launch {
            authRepository.ensureInitialized()

            // Clean up subscription
            pushRepository.unsubscribeFromPush(accountId)
            pushRepository.deletePushConfig(accountId)
        }
    }

    /**
     * Provide WorkManager configuration with Hilt worker factory.
     * Required for @HiltWorker dependency injection.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

