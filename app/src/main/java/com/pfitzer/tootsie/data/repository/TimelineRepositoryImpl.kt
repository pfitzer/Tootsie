package com.pfitzer.tootsie.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pfitzer.tootsie.data.local.dao.StatusDao
import com.pfitzer.tootsie.data.paging.TimelinePagingSource
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Status
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
    private val statusDao: StatusDao
) : TimelineRepository {

    override fun getHomeTimeline(): Flow<PagingData<Status>> {
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
                    timelineType = "home"
                )
            }
        ).flow
    }

    override fun getPublicTimeline(local: Boolean): Flow<PagingData<Status>> {
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
                    timelineType = "public",
                    local = local
                )
            }
        ).flow
    }

    override suspend fun refreshHomeTimeline() {
        // Clear cached home timeline statuses
        statusDao.deleteStatusesByTimelineType("home")
    }

    override suspend fun refreshPublicTimeline() {
        // Clear cached public timeline statuses
        statusDao.deleteStatusesByTimelineType("public")
    }
}
