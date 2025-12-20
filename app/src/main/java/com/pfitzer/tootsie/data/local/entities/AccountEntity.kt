package com.pfitzer.tootsie.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a Mastodon account stored in the local database.
 * Used for caching account information and supporting multi-account functionality.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    val acct: String,
    val displayName: String,
    val note: String,
    val url: String,
    val avatar: String,
    val header: String,
    val locked: Boolean,
    val followersCount: Int,
    val followingCount: Int,
    val statusesCount: Int,
    val createdAt: String,
    val bot: Boolean,

    // Authentication data (only for logged-in accounts)
    val accessToken: String? = null,
    val instanceUrl: String? = null,
    val isActive: Boolean = false, // Whether this is the currently active account

    // Timestamps
    val lastSyncedAt: Long = System.currentTimeMillis()
)
