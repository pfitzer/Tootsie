package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Plain text source of a status, from /api/v1/statuses/:id/source.
 */
@Serializable
data class StatusSourceDto(
    @SerialName("id") val id: String,
    @SerialName("text") val text: String = "",
    @SerialName("spoiler_text") val spoilerText: String = ""
)

/**
 * One version of a status, from /api/v1/statuses/:id/history.
 */
@Serializable
data class StatusEditDto(
    @SerialName("content") val content: String = "",
    @SerialName("spoiler_text") val spoilerText: String = "",
    @SerialName("sensitive") val sensitive: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("account") val account: AccountDto,
    @SerialName("poll") val poll: StatusEditPollDto? = null,
    @SerialName("media_attachments") val mediaAttachments: List<MediaAttachmentDto> = emptyList(),
    @SerialName("emojis") val emojis: List<CustomEmojiDto> = emptyList()
)

@Serializable
data class StatusEditPollDto(
    @SerialName("options") val options: List<StatusEditPollOptionDto> = emptyList()
)

@Serializable
data class StatusEditPollOptionDto(
    @SerialName("title") val title: String
)
