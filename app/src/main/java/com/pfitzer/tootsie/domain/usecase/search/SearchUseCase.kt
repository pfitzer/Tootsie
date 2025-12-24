package com.pfitzer.tootsie.domain.usecase.search

import com.pfitzer.tootsie.domain.model.SearchResults
import com.pfitzer.tootsie.domain.repository.SearchRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for searching accounts, statuses, and hashtags.
 */
class SearchUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    /**
     * Perform a search query.
     *
     * @param query Search query string
     * @param type Type of results to return (accounts, statuses, hashtags) or null for all
     * @param resolve Attempt WebFinger lookup for remote accounts
     * @param following Only search among accounts the user follows
     * @param limit Maximum number of results per category
     * @param offset Skip this many results (for pagination)
     * @return Search results
     */
    suspend operator fun invoke(
        query: String,
        type: String? = null,
        resolve: Boolean = false,
        following: Boolean = false,
        limit: Int = 20,
        offset: Int = 0
    ): Result<SearchResults> {
        if (query.isBlank()) {
            return Result.Success(SearchResults())
        }
        return searchRepository.search(
            query = query,
            type = type,
            resolve = resolve,
            following = following,
            limit = limit,
            offset = offset
        )
    }
}
