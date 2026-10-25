package com.pfitzer.tootsie.domain.model

/**
 * Source of an own status for editing: the plain text as written, not the rendered HTML.
 */
data class StatusSource(
    val id: String,
    val text: String,
    val spoilerText: String
)

/**
 * One version of an edited status, from its edit history.
 */
data class StatusEditVersion(
    val content: String, // HTML content
    val spoilerText: String,
    val sensitive: Boolean,
    val createdAt: String, // ISO 8601 timestamp of this version
    val account: Account,
    val pollOptions: List<String> = emptyList(),
    val mediaAttachments: List<MediaAttachment> = emptyList(),
    val emojis: List<CustomEmoji> = emptyList()
)
