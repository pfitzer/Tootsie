package com.pfitzer.tootsie.data.repository

import android.util.Log
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.mapper.toEntity
import com.pfitzer.tootsie.data.remote.api.AuthApiService
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.interceptors.InstanceUrlProvider
import com.pfitzer.tootsie.data.remote.interceptors.TokenProvider
import com.pfitzer.tootsie.domain.model.AccessToken
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.AppCredentials
import com.pfitzer.tootsie.domain.repository.AuthRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthRepositoryImpl"

/**
 * Implementation of AuthRepository.
 * Handles authentication operations with Mastodon API and local storage.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val mastodonApiService: MastodonApiService,
    private val accountDao: AccountDao,
    private val preferencesManager: PreferencesManager,
    private val instanceUrlProvider: InstanceUrlProvider,
    private val tokenProvider: TokenProvider
) : AuthRepository {

    override suspend fun registerApp(instanceUrl: String): Result<AppCredentials> {
        Log.d(TAG, "registerApp() called with instanceUrl: $instanceUrl")
        return try {
            // Save instance URL for future API calls
            Log.d(TAG, "Saving instance URL to preferences")
            preferencesManager.saveInstanceUrl(instanceUrl)

            // CRITICAL: Set instance URL in provider for OkHttp interceptor
            // This must be synchronous to avoid deadlocks in the interceptor
            instanceUrlProvider.setInstanceUrl(instanceUrl)
            Log.d(TAG, "Instance URL set in provider: $instanceUrl")

            // Register app with the instance
            // Use deep link redirect URI for OAuth callback
            val redirectUri = "tootsie://oauth"
            Log.d(TAG, "Making API call to register app with redirect URI: $redirectUri")

            val response = authApiService.registerApp(
                clientName = "Tootsie",
                redirectUris = redirectUri,
                scopes = "read write follow push",
                website = null
            )

            Log.d(TAG, "API call successful, received client_id: ${response.clientId}")
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "registerApp() failed with exception", e)
            Result.Error(
                message = "Failed to register app: ${e.message}",
                exception = e
            )
        }
    }

    override suspend fun getAccessToken(
        instanceUrl: String,
        clientId: String,
        clientSecret: String,
        code: String
    ): Result<AccessToken> {
        return try {
            // Ensure instance URL is set
            preferencesManager.saveInstanceUrl(instanceUrl)
            instanceUrlProvider.setInstanceUrl(instanceUrl)

            // Exchange authorization code for access token
            // Must match the redirect URI used during registration
            val redirectUri = "tootsie://oauth"
            val response = authApiService.getAccessToken(
                clientId = clientId,
                clientSecret = clientSecret,
                redirectUri = redirectUri,
                code = code,
                grantType = "authorization_code",
                scope = "read write follow push"
            )

            val accessToken = response.toDomain()

            // Save access token to DataStore
            preferencesManager.saveAccessToken(accessToken.accessToken)

            // CRITICAL: Set token in provider for OkHttp interceptor
            // This must be synchronous to avoid deadlocks in the interceptor
            tokenProvider.setToken(accessToken.accessToken)
            Log.d(TAG, "Access token set in provider")

            Result.Success(accessToken)
        } catch (e: Exception) {
            Result.Error(
                message = "Failed to get access token: ${e.message}",
                exception = e
            )
        }
    }

    override suspend fun verifyCredentials(): Result<Account> {
        return try {
            // Fetch current user's account from API
            val accountDto = mastodonApiService.verifyCredentials()
            val account = accountDto.toDomain()

            // Get current instance URL and access token
            val instanceUrl = preferencesManager.getInstanceUrl()
            val accessToken = preferencesManager.getAccessToken()

            // Save account to database
            val accountEntity = accountDto.toEntity(
                accessToken = accessToken,
                instanceUrl = instanceUrl,
                isActive = true
            )
            accountDao.insertAccount(accountEntity)

            // Set as active account
            accountDao.setActiveAccount(accountEntity.id)
            preferencesManager.saveActiveAccountId(accountEntity.id)

            Result.Success(account)
        } catch (e: Exception) {
            Result.Error(
                message = "Failed to verify credentials: ${e.message}",
                exception = e
            )
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            // Get active account
            val activeAccount = accountDao.getActiveAccount()

            // Clear preferences
            preferencesManager.clearAuthData()

            // Clear token from provider
            tokenProvider.clearToken()
            Log.d(TAG, "Token cleared from provider")

            // Delete account from database if exists
            activeAccount?.let {
                accountDao.deleteAccount(it)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                message = "Failed to logout: ${e.message}",
                exception = e
            )
        }
    }

    override fun getActiveAccount(): Flow<Account?> {
        return accountDao.getActiveAccountFlow()
            .map { it?.toDomain() }
    }

    override fun isLoggedIn(): Flow<Boolean> {
        return preferencesManager.accessToken.map { token ->
            !token.isNullOrEmpty()
        }
    }
}
