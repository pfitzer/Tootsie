package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for unmuting an account.
 */
class UnmuteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Unmute an account.
     *
     * @param accountId Account ID to unmute
     * @return Updated relationship status
     */
    suspend operator fun invoke(accountId: String): Result<Relationship> {
        return accountRepository.unmuteAccount(accountId)
    }
}
