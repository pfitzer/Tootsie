package com.pfitzer.tootsie.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pfitzer.tootsie.data.local.entities.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Account operations.
 * Provides methods for querying and managing cached accounts.
 */
@Dao
interface AccountDao {

    /**
     * Insert or replace an account.
     * Used when logging in or refreshing account data.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    /**
     * Insert multiple accounts.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    /**
     * Update an existing account.
     */
    @Update
    suspend fun updateAccount(account: AccountEntity)

    /**
     * Delete an account.
     * Used when logging out.
     */
    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    /**
     * Get account by ID.
     */
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: String): AccountEntity?

    /**
     * Get account by ID as Flow for reactive updates.
     */
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountByIdFlow(accountId: String): Flow<AccountEntity?>

    /**
     * Get the currently active account.
     * Only one account should have isActive = true at a time.
     */
    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveAccount(): AccountEntity?

    /**
     * Get the currently active account as Flow.
     */
    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    fun getActiveAccountFlow(): Flow<AccountEntity?>

    /**
     * Get all logged-in accounts.
     * Accounts with accessToken are considered logged in.
     */
    @Query("SELECT * FROM accounts WHERE accessToken IS NOT NULL ORDER BY lastSyncedAt DESC")
    suspend fun getAllLoggedInAccounts(): List<AccountEntity>

    /**
     * Get all logged-in accounts as Flow.
     */
    @Query("SELECT * FROM accounts WHERE accessToken IS NOT NULL ORDER BY lastSyncedAt DESC")
    fun getAllLoggedInAccountsFlow(): Flow<List<AccountEntity>>

    /**
     * Set an account as active and deactivate all others.
     * This should be done in a transaction.
     */
    @Query("UPDATE accounts SET isActive = CASE WHEN id = :accountId THEN 1 ELSE 0 END")
    suspend fun setActiveAccount(accountId: String)

    /**
     * Clear all accounts (logout all).
     */
    @Query("DELETE FROM accounts")
    suspend fun clearAllAccounts()

    /**
     * Get account count.
     */
    @Query("SELECT COUNT(*) FROM accounts WHERE accessToken IS NOT NULL")
    suspend fun getAccountCount(): Int
}
