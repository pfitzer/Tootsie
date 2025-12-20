package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Network DTO for Mastodon Account API responses.
 * Maps to the JSON structure returned by the Mastodon API.
 */
@Serializable
data class AccountDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("acct") val acct: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("note") val note: String,
    @SerialName("url") val url: String,
    @SerialName("avatar") val avatar: String,
    @SerialName("header") val header: String,
    @SerialName("locked") val locked: Boolean,
    @SerialName("followers_count") val followersCount: Int,
    @SerialName("following_count") val followingCount: Int,
    @SerialName("statuses_count") val statusesCount: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("bot") val bot: Boolean = false,
    @SerialName("emojis") val emojis: List<CustomEmojiDto> = emptyList(),
    @SerialName("fields") val fields: List<FieldDto> = emptyList()
)

@Serializable
data class CustomEmojiDto(
    @SerialName("shortcode") val shortcode: String,
    @SerialName("url") val url: String,
    @SerialName("static_url") val staticUrl: String,
    @SerialName("visible_in_picker") val visibleInPicker: Boolean = true
)

@Serializable
data class FieldDto(
    @SerialName("name") val name: String,
    @SerialName("value") val value: String,
    @SerialName("verified_at") val verifiedAt: String? = null
)
