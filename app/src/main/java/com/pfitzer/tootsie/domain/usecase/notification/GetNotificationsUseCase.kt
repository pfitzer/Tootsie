package com.pfitzer.tootsie.domain.usecase.notification

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Notification
import com.pfitzer.tootsie.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting notifications with paging.
 */
class GetNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    /**
     * Get notifications.
     *
     * @param excludeTypes List of notification types to exclude
     * @param accountId Only show notifications from this account
     * @return Flow of paged notifications
     */
    operator fun invoke(
        excludeTypes: List<String>? = null,
        accountId: String? = null
    ): Flow<PagingData<Notification>> {
        return notificationRepository.getNotifications(
            excludeTypes = excludeTypes,
            accountId = accountId
        )
    }
}
