package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.mapper.toEntity
import com.pfitzer.tootsie.data.remote.api.AuthApiService
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.AccessToken
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.model.AppCredentials
import com.pfitzer.tootsie.domain.repository.AuthRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository.
 * Handles authentication operations with Mastodon API and local storage.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val mastodonApiService: MastodonApiService,
    private val accountDao: AccountDao,
    private val preferencesManager: PreferencesManager
) : AuthRepository {

    override suspend fun registerApp(instanceUrl: String): Result<AppCredentials> {
        return try {
            // Save instance URL for future API calls
            preferencesManager.saveInstanceUrl(instanceUrl)

            // Register app with the instance
            // Use deep link redirect URI for OAuth callback
            val redirectUri = "tootsie://oauth"
            val response = authApiService.registerApp(
                clientName = "Tootsie",
                redirectUris = redirectUri,
                scopes = "read write follow push",
                website = null
            )

            Result.Success(response.toDomain())
        } catch (e: Exception) {
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

            // Save access token
            preferencesManager.saveAccessToken(accessToken.accessToken)

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
