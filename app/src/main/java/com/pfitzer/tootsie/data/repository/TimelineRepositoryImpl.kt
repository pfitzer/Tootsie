package com.pfitzer.tootsie.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.dao.StatusDao
import com.pfitzer.tootsie.data.paging.TimelinePagingSource
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Filter
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.model.TimelineFilter
import com.pfitzer.tootsie.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TimelineRepository.
 * Provides paginated timeline data with offline caching support.
 */
@Singleton
class TimelineRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService,
    private val statusDao: StatusDao,
    private val accountDao: AccountDao
) : TimelineRepository {

    override fun getHomeTimeline(
        filter: TimelineFilter,
        keywordFilters: List<Filter>
    ): Flow<PagingData<Status>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                TimelinePagingSource(
                    mastodonApi = mastodonApi,
                    statusDao = statusDao,
                    accountDao = accountDao,
                    timelineType = "home",
                    filter = filter,
                    keywordFilters = keywordFilters
                )
            }
        ).flow
    }

    override fun getPublicTimeline(
        local: Boolean,
        filter: TimelineFilter,
        keywordFilters: List<Filter>
    ): Flow<PagingData<Status>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                TimelinePagingSource(
                    mastodonApi = mastodonApi,
                    statusDao = statusDao,
                    accountDao = accountDao,
                    timelineType = "public",
                    local = local,
                    filter = filter,
                    keywordFilters = keywordFilters
                )
            }
        ).flow
    }

    override suspend fun refreshHomeTimeline() {
        // Get active account and clear its cached home timeline statuses
        val activeAccount = accountDao.getActiveAccount()
        if (activeAccount != null) {
            statusDao.deleteStatusesByTimelineType("home", activeAccount.id)
        }
    }

    override suspend fun refreshPublicTimeline() {
        // Get active account and clear its cached public timeline statuses
        val activeAccount = accountDao.getActiveAccount()
        if (activeAccount != null) {
            statusDao.deleteStatusesByTimelineType("public", activeAccount.id)
        }
    }

    override suspend fun cleanupOldStatuses(retentionHours: Int) {
        val cutoffTimestamp = System.currentTimeMillis() - (retentionHours * 60 * 60 * 1000L)
        statusDao.deleteOldStatuses(cutoffTimestamp)
    }
}
