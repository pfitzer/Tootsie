package com.pfitzer.tootsie.domain.model

/**
 * Domain model representing a Mastodon notification.
 * Notifications inform the user about interactions with their account.
 */
data class Notification(
    val id: String,
    val type: NotificationType,
    val createdAt: String, // ISO 8601 timestamp
    val account: Account, // The account that performed the action
    val status: Status? = null // The status involved, if applicable (null for follow notifications)
)

/**
 * Types of notifications supported by Mastodon.
 */
enum class NotificationType(val apiValue: String) {
    /** Someone followed you */
    FOLLOW("follow"),

    /** Someone requested to follow you (for locked accounts) */
    FOLLOW_REQUEST("follow_request"),

    /** Someone mentioned you in their status */
    MENTION("mention"),

    /** Someone boosted one of your statuses */
    REBLOG("reblog"),

    /** Someone favourited one of your statuses */
    FAVOURITE("favourite"),

    /** A poll you voted in or created has ended */
    POLL("poll"),

    /** Someone you enabled notifications for has posted a status */
    STATUS("status"),

    /** A status you interacted with has been edited */
    UPDATE("update"),

    /** Unknown notification type (for forward compatibility) */
    UNKNOWN("unknown");

    companion object {
        /**
         * Get notification type from API value.
         * Returns UNKNOWN for unrecognized types.
         */
        fun fromApiValue(value: String): NotificationType {
            return entries.find { it.apiValue == value } ?: UNKNOWN
        }
    }
}
