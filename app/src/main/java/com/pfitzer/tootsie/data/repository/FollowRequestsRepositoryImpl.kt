package com.pfitzer.tootsie.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.paging.FollowRequestsPagingSource
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.FollowRequestsRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FollowRequestsRepository.
 * Manages follow request operations with the Mastodon API.
 */
@Singleton
class FollowRequestsRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : FollowRequestsRepository {

    override fun getFollowRequests(): Flow<PagingData<Account>> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                enablePlaceholders = false,
                initialLoadSize = 40
            ),
            pagingSourceFactory = {
                FollowRequestsPagingSource(mastodonApi)
            }
        ).flow
    }

    override suspend fun authorizeFollowRequest(accountId: String): Result<Relationship> {
        return try {
            val relationshipDto = mastodonApi.authorizeFollowRequest(accountId)
            Result.Success(relationshipDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to authorize follow request")
        }
    }

    override suspend fun rejectFollowRequest(accountId: String): Result<Relationship> {
        return try {
            val relationshipDto = mastodonApi.rejectFollowRequest(accountId)
            Result.Success(relationshipDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to reject follow request")
        }
    }
}
