package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.toDomain
import com.pfitzer.tootsie.domain.model.TrendingTag
import com.pfitzer.tootsie.domain.repository.TrendingRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TrendingRepository.
 * Fetches trending content from the Mastodon API.
 */
@Singleton
class TrendingRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : TrendingRepository {

    override suspend fun getTrendingHashtags(
        limit: Int,
        offset: Int
    ): Result<List<TrendingTag>> {
        return try {
            val trendingTagsDto = mastodonApi.getTrendingTags(limit, offset)
            val trendingTags = trendingTagsDto.map { it.toDomain() }
            Result.Success(trendingTags)
        } catch (e: Exception) {
            Result.Error(e.message)
        }
    }
}
