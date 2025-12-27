package com.pfitzer.tootsie.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.dao.StatusDao
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.mapper.toEntity
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.TimelineFilter
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "TimelinePagingSource"

/**
 * PagingSource for Timeline data.
 * Fetches data from API and caches to local database for offline support.
 * Data is scoped to the active account via ownerAccountId.
 */
class TimelinePagingSource(
    private val mastodonApi: MastodonApiService,
    private val statusDao: StatusDao,
    private val accountDao: AccountDao,
    private val timelineType: String,
    private val local: Boolean = false,
    private val filter: TimelineFilter = TimelineFilter.DEFAULT
) : PagingSource<String, Status>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Status> {
        // Get active account for scoping cached data
        val activeAccount = accountDao.getActiveAccount()
        if (activeAccount == null) {
            Log.w(TAG, "No active account, returning empty result")
            return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
        }

        return try {
            val maxId = params.key // For pagination, load older posts
            Log.d(TAG, "load() called - timelineType=$timelineType, ownerAccountId=${activeAccount.id}, maxId=$maxId, loadSize=${params.loadSize}")

            // Fetch from API
            val statusDtos = when (timelineType) {
                "home" -> {
                    Log.d(TAG, "Fetching home timeline from API...")
                    mastodonApi.getHomeTimeline(
                        maxId = maxId,
                        limit = params.loadSize
                    )
                }
                "public" -> {
                    Log.d(TAG, "Fetching public timeline from API...")
                    mastodonApi.getPublicTimeline(
                        local = local,
                        maxId = maxId,
                        limit = params.loadSize
                    )
                }
                else -> {
                    Log.w(TAG, "Unknown timeline type: $timelineType")
                    emptyList()
                }
            }

            Log.d(TAG, "API returned ${statusDtos.size} statuses")

            // Cache to database with ownerAccountId
            if (statusDtos.isNotEmpty()) {
                val entities = statusDtos.map {
                    it.toEntity(
                        timelineType = timelineType,
                        ownerAccountId = activeAccount.id
                    )
                }
                statusDao.insertStatuses(entities)
            }

            // Convert to domain models
            val statuses = statusDtos.map { it.toDomain() }

            // Apply filters
            val filteredStatuses = applyFilter(statuses)

            // Determine next page key (ID of oldest status in unfiltered list)
            val nextKey = if (statuses.isEmpty()) {
                null
            } else {
                statuses.last().id
            }

            Log.d(TAG, "Returning ${filteredStatuses.size} statuses (${statuses.size} before filtering), nextKey=$nextKey")
            LoadResult.Page(
                data = filteredStatuses,
                prevKey = null, // Only load older posts (forward pagination)
                nextKey = nextKey
            )
        } catch (e: IOException) {
            // Network error - try loading from cache
            Log.e(TAG, "Network error loading timeline", e)
            tryLoadFromCache(params, activeAccount.id)
        } catch (e: HttpException) {
            // HTTP error - try loading from cache
            Log.e(TAG, "HTTP error loading timeline: ${e.code()} ${e.message()}", e)
            tryLoadFromCache(params, activeAccount.id)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading timeline", e)
            LoadResult.Error(e)
        }
    }

    /**
     * Fallback to cached data when network is unavailable.
     * Filters by ownerAccountId to only load current account's cache.
     */
    private suspend fun tryLoadFromCache(
        params: LoadParams<String>,
        ownerAccountId: String
    ): LoadResult<String, Status> {
        return try {
            Log.d(TAG, "Attempting to load from cache for account: $ownerAccountId...")
            // Collect the first emission from the Flow, filtered by owner account
            val cachedEntities = statusDao.getStatusesFlow(
                timelineType = timelineType,
                ownerAccountId = ownerAccountId,
                limit = params.loadSize
            ).first()

            Log.d(TAG, "Found ${cachedEntities.size} cached statuses")

            // Convert entities to domain models
            val cachedStatuses = cachedEntities.map { it.toDomain() }

            // Apply filters
            val filteredCachedStatuses = applyFilter(cachedStatuses)

            // For offline support, return cached data
            // Note: This is a simplified approach - a production app would use
            // RemoteMediator for more sophisticated caching
            LoadResult.Page(
                data = filteredCachedStatuses,
                prevKey = null,
                nextKey = if (cachedStatuses.isNotEmpty()) cachedStatuses.last().id else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from cache", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Status>): String? {
        // Return null to always start from the beginning when refreshing
        return null
    }

    /**
     * Apply timeline filters to a list of statuses.
     */
    private fun applyFilter(statuses: List<Status>): List<Status> {
        if (!filter.hasActiveFilters) {
            return statuses
        }

        return statuses.filter { status ->
            // Filter boosts (reblogs)
            if (filter.hideBoosts && status.reblog != null) {
                return@filter false
            }

            // Filter replies
            if (filter.hideReplies && status.inReplyToId != null) {
                return@filter false
            }

            // Filter posts without media
            if (filter.showOnlyMedia && status.mediaAttachments.isEmpty()) {
                return@filter false
            }

            true
        }
    }
}
