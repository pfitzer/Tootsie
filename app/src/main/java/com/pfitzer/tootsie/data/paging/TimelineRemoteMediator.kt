package com.pfitzer.tootsie.data.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.pfitzer.tootsie.data.local.TootsieDatabase
import com.pfitzer.tootsie.data.local.entities.StatusEntity
import com.pfitzer.tootsie.data.mapper.toEntity
import com.pfitzer.tootsie.data.remote.dto.StatusDto
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "TimelineRemoteMediator"

/**
 * Loads a timeline from the API into the Room cache, which is the single source of truth
 * for the timeline UI (offline-first).
 *
 * - REFRESH: replaces the cached timeline with the newest page
 * - APPEND: loads the page older than the oldest cached status (`max_id`)
 * - PREPEND: not needed, new posts arrive via refresh
 *
 * @param timelineType "home", "local" or "federated"
 * @param ownerAccountId Logged-in account the timeline belongs to
 * @param fetchPage Loads a page from the API: (maxId, limit) -> statuses, newest first
 */
@OptIn(ExperimentalPagingApi::class)
class TimelineRemoteMediator(
    private val database: TootsieDatabase,
    private val timelineType: String,
    private val ownerAccountId: String,
    private val fetchPage: suspend (maxId: String?, limit: Int) -> List<StatusDto>,
    private val now: () -> Long = System::currentTimeMillis
) : RemoteMediator<Int, StatusEntity>() {

    private val statusDao = database.statusDao()

    override suspend fun initialize(): InitializeAction {
        // A recently loaded cache is shown as is, e.g. when switching tabs or changing
        // a filter; otherwise load the newest posts on start
        val lastSync = statusDao.getLastSyncTime(timelineType, ownerAccountId)
        return if (lastSync != null && now() - lastSync < CACHE_TIMEOUT_MS) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, StatusEntity>
    ): MediatorResult {
        val maxId = when (loadType) {
            LoadType.REFRESH -> null
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> statusDao.getOldestStatusId(timelineType, ownerAccountId)
                // Nothing cached yet: the refresh loads the first page
                ?: return MediatorResult.Success(endOfPaginationReached = false)
        }
        val limit = if (loadType == LoadType.REFRESH) state.config.initialLoadSize else state.config.pageSize

        return try {
            val statuses = fetchPage(maxId, limit.coerceAtMost(MAX_PAGE_SIZE))
            Log.d(TAG, "$loadType $timelineType: ${statuses.size} statuses (maxId=$maxId)")

            val entities = statuses.map { it.toEntity(timelineType, ownerAccountId) }
            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    statusDao.deleteStatusesByTimelineType(timelineType, ownerAccountId)
                }
                statusDao.insertStatuses(entities)
            }
            MediatorResult.Success(endOfPaginationReached = statuses.isEmpty())
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    companion object {
        /** Cache younger than this is not refreshed automatically. */
        val CACHE_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5)

        /** Maximum page size of the Mastodon timeline API. */
        const val MAX_PAGE_SIZE = 40
    }
}
