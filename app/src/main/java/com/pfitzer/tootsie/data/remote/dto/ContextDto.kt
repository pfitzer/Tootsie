package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for conversation context.
 * Represents the API response for fetching a status's conversation thread.
 */
@Serializable
data class ContextDto(
    /**
     * Posts before this status in the conversation.
     */
    val ancestors: List<StatusDto>,

    /**
     * Replies to this status.
     */
    val descendants: List<StatusDto>
)
