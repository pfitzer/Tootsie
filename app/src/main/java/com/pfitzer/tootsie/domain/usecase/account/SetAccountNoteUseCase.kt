package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for setting the private note about an account.
 */
class SetAccountNoteUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Set the private note about an account, only visible to the user.
     *
     * @param accountId Account ID
     * @param note Note text; empty removes it
     * @return Updated relationship status
     */
    suspend operator fun invoke(accountId: String, note: String): Result<Relationship> {
        return accountRepository.setNote(accountId, note)
    }
}
