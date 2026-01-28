package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for unpinning a status from profile.
 * Removes the status from the pinned section of the user's profile.
 */
class UnpinStatusUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    /**
     * Unpins a status with the given ID from the user's profile.
     *
     * @param statusId The ID of the status to unpin
     * @return Result containing the updated Status with pinned=false
     */
    suspend operator fun invoke(statusId: String): Result<Status> {
        return statusRepository.unpinStatus(statusId)
    }
}
