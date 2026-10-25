package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * Scheduled status from /api/v1/scheduled_statuses.
 */
@Serializable
data class ScheduledStatusDto(
    @SerialName("id") val id: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("params") val params: ScheduledStatusParamsDto,
    @SerialName("media_attachments") val mediaAttachments: List<MediaAttachmentDto> = emptyList()
)

/**
 * Parameters the status is created with. Mastodon stores them as they were sent, so for
 * form requests booleans and numbers arrive as strings; they are read as primitives.
 */
@Serializable
data class ScheduledStatusParamsDto(
    @SerialName("text") val text: String? = null,
    @SerialName("spoiler_text") val spoilerText: String? = null,
    @SerialName("visibility") val visibility: String? = null,
    @SerialName("sensitive") val sensitive: JsonPrimitive? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("in_reply_to_id") val inReplyToId: JsonPrimitive? = null,
    @SerialName("poll") val poll: ScheduledPollParamsDto? = null
)

@Serializable
data class ScheduledPollParamsDto(
    @SerialName("options") val options: List<String> = emptyList(),
    @SerialName("expires_in") val expiresIn: JsonPrimitive? = null,
    @SerialName("multiple") val multiple: JsonPrimitive? = null,
    @SerialName("hide_totals") val hideTotals: JsonPrimitive? = null
)
