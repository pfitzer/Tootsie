package com.pfitzer.tootsie.domain.usecase.followrequests

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.FollowRequestsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting pending follow requests.
 */
class GetFollowRequestsUseCase @Inject constructor(
    private val followRequestsRepository: FollowRequestsRepository
) {
    /**
     * Get a paginated flow of pending follow requests.
     *
     * @return Flow of paginated Account data
     */
    operator fun invoke(): Flow<PagingData<Account>> {
        return followRequestsRepository.getFollowRequests()
    }
}
