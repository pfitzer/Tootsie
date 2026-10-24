package com.pfitzer.tootsie.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.domain.model.PushStatus
import com.pfitzer.tootsie.domain.repository.NotificationRepository
import com.pfitzer.tootsie.push.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import com.pfitzer.tootsie.util.Result as DomainResult

/**
 * WorkManager worker for polling notifications when UnifiedPush is unavailable.
 * Polls every 15 minutes as a fallback mechanism.
 */
@HiltWorker
class NotificationPollWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val accountDao: AccountDao,
    private val notificationRepository: NotificationRepository,
    private val preferencesManager: PreferencesManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        // Poll every logged-in account that wants notifications but has no working push.
        // Each request uses that account's own credentials, not the active account's.
        val accountIds = accountDao.getAllLoggedInAccounts()
            .map { it.id }
            .filter { accountId ->
                val config = preferencesManager.getPushConfig(accountId)
                config.enabled && config.status != PushStatus.ACTIVE
            }

        if (accountIds.isEmpty()) {
            // Push works (or is disabled) for all accounts: polling is no longer needed
            cancel(applicationContext)
            return androidx.work.ListenableWorker.Result.success()
        }

        var anyFailed = false
        for (accountId in accountIds) {
            if (!pollAccount(accountId)) anyFailed = true
        }
        return if (anyFailed) {
            androidx.work.ListenableWorker.Result.retry()
        } else {
            androidx.work.ListenableWorker.Result.success()
        }
    }

    /**
     * Fetch and show new notifications of one account. Returns false if the request failed.
     */
    private suspend fun pollAccount(accountId: String): Boolean {
        val sinceId = preferencesManager.getLastNotificationId(accountId)
        return when (val result = notificationRepository.fetchRecentNotifications(
            sinceId = sinceId,
            accountId = accountId
        )) {
            is DomainResult.Success -> {
                val notifications = result.data
                if (notifications.isNotEmpty()) {
                    notifications.forEach { notification ->
                        notificationHelper.showNotification(notification, accountId)
                    }
                    // The first notification in the list is the most recent from Mastodon API
                    preferencesManager.saveLastNotificationId(accountId, notifications.first().id)
                }
                true
            }
            else -> false
        }
    }

    companion object {
        private const val WORK_NAME = "notification_poll"
        private const val POLL_INTERVAL_MINUTES = 15L

        /**
         * Schedule periodic notification polling.
         * Only called when UnifiedPush is unavailable.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<NotificationPollWorker>(
                POLL_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }

        /**
         * Cancel notification polling.
         * The worker also cancels itself once no account needs polling anymore.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME)
        }
    }
}
