package com.pfitzer.tootsie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for relationship data from Mastodon API.
 * Represents the relationship between the current user and another account.
 */
@Serializable
data class RelationshipDto(
    @SerialName("id")
    val id: String,

    @SerialName("following")
    val following: Boolean,

    @SerialName("followed_by")
    val followedBy: Boolean,

    @SerialName("blocking")
    val blocking: Boolean,

    @SerialName("blocked_by")
    val blockedBy: Boolean,

    @SerialName("muting")
    val muting: Boolean,

    @SerialName("muting_notifications")
    val mutingNotifications: Boolean,

    @SerialName("requested")
    val requested: Boolean,

    @SerialName("domain_blocking")
    val domainBlocking: Boolean,

    @SerialName("showing_reblogs")
    val showingReblogs: Boolean,

    @SerialName("endorsed")
    val endorsed: Boolean,

    @SerialName("notifying")
    val notifying: Boolean,

    @SerialName("note")
    val note: String? = null
)
