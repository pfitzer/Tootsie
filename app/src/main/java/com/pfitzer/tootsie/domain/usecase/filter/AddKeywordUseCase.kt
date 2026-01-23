package com.pfitzer.tootsie.domain.usecase.filter

import com.pfitzer.tootsie.domain.model.FilterKeyword
import com.pfitzer.tootsie.domain.repository.FilterRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for adding a keyword to a filter.
 */
class AddKeywordUseCase @Inject constructor(
    private val filterRepository: FilterRepository
) {
    suspend operator fun invoke(
        filterId: String,
        keyword: String,
        wholeWord: Boolean
    ): Result<FilterKeyword> {
        return filterRepository.addKeyword(
            filterId = filterId,
            keyword = keyword,
            wholeWord = wholeWord
        )
    }
}
