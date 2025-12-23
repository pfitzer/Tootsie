package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.remote.dto.RelationshipDto
import com.pfitzer.tootsie.domain.model.Relationship

/**
 * Extension function to convert RelationshipDto to Relationship domain model.
 */
fun RelationshipDto.toDomain(): Relationship {
    return Relationship(
        id = id,
        following = following,
        followedBy = followedBy,
        blocking = blocking,
        blockedBy = blockedBy,
        muting = muting,
        mutingNotifications = mutingNotifications,
        requested = requested,
        domainBlocking = domainBlocking,
        showingReblogs = showingReblogs,
        endorsed = endorsed,
        notifying = notifying,
        note = note ?: ""
    )
}
