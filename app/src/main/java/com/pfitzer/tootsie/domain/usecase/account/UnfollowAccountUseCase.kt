package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for unfollowing an account.
 */
class UnfollowAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Unfollow an account.
     *
     * @param accountId Account ID to unfollow
     * @return Updated relationship status
     */
    suspend operator fun invoke(accountId: String): Result<Relationship> {
        return accountRepository.unfollowAccount(accountId)
    }
}
