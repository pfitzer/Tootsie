package com.pfitzer.tootsie.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pfitzer.tootsie.data.local.dao.StatusDao
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.mapper.toEntity
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Status
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "TimelinePagingSource"

/**
 * PagingSource for Timeline data.
 * Fetches data from API and caches to local database for offline support.
 */
class TimelinePagingSource(
    private val mastodonApi: MastodonApiService,
    private val statusDao: StatusDao,
    private val timelineType: String,
    private val local: Boolean = false
) : PagingSource<String, Status>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Status> {
        return try {
            val maxId = params.key // For pagination, load older posts
            Log.d(TAG, "load() called - timelineType=$timelineType, maxId=$maxId, loadSize=${params.loadSize}")

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

            // Cache to database
            if (statusDtos.isNotEmpty()) {
                val entities = statusDtos.map { it.toEntity(timelineType = timelineType) }
                statusDao.insertStatuses(entities)
            }

            // Convert to domain models
            val statuses = statusDtos.map { it.toDomain() }

            // Determine next page key (ID of oldest status)
            val nextKey = if (statuses.isEmpty()) {
                null
            } else {
                statuses.last().id
            }

            Log.d(TAG, "Returning ${statuses.size} statuses, nextKey=$nextKey")
            LoadResult.Page(
                data = statuses,
                prevKey = null, // Only load older posts (forward pagination)
                nextKey = nextKey
            )
        } catch (e: IOException) {
            // Network error - try loading from cache
            Log.e(TAG, "Network error loading timeline", e)
            tryLoadFromCache(params)
        } catch (e: HttpException) {
            // HTTP error - try loading from cache
            Log.e(TAG, "HTTP error loading timeline: ${e.code()} ${e.message()}", e)
            tryLoadFromCache(params)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading timeline", e)
            LoadResult.Error(e)
        }
    }

    /**
     * Fallback to cached data when network is unavailable.
     */
    private suspend fun tryLoadFromCache(params: LoadParams<String>): LoadResult<String, Status> {
        return try {
            Log.d(TAG, "Attempting to load from cache...")
            // Collect the first emission from the Flow
            val cachedEntities = statusDao.getStatusesFlow(
                timelineType = timelineType,
                limit = params.loadSize
            ).first()

            Log.d(TAG, "Found ${cachedEntities.size} cached statuses")

            // Convert entities to domain models
            val cachedStatuses = cachedEntities.map { it.toDomain() }

            // For offline support, return cached data
            // Note: This is a simplified approach - a production app would use
            // RemoteMediator for more sophisticated caching
            LoadResult.Page(
                data = cachedStatuses,
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
}
