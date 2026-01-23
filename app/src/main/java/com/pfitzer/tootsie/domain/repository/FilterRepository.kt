package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.domain.model.FilterKeyword
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Mastodon Filters.
 * Filters allow users to hide or show warnings for posts containing specific keywords.
 */
interface FilterRepository {
    /**
     * Get all filters owned by the authenticated user.
     * This fetches from the API and caches locally.
     */
    suspend fun getFilters(): Result<List<Filter>>

    /**
     * Get a specific filter by ID.
     */
    suspend fun getFilter(id: String): Result<Filter>

    /**
     * Create a new filter.
     *
     * @param title Name of the filter
     * @param context List of contexts where the filter applies
     * @param filterAction Action to take (warn or hide)
     * @param expiresIn Optional expiration time in seconds
     * @param keywords Initial keywords for the filter
     */
    suspend fun createFilter(
        title: String,
        context: List<FilterContext>,
        filterAction: FilterAction,
        expiresIn: Int? = null,
        keywords: List<Pair<String, Boolean>> = emptyList() // keyword, wholeWord
    ): Result<Filter>

    /**
     * Update an existing filter.
     *
     * @param id Filter ID
     * @param title New title (null to keep current)
     * @param context New contexts (null to keep current)
     * @param filterAction New action (null to keep current)
     * @param expiresIn New expiration in seconds (null to keep current, 0 to make permanent)
     */
    suspend fun updateFilter(
        id: String,
        title: String? = null,
        context: List<FilterContext>? = null,
        filterAction: FilterAction? = null,
        expiresIn: Int? = null
    ): Result<Filter>

    /**
     * Delete a filter.
     *
     * @param id Filter ID
     */
    suspend fun deleteFilter(id: String): Result<Unit>

    /**
     * Add a keyword to a filter.
     *
     * @param filterId Filter ID
     * @param keyword The keyword text
     * @param wholeWord Whether to match whole words only
     */
    suspend fun addKeyword(
        filterId: String,
        keyword: String,
        wholeWord: Boolean
    ): Result<FilterKeyword>

    /**
     * Delete a keyword from a filter.
     *
     * @param keywordId Keyword ID
     */
    suspend fun deleteKeyword(keywordId: String): Result<Unit>

    /**
     * Get filters that apply to a specific context.
     * Returns a Flow for reactive updates from local cache.
     *
     * @param context The context to filter by
     */
    fun getFiltersForContext(context: FilterContext): Flow<List<Filter>>

    /**
     * Refresh filters from the API and update local cache.
     */
    suspend fun refreshFilters(): Result<List<Filter>>
}
