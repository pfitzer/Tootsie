package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for blocking an account.
 */
class BlockAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Block an account.
     *
     * @param accountId Account ID to block
     * @return Updated relationship status
     */
    suspend operator fun invoke(accountId: String): Result<Relationship> {
        return accountRepository.blockAccount(accountId)
    }
}
