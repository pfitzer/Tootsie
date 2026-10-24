package com.pfitzer.tootsie.data.local.crypto

import android.util.Log
import com.pfitzer.tootsie.data.local.TootsieDatabase
import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CredentialMigration"

/**
 * One-time migration to [CredentialCipher]-encrypted credentials. Idempotent, runs on every start.
 *
 * 1. Encrypts plaintext access tokens of all accounts in Room.
 * 2. Restores the active account's token and instance URL into PreferencesManager if missing.
 *    They were previously kept in EncryptedSharedPreferences, which is no longer read;
 *    Room holds the same values for every logged-in account.
 * 3. Deletes the legacy EncryptedSharedPreferences file and master key.
 * 4. Compacts the database so the replaced plaintext tokens do not remain in free pages.
 *
 * Must run on Dispatchers.IO before the providers are initialized.
 */
@Singleton
class CredentialMigration @Inject constructor(
    private val database: TootsieDatabase,
    private val accountDao: AccountDao,
    private val preferencesManager: PreferencesManager,
    private val credentialCipher: CredentialCipher,
    private val legacySecureStorage: LegacySecureStorage
) {

    suspend fun migrate() {
        val encryptedCount = encryptAccountTokens()
        restoreActiveCredentials()

        try {
            legacySecureStorage.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete legacy secure storage", e)
        }

        if (encryptedCount > 0) {
            compactDatabase()
            Log.i(TAG, "Encrypted access tokens of $encryptedCount account(s)")
        }
    }

    private suspend fun encryptAccountTokens(): Int {
        val plaintextAccounts = accountDao.getAllLoggedInAccounts().filter { account ->
            account.accessToken?.let { !credentialCipher.isEncrypted(it) } == true
        }
        plaintextAccounts.forEach { account ->
            val token = requireNotNull(account.accessToken)
            accountDao.updateAccount(account.copy(accessToken = credentialCipher.encrypt(token)))
        }
        return plaintextAccounts.size
    }

    private suspend fun restoreActiveCredentials() {
        val hasToken = preferencesManager.getAccessToken() != null
        val hasInstanceUrl = preferencesManager.getInstanceUrl() != null
        if (hasToken && hasInstanceUrl) return

        val activeAccount = accountDao.getActiveAccount() ?: return
        if (!hasToken) {
            activeAccount.accessToken
                ?.let { credentialCipher.decrypt(it) }
                ?.let { preferencesManager.saveAccessToken(it) }
        }
        if (!hasInstanceUrl) {
            activeAccount.instanceUrl?.let { preferencesManager.saveInstanceUrl(it) }
        }
    }

    private fun compactDatabase() {
        val db = database.openHelper.writableDatabase
        db.execSQL("VACUUM")
        db.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
    }
}
