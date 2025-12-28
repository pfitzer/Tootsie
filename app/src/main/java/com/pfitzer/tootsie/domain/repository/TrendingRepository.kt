package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.TrendingTag
import com.pfitzer.tootsie.util.Result

/**
 * Repository interface for trending content.
 */
interface TrendingRepository {

    /**
     * Get trending hashtags.
     * Returns tags that are being used more frequently than usual.
     *
     * @param limit Maximum number of results (default 10, max 20)
     * @param offset Skip the first n results (for pagination)
     */
    suspend fun getTrendingHashtags(
        limit: Int = 10,
        offset: Int = 0
    ): Result<List<TrendingTag>>
}
