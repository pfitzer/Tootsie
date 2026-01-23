package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.local.dao.FilterDao
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.mapper.toEntity
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.FilterAction
import com.pfitzer.tootsie.domain.model.FilterContext
import com.pfitzer.tootsie.domain.model.FilterKeyword
import com.pfitzer.tootsie.domain.repository.FilterRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FilterRepository.
 * Manages Mastodon Filters via the API and caches them locally.
 */
@Singleton
class FilterRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService,
    private val filterDao: FilterDao,
    private val preferencesManager: PreferencesManager
) : FilterRepository {

    override suspend fun getFilters(): Result<List<Filter>> {
        return try {
            val filtersDto = mastodonApi.getFilters()
            val filters = filtersDto.map { it.toDomain() }

            // Cache filters locally
            val accountId = preferencesManager.getActiveAccountId()
            if (accountId != null) {
                filterDao.deleteAllFilters(accountId)
                filterDao.insertFilters(filters.map { it.toEntity(accountId) })
            }

            Result.Success(filters)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch filters")
        }
    }

    override suspend fun getFilter(id: String): Result<Filter> {
        return try {
            val filterDto = mastodonApi.getFilter(id)
            Result.Success(filterDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch filter")
        }
    }

    override suspend fun createFilter(
        title: String,
        context: List<FilterContext>,
        filterAction: FilterAction,
        expiresIn: Int?,
        keywords: List<Pair<String, Boolean>>
    ): Result<Filter> {
        return try {
            // Create the filter
            val filterDto = mastodonApi.createFilter(
                title = title,
                context = context.map { it.toApiString() },
                filterAction = filterAction.toApiString(),
                expiresIn = expiresIn
            )

            var filter = filterDto.toDomain()

            // Add keywords if provided
            if (keywords.isNotEmpty()) {
                val addedKeywords = mutableListOf<FilterKeyword>()
                for ((keyword, wholeWord) in keywords) {
                    val keywordDto = mastodonApi.addFilterKeyword(
                        filterId = filter.id,
                        keyword = keyword,
                        wholeWord = wholeWord
                    )
                    addedKeywords.add(keywordDto.toDomain())
                }
                filter = filter.copy(keywords = addedKeywords)
            }

            // Cache locally
            val accountId = preferencesManager.getActiveAccountId()
            if (accountId != null) {
                filterDao.insertFilter(filter.toEntity(accountId))
            }

            Result.Success(filter)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to create filter")
        }
    }

    override suspend fun updateFilter(
        id: String,
        title: String?,
        context: List<FilterContext>?,
        filterAction: FilterAction?,
        expiresIn: Int?
    ): Result<Filter> {
        return try {
            val filterDto = mastodonApi.updateFilter(
                id = id,
                title = title,
                context = context?.map { it.toApiString() },
                filterAction = filterAction?.toApiString(),
                expiresIn = expiresIn
            )
            val filter = filterDto.toDomain()

            // Update local cache
            val accountId = preferencesManager.getActiveAccountId()
            if (accountId != null) {
                filterDao.insertFilter(filter.toEntity(accountId))
            }

            Result.Success(filter)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update filter")
        }
    }

    override suspend fun deleteFilter(id: String): Result<Unit> {
        return try {
            mastodonApi.deleteFilter(id)
            filterDao.deleteFilter(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete filter")
        }
    }

    override suspend fun addKeyword(
        filterId: String,
        keyword: String,
        wholeWord: Boolean
    ): Result<FilterKeyword> {
        return try {
            val keywordDto = mastodonApi.addFilterKeyword(
                filterId = filterId,
                keyword = keyword,
                wholeWord = wholeWord
            )

            // Refresh the filter to update local cache
            refreshFilter(filterId)

            Result.Success(keywordDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to add keyword")
        }
    }

    override suspend fun deleteKeyword(keywordId: String): Result<Unit> {
        return try {
            mastodonApi.deleteFilterKeyword(keywordId)
            // Note: We'd need the filterId to refresh the local cache
            // For now, the next getFilters() call will sync
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete keyword")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFiltersForContext(context: FilterContext): Flow<List<Filter>> {
        return preferencesManager.activeAccountId.flatMapLatest { accountId ->
            if (accountId == null) {
                flowOf(emptyList())
            } else {
                filterDao.getFiltersForContext(accountId, context.toApiString())
                    .map { entities -> entities.map { it.toDomain() } }
            }
        }
    }

    override suspend fun refreshFilters(): Result<List<Filter>> {
        return getFilters() // getFilters() already syncs to local cache
    }

    /**
     * Refresh a single filter and update local cache.
     */
    private suspend fun refreshFilter(filterId: String) {
        try {
            val filterDto = mastodonApi.getFilter(filterId)
            val filter = filterDto.toDomain()
            val accountId = preferencesManager.getActiveAccountId()
            if (accountId != null) {
                filterDao.insertFilter(filter.toEntity(accountId))
            }
        } catch (e: Exception) {
            // Ignore errors during refresh
        }
    }
}
