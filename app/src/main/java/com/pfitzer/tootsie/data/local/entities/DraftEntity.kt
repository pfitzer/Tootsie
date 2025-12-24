package com.pfitzer.tootsie.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a draft post stored in the local database.
 * Drafts are auto-saved while composing and can be resumed later.
 */
@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val mediaIds: String? = null, // JSON array of uploaded media attachment IDs
    val visibility: String = "public", // public, unlisted, private, direct
    val sensitive: Boolean = false,
    val spoilerText: String? = null, // Content warning
    val inReplyToId: String? = null, // ID of status being replied to
    val createdAt: Long, // Timestamp when draft was created
    val updatedAt: Long // Timestamp when draft was last updated
)
