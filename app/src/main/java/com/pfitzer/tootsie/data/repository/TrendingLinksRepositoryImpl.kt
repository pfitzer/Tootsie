package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.TrendingLinkDto
import com.pfitzer.tootsie.data.remote.dto.toDomain
import com.pfitzer.tootsie.domain.model.TrendingLink
import com.pfitzer.tootsie.domain.repository.TrendingLinksRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TrendingLinksRepository.
 * Fetches trending links from the Mastodon API.
 */
@Singleton
class TrendingLinksRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : TrendingLinksRepository {

    override suspend fun getTrendingLinks(
        limit: Int,
        offset: Int
    ): Result<List<TrendingLink>> {
        return try {
            val trendingLinksDto: List<TrendingLinkDto> = mastodonApi.getTrendingLinks(limit, offset)
            val trendingLinks: List<TrendingLink> = trendingLinksDto.map { (it as TrendingLinkDto).toDomain() }
            Result.Success(trendingLinks)
        } catch (e: Exception) {
            Result.Error(e.message)
        }
    }
}
