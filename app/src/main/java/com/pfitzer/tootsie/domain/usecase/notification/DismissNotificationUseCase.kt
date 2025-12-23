package com.pfitzer.tootsie.domain.usecase.notification

import com.pfitzer.tootsie.domain.repository.NotificationRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for dismissing a notification.
 */
class DismissNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    /**
     * Dismiss a notification.
     *
     * @param notificationId Notification ID
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(notificationId: String): Result<Unit> {
        return notificationRepository.dismissNotification(notificationId)
    }
}
