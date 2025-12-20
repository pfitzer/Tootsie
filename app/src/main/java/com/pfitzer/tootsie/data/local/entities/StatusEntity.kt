package com.pfitzer.tootsie.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a Mastodon status (post) stored in the local database.
 * Simplified version for caching - some nested objects are stored as JSON strings.
 */
@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey
    val id: String,
    val accountId: String, // Foreign key to AccountEntity
    val uri: String,
    val url: String?,
    val content: String,
    val createdAt: String,
    val editedAt: String?,
    val visibility: String,
    val sensitive: Boolean,
    val spoilerText: String,
    val reblogsCount: Int,
    val favouritesCount: Int,
    val repliesCount: Int,
    val inReplyToId: String?,
    val inReplyToAccountId: String?,
    val reblogged: Boolean,
    val favourited: Boolean,
    val bookmarked: Boolean,
    val pinned: Boolean,
    val language: String?,

    // Simplified nested data (stored as JSON strings)
    val accountJson: String, // Serialized Account data
    val mediaAttachmentsJson: String?, // Serialized list of media
    val mentionsJson: String?, // Serialized list of mentions
    val tagsJson: String?, // Serialized list of tags
    val emojisJson: String?, // Serialized list of emojis
    val pollJson: String?, // Serialized poll data
    val reblogJson: String?, // Serialized reblog status
    val cardJson: String?, // Serialized preview card

    // Timeline association
    val timelineType: String? = null, // "home", "local", "federated", etc.

    // Cache metadata
    val lastSyncedAt: Long = System.currentTimeMillis()
)
