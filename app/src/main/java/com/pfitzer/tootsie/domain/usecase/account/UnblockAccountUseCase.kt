package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for unblocking an account.
 */
class UnblockAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Unblock an account.
     *
     * @param accountId Account ID to unblock
     * @return Updated relationship status
     */
    suspend operator fun invoke(accountId: String): Result<Relationship> {
        return accountRepository.unblockAccount(accountId)
    }
}
