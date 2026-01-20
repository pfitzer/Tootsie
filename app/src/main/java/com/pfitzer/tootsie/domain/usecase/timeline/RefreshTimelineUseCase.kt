package com.pfitzer.tootsie.domain.usecase.timeline

import com.pfitzer.tootsie.domain.model.TimelineType
import com.pfitzer.tootsie.domain.repository.TimelineRepository
import javax.inject.Inject

/**
 * Use case to refresh a timeline based on the specified type.
 */
class RefreshTimelineUseCase @Inject constructor(
    private val timelineRepository: TimelineRepository
) {
    suspend operator fun invoke(type: TimelineType) {
        when (type) {
            TimelineType.HOME -> timelineRepository.refreshHomeTimeline()
            TimelineType.PUBLIC_LOCAL, TimelineType.PUBLIC_FEDERATED -> timelineRepository.refreshPublicTimeline()
        }
    }
}
