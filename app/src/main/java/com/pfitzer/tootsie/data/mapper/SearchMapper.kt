package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.remote.dto.HashtagDto
import com.pfitzer.tootsie.data.remote.dto.HashtagHistoryDto
import com.pfitzer.tootsie.data.remote.dto.SearchResultsDto
import com.pfitzer.tootsie.domain.model.Hashtag
import com.pfitzer.tootsie.domain.model.HashtagHistory
import com.pfitzer.tootsie.domain.model.SearchResults

/**
 * Maps SearchResultsDto (network) to SearchResults (domain).
 */
fun SearchResultsDto.toDomain(): SearchResults {
    return SearchResults(
        accounts = accounts.map { it.toDomain() },
        statuses = statuses.map { it.toDomain() },
        hashtags = hashtags.map { it.toDomain() }
    )
}

/**
 * Maps HashtagDto (network) to Hashtag (domain).
 */
fun HashtagDto.toDomain(): Hashtag {
    return Hashtag(
        name = name,
        url = url,
        history = history?.map { it.toDomain() },
        following = following
    )
}

/**
 * Maps HashtagHistoryDto (network) to HashtagHistory (domain).
 */
fun HashtagHistoryDto.toDomain(): HashtagHistory {
    return HashtagHistory(
        day = day,
        uses = uses.toIntOrNull() ?: 0,
        accounts = accounts.toIntOrNull() ?: 0
    )
}
