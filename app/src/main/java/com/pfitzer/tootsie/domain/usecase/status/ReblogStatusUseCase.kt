package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for reblogging (boosting/retweeting) a status.
 * Reblogs share the status to the user's followers.
 */
class ReblogStatusUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    /**
     * Reblogs a status with the given ID.
     *
     * @param statusId The ID of the status to reblog
     * @return Result containing the updated Status with reblogged=true
     */
    suspend operator fun invoke(statusId: String): Result<Status> {
        return statusRepository.reblogStatus(statusId)
    }
}
