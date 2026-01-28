package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for pinning a status to profile.
 * Pinned statuses appear at the top of the user's profile.
 * Only own statuses can be pinned (max 5 typically).
 */
class PinStatusUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    /**
     * Pins a status with the given ID to the user's profile.
     *
     * @param statusId The ID of the status to pin
     * @return Result containing the updated Status with pinned=true
     */
    suspend operator fun invoke(statusId: String): Result<Status> {
        return statusRepository.pinStatus(statusId)
    }
}
