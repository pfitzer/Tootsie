package com.pfitzer.tootsie.domain.usecase.auth

import com.pfitzer.tootsie.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for checking if a user is currently logged in.
 * Returns a Flow that emits updates when login status changes.
 */
class CheckLoginStatusUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Returns a Flow that emits the current login status.
     * Emits true if user is logged in, false otherwise.
     *
     * @return Flow<Boolean> representing login state
     */
    operator fun invoke(): Flow<Boolean> {
        return authRepository.isLoggedIn()
    }
}
