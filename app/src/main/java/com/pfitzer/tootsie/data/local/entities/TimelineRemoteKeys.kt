package com.pfitzer.tootsie.data.local.entities

import androidx.room.Entity

/**
 * Room entity for storing pagination keys for timelines.
 * Used by Paging 3 RemoteMediator to track next/previous page IDs.
 */
@Entity(
    tableName = "timeline_remote_keys",
    primaryKeys = ["statusId", "timelineType", "ownerAccountId"]
)
data class TimelineRemoteKeys(
    val statusId: String,
    val timelineType: String, // "home", "local", "federated"
    val ownerAccountId: String, // ID of logged-in account that owns this timeline
    val prevKey: String?,
    val nextKey: String?,
    val lastRefreshed: Long = System.currentTimeMillis()
)
