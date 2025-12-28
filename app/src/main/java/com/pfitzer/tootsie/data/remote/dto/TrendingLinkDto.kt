package com.pfitzer.tootsie.data.remote.dto

import com.pfitzer.tootsie.domain.model.TrendingLink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data transfer object for trending links from Mastodon API.
 * Represents links that have been shared more than others.
 */
@Serializable
data class TrendingLinkDto(
    @SerialName("url") val url: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("type") val type: String,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("author_url") val authorUrl: String? = null,
    @SerialName("provider_name") val providerName: String? = null,
    @SerialName("provider_url") val providerUrl: String? = null,
    @SerialName("html") val html: String? = null,
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("embed_url") val embedUrl: String? = null,
    @SerialName("blurhash") val blurhash: String? = null,
    @SerialName("history") val history: List<TrendingLinkHistoryDto>
)

/**
 * Historical usage data for a trending link.
 */
@Serializable
data class TrendingLinkHistoryDto(
    @SerialName("day") val day: String,
    @SerialName("accounts") val accounts: String,
    @SerialName("uses") val uses: String
)

/**
 * Maps TrendingLinkDto to domain model TrendingLink.
 */
fun TrendingLinkDto.toDomain(): TrendingLink {
    return TrendingLink(
        url = url,
        title = title,
        description = description,
        type = type,
        authorName = authorName,
        authorUrl = authorUrl,
        providerName = providerName,
        providerUrl = providerUrl,
        html = html,
        width = width,
        height = height,
        image = image,
        embedUrl = embedUrl,
        blurhash = blurhash,
        history = history.map { it.toDomain() }
    )
}

/**
 * Maps TrendingLinkHistoryDto to domain model.
 */
fun TrendingLinkHistoryDto.toDomain(): TrendingLink.LinkHistory {
    return TrendingLink.LinkHistory(
        day = day,
        accounts = accounts.toIntOrNull() ?: 0,
        uses = uses.toIntOrNull() ?: 0
    )
}
