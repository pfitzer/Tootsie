package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for notification data from Mastodon API.
 * Represents a notification about an interaction with the user's account.
 */
@Serializable
data class NotificationDto(
    @SerialName("id")
    val id: String,

    @SerialName("type")
    val type: String,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("account")
    val account: AccountDto,

    @SerialName("status")
    val status: StatusDto? = null
)
