package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of GET /api/v1_alpha/async_refreshes/:id
 */
@Serializable
data class AsyncRefreshResponseDto(
    @SerialName("async_refresh") val asyncRefresh: AsyncRefreshDto
)

/**
 * AsyncRefresh entity (experimental, Mastodon >= 4.4).
 */
@Serializable
data class AsyncRefreshDto(
    val id: String,
    /** "running" or "finished" */
    val status: String,
    @SerialName("result_count") val resultCount: Int? = null
)
