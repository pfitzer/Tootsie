package com.pfitzer.tootsie.domain.usecase.push

import com.pfitzer.tootsie.domain.model.AccountPushConfig
import com.pfitzer.tootsie.domain.repository.PushRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for getting push configuration.
 */
class GetPushConfigUseCase @Inject constructor(
    private val pushRepository: PushRepository
) {
    /**
     * Get push configuration for an account.
     *
     * @param accountId Account identifier
     * @return Result with AccountPushConfig
     */
    suspend operator fun invoke(accountId: String): Result<AccountPushConfig> {
        return pushRepository.getPushConfig(accountId)
    }
}
