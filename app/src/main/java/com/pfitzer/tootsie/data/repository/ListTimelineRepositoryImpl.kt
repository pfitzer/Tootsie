package com.pfitzer.tootsie.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pfitzer.tootsie.data.paging.ListTimelinePagingSource
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.ListTimelineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ListTimelineRepository.
 * Provides paginated access to statuses from a specific Mastodon list.
 */
@Singleton
class ListTimelineRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : ListTimelineRepository {

    override fun getListTimeline(listId: String): Flow<PagingData<Status>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                ListTimelinePagingSource(
                    mastodonApi = mastodonApi,
                    listId = listId
                )
            }
        ).flow
    }
}
