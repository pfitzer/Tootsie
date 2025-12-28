package com.pfitzer.tootsie.domain.usecase.lists

import com.pfitzer.tootsie.domain.model.MastodonList
import com.pfitzer.tootsie.domain.model.RepliesPolicy
import com.pfitzer.tootsie.domain.repository.ListsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for updating an existing list.
 */
class UpdateListUseCase @Inject constructor(
    private val listsRepository: ListsRepository
) {
    suspend operator fun invoke(
        id: String,
        title: String? = null,
        repliesPolicy: RepliesPolicy? = null
    ): Result<MastodonList> {
        return listsRepository.updateList(id, title, repliesPolicy)
    }
}
