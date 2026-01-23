package com.pfitzer.tootsie.domain.usecase.filter

import com.pfitzer.tootsie.domain.repository.FilterRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for deleting a filter.
 */
class DeleteFilterUseCase @Inject constructor(
    private val filterRepository: FilterRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return filterRepository.deleteFilter(id)
    }
}
