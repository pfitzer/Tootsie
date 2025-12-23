package com.pfitzer.tootsie.domain.model

/**
 * Domain model representing the relationship between the current user and another account.
 * Contains information about follow status, blocking, muting, etc.
 */
data class Relationship(
    val id: String, // Target account ID
    val following: Boolean, // Whether the current user follows this account
    val followedBy: Boolean, // Whether this account follows the current user
    val blocking: Boolean, // Whether the current user blocks this account
    val blockedBy: Boolean, // Whether this account blocks the current user
    val muting: Boolean, // Whether the current user mutes this account
    val mutingNotifications: Boolean, // Whether the current user mutes notifications from this account
    val requested: Boolean, // Whether the current user has requested to follow this account
    val domainBlocking: Boolean, // Whether the current user blocks the entire domain
    val showingReblogs: Boolean, // Whether the current user sees boosts from this account
    val endorsed: Boolean, // Whether the current user has endorsed this account
    val notifying: Boolean, // Whether the current user has enabled notifications for this account
    val note: String // Private note about this account (visible only to current user)
)
