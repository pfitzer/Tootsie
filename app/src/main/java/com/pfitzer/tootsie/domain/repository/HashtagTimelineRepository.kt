package com.pfitzer.tootsie.domain.repository

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Status
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for hashtag timeline operations.
 */
interface HashtagTimelineRepository {
    /**
     * Get a paginated flow of statuses for a specific hashtag.
     *
     * @param hashtag The hashtag name (without the # symbol)
     * @return Flow of paging data containing statuses
     */
    fun getHashtagTimeline(hashtag: String): Flow<PagingData<Status>>
}
