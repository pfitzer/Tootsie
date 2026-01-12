package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.remote.dto.*
import com.pfitzer.tootsie.domain.model.*

/**
 * Converts PushSubscriptionDto to domain PushSubscription.
 */
fun PushSubscriptionDto.toDomain() = PushSubscription(
    id = id,
    endpoint = endpoint,
    serverKey = serverKey,
    alerts = alerts.toDomain()
)

/**
 * Converts PushAlertsDto to domain PushAlerts.
 */
fun PushAlertsDto.toDomain() = PushAlerts(
    mention = mention,
    follow = follow,
    favourite = favourite,
    reblog = reblog,
    poll = poll,
    status = status,
    update = update,
    followRequest = followRequest
)

/**
 * Converts domain PushAlerts to PushAlertsDto.
 */
fun PushAlerts.toDto() = PushAlertsDto(
    mention = mention,
    follow = follow,
    favourite = favourite,
    reblog = reblog,
    poll = poll,
    status = status,
    update = update,
    followRequest = followRequest
)
