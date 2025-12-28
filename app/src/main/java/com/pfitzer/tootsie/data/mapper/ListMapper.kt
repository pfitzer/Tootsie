package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.remote.dto.ListDto
import com.pfitzer.tootsie.domain.model.MastodonList
import com.pfitzer.tootsie.domain.model.RepliesPolicy

/**
 * Converts ListDto to MastodonList domain model.
 */
fun ListDto.toDomain(): MastodonList {
    return MastodonList(
        id = id,
        title = title,
        repliesPolicy = RepliesPolicy.fromString(repliesPolicy)
    )
}
