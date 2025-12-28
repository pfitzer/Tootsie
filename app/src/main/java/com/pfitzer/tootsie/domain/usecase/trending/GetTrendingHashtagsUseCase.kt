package com.pfitzer.tootsie.domain.usecase.trending

import com.pfitzer.tootsie.domain.model.TrendingTag
import com.pfitzer.tootsie.domain.repository.TrendingRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for getting trending hashtags.
 */
class GetTrendingHashtagsUseCase @Inject constructor(
    private val trendingRepository: TrendingRepository
) {
    suspend operator fun invoke(
        limit: Int = 10,
        offset: Int = 0
    ): Result<List<TrendingTag>> {
        return trendingRepository.getTrendingHashtags(limit, offset)
    }
}
