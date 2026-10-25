package com.pfitzer.tootsie.domain.model

/**
 * Domain model representing a Mastodon account.
 */
data class Account(
    val id: String,
    val username: String,
    val acct: String, // username@domain for remote accounts, username for local
    val displayName: String,
    val note: String, // Bio/description (may contain HTML)
    val url: String, // Profile URL
    val avatar: String, // Avatar image URL
    val header: String, // Header/banner image URL
    val locked: Boolean, // Whether account is locked (requires approval to follow)
    val followersCount: Int,
    val followingCount: Int,
    val statusesCount: Int,
    val createdAt: String,
    val bot: Boolean, // Whether this is a bot account
    val emojis: List<CustomEmoji> = emptyList(),
    val fields: List<Field> = emptyList() // Profile metadata fields
)

/**
 * Custom emoji used in account profiles and statuses.
 */
data class CustomEmoji(
    val shortcode: String, // e.g., "blobcat"
    val url: String, // Image URL
    val staticUrl: String, // Non-animated version URL
    val visibleInPicker: Boolean = true
)

/**
 * Server of a remote account (the part after the @ of [Account.acct]), null for accounts
 * of the own server.
 */
val Account.remoteDomain: String?
    get() = acct.substringAfter('@', "").takeIf { it.isNotEmpty() }

/**
 * Profile metadata field (key-value pair).
 */
data class Field(
    val name: String,
    val value: String, // May contain HTML
    val verifiedAt: String? = null // ISO 8601 timestamp if verified
)
