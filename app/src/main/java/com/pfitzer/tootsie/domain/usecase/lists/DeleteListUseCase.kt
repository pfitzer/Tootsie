package com.pfitzer.tootsie.domain.usecase.lists

import com.pfitzer.tootsie.domain.repository.ListsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for deleting a list.
 */
class DeleteListUseCase @Inject constructor(
    private val listsRepository: ListsRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return listsRepository.deleteList(id)
    }
}
