package com.pfitzer.tootsie.domain.usecase.followrequests

import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.FollowRequestsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for rejecting a follow request.
 */
class RejectFollowRequestUseCase @Inject constructor(
    private val followRequestsRepository: FollowRequestsRepository
) {
    /**
     * Reject a follow request from an account.
     *
     * @param accountId The ID of the account to reject
     * @return Result containing the updated Relationship
     */
    suspend operator fun invoke(accountId: String): Result<Relationship> {
        return followRequestsRepository.rejectFollowRequest(accountId)
    }
}
