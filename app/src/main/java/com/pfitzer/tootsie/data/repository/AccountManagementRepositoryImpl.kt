package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.local.crypto.CredentialCipher
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.data.local.entities.AccountEntity
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.dto.AccountDto
import com.pfitzer.tootsie.data.remote.interceptors.InstanceUrlProvider
import com.pfitzer.tootsie.data.remote.interceptors.TokenProvider
import com.pfitzer.tootsie.domain.model.Account
import com.pfitzer.tootsie.domain.repository.AccountManagementRepository
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AccountManagementRepository.
 * Manages multiple logged-in accounts with atomic switching.
 */
@Singleton
class AccountManagementRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val preferencesManager: PreferencesManager,
    private val tokenProvider: TokenProvider,
    private val instanceUrlProvider: InstanceUrlProvider,
    private val credentialCipher: CredentialCipher,
    private val statusInteractionStore: StatusInteractionStore
) : AccountManagementRepository {

    override suspend fun getAllLoggedInAccounts(): List<Account> {
        return accountDao.getAllLoggedInAccounts().map { it.toDomain() }
    }

    override suspend fun getActiveAccount(): Account? {
        return accountDao.getActiveAccount()?.toDomain()
    }

    override fun observeActiveAccount(): Flow<Account?> {
        return accountDao.getActiveAccountFlow().map { it?.toDomain() }
    }

    override suspend fun switchAccount(accountId: String) {
        // 1. Get target account from database
        val targetAccount = accountDao.getAccountById(accountId)
            ?: throw IllegalArgumentException("Account with ID $accountId not found")

        // 2. Atomically update isActive flags in database
        accountDao.setActiveAccount(accountId)
        statusInteractionStore.clear()

        // 3. Update PreferencesManager
        val token = decryptToken(targetAccount)
        preferencesManager.saveActiveAccountId(accountId)
        token?.let { preferencesManager.saveAccessToken(it) }
        targetAccount.instanceUrl?.let { preferencesManager.saveInstanceUrl(it) }

        // 4. Update providers for OkHttp interceptors
        tokenProvider.setToken(token)
        targetAccount.instanceUrl?.let { instanceUrlProvider.setInstanceUrl(it) }
    }

    override suspend fun addAccount(account: Account, token: String, instanceUrl: String) {
        // Create AccountEntity from domain model (token encrypted at rest)
        val encryptedToken = withContext(Dispatchers.IO) { credentialCipher.encrypt(token) }
        val accountEntity = AccountEntity(
            id = account.id,
            username = account.username,
            acct = account.acct,
            displayName = account.displayName,
            note = account.note,
            url = account.url,
            avatar = account.avatar,
            header = account.header,
            locked = account.locked,
            followersCount = account.followersCount,
            followingCount = account.followingCount,
            statusesCount = account.statusesCount,
            createdAt = account.createdAt,
            bot = account.bot,
            accessToken = encryptedToken,
            instanceUrl = instanceUrl,
            isActive = true, // New account becomes active
            lastSyncedAt = System.currentTimeMillis()
        )

        // Insert into database (setActiveAccount will handle deactivating other accounts)
        accountDao.insertAccount(accountEntity)
        accountDao.setActiveAccount(account.id)
        statusInteractionStore.clear()

        // Update PreferencesManager
        preferencesManager.saveActiveAccountId(account.id)
        preferencesManager.saveAccessToken(token)
        preferencesManager.saveInstanceUrl(instanceUrl)

        // Update providers
        tokenProvider.setToken(token)
        instanceUrlProvider.setInstanceUrl(instanceUrl)
    }

    override suspend fun removeAccount(accountId: String): Boolean {
        // Get account to check if it's active
        val accountToRemove = accountDao.getAccountById(accountId)
            ?: throw IllegalArgumentException("Account with ID $accountId not found")

        val isActive = accountToRemove.isActive
        if (isActive) statusInteractionStore.clear()

        // Delete the account from database
        accountDao.deleteAccount(accountToRemove)

        // If we removed the active account, switch to another or logout
        if (isActive) {
            val remainingAccounts = accountDao.getAllLoggedInAccounts()

            if (remainingAccounts.isNotEmpty()) {
                // Switch to the first remaining account
                val newActiveAccount = remainingAccounts.first()
                accountDao.setActiveAccount(newActiveAccount.id)

                // Update PreferencesManager
                val token = decryptToken(newActiveAccount)
                preferencesManager.saveActiveAccountId(newActiveAccount.id)
                token?.let { preferencesManager.saveAccessToken(it) }
                newActiveAccount.instanceUrl?.let { preferencesManager.saveInstanceUrl(it) }

                // Update providers
                tokenProvider.setToken(token)
                newActiveAccount.instanceUrl?.let { instanceUrlProvider.setInstanceUrl(it) }

                return false // No logout needed
            } else {
                // No accounts remain - clear everything
                preferencesManager.clearActiveAccountId()
                preferencesManager.clearAccessToken()
                preferencesManager.clearInstanceUrl()
                tokenProvider.clearToken()

                return true // Logout needed
            }
        }

        return false // Removed non-active account, no logout needed
    }

    private suspend fun decryptToken(account: AccountEntity): String? =
        account.accessToken?.let { withContext(Dispatchers.IO) { credentialCipher.decrypt(it) } }
}
