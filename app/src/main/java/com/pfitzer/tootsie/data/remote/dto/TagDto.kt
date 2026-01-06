package com.pfitzer.tootsie.data.remote.dto

import com.pfitzer.tootsie.domain.model.FollowedTag
import com.pfitzer.tootsie.domain.model.TagHistory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data transfer object for FollowedTag from Mastodon API.
 * Represents a hashtag that can be followed.
 */
@Serializable
data class FollowedTagDto(
    @SerialName("name") val name: String,
    @SerialName("url") val url: String,
    @SerialName("history") val history: List<FollowedTagHistoryDto>,
    @SerialName("following") val following: Boolean? = null
)

/**
 * Historical usage data for a followed tag.
 */
@Serializable
data class FollowedTagHistoryDto(
    @SerialName("day") val day: String,
    @SerialName("accounts") val accounts: String,
    @SerialName("uses") val uses: String
)

/**
 * Maps FollowedTagDto to domain model FollowedTag.
 */
fun FollowedTagDto.toDomain(): FollowedTag {
    return FollowedTag(
        name = name,
        url = url,
        history = history.map { tagHistory ->
            TagHistory(
                day = tagHistory.day,
                accounts = tagHistory.accounts.toIntOrNull() ?: 0,
                uses = tagHistory.uses.toIntOrNull() ?: 0
            )
        },
        following = following ?: false
    )
}
