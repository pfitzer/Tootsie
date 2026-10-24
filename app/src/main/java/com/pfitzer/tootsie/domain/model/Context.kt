package com.pfitzer.tootsie.domain.model

/**
 * Represents the context of a status in a conversation thread.
 * Contains the posts before (ancestors) and replies after (descendants).
 */
data class Context(
    /**
     * Posts before this status in the conversation thread.
     * Ordered from oldest to newest.
     */
    val ancestors: List<Status>,

    /**
     * Replies to this status.
     * Ordered from oldest to newest.
     */
    val descendants: List<Status>,

    /**
     * Server job fetching replies from other instances that are not known yet, if one was
     * started for this request (Mastodon >= 4.5). Reload the context once it has finished.
     */
    val asyncRefresh: AsyncRefresh? = null
)
