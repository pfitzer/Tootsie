package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.util.Result

/**
 * Repository interface for trending posts.
 */
interface TrendingPostsRepository {

    /**
     * Get trending posts/statuses.
     * Returns statuses that have been interacted with more than others.
     *
     * @param limit Maximum number of results (default 20, max 40)
     * @param offset Skip the first n results (for pagination)
     */
    suspend fun getTrendingPosts(
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<Status>>
}
