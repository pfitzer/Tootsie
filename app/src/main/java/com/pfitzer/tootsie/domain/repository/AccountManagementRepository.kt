package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.Account
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing multiple logged-in accounts.
 * Handles account switching, adding, and removing accounts.
 */
interface AccountManagementRepository {

    /**
     * Get all logged-in accounts.
     * @return List of all accounts stored in the database
     */
    suspend fun getAllLoggedInAccounts(): List<Account>

    /**
     * Get the currently active account.
     * @return Active account or null if no account is active
     */
    suspend fun getActiveAccount(): Account?

    /**
     * Observe the currently active account.
     * Emits whenever the active account changes.
     * @return Flow of active account (null if no account is active)
     */
    fun observeActiveAccount(): Flow<Account?>

    /**
     * Switch to a different account.
     * Updates database, PreferencesManager, and provider instances atomically.
     * @param accountId ID of the account to switch to
     * @throws IllegalArgumentException if account does not exist
     */
    suspend fun switchAccount(accountId: String)

    /**
     * Add a new account to the database.
     * Sets the new account as active.
     * @param account Account details
     * @param token Access token for the account
     * @param instanceUrl Instance URL for the account
     */
    suspend fun addAccount(account: Account, token: String, instanceUrl: String)

    /**
     * Remove an account from the database.
     * If removing the active account, automatically switches to another account
     * or logs out if no other accounts exist.
     * @param accountId ID of the account to remove
     * @return Boolean indicating if logout is needed (true if no accounts remain)
     */
    suspend fun removeAccount(accountId: String): Boolean
}
