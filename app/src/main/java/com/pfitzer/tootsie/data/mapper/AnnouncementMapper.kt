package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.remote.dto.AnnouncementDto
import com.pfitzer.tootsie.data.remote.dto.AnnouncementReactionDto
import com.pfitzer.tootsie.data.remote.dto.AnnouncementTagDto
import com.pfitzer.tootsie.data.remote.dto.MentionDto
import com.pfitzer.tootsie.domain.model.Announcement
import com.pfitzer.tootsie.domain.model.AnnouncementReaction
import com.pfitzer.tootsie.domain.model.Mention
import com.pfitzer.tootsie.domain.model.Tag

/**
 * Maps AnnouncementDto to domain model Announcement.
 */
fun AnnouncementDto.toDomain(): Announcement {
    return Announcement(
        id = id,
        content = content,
        startsAt = startsAt,
        endsAt = endsAt,
        allDay = allDay,
        publishedAt = publishedAt,
        updatedAt = updatedAt,
        read = read,
        mentions = mentions.map { it.toDomain() },
        tags = tags.map { it.toDomain() },
        reactions = reactions.map { it.toDomain() }
    )
}

/**
 * Maps MentionDto to domain model Mention.
 */
private fun MentionDto.toDomain(): Mention {
    return Mention(
        id = id,
        username = username,
        acct = acct,
        url = url
    )
}

/**
 * Maps AnnouncementTagDto to domain model Tag.
 */
private fun AnnouncementTagDto.toDomain(): Tag {
    return Tag(
        name = name,
        url = url
    )
}

/**
 * Maps AnnouncementReactionDto to domain model AnnouncementReaction.
 */
private fun AnnouncementReactionDto.toDomain(): AnnouncementReaction {
    return AnnouncementReaction(
        name = name,
        count = count,
        me = me,
        url = url,
        staticUrl = staticUrl
    )
}
