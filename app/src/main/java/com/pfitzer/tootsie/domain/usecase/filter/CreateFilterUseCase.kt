package com.pfitzer.tootsie.domain.usecase.filter

import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.domain.repository.FilterRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for creating a new filter.
 */
class CreateFilterUseCase @Inject constructor(
    private val filterRepository: FilterRepository
) {
    suspend operator fun invoke(
        title: String,
        context: List<FilterContext>,
        filterAction: FilterAction,
        expiresIn: Int? = null,
        keywords: List<Pair<String, Boolean>> = emptyList()
    ): Result<Filter> {
        return filterRepository.createFilter(
            title = title,
            context = context,
            filterAction = filterAction,
            expiresIn = expiresIn,
            keywords = keywords
        )
    }
}
