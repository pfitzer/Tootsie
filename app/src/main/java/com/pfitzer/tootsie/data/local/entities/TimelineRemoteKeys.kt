package com.pfitzer.tootsie.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing pagination keys for timelines.
 * Used by Paging 3 RemoteMediator to track next/previous page IDs.
 */
@Entity(tableName = "timeline_remote_keys")
data class TimelineRemoteKeys(
    @PrimaryKey
    val statusId: String,
    val timelineType: String, // "home", "local", "federated"
    val prevKey: String?,
    val nextKey: String?,
    val lastRefreshed: Long = System.currentTimeMillis()
)
