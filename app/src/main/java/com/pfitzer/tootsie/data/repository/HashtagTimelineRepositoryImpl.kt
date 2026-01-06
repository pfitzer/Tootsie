package com.pfitzer.tootsie.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pfitzer.tootsie.data.paging.HashtagTimelinePagingSource
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.HashtagTimelineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of HashtagTimelineRepository.
 * Provides paginated access to statuses containing a specific hashtag.
 */
@Singleton
class HashtagTimelineRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : HashtagTimelineRepository {

    override fun getHashtagTimeline(hashtag: String): Flow<PagingData<Status>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                HashtagTimelinePagingSource(
                    mastodonApi = mastodonApi,
                    hashtag = hashtag
                )
            }
        ).flow
    }
}
