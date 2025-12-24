package com.pfitzer.tootsie.domain.repository

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.TimelineFilter
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for timeline operations.
 * Defines the contract for fetching and caching timeline data.
 */
interface TimelineRepository {

    /**
     * Get home timeline with paging support.
     * Returns a Flow of PagingData for infinite scrolling.
     *
     * @param filter Timeline filter to apply
     * @return Flow of paged Status items
     */
    fun getHomeTimeline(filter: TimelineFilter = TimelineFilter.DEFAULT): Flow<PagingData<Status>>

    /**
     * Get public timeline with paging support.
     *
     * @param local Whether to show only local instance posts
     * @param filter Timeline filter to apply
     * @return Flow of paged Status items
     */
    fun getPublicTimeline(local: Boolean = false, filter: TimelineFilter = TimelineFilter.DEFAULT): Flow<PagingData<Status>>

    /**
     * Refresh home timeline.
     * Clears cache and fetches fresh data.
     */
    suspend fun refreshHomeTimeline()

    /**
     * Refresh public timeline.
     */
    suspend fun refreshPublicTimeline()
}
