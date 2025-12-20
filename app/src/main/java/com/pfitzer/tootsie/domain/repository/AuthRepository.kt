package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.AccessToken
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.AppCredentials
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations.
 * Defines the contract for auth-related data operations.
 */
interface AuthRepository {

    /**
     * Register app with a Mastodon instance.
     * Required before initiating OAuth flow.
     *
     * @param instanceUrl The Mastodon instance URL (e.g., "mastodon.social")
     * @return AppCredentials containing client_id and client_secret
     */
    suspend fun registerApp(instanceUrl: String): Result<AppCredentials>

    /**
     * Exchange authorization code for access token.
     *
     * @param instanceUrl The Mastodon instance URL
     * @param clientId Application client ID
     * @param clientSecret Application client secret
     * @param code Authorization code from OAuth flow
     * @return AccessToken for API requests
     */
    suspend fun getAccessToken(
        instanceUrl: String,
        clientId: String,
        clientSecret: String,
        code: String
    ): Result<AccessToken>

    /**
     * Verify credentials and get current user account.
     * Uses stored access token.
     *
     * @return Current user's Account information
     */
    suspend fun verifyCredentials(): Result<Account>

    /**
     * Logout current user.
     * Clears access token and auth data.
     */
    suspend fun logout(): Result<Unit>

    /**
     * Get currently active account as Flow.
     * Emits updates when active account changes.
     */
    fun getActiveAccount(): Flow<Account?>

    /**
     * Check if user is logged in.
     */
    fun isLoggedIn(): Flow<Boolean>
}
