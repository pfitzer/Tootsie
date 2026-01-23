package com.pfitzer.tootsie.domain.usecase.filter

import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.repository.FilterRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for getting all user's filters.
 */
class GetFiltersUseCase @Inject constructor(
    private val filterRepository: FilterRepository
) {
    suspend operator fun invoke(): Result<List<Filter>> {
        return filterRepository.getFilters()
    }
}
