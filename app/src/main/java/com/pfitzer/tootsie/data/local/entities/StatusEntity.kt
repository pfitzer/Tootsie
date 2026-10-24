package com.pfitzer.tootsie.data.local.entities

import androidx.room.Entity
import androidx.room.Index

/**
 * Room entity for a status in a cached timeline.
 *
 * The cache is the single source of truth for the home, local and federated timelines
 * (see TimelineRemoteMediator). A status can be part of several timelines and of the
 * timelines of several logged-in accounts, so one row exists per (status, timeline, owner).
 *
 * The full status is stored as JSON, so nothing (quotes, polls, cards, …) is lost compared
 * to the network model. Interaction state is kept in separate columns so it can be updated
 * in place after favouriting, boosting, etc.
 */
@Entity(
    tableName = "statuses",
    primaryKeys = ["id", "timelineType", "ownerAccountId"],
    indices = [
        Index(value = ["ownerAccountId", "timelineType"]),
        Index(value = ["contentId"])
    ]
)
data class StatusEntity(
    /** Timeline entry ID: for a boost, the ID of the boost itself. */
    val id: String,
    /** "home", "local" or "federated". */
    val timelineType: String,
    /** ID of the logged-in account whose timeline this row belongs to. */
    val ownerAccountId: String,
    /** ID of the status that is shown: the boosted status for a boost, otherwise [id]. */
    val contentId: String,
    /** The complete status as returned by the API (StatusDto). */
    val statusJson: String,

    // Interaction state of the shown status (contentId), updated in place
    val favourited: Boolean,
    val favouritesCount: Int,
    val reblogged: Boolean,
    val reblogsCount: Int,
    val bookmarked: Boolean,
    val pinned: Boolean,

    // Cache metadata
    val lastSyncedAt: Long = System.currentTimeMillis()
)
