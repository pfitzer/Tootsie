package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for bookmarking a status.
 * Bookmarks are private and used to save posts for later reading.
 */
class BookmarkStatusUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    /**
     * Bookmarks a status with the given ID.
     *
     * @param statusId The ID of the status to bookmark
     * @return Result containing the updated Status with bookmarked=true
     */
    suspend operator fun invoke(statusId: String): Result<Status> {
        return statusRepository.bookmarkStatus(statusId)
    }
}
