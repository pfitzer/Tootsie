package com.pfitzer.tootsie.domain.usecase.followrequests

import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.FollowRequestsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for authorizing a follow request.
 */
class AuthorizeFollowRequestUseCase @Inject constructor(
    private val followRequestsRepository: FollowRequestsRepository
) {
    /**
     * Authorize a follow request from an account.
     *
     * @param accountId The ID of the account to authorize
     * @return Result containing the updated Relationship
     */
    suspend operator fun invoke(accountId: String): Result<Relationship> {
        return followRequestsRepository.authorizeFollowRequest(accountId)
    }
}
