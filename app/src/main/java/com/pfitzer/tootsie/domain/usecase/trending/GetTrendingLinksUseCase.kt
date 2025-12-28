package com.pfitzer.tootsie.domain.usecase.trending

import com.pfitzer.tootsie.domain.model.TrendingLink
import com.pfitzer.tootsie.domain.repository.TrendingLinksRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for getting trending links/articles.
 */
class GetTrendingLinksUseCase @Inject constructor(
    private val trendingLinksRepository: TrendingLinksRepository
) {
    suspend operator fun invoke(
        limit: Int = 10,
        offset: Int = 0
    ): Result<List<TrendingLink>> {
        return trendingLinksRepository.getTrendingLinks(limit, offset)
    }
}
