package com.pfitzer.tootsie.data.mapper

import com.pfitzer.tootsie.data.remote.dto.ScheduledStatusDto
import com.pfitzer.tootsie.domain.model.PollConfig
import com.pfitzer.tootsie.domain.model.ScheduledStatus
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant

fun ScheduledStatusDto.toDomain(): ScheduledStatus = ScheduledStatus(
    id = id,
    scheduledAt = Instant.parse(scheduledAt),
    text = params.text.orEmpty(),
    spoilerText = params.spoilerText?.takeIf { it.isNotBlank() },
    visibility = params.visibility ?: "public",
    sensitive = params.sensitive.asBoolean(),
    language = params.language?.takeIf { it.isNotBlank() },
    inReplyToId = params.inReplyToId.asString(),
    poll = params.poll?.takeIf { it.options.isNotEmpty() }?.let { poll ->
        PollConfig(
            options = poll.options,
            expiresIn = poll.expiresIn.asString()?.toIntOrNull() ?: DEFAULT_POLL_EXPIRES_IN,
            multiple = poll.multiple.asBoolean(),
            hideTotals = poll.hideTotals.asBoolean()
        )
    },
    mediaAttachments = mediaAttachments.map { it.toDomain() }
)

/** Value of a JSON string or number, null for a missing value, JSON null or "". */
private fun JsonPrimitive?.asString(): String? =
    this?.takeIf { it !is JsonNull }?.content?.takeIf { it.isNotEmpty() }

/** JSON boolean or a form value ("true", "1"). */
private fun JsonPrimitive?.asBoolean(): Boolean = asString() in setOf("true", "1")

private const val DEFAULT_POLL_EXPIRES_IN = 86400
