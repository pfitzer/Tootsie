package com.pfitzer.tootsie.domain.usecase.lists

import com.pfitzer.tootsie.domain.model.MastodonList
import com.pfitzer.tootsie.domain.model.RepliesPolicy
import com.pfitzer.tootsie.domain.repository.ListsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for creating a new list.
 */
class CreateListUseCase @Inject constructor(
    private val listsRepository: ListsRepository
) {
    suspend operator fun invoke(
        title: String,
        repliesPolicy: RepliesPolicy = RepliesPolicy.LIST
    ): Result<MastodonList> {
        return listsRepository.createList(title, repliesPolicy)
    }
}
