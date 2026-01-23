package com.pfitzer.tootsie.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for cached filters.
 * Stores filter data locally for offline access and reactive updates.
 */
@Entity(tableName = "filters")
data class FilterEntity(
    /**
     * Unique identifier of the filter.
     */
    @PrimaryKey
    val id: String,

    /**
     * User-defined title of the filter.
     */
    val title: String,

    /**
     * JSON array of contexts where the filter applies.
     * Example: ["home", "notifications"]
     */
    val contextJson: String,

    /**
     * ISO 8601 timestamp when the filter expires, or null if permanent.
     */
    val expiresAt: String?,

    /**
     * Action to take: "warn" or "hide"
     */
    val filterAction: String,

    /**
     * JSON array of keywords.
     * Example: [{"id":"1","keyword":"test","wholeWord":true}]
     */
    val keywordsJson: String,

    /**
     * Account ID that owns this filter (for multi-account support).
     */
    val ownerAccountId: String
)
