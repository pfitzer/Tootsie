package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for removing a bookmark from a status.
 * Removes the status from the user's bookmarks.
 */
class UnbookmarkStatusUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    /**
     * Unbookmarks a status with the given ID.
     *
     * @param statusId The ID of the status to unbookmark
     * @return Result containing the updated Status with bookmarked=false
     */
    suspend operator fun invoke(statusId: String): Result<Status> {
        return statusRepository.unbookmarkStatus(statusId)
    }
}
