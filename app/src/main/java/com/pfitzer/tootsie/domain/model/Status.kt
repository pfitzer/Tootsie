package com.pfitzer.tootsie.domain.model

/**
 * Domain model representing a Mastodon status (post/toot).
 */
data class Status(
    val id: String,
    val uri: String, // Permanent URI
    val url: String?, // Public URL (may be null for direct messages)
    val account: Account,
    val content: String, // HTML content
    val createdAt: String, // ISO 8601 timestamp
    val editedAt: String? = null, // ISO 8601 timestamp if edited
    val visibility: StatusVisibility,
    val sensitive: Boolean, // Whether media is marked as sensitive
    val spoilerText: String, // Content warning text
    val mediaAttachments: List<MediaAttachment> = emptyList(),
    val mentions: List<Mention> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val emojis: List<CustomEmoji> = emptyList(),
    val reblogsCount: Int,
    val favouritesCount: Int,
    val repliesCount: Int,
    val poll: Poll? = null,
    val reblog: Status? = null, // If this is a boost, the original status
    val inReplyToId: String? = null,
    val inReplyToAccountId: String? = null,
    val reblogged: Boolean = false, // Whether current user has boosted this
    val favourited: Boolean = false, // Whether current user has favorited this
    val bookmarked: Boolean = false, // Whether current user has bookmarked this
    val pinned: Boolean = false, // Whether this status is pinned
    val card: Card? = null, // Preview card for links
    val language: String? = null // ISO 639 language code
)

/**
 * Status visibility levels.
 */
enum class StatusVisibility {
    PUBLIC, // Visible to everyone, shown in public timelines
    UNLISTED, // Visible to everyone, but not in public timelines
    PRIVATE, // Visible to followers only
    DIRECT // Visible to mentioned users only
}

/**
 * Media attachment (image, video, audio, or unknown).
 */
data class MediaAttachment(
    val id: String,
    val type: MediaType,
    val url: String, // Full-size URL
    val previewUrl: String?, // Thumbnail URL (nullable - may not exist for some media)
    val description: String? = null, // Alt text
    val blurhash: String? = null, // Blurhash for placeholder
    val meta: MediaMeta? = null // Metadata (dimensions, duration, etc.)
)

/**
 * Media attachment type.
 */
enum class MediaType {
    IMAGE,
    VIDEO,
    GIFV, // Looping video without sound
    AUDIO,
    UNKNOWN
}

/**
 * Media metadata.
 */
data class MediaMeta(
    val width: Int? = null,
    val height: Int? = null,
    val duration: Float? = null, // In seconds
    val aspect: Float? = null
)

/**
 * Mention of another account in a status.
 */
data class Mention(
    val id: String, // Account ID
    val username: String,
    val acct: String, // username@domain
    val url: String // Account URL
)

/**
 * Hashtag.
 */
data class Tag(
    val name: String, // Without the # prefix
    val url: String // Tag URL
)

/**
 * Poll attached to a status.
 */
data class Poll(
    val id: String,
    val expiresAt: String?, // ISO 8601 timestamp
    val expired: Boolean,
    val multiple: Boolean, // Whether multiple choices are allowed
    val votesCount: Int,
    val votersCount: Int?, // Null if multiple choices allowed
    val voted: Boolean, // Whether current user has voted
    val ownVotes: List<Int> = emptyList(), // Indices of user's votes
    val options: List<PollOption>
)

/**
 * Configuration for a poll being created.
 */
data class PollConfig(
    val options: List<String>,
    val expiresIn: Int, // In seconds
    val multiple: Boolean = false,
    val hideTotals: Boolean = false
)

/**
 * Poll option.
 */
data class PollOption(
    val title: String,
    val votesCount: Int? = null
)

/**
 * Preview card for links in statuses.
 */
data class Card(
    val url: String,
    val title: String,
    val description: String,
    val type: CardType,
    val authorName: String? = null,
    val authorUrl: String? = null,
    val providerName: String? = null,
    val providerUrl: String? = null,
    val html: String? = null, // For embedded content
    val width: Int? = null,
    val height: Int? = null,
    val image: String? = null, // Preview image URL
    val embedUrl: String? = null
)

/**
 * Preview card type.
 */
enum class CardType {
    LINK,
    PHOTO,
    VIDEO,
    RICH // Embedded content
}
