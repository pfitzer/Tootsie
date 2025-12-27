package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import javax.inject.Inject

/**
 * Use case for adding a new account to the app.
 * Called after successful login/authentication.
 * The new account becomes the active account.
 */
class AddAccountUseCase @Inject constructor(
    private val accountManagementRepository: AccountManagementRepository
) {
    /**
     * Add a new account and make it active.
     * @param account Account details from the API
     * @param token Access token for the account
     * @param instanceUrl Instance URL for the account
     */
    suspend operator fun invoke(account: Account, token: String, instanceUrl: String) {
        accountManagementRepository.addAccount(account, token, instanceUrl)
    }
}
