package com.pfitzer.tootsie.domain.usecase.bookmarks

import androidx.paging.PagingData
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.BookmarksRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting paginated bookmarked statuses.
 */
class GetBookmarksUseCase @Inject constructor(
    private val bookmarksRepository: BookmarksRepository
) {
    /**
     * Get a flow of paginated bookmarked statuses.
     */
    operator fun invoke(): Flow<PagingData<Status>> {
        return bookmarksRepository.getBookmarks()
    }
}
