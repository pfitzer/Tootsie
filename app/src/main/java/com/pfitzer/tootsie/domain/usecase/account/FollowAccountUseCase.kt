package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for following an account.
 */
class FollowAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Follow an account.
     *
     * @param accountId Account ID to follow
     * @param notify Whether to enable notifications for this account
     * @param showReblogs Whether to show boosts from this account
     * @return Updated relationship status
     */
    suspend operator fun invoke(
        accountId: String,
        notify: Boolean = false,
        showReblogs: Boolean = true
    ): Result<Relationship> {
        return accountRepository.followAccount(accountId, notify, showReblogs)
    }
}
