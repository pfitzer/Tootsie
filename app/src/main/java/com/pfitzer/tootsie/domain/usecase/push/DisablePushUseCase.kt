package com.pfitzer.tootsie.domain.usecase.push

import android.content.Context
import com.pfitzer.tootsie.domain.repository.PushRepository
import com.pfitzer.tootsie.push.UnifiedPushManager
import com.pfitzer.tootsie.util.Result
import com.pfitzer.tootsie.worker.NotificationPollWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Use case for disabling push notifications.
 *
 * Flow:
 * 1. Unregister from UnifiedPush distributor
 * 2. Unsubscribe from Mastodon push API
 * 3. Delete local push configuration
 * 4. Cancel WorkManager polling
 */
class DisablePushUseCase @Inject constructor(
    private val pushRepository: PushRepository,
    private val unifiedPushManager: UnifiedPushManager,
    @ApplicationContext private val context: Context
) {
    /**
     * Disable push notifications for the specified account.
     *
     * @param accountId Account to disable push for
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(accountId: String): Result<Unit> {
        return try {
            // Unregister from UnifiedPush
            unifiedPushManager.unregisterFromPush(accountId)

            // Unsubscribe from Mastodon push API
            // Note: This may fail if already unsubscribed, which is okay
            pushRepository.unsubscribeFromPush(accountId)

            // Delete local configuration
            pushRepository.deletePushConfig(accountId)

            // Cancel WorkManager polling
            NotificationPollWorker.cancel(context)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to disable push notifications")
        }
    }
}
