package com.pfitzer.tootsie.domain.repository

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Status
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing favorited statuses.
 */
interface FavoritesRepository {

    /**
     * Get a paginated flow of favorited statuses.
     * @return Flow of PagingData containing favorited statuses
     */
    fun getFavorites(): Flow<PagingData<Status>>
}
