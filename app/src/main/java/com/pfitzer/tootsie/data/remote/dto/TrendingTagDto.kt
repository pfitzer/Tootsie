package com.pfitzer.tootsie.data.remote.dto

import com.pfitzer.tootsie.domain.model.TrendingTag
import com.pfitzer.tootsie.domain.model.TagHistory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Trending tag DTO from Mastodon API.
 * Represents a hashtag that is being used more frequently than usual.
 */
@Serializable
data class TrendingTagDto(
    @SerialName("name") val name: String,
    @SerialName("url") val url: String,
    @SerialName("history") val history: List<TagHistoryDto>
)

/**
 * Tag history DTO - usage statistics for a tag.
 */
@Serializable
data class TagHistoryDto(
    @SerialName("day") val day: String,  // UNIX timestamp as string
    @SerialName("uses") val uses: String,  // Number of uses as string
    @SerialName("accounts") val accounts: String  // Number of accounts as string
)

/**
 * Extension function to convert TrendingTagDto to domain model.
 */
fun TrendingTagDto.toDomain(): TrendingTag {
    return TrendingTag(
        name = name,
        url = url,
        history = history.map { it.toDomain() }
    )
}

/**
 * Extension function to convert TagHistoryDto to domain model.
 */
fun TagHistoryDto.toDomain(): TagHistory {
    return TagHistory(
        day = day,
        uses = uses.toIntOrNull() ?: 0,
        accounts = accounts.toIntOrNull() ?: 0
    )
}
