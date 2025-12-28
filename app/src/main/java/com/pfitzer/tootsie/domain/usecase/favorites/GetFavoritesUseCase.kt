package com.pfitzer.tootsie.domain.usecase.favorites

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting paginated favorited statuses.
 */
class GetFavoritesUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) {
    /**
     * Get a flow of paginated favorited statuses.
     */
    operator fun invoke(): Flow<PagingData<Status>> {
        return favoritesRepository.getFavorites()
    }
}
