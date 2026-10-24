package com.pfitzer.tootsie.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.pfitzer.tootsie.data.local.TootsieDatabase
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.dao.StatusDao
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.paging.TimelineRemoteMediator
import com.pfitzer.tootsie.data.paging.TimelineStatusFilter
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.StatusDto
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.TimelineFilter
import com.pfitzer.tootsie.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TimelineRepository.
 * Timelines are offline-first: a RemoteMediator loads pages from the API into Room, and
 * the UI pages through the Room cache.
 */
@Singleton
class TimelineRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService,
    private val database: TootsieDatabase,
    private val statusDao: StatusDao,
    private val accountDao: AccountDao
) : TimelineRepository {

    override fun getHomeTimeline(
        filter: TimelineFilter,
        keywordFilters: List<Filter>
    ): Flow<PagingData<Status>> = timeline(TIMELINE_HOME, filter, keywordFilters) { maxId, limit ->
        mastodonApi.getHomeTimeline(maxId = maxId, limit = limit)
    }

    override fun getPublicTimeline(
        local: Boolean,
        filter: TimelineFilter,
        keywordFilters: List<Filter>
    ): Flow<PagingData<Status>> {
        val timelineType = if (local) TIMELINE_LOCAL else TIMELINE_FEDERATED
        return timeline(timelineType, filter, keywordFilters) { maxId, limit ->
            mastodonApi.getPublicTimeline(local = local, maxId = maxId, limit = limit)
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    private fun timeline(
        timelineType: String,
        filter: TimelineFilter,
        keywordFilters: List<Filter>,
        fetchPage: suspend (maxId: String?, limit: Int) -> List<StatusDto>
    ): Flow<PagingData<Status>> = flow {
        // The cache is scoped to the account that is active when the timeline is opened;
        // an account switch creates a new pager
        val ownerAccountId = accountDao.getActiveAccount()?.id
        if (ownerAccountId == null) {
            emit(PagingData.empty())
            return@flow
        }

        val statusFilter = TimelineStatusFilter(filter, keywordFilters)
        val pager = Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                enablePlaceholders = false,
                initialLoadSize = 40
            ),
            remoteMediator = TimelineRemoteMediator(
                database = database,
                timelineType = timelineType,
                ownerAccountId = ownerAccountId,
                fetchPage = fetchPage
            ),
            pagingSourceFactory = { statusDao.pagingSource(timelineType, ownerAccountId) }
        )
        emitAll(
            pager.flow.map { pagingData ->
                // Map first so the filter runs once per status; PagingData can't hold nulls
                pagingData
                    .map { entity -> Filtered(statusFilter.apply(entity.toDomain())) }
                    .filter { it.status != null }
                    .map { it.status!! }
            }
        )
    }

    override suspend fun cleanupOldStatuses(retentionHours: Int) {
        val cutoffTimestamp = System.currentTimeMillis() - (retentionHours * 60 * 60 * 1000L)
        statusDao.deleteOldStatuses(cutoffTimestamp)
    }

    /** A status after filtering; null if it is hidden. */
    private class Filtered(val status: Status?)

    private companion object {
        const val TIMELINE_HOME = "home"
        const val TIMELINE_LOCAL = "local"
        const val TIMELINE_FEDERATED = "federated"
    }
}
