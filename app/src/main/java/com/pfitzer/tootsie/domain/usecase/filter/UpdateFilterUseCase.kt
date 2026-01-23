package com.pfitzer.tootsie.domain.usecase.filter

import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.domain.repository.FilterRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for updating an existing filter.
 */
class UpdateFilterUseCase @Inject constructor(
    private val filterRepository: FilterRepository
) {
    suspend operator fun invoke(
        id: String,
        title: String? = null,
        context: List<FilterContext>? = null,
        filterAction: FilterAction? = null,
        expiresIn: Int? = null
    ): Result<Filter> {
        return filterRepository.updateFilter(
            id = id,
            title = title,
            context = context,
            filterAction = filterAction,
            expiresIn = expiresIn
        )
    }
}
