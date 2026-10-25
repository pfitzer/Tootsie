package com.pfitzer.tootsie.domain.model

import java.time.Instant

/**
 * A status that the server publishes at [scheduledAt].
 */
data class ScheduledStatus(
    val id: String,
    val scheduledAt: Instant,
    val text: String,
    val spoilerText: String?,
    val visibility: String,
    val sensitive: Boolean,
    val language: String?,
    val inReplyToId: String?,
    val poll: PollConfig?,
    val mediaAttachments: List<MediaAttachment>
)

/**
 * Content of a status to publish (now or scheduled).
 */
data class NewStatus(
    val content: String,
    val visibility: String = "public",
    val inReplyToId: String? = null,
    val mediaIds: List<String>? = null,
    val sensitive: Boolean = false,
    val spoilerText: String? = null,
    val pollConfig: PollConfig? = null,
    val quotedStatusId: String? = null,
    val language: String? = null
)
