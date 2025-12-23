package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for getting account information.
 */
class GetAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Get account by ID.
     *
     * @param accountId Account ID
     * @return Account details
     */
    suspend operator fun invoke(accountId: String): Result<Account> {
        return accountRepository.getAccount(accountId)
    }
}
