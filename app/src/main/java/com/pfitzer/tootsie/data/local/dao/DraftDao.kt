package com.pfitzer.tootsie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pfitzer.tootsie.data.local.entities.DraftEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Draft operations.
 * Provides methods for managing draft posts.
 */
@Dao
interface DraftDao {

    /**
     * Insert a new draft.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftEntity): Long

    /**
     * Update an existing draft.
     */
    @Update
    suspend fun updateDraft(draft: DraftEntity)

    /**
     * Get all drafts for a specific account, ordered by most recently updated.
     */
    @Query("SELECT * FROM drafts WHERE accountId = :accountId ORDER BY updatedAt DESC")
    fun getAllDrafts(accountId: String): Flow<List<DraftEntity>>

    /**
     * Get a draft by ID for a specific account.
     * Ensures the draft belongs to the specified account.
     */
    @Query("SELECT * FROM drafts WHERE id = :draftId AND accountId = :accountId")
    suspend fun getDraftById(accountId: String, draftId: Long): DraftEntity?

    /**
     * Get a draft by ID as Flow for a specific account.
     */
    @Query("SELECT * FROM drafts WHERE id = :draftId AND accountId = :accountId")
    fun getDraftByIdFlow(accountId: String, draftId: Long): Flow<DraftEntity?>

    /**
     * Delete a specific draft for a specific account.
     * Ensures the draft belongs to the specified account.
     */
    @Query("DELETE FROM drafts WHERE id = :draftId AND accountId = :accountId")
    suspend fun deleteDraft(accountId: String, draftId: Long)

    /**
     * Delete all drafts for a specific account.
     */
    @Query("DELETE FROM drafts WHERE accountId = :accountId")
    suspend fun deleteAllDrafts(accountId: String)

    /**
     * Get count of drafts for a specific account.
     */
    @Query("SELECT COUNT(*) FROM drafts WHERE accountId = :accountId")
    fun getDraftCount(accountId: String): Flow<Int>

    /**
     * Get the most recent draft for a specific account (for auto-resuming).
     */
    @Query("SELECT * FROM drafts WHERE accountId = :accountId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getMostRecentDraft(accountId: String): DraftEntity?
}
