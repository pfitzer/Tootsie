package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for unreblogging (removing a boost) a status.
 * Removes the reblog from the user's timeline.
 */
class UnreblogStatusUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    /**
     * Unreblogs a status with the given ID.
     *
     * @param statusId The ID of the status to unreblog
     * @return Result containing the updated Status with reblogged=false
     */
    suspend operator fun invoke(statusId: String): Result<Status> {
        return statusRepository.unreblogStatus(statusId)
    }
}
