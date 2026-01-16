package com.pfitzer.tootsie.domain.repository

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Notification
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for notification operations.
 * Defines the contract for interacting with notifications.
 */
interface NotificationRepository {

    /**
     * Get notifications with paging support.
     * Returns a Flow of PagingData for infinite scrolling.
     *
     * @param excludeTypes List of notification types to exclude
     * @param accountId Only show notifications from this account
     * @return Flow of paged notifications
     */
    fun getNotifications(
        excludeTypes: List<String>? = null,
        accountId: String? = null
    ): Flow<PagingData<Notification>>

    /**
     * Dismiss a single notification.
     *
     * @param notificationId Notification ID to dismiss
     * @return Result indicating success or failure
     */
    suspend fun dismissNotification(notificationId: String): Result<Unit>

    /**
     * Clear all notifications.
     *
     * @return Result indicating success or failure
     */
    suspend fun clearAllNotifications(): Result<Unit>

    /**
     * Fetch recent notifications as a simple list (for polling).
     * Used by WorkManager fallback when UnifiedPush is unavailable.
     *
     * @param limit Maximum number of notifications to fetch
     * @param sinceId Only fetch notifications after this ID
     * @return Result with list of notifications
     */
    suspend fun fetchRecentNotifications(
        limit: Int = 20,
        sinceId: String? = null
    ): Result<List<Notification>>
}
