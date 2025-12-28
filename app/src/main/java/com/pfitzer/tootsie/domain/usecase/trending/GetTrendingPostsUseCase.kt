package com.pfitzer.tootsie.domain.usecase.trending

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.TrendingPostsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for getting trending posts/statuses.
 */
class GetTrendingPostsUseCase @Inject constructor(
    private val trendingPostsRepository: TrendingPostsRepository
) {
    suspend operator fun invoke(
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<Status>> {
        return trendingPostsRepository.getTrendingPosts(limit, offset)
    }
}
