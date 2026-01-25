package com.pfitzer.tootsie.domain.usecase.filter

import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.repository.FilterRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for getting a single filter by ID.
 */
class GetFilterUseCase @Inject constructor(
    private val filterRepository: FilterRepository
) {
    suspend operator fun invoke(id: String): Result<Filter> {
        return filterRepository.getFilter(id)
    }
}
