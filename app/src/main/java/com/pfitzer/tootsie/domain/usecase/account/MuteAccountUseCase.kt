package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Relationship
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for muting an account.
 */
class MuteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Mute an account.
     *
     * @param accountId Account ID to mute
     * @param notifications Whether to mute notifications (default true)
     * @param duration Duration of mute in seconds (0 = indefinite)
     * @return Updated relationship status
     */
    suspend operator fun invoke(
        accountId: String,
        notifications: Boolean = true,
        duration: Int = 0
    ): Result<Relationship> {
        return accountRepository.muteAccount(accountId, notifications, duration)
    }
}
