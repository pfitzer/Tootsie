package com.pfitzer.tootsie.domain.model

/**
 * Domain model representing a Mastodon filter.
 * Filters allow users to hide or show warnings for posts containing specific keywords.
 *
 * @property id Unique identifier of the filter
 * @property title User-defined name of the filter
 * @property context List of contexts where the filter applies
 * @property expiresAt ISO 8601 timestamp when the filter expires, or null if permanent
 * @property filterAction Action to take when a post matches the filter
 * @property keywords List of keywords that trigger the filter
 */
data class Filter(
    val id: String,
    val title: String,
    val context: List<FilterContext>,
    val expiresAt: String?,
    val filterAction: FilterAction,
    val keywords: List<FilterKeyword>
)

/**
 * Context where a filter applies.
 */
enum class FilterContext {
    /** Home timeline and lists */
    HOME,

    /** Notifications timeline */
    NOTIFICATIONS,

    /** Public timelines (local and federated) */
    PUBLIC,

    /** Expanded thread/conversation view */
    THREAD,

    /** Profile viewing */
    ACCOUNT;

    companion object {
        fun fromString(value: String): FilterContext {
            return when (value.lowercase()) {
                "home" -> HOME
                "notifications" -> NOTIFICATIONS
                "public" -> PUBLIC
                "thread" -> THREAD
                "account" -> ACCOUNT
                else -> HOME // Default
            }
        }
    }

    fun toApiString(): String {
        return when (this) {
            HOME -> "home"
            NOTIFICATIONS -> "notifications"
            PUBLIC -> "public"
            THREAD -> "thread"
            ACCOUNT -> "account"
        }
    }
}

/**
 * Action to take when a post matches a filter.
 */
enum class FilterAction {
    /** Show a warning with option to reveal content */
    WARN,

    /** Hide the post completely */
    HIDE;

    companion object {
        fun fromString(value: String): FilterAction {
            return when (value.lowercase()) {
                "warn" -> WARN
                "hide" -> HIDE
                else -> WARN // Default to warn
            }
        }
    }

    fun toApiString(): String {
        return when (this) {
            WARN -> "warn"
            HIDE -> "hide"
        }
    }
}

/**
 * A keyword that triggers a filter.
 *
 * @property id Unique identifier of the keyword
 * @property keyword The text to match
 * @property wholeWord Whether to match whole words only
 */
data class FilterKeyword(
    val id: String,
    val keyword: String,
    val wholeWord: Boolean
)
