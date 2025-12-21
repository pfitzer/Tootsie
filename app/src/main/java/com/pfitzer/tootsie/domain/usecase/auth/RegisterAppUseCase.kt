package com.pfitzer.tootsie.domain.usecase.auth

import android.util.Log
import com.pfitzer.tootsie.domain.model.AppCredentials
import com.pfitzer.tootsie.domain.repository.AuthRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

private const val TAG = "RegisterAppUseCase"

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
        Log.d(TAG, "invoke() called with instanceUrl: $instanceUrl")

        // Normalize instance URL (remove trailing slash, ensure https)
        val normalizedUrl = normalizeInstanceUrl(instanceUrl)
        Log.d(TAG, "Normalized URL: $normalizedUrl")

        Log.d(TAG, "Calling authRepository.registerApp()")
        val result = authRepository.registerApp(normalizedUrl)

        when (result) {
            is Result.Success -> Log.d(TAG, "registerApp() returned Success")
            is Result.Error -> Log.e(TAG, "registerApp() returned Error: ${result.message}")
            is Result.Loading -> Log.d(TAG, "registerApp() returned Loading")
        }

        return result
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
