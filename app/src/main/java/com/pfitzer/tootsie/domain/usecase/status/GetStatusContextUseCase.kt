package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.Context
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for fetching conversation context for a status.
 * Returns the posts before (ancestors) and replies after (descendants).
 */
class GetStatusContextUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    /**
     * Gets the conversation context for the specified status.
     *
     * @param statusId The ID of the status to get context for
     * @return Result containing Context with ancestors and descendants
     */
    suspend operator fun invoke(statusId: String): Result<Context> {
        return statusRepository.getStatusContext(statusId)
    }
}
