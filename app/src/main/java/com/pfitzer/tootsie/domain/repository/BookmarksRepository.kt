package com.pfitzer.tootsie.domain.repository

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Status
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing bookmarked statuses.
 */
interface BookmarksRepository {

    /**
     * Get a paginated flow of bookmarked statuses.
     * @return Flow of PagingData containing bookmarked statuses
     */
    fun getBookmarks(): Flow<PagingData<Status>>
}
