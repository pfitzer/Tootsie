package com.pfitzer.tootsie.domain.model

/**
 * Domain model for a server announcement.
 * Represents an announcement from instance administrators.
 */
data class Announcement(
    val id: String,
    val content: String,
    val startsAt: String?,
    val endsAt: String?,
    val allDay: Boolean,
    val publishedAt: String,
    val updatedAt: String?,
    val read: Boolean,
    val mentions: List<Mention>,
    val tags: List<Tag>,
    val reactions: List<AnnouncementReaction>
)

/**
 * Reaction to an announcement (emoji reactions).
 */
data class AnnouncementReaction(
    val name: String,
    val count: Int,
    val me: Boolean,
    val url: String?,
    val staticUrl: String?
)
