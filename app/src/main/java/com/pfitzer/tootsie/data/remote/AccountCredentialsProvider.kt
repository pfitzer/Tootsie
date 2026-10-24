package com.pfitzer.tootsie.data.remote

import com.pfitzer.tootsie.data.local.crypto.CredentialCipher
import com.pfitzer.tootsie.data.local.dao.AccountDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credentials of one specific account, for requests that must not use the active account
 * (push subscriptions, background polling). Passed to the API as explicit headers, which
 * AuthInterceptor and InstanceInterceptor leave untouched.
 */
data class AccountCredentials(
    /** Value for the Authorization header, e.g. "Bearer …" */
    val authorization: String,
    /** Instance host, e.g. "mastodon.social" */
    val instance: String
)

/**
 * Resolves [AccountCredentials] from the logged-in accounts stored in Room.
 */
@Singleton
class AccountCredentialsProvider @Inject constructor(
    private val accountDao: AccountDao,
    private val credentialCipher: CredentialCipher
) {

    /**
     * Credentials of [accountId], or null if the account is not logged in on this device.
     */
    suspend fun forAccount(accountId: String): AccountCredentials? = withContext(Dispatchers.IO) {
        val account = accountDao.getAccountById(accountId) ?: return@withContext null
        val token = account.accessToken?.let { credentialCipher.decrypt(it) } ?: return@withContext null
        val instance = account.instanceUrl ?: return@withContext null
        AccountCredentials(authorization = "Bearer $token", instance = instance)
    }
}
