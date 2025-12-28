package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for Mastodon List from API.
 * Represents a user-created list of accounts.
 */
@Serializable
data class ListDto(
    /**
     * The ID of the list.
     */
    @SerialName("id")
    val id: String,

    /**
     * The user-defined title of the list.
     */
    @SerialName("title")
    val title: String,

    /**
     * Which replies should be shown in the list timeline.
     * - "followed": Show replies to any followed user
     * - "list": Show replies to members of the list
     * - "none": Show no replies
     */
    @SerialName("replies_policy")
    val repliesPolicy: String = "list"
)
