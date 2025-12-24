package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Network DTO for Mastodon Search API v2 responses.
 * Contains search results for accounts, statuses, and hashtags.
 */
@Serializable
data class SearchResultsDto(
    @SerialName("accounts") val accounts: List<AccountDto> = emptyList(),
    @SerialName("statuses") val statuses: List<StatusDto> = emptyList(),
    @SerialName("hashtags") val hashtags: List<HashtagDto> = emptyList()
)

/**
 * Network DTO for Mastodon Hashtag (Tag) API responses.
 */
@Serializable
data class HashtagDto(
    @SerialName("name") val name: String,
    @SerialName("url") val url: String,
    @SerialName("history") val history: List<HashtagHistoryDto>? = null
)

/**
 * Network DTO for hashtag usage history.
 */
@Serializable
data class HashtagHistoryDto(
    @SerialName("day") val day: String,
    @SerialName("uses") val uses: String,
    @SerialName("accounts") val accounts: String
)
