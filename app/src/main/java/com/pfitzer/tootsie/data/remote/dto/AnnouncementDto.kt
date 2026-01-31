package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data transfer object for Announcement from Mastodon API.
 * Represents an announcement from instance administrators.
 */
@Serializable
data class AnnouncementDto(
    @SerialName("id") val id: String,
    @SerialName("content") val content: String,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("all_day") val allDay: Boolean = false,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("read") val read: Boolean = false,
    @SerialName("mentions") val mentions: List<MentionDto> = emptyList(),
    @SerialName("tags") val tags: List<AnnouncementTagDto> = emptyList(),
    @SerialName("emojis") val emojis: List<CustomEmojiDto> = emptyList(),
    @SerialName("reactions") val reactions: List<AnnouncementReactionDto> = emptyList()
)

/**
 * Tag reference in an announcement.
 */
@Serializable
data class AnnouncementTagDto(
    @SerialName("name") val name: String,
    @SerialName("url") val url: String
)

/**
 * Reaction to an announcement (emoji reactions).
 */
@Serializable
data class AnnouncementReactionDto(
    @SerialName("name") val name: String,
    @SerialName("count") val count: Int,
    @SerialName("me") val me: Boolean = false,
    @SerialName("url") val url: String? = null,
    @SerialName("static_url") val staticUrl: String? = null
)
