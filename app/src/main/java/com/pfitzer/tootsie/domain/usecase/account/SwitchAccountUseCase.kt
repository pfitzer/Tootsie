package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import javax.inject.Inject

/**
 * Use case for switching between logged-in accounts.
 * Updates database, preferences, and providers atomically.
 */
class SwitchAccountUseCase @Inject constructor(
    private val accountManagementRepository: AccountManagementRepository
) {
    /**
     * Switch to a different account.
     * @param accountId ID of the account to switch to
     * @throws IllegalArgumentException if account does not exist
     */
    suspend operator fun invoke(accountId: String) {
        accountManagementRepository.switchAccount(accountId)
    }
}
