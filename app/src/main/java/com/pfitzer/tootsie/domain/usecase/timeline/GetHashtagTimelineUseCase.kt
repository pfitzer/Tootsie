package com.pfitzer.tootsie.domain.usecase.timeline

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.HashtagTimelineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting a hashtag timeline.
 * Retrieves a paginated flow of statuses containing a specific hashtag.
 */
class GetHashtagTimelineUseCase @Inject constructor(
    private val hashtagTimelineRepository: HashtagTimelineRepository
) {
    /**
     * Get a paginated flow of statuses for a specific hashtag.
     *
     * @param hashtag The hashtag name (without the # symbol)
     * @return Flow of paging data containing statuses
     */
    operator fun invoke(hashtag: String): Flow<PagingData<Status>> {
        return hashtagTimelineRepository.getHashtagTimeline(hashtag)
    }
}
