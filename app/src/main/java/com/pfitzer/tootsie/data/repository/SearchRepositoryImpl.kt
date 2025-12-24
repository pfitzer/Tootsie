package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.SearchResults
import com.pfitzer.tootsie.domain.repository.SearchRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SearchRepository.
 * Manages search operations with the Mastodon API.
 */
@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : SearchRepository {

    override suspend fun search(
        query: String,
        type: String?,
        resolve: Boolean,
        following: Boolean,
        limit: Int,
        offset: Int
    ): Result<SearchResults> {
        return try {
            val searchResultsDto = mastodonApi.search(
                query = query,
                type = type,
                resolve = resolve,
                following = following,
                limit = limit,
                offset = offset
            )
            Result.Success(searchResultsDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to perform search")
        }
    }
}
