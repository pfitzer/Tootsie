package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.SearchResults
import com.pfitzer.tootsie.util.Result

/**
 * Repository interface for search operations.
 * Defines the contract for searching accounts, statuses, and hashtags.
 */
interface SearchRepository {

    /**
     * Search for accounts, statuses, and hashtags.
     *
     * @param query Search query string
     * @param type Type of results to return (accounts, statuses, hashtags) or null for all
     * @param resolve Attempt WebFinger lookup for remote accounts
     * @param following Only search among accounts the user follows
     * @param limit Maximum number of results per category
     * @param offset Skip this many results (for pagination)
     * @return Search results
     */
    suspend fun search(
        query: String,
        type: String? = null,
        resolve: Boolean = false,
        following: Boolean = false,
        limit: Int = 20,
        offset: Int = 0
    ): Result<SearchResults>
}
