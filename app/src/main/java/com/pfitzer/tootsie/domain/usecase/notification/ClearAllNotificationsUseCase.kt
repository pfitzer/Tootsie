package com.pfitzer.tootsie.domain.usecase.notification

import com.pfitzer.tootsie.domain.repository.NotificationRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for clearing all notifications.
 */
class ClearAllNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    /**
     * Clear all notifications.
     *
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(): Result<Unit> {
        return notificationRepository.clearAllNotifications()
    }
}
