package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Reading position of a timeline, from /api/v1/markers.
 */
@Serializable
data class MarkerDto(
    @SerialName("last_read_id") val lastReadId: String,
    @SerialName("version") val version: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null
)
