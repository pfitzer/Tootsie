package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import com.pfitzer.tootsie.domain.repository.PushRepository
import com.pfitzer.tootsie.domain.usecase.push.DisablePushUseCase
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for removing an account from the app, with all its data on the device.
 * If removing the active account, automatically switches to another account
 * or returns true to indicate logout is needed.
 */
class RemoveAccountUseCase @Inject constructor(
    private val accountManagementRepository: AccountManagementRepository,
    private val pushRepository: PushRepository,
    private val disablePushUseCase: DisablePushUseCase
) {
    /**
     * Remove an account.
     * @param accountId ID of the account to remove
     * @return Boolean indicating if logout is needed (true if no accounts remain)
     * @throws IllegalArgumentException if account does not exist
     */
    suspend operator fun invoke(accountId: String): Boolean {
        // Unsubscribe push on the server and at the distributor while the account's
        // credentials still exist. Best effort: offline, the local data is removed anyway
        val pushConfig = (pushRepository.getPushConfig(accountId) as? Result.Success)?.data
        if (pushConfig?.enabled == true) {
            disablePushUseCase(accountId)
        }
        return accountManagementRepository.removeAccount(accountId)
    }
}
