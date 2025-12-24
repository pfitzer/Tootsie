package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.local.dao.DraftDao
import com.pfitzer.tootsie.data.local.entities.DraftEntity
import com.pfitzer.tootsie.domain.repository.DraftRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DraftRepository.
 * Manages draft posts stored locally in the database.
 */
@Singleton
class DraftRepositoryImpl @Inject constructor(
    private val draftDao: DraftDao
) : DraftRepository {

    override suspend fun saveDraft(draft: DraftEntity): Result<Long> {
        return try {
            val draftId = if (draft.id == 0L) {
                // New draft - insert
                draftDao.insertDraft(draft)
            } else {
                // Existing draft - update
                draftDao.updateDraft(draft)
                draft.id
            }
            Result.Success(draftId)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to save draft")
        }
    }

    override fun getAllDrafts(): Flow<List<DraftEntity>> {
        return draftDao.getAllDrafts()
    }

    override suspend fun getDraft(draftId: Long): Result<DraftEntity?> {
        return try {
            val draft = draftDao.getDraftById(draftId)
            Result.Success(draft)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get draft")
        }
    }

    override suspend fun deleteDraft(draftId: Long): Result<Unit> {
        return try {
            draftDao.deleteDraft(draftId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete draft")
        }
    }

    override suspend fun deleteAllDrafts(): Result<Unit> {
        return try {
            draftDao.deleteAllDrafts()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete all drafts")
        }
    }

    override fun getDraftCount(): Flow<Int> {
        return draftDao.getDraftCount()
    }

    override suspend fun getMostRecentDraft(): Result<DraftEntity?> {
        return try {
            val draft = draftDao.getMostRecentDraft()
            Result.Success(draft)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get recent draft")
        }
    }
}
