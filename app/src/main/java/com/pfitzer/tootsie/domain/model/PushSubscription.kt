package com.pfitzer.tootsie.domain.model

/**
 * Push subscription model representing a Web Push subscription.
 */
data class PushSubscription(
    val id: String,
    val endpoint: String,
    val serverKey: String,
    val alerts: PushAlerts
)

/**
 * Push alerts configuration for notification types.
 */
data class PushAlerts(
    val mention: Boolean = true,
    val follow: Boolean = true,
    val favourite: Boolean = true,
    val reblog: Boolean = true,
    val poll: Boolean = false,
    val status: Boolean = false,
    val update: Boolean = false,
    val followRequest: Boolean = false
)

/**
 * Account-specific push configuration stored locally.
 */
data class AccountPushConfig(
    val accountId: String,
    val enabled: Boolean,
    val endpoint: String?,
    val vapidPublicKey: String?,
    val vapidPrivateKey: String?,
    val authSecret: String?,
    val alerts: PushAlerts,
    val status: PushStatus
)

/**
 * Push notification status enum.
 */
enum class PushStatus {
    ACTIVE,    // UnifiedPush active
    POLLING,   // Fallback to WorkManager
    DISABLED,  // User disabled
    ERROR      // Subscription failed
}
