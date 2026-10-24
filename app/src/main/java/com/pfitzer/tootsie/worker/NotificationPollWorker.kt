package com.pfitzer.tootsie.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.data.repository.AuthRepositoryImpl
import com.pfitzer.tootsie.domain.model.PushStatus
import com.pfitzer.tootsie.domain.repository.NotificationRepository
import com.pfitzer.tootsie.push.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import java.util.concurrent.TimeUnit
import com.pfitzer.tootsie.util.Result as DomainResult

private const val TAG = "NotificationPollWorker"

internal enum class PollOutcome { SUCCESS, RETRY, AUTH_FAILED }

/**
 * 401/403 mean the account's token was rejected and retrying can't help. Everything else
 * (network errors, 429 rate limit, server errors) is treated as transient.
 */
internal fun pollOutcomeOf(error: DomainResult.Error): PollOutcome {
    val code = (error.exception as? HttpException)?.code()
    return if (code == 401 || code == 403) PollOutcome.AUTH_FAILED else PollOutcome.RETRY
}

/**
 * WorkManager worker for polling notifications when UnifiedPush is unavailable.
 * Polls every 15 minutes as a fallback mechanism.
 */
@HiltWorker
class NotificationPollWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepositoryImpl,
    private val accountDao: AccountDao,
    private val notificationRepository: NotificationRepository,
    private val preferencesManager: PreferencesManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        // The worker may run in a cold process where the splash screen never ran the
        // credential migration
        authRepository.ensureInitialized()

        // Poll every logged-in account that wants notifications but has no working push.
        // Each request uses that account's own credentials, not the active account's.
        val accountIds = accountDao.getAllLoggedInAccounts()
            .map { it.id }
            .filter { accountId ->
                val config = preferencesManager.getPushConfig(accountId)
                // ERROR: polling stopped after the server rejected the account's token
                config.enabled && config.status != PushStatus.ACTIVE && config.status != PushStatus.ERROR
            }

        if (accountIds.isEmpty()) {
            // Push works (or is disabled) for all accounts: polling is no longer needed
            cancel(applicationContext)
            return androidx.work.ListenableWorker.Result.success()
        }

        val outcomes = accountIds.map { pollAccount(it) }
        return when {
            // Transient failure (network, rate limit, server error): retry with backoff
            PollOutcome.RETRY in outcomes -> androidx.work.ListenableWorker.Result.retry()
            // Every remaining account was rejected: nothing left to poll
            outcomes.all { it == PollOutcome.AUTH_FAILED } -> {
                cancel(applicationContext)
                androidx.work.ListenableWorker.Result.failure()
            }
            else -> androidx.work.ListenableWorker.Result.success()
        }
    }

    /**
     * Fetch and show new notifications of one account.
     */
    private suspend fun pollAccount(accountId: String): PollOutcome {
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
                PollOutcome.SUCCESS
            }
            is DomainResult.Error -> {
                val outcome = pollOutcomeOf(result)
                if (outcome == PollOutcome.AUTH_FAILED) {
                    // Token revoked or expired: retrying can't succeed. Stop polling this
                    // account; the push settings show the error state.
                    Log.w(TAG, "Polling rejected for account $accountId, stopping")
                    preferencesManager.savePushConfig(
                        preferencesManager.getPushConfig(accountId).copy(status = PushStatus.ERROR)
                    )
                }
                outcome
            }
            DomainResult.Loading -> PollOutcome.RETRY
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
