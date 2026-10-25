package com.pfitzer.tootsie.domain.repository

import androidx.paging.PagingData
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for blocked domains (instances).
 */
interface DomainBlockRepository {

    /**
     * Get blocked domains.
     *
     * @return Flow of paged domain names
     */
    fun getBlockedDomains(): Flow<PagingData<String>>

    /**
     * Block a domain: hides all its posts and notifications, and removes followers from it.
     *
     * @param domain Domain to block, e.g. "example.social"
     */
    suspend fun blockDomain(domain: String): Result<Unit>

    /**
     * Unblock a domain.
     *
     * @param domain Domain to unblock
     */
    suspend fun unblockDomain(domain: String): Result<Unit>
}
