package com.pfitzer.tootsie.domain.usecase.listtimeline

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.ListTimelineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting paginated statuses from a specific list.
 */
class GetListTimelineUseCase @Inject constructor(
    private val listTimelineRepository: ListTimelineRepository
) {
    /**
     * Get a flow of paginated statuses from a specific list.
     *
     * @param listId The ID of the list to get statuses from
     * @return Flow of PagingData containing statuses
     */
    operator fun invoke(listId: String): Flow<PagingData<Status>> {
        return listTimelineRepository.getListTimeline(listId)
    }
}
