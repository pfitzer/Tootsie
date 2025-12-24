package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.data.local.entities.DraftEntity
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for draft operations.
 * Drafts are stored locally only and are not synced to the server.
 */
interface DraftRepository {

    /**
     * Save a new draft or update an existing one.
     *
     * @param draft Draft to save
     * @return ID of the saved draft
     */
    suspend fun saveDraft(draft: DraftEntity): Result<Long>

    /**
     * Get all drafts as a Flow.
     * Automatically updates when drafts change.
     */
    fun getAllDrafts(): Flow<List<DraftEntity>>

    /**
     * Get a specific draft by ID.
     *
     * @param draftId Draft ID
     * @return Draft entity or null if not found
     */
    suspend fun getDraft(draftId: Long): Result<DraftEntity?>

    /**
     * Delete a specific draft.
     *
     * @param draftId Draft ID
     */
    suspend fun deleteDraft(draftId: Long): Result<Unit>

    /**
     * Delete all drafts.
     */
    suspend fun deleteAllDrafts(): Result<Unit>

    /**
     * Get count of drafts as a Flow.
     */
    fun getDraftCount(): Flow<Int>

    /**
     * Get the most recent draft (for auto-resuming).
     */
    suspend fun getMostRecentDraft(): Result<DraftEntity?>
}
