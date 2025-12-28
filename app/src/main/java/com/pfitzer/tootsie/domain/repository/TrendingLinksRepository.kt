package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.TrendingLink
import com.pfitzer.tootsie.util.Result

/**
 * Repository interface for trending links/articles operations.
 */
interface TrendingLinksRepository {
    /**
     * Get trending links from the API.
     *
     * @param limit Maximum number of results to return
     * @param offset Skip the first n results (for pagination)
     * @return Result containing list of trending links
     */
    suspend fun getTrendingLinks(
        limit: Int = 10,
        offset: Int = 0
    ): Result<List<TrendingLink>>
}
