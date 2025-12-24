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
     * Get all drafts ordered by most recently updated.
     */
    @Query("SELECT * FROM drafts ORDER BY updatedAt DESC")
    fun getAllDrafts(): Flow<List<DraftEntity>>

    /**
     * Get a draft by ID.
     */
    @Query("SELECT * FROM drafts WHERE id = :draftId")
    suspend fun getDraftById(draftId: Long): DraftEntity?

    /**
     * Get a draft by ID as Flow.
     */
    @Query("SELECT * FROM drafts WHERE id = :draftId")
    fun getDraftByIdFlow(draftId: Long): Flow<DraftEntity?>

    /**
     * Delete a specific draft.
     */
    @Query("DELETE FROM drafts WHERE id = :draftId")
    suspend fun deleteDraft(draftId: Long)

    /**
     * Delete all drafts.
     */
    @Query("DELETE FROM drafts")
    suspend fun deleteAllDrafts()

    /**
     * Get count of all drafts.
     */
    @Query("SELECT COUNT(*) FROM drafts")
    fun getDraftCount(): Flow<Int>

    /**
     * Get the most recent draft (for auto-resuming).
     */
    @Query("SELECT * FROM drafts ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getMostRecentDraft(): DraftEntity?
}
