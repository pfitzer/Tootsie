package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.remote.dto.NotificationDto
import com.pfitzer.tootsie.domain.model.Notification
import com.pfitzer.tootsie.domain.model.NotificationType

/**
 * Extension function to convert NotificationDto to Notification domain model.
 */
fun NotificationDto.toDomain(): Notification {
    return Notification(
        id = id,
        type = NotificationType.fromApiValue(type),
        createdAt = createdAt,
        account = account.toDomain(),
        status = status?.toDomain()
    )
}
