package com.pfitzer.tootsie.domain.usecase.auth

import com.pfitzer.tootsie.domain.model.AppCredentials
import com.pfitzer.tootsie.domain.repository.AuthRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for registering the Tootsie app with a Mastodon instance.
 * This is the first step in the OAuth2 authentication flow.
 *
 * The registration creates client credentials (client_id and client_secret)
 * that will be used for the OAuth authorization flow.
 */
class RegisterAppUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Registers the app with the specified Mastodon instance.
     *
     * @param instanceUrl The Mastodon instance URL (e.g., "mastodon.social")
     * @return Result containing AppCredentials on success, or error
     */
    suspend operator fun invoke(instanceUrl: String): Result<AppCredentials> {
        // Normalize instance URL (remove trailing slash, ensure https)
        val normalizedUrl = normalizeInstanceUrl(instanceUrl)

        return authRepository.registerApp(normalizedUrl)
    }

    /**
     * Normalizes the instance URL to ensure consistency.
     * - Removes http/https prefix if present
     * - Removes trailing slashes
     * - Removes www prefix if present
     */
    private fun normalizeInstanceUrl(url: String): String {
        return url.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removeSuffix("/")
    }
}
