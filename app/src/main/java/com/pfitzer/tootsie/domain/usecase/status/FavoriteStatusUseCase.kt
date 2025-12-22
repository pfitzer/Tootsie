package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for favoriting (liking) a status.
 * Favorites are visible to others and show appreciation for the post.
 */
class FavoriteStatusUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    /**
     * Favorites a status with the given ID.
     *
     * @param statusId The ID of the status to favorite
     * @return Result containing the updated Status with favourited=true
     */
    suspend operator fun invoke(statusId: String): Result<Status> {
        return statusRepository.favoriteStatus(statusId)
    }
}
