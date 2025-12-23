package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for getting relationship status with an account.
 */
class GetRelationshipUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Get relationship with an account.
     *
     * @param accountId Account ID
     * @return Relationship status
     */
    suspend operator fun invoke(accountId: String): Result<Relationship> {
        return accountRepository.getRelationship(accountId)
    }
}
