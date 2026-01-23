package com.pfitzer.tootsie.domain.usecase.filter

import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.domain.repository.FilterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting filters that apply to a specific context.
 * Returns a Flow for reactive updates.
 */
class GetFiltersForContextUseCase @Inject constructor(
    private val filterRepository: FilterRepository
) {
    operator fun invoke(context: FilterContext): Flow<List<Filter>> {
        return filterRepository.getFiltersForContext(context)
    }
}
