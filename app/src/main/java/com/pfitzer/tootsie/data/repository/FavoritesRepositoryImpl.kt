package com.pfitzer.tootsie.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pfitzer.tootsie.data.paging.FavoritesPagingSource
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FavoritesRepository.
 * Provides paginated access to favorited statuses.
 */
@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : FavoritesRepository {

    override fun getFavorites(): Flow<PagingData<Status>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                FavoritesPagingSource(mastodonApi)
            }
        ).flow
    }
}
