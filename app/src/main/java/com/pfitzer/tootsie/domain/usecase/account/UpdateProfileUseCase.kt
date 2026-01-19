package com.pfitzer.tootsie.domain.usecase.account

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for updating the authenticated user's profile.
 */
class UpdateProfileUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Update account profile.
     *
     * @param displayName New display name
     * @param note New bio/note
     * @return Updated account details
     */
    suspend operator fun invoke(
        displayName: String? = null,
        note: String? = null
    ): Result<Account> {
        return accountRepository.updateProfile(displayName, note)
    }
}
