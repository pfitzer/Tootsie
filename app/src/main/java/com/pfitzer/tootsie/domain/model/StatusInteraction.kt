package com.pfitzer.tootsie.domain.model

/**
 * The user's interaction state with a status, as last confirmed by the server.
 * Applied on top of lists that were loaded before the interaction happened.
 */
data class StatusInteraction(
    val favourited: Boolean,
    val favouritesCount: Int,
    val reblogged: Boolean,
    val reblogsCount: Int,
    val bookmarked: Boolean,
    val pinned: Boolean
)

/**
 * Snapshot of the interaction state of this status.
 */
fun Status.toInteraction() = StatusInteraction(
    favourited = favourited,
    favouritesCount = favouritesCount,
    reblogged = reblogged,
    reblogsCount = reblogsCount,
    bookmarked = bookmarked,
    pinned = pinned
)

/**
 * Apply known interaction overrides to this status and its boosted status.
 */
fun Status.withInteractions(overrides: Map<String, StatusInteraction>): Status {
    if (overrides.isEmpty()) return this
    val updatedReblog = reblog?.withInteractions(overrides)
    val interaction = overrides[id]
    if (interaction == null && updatedReblog === reblog) return this
    val base = if (updatedReblog === reblog) this else copy(reblog = updatedReblog)
    return interaction?.let {
        base.copy(
            favourited = it.favourited,
            favouritesCount = it.favouritesCount,
            reblogged = it.reblogged,
            reblogsCount = it.reblogsCount,
            bookmarked = it.bookmarked,
            pinned = it.pinned
        )
    } ?: base
}
