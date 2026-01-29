package com.pfitzer.tootsie.domain.repository

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing follow requests.
 */
interface FollowRequestsRepository {
    /**
     * Get a paginated flow of pending follow requests.
     */
    fun getFollowRequests(): Flow<PagingData<Account>>

    /**
     * Authorize a follow request from an account.
     *
     * @param accountId The ID of the account to authorize
     * @return Result containing the updated Relationship
     */
    suspend fun authorizeFollowRequest(accountId: String): Result<Relationship>

    /**
     * Reject a follow request from an account.
     *
     * @param accountId The ID of the account to reject
     * @return Result containing the updated Relationship
     */
    suspend fun rejectFollowRequest(accountId: String): Result<Relationship>
}
