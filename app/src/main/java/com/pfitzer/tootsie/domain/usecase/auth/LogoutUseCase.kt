package com.pfitzer.tootsie.domain.usecase.auth

import com.pfitzer.tootsie.domain.repository.AuthRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for logging out the current user.
 * Clears all authentication data and returns to login screen.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Logs out the current user by clearing all auth data.
     *
     * @return Result<Unit> indicating success or failure
     */
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.logout()
    }
}
