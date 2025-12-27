package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import javax.inject.Inject

/**
 * Use case for getting all logged-in accounts.
 * Used to display the account list in settings.
 */
class GetAllAccountsUseCase @Inject constructor(
    private val accountManagementRepository: AccountManagementRepository
) {
    suspend operator fun invoke(): List<Account> {
        return accountManagementRepository.getAllLoggedInAccounts()
    }
}
