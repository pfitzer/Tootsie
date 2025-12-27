package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import javax.inject.Inject

/**
 * Use case for removing an account from the app.
 * If removing the active account, automatically switches to another account
 * or returns true to indicate logout is needed.
 */
class RemoveAccountUseCase @Inject constructor(
    private val accountManagementRepository: AccountManagementRepository
) {
    /**
     * Remove an account.
     * @param accountId ID of the account to remove
     * @return Boolean indicating if logout is needed (true if no accounts remain)
     * @throws IllegalArgumentException if account does not exist
     */
    suspend operator fun invoke(accountId: String): Boolean {
        return accountManagementRepository.removeAccount(accountId)
    }
}
