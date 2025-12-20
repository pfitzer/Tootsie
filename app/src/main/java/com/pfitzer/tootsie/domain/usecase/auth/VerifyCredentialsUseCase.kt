package com.pfitzer.tootsie.domain.usecase.auth

import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AuthRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for verifying user credentials and fetching account information.
 * This is the final step of the OAuth flow, confirming the token works.
 */
class VerifyCredentialsUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Verifies the stored access token and retrieves the user's account.
     *
     * @return Result containing the authenticated Account
     */
    suspend operator fun invoke(): Result<Account> {
        return authRepository.verifyCredentials()
    }
}
