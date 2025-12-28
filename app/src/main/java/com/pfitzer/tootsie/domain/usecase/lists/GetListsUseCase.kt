package com.pfitzer.tootsie.domain.usecase.lists

import com.pfitzer.tootsie.domain.model.MastodonList
import com.pfitzer.tootsie.domain.repository.ListsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for getting all user's lists.
 */
class GetListsUseCase @Inject constructor(
    private val listsRepository: ListsRepository
) {
    suspend operator fun invoke(): Result<List<MastodonList>> {
        return listsRepository.getLists()
    }
}
