package com.pfitzer.tootsie.domain.usecase.timeline

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.TimelineFilter
import com.pfitzer.tootsie.domain.model.TimelineType
import com.pfitzer.tootsie.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get a timeline based on the specified type and filter.
 */
class GetTimelineUseCase @Inject constructor(
    private val timelineRepository: TimelineRepository
) {
    operator fun invoke(
        type: TimelineType,
        filter: TimelineFilter = TimelineFilter.DEFAULT,
        keywordFilters: List<Filter> = emptyList()
    ): Flow<PagingData<Status>> {
        return when (type) {
            TimelineType.HOME -> timelineRepository.getHomeTimeline(filter, keywordFilters)
            TimelineType.PUBLIC_LOCAL -> timelineRepository.getPublicTimeline(
                local = true,
                filter = filter,
                keywordFilters = keywordFilters
            )
            TimelineType.PUBLIC_FEDERATED -> timelineRepository.getPublicTimeline(
                local = false,
                filter = filter,
                keywordFilters = keywordFilters
            )
        }
    }
}
