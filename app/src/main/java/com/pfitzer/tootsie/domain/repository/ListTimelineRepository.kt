package com.pfitzer.tootsie.domain.repository

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Status
import kotlinx.coroutines.flow.Flow

/**
 * Repository for List Timeline operations.
 * Provides paginated access to statuses from a specific Mastodon list.
 */
interface ListTimelineRepository {
    /**
     * Get a flow of paginated statuses from a specific list.
     *
     * @param listId The ID of the list
     * @return Flow of PagingData containing statuses
     */
    fun getListTimeline(listId: String): Flow<PagingData<Status>>
}
