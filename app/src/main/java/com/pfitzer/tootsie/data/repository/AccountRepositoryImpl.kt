package com.pfitzer.tootsie.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.paging.AccountStatusesPagingSource
import com.pfitzer.tootsie.data.paging.BlockedAccountsPagingSource
import com.pfitzer.tootsie.data.paging.MutedAccountsPagingSource
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AccountRepository.
 * Handles account operations via Mastodon API.
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : AccountRepository {

    override suspend fun getAccount(accountId: String): Result<Account> {
        return try {
            val accountDto = mastodonApi.getAccount(accountId)
            Result.Success(accountDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get account")
        }
    }

    override suspend fun getCurrentAccount(): Result<Account> {
        return try {
            val accountDto = mastodonApi.verifyCredentials()
            Result.Success(accountDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get current account")
        }
    }

    override suspend fun getRelationship(accountId: String): Result<Relationship> {
        return try {
            val relationships = mastodonApi.getRelationships(listOf(accountId))
            if (relationships.isEmpty()) {
                Result.Error("No relationship found")
            } else {
                Result.Success(relationships.first().toDomain())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get relationship")
        }
    }

    override suspend fun getRelationships(accountIds: List<String>): Result<List<Relationship>> {
        return try {
            val relationships = mastodonApi.getRelationships(accountIds)
            Result.Success(relationships.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get relationships")
        }
    }

    override suspend fun followAccount(
        accountId: String,
        notify: Boolean,
        reblogs: Boolean
    ): Result<Relationship> {
        return try {
            val relationship = mastodonApi.followAccount(
                id = accountId,
                notify = notify,
                reblogs = reblogs
            )
            Result.Success(relationship.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to follow account")
        }
    }

    override suspend fun unfollowAccount(accountId: String): Result<Relationship> {
        return try {
            val relationship = mastodonApi.unfollowAccount(accountId)
            Result.Success(relationship.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unfollow account")
        }
    }

    override suspend fun muteAccount(
        accountId: String,
        notifications: Boolean,
        duration: Int
    ): Result<Relationship> {
        return try {
            val relationship = mastodonApi.muteAccount(
                id = accountId,
                notifications = notifications,
                duration = duration
            )
            Result.Success(relationship.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to mute account")
        }
    }

    override suspend fun unmuteAccount(accountId: String): Result<Relationship> {
        return try {
            val relationship = mastodonApi.unmuteAccount(accountId)
            Result.Success(relationship.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unmute account")
        }
    }

    override suspend fun blockAccount(accountId: String): Result<Relationship> {
        return try {
            val relationship = mastodonApi.blockAccount(accountId)
            Result.Success(relationship.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to block account")
        }
    }

    override suspend fun unblockAccount(accountId: String): Result<Relationship> {
        return try {
            val relationship = mastodonApi.unblockAccount(accountId)
            Result.Success(relationship.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unblock account")
        }
    }

    override fun getMutedAccounts(): Flow<PagingData<Account>> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                enablePlaceholders = false,
                initialLoadSize = 40
            ),
            pagingSourceFactory = {
                MutedAccountsPagingSource(mastodonApi)
            }
        ).flow
    }

    override fun getBlockedAccounts(): Flow<PagingData<Account>> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                enablePlaceholders = false,
                initialLoadSize = 40
            ),
            pagingSourceFactory = {
                BlockedAccountsPagingSource(mastodonApi)
            }
        ).flow
    }

    override fun getAccountStatuses(
        accountId: String,
        excludeReplies: Boolean,
        excludeReblogs: Boolean,
        onlyMedia: Boolean,
        pinned: Boolean
    ): Flow<PagingData<Status>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                AccountStatusesPagingSource(
                    mastodonApi = mastodonApi,
                    accountId = accountId,
                    excludeReplies = excludeReplies,
                    excludeReblogs = excludeReblogs,
                    onlyMedia = onlyMedia,
                    pinned = pinned
                )
            }
        ).flow
    }

    override fun getFollowers(accountId: String): Flow<PagingData<Account>> {
        // TODO: Implement followers paging source (Phase 2c or later)
        throw NotImplementedError("Followers list not yet implemented")
    }

    override fun getFollowing(accountId: String): Flow<PagingData<Account>> {
        // TODO: Implement following paging source (Phase 2c or later)
        throw NotImplementedError("Following list not yet implemented")
    }

    override suspend fun updateProfile(displayName: String?, note: String?): Result<Account> {
        return try {
            val accountDto = mastodonApi.updateCredentials(
                displayName = displayName,
                note = note
            )
            Result.Success(accountDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update profile")
        }
    }
}
