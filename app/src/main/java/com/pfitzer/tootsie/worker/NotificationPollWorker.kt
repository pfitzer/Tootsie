package com.pfitzer.tootsie.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
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
    private val notificationRepository: NotificationRepository,
    private val preferencesManager: PreferencesManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val activeAccountId = preferencesManager.getActiveAccountId()
            ?: return androidx.work.ListenableWorker.Result.failure()

        val sinceId = preferencesManager.getLastNotificationId(activeAccountId)

        // Fetch latest notifications
        when (val result = notificationRepository.fetchRecentNotifications(sinceId = sinceId)) {
            is DomainResult.Success -> {
                val notifications = result.data
                if (notifications.isNotEmpty()) {
                    // Display new notifications (reverse to show oldest first if multiple, 
                    // though usually we just want to show them)
                    notifications.forEach { notification ->
                        notificationHelper.showNotification(notification, activeAccountId)
                    }
                    // Update last seen notification ID
                    // The first notification in the list is the most recent from Mastodon API
                    preferencesManager.saveLastNotificationId(activeAccountId, notifications.first().id)
                }
                return androidx.work.ListenableWorker.Result.success()
            }
            else -> return androidx.work.ListenableWorker.Result.retry()
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
         * Called when UnifiedPush becomes available.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME)
        }
    }
}
