package com.pfitzer.tootsie.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pfitzer.tootsie.data.paging.BlockedDomainsPagingSource
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.repository.DomainBlockRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DomainBlockRepository.
 */
@Singleton
class DomainBlockRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : DomainBlockRepository {

    override fun getBlockedDomains(): Flow<PagingData<String>> {
        return Pager(
            config = PagingConfig(
                pageSize = 100,
                enablePlaceholders = false,
                initialLoadSize = 100
            ),
            pagingSourceFactory = {
                BlockedDomainsPagingSource(mastodonApi)
            }
        ).flow
    }

    override suspend fun blockDomain(domain: String): Result<Unit> {
        return try {
            mastodonApi.blockDomain(domain)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to block domain")
        }
    }

    override suspend fun unblockDomain(domain: String): Result<Unit> {
        return try {
            mastodonApi.unblockDomain(domain)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unblock domain")
        }
    }
}
