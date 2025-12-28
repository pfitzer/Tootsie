package com.pfitzer.tootsie.domain.model

/**
 * Domain model representing a Mastodon List.
 * Lists allow users to group accounts and view a timeline of only those accounts.
 *
 * @property id Unique identifier of the list
 * @property title User-defined title of the list
 * @property repliesPolicy Which replies should be shown ("followed", "list", or "none")
 */
data class MastodonList(
    val id: String,
    val title: String,
    val repliesPolicy: RepliesPolicy
)

/**
 * Replies policy for a list timeline.
 */
enum class RepliesPolicy {
    /** Show replies to any followed user */
    FOLLOWED,

    /** Show replies to members of the list */
    LIST,

    /** Show no replies */
    NONE;

    companion object {
        fun fromString(value: String): RepliesPolicy {
            return when (value.lowercase()) {
                "followed" -> FOLLOWED
                "list" -> LIST
                "none" -> NONE
                else -> LIST // Default to LIST if unknown
            }
        }
    }

    fun toApiString(): String {
        return when (this) {
            FOLLOWED -> "followed"
            LIST -> "list"
            NONE -> "none"
        }
    }
}
