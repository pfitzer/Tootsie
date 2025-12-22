package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for unfavoriting (unliking) a status.
 * Removes the favorite from the status.
 */
class UnfavoriteStatusUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    /**
     * Unfavorites a status with the given ID.
     *
     * @param statusId The ID of the status to unfavorite
     * @return Result containing the updated Status with favourited=false
     */
    suspend operator fun invoke(statusId: String): Result<Status> {
        return statusRepository.unfavoriteStatus(statusId)
    }
}
