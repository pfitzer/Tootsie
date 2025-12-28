package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.TrendingPostsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TrendingPostsRepository.
 * Fetches trending posts from the Mastodon API.
 */
@Singleton
class TrendingPostsRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : TrendingPostsRepository {

    override suspend fun getTrendingPosts(
        limit: Int,
        offset: Int
    ): Result<List<Status>> {
        return try {
            val trendingStatusesDto = mastodonApi.getTrendingStatuses(limit, offset)
            val trendingStatuses = trendingStatusesDto.map { it.toDomain() }
            Result.Success(trendingStatuses)
        } catch (e: Exception) {
            Result.Error(e.message)
        }
    }
}
