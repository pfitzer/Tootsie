package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import javax.inject.Inject

/**
 * Use case to get the currently active account (one-time read).
 */
class GetActiveAccountUseCase @Inject constructor(
    private val accountManagementRepository: AccountManagementRepository
) {
    suspend operator fun invoke(): Account? {
        return accountManagementRepository.getActiveAccount()
    }
}
