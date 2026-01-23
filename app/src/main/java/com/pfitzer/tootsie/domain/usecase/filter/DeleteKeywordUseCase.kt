package com.pfitzer.tootsie.domain.usecase.filter

import com.pfitzer.tootsie.domain.repository.FilterRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for deleting a keyword from a filter.
 */
class DeleteKeywordUseCase @Inject constructor(
    private val filterRepository: FilterRepository
) {
    suspend operator fun invoke(keywordId: String): Result<Unit> {
        return filterRepository.deleteKeyword(keywordId)
    }
}
