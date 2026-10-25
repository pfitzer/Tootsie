package com.pfitzer.tootsie.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pfitzer.tootsie.domain.model.PollConfig
import kotlinx.serialization.Serializable

/**
 * Room entity representing a draft post stored in the local database.
 * Drafts are auto-saved while composing and can be resumed later.
 */
@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: String = "", // ID of the account that owns this draft (assigned by repository)
    val content: String,
    val mediaIds: String? = null, // JSON array of uploaded media attachment IDs
    val visibility: String = "public", // public, unlisted, private, direct
    val sensitive: Boolean = false,
    val spoilerText: String? = null, // Content warning
    val inReplyToId: String? = null, // ID of status being replied to
    val createdAt: Long, // Timestamp when draft was created
    val updatedAt: Long, // Timestamp when draft was last updated
    val pollJson: String? = null, // DraftPoll as JSON
    val language: String? = null // ISO 639-1 language of the post
)

/**
 * Poll of a draft, stored as JSON in [DraftEntity.pollJson].
 */
@Serializable
data class DraftPoll(
    val options: List<String>,
    val expiresIn: Int,
    val multiple: Boolean = false,
    val hideTotals: Boolean = false
) {
    fun toPollConfig() = PollConfig(options = options, expiresIn = expiresIn, multiple = multiple, hideTotals = hideTotals)

    companion object {
        fun from(poll: PollConfig) =
            DraftPoll(options = poll.options, expiresIn = poll.expiresIn, multiple = poll.multiple, hideTotals = poll.hideTotals)
    }
}
