package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the currently active account.
 */
class ObserveActiveAccountUseCase @Inject constructor(
    private val accountManagementRepository: AccountManagementRepository
) {
    operator fun invoke(): Flow<Account?> {
        return accountManagementRepository.observeActiveAccount()
    }
}
