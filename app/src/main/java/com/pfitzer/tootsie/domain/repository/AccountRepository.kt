package com.pfitzer.tootsie.domain.repository

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for account operations.
 * Defines the contract for interacting with user accounts.
 */
interface AccountRepository {

    /**
     * Get account information by ID.
     *
     * @param accountId Account ID
     * @return Account details
     */
    suspend fun getAccount(accountId: String): Result<Account>

    /**
     * Get the currently authenticated user's account.
     *
     * @return Current user's account details
     */
    suspend fun getCurrentAccount(): Result<Account>

    /**
     * Get relationship status with another account.
     *
     * @param accountId Account ID
     * @return Relationship status
     */
    suspend fun getRelationship(accountId: String): Result<Relationship>

    /**
     * Get relationships with multiple accounts.
     *
     * @param accountIds List of account IDs
     * @return List of relationship statuses
     */
    suspend fun getRelationships(accountIds: List<String>): Result<List<Relationship>>

    /**
     * Follow an account.
     *
     * @param accountId Account ID to follow
     * @param notify Whether to enable notifications for this account
     * @param reblogs Whether to show boosts from this account
     * @return Updated relationship status
     */
    suspend fun followAccount(
        accountId: String,
        notify: Boolean = false,
        reblogs: Boolean = true
    ): Result<Relationship>

    /**
     * Unfollow an account.
     *
     * @param accountId Account ID to unfollow
     * @return Updated relationship status
     */
    suspend fun unfollowAccount(accountId: String): Result<Relationship>

    /**
     * Mute an account.
     * Hides the account's posts from timelines and notifications.
     *
     * @param accountId Account ID to mute
     * @param notifications Whether to mute notifications (default true)
     * @param duration Duration of mute in seconds (0 = indefinite)
     * @return Updated relationship status
     */
    suspend fun muteAccount(
        accountId: String,
        notifications: Boolean = true,
        duration: Int = 0
    ): Result<Relationship>

    /**
     * Unmute an account.
     *
     * @param accountId Account ID to unmute
     * @return Updated relationship status
     */
    suspend fun unmuteAccount(accountId: String): Result<Relationship>

    /**
     * Block an account.
     * Prevents the account from following you and viewing your content.
     *
     * @param accountId Account ID to block
     * @return Updated relationship status
     */
    suspend fun blockAccount(accountId: String): Result<Relationship>

    /**
     * Unblock an account.
     *
     * @param accountId Account ID to unblock
     * @return Updated relationship status
     */
    suspend fun unblockAccount(accountId: String): Result<Relationship>

    /**
     * Get muted accounts.
     *
     * @return Flow of paged muted accounts
     */
    fun getMutedAccounts(): Flow<PagingData<Account>>

    /**
     * Get blocked accounts.
     *
     * @return Flow of paged blocked accounts
     */
    fun getBlockedAccounts(): Flow<PagingData<Account>>

    /**
     * Get statuses posted by an account (user timeline).
     * Returns a Flow of PagingData for infinite scrolling.
     *
     * @param accountId Account ID
     * @param excludeReplies Whether to exclude replies (default: false)
     * @param excludeReblogs Whether to exclude boosts (default: false)
     * @param onlyMedia Whether to only show posts with media (default: false)
     * @param pinned Whether to only show pinned posts (default: false)
     * @return Flow of paged statuses
     */
    fun getAccountStatuses(
        accountId: String,
        excludeReplies: Boolean = false,
        excludeReblogs: Boolean = false,
        onlyMedia: Boolean = false,
        pinned: Boolean = false
    ): Flow<PagingData<Status>>

    /**
     * Get accounts following this account (followers).
     *
     * @param accountId Account ID
     * @return Flow of paged accounts
     */
    fun getFollowers(accountId: String): Flow<PagingData<Account>>

    /**
     * Get accounts this account is following (following).
     *
     * @param accountId Account ID
     * @return Flow of paged accounts
     */
    fun getFollowing(accountId: String): Flow<PagingData<Account>>

    /**
     * Update account profile.
     *
     * @param displayName New display name
     * @param note New bio/note
     * @return Updated account details
     */
    suspend fun updateProfile(
        displayName: String? = null,
        note: String? = null
    ): Result<Account>
}
