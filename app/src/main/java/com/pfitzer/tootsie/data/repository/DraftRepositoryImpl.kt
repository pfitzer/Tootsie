package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.local.dao.AccountDao
import com.pfitzer.tootsie.data.local.dao.DraftDao
import com.pfitzer.tootsie.data.local.entities.DraftEntity
import com.pfitzer.tootsie.domain.repository.DraftRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DraftRepository.
 * Manages draft posts stored locally in the database.
 * All operations are scoped to the active account.
 */
@Singleton
class DraftRepositoryImpl @Inject constructor(
    private val draftDao: DraftDao,
    private val accountDao: AccountDao
) : DraftRepository {

    override suspend fun saveDraft(draft: DraftEntity): Result<Long> {
        return try {
            // Get active account to ensure draft is associated with it
            val activeAccount = accountDao.getActiveAccount()
                ?: return Result.Error("No active account found")

            // Ensure draft has the correct accountId
            val draftWithAccountId = draft.copy(accountId = activeAccount.id)

            val draftId = if (draftWithAccountId.id == 0L) {
                // New draft - insert
                draftDao.insertDraft(draftWithAccountId)
            } else {
                // Existing draft - update
                draftDao.updateDraft(draftWithAccountId)
                draftWithAccountId.id
            }
            Result.Success(draftId)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to save draft")
        }
    }

    override fun getAllDrafts(): Flow<List<DraftEntity>> {
        // Reactively filter drafts by active account
        return accountDao.getActiveAccountFlow().flatMapLatest { activeAccount ->
            if (activeAccount != null) {
                draftDao.getAllDrafts(activeAccount.id)
            } else {
                flowOf(emptyList())
            }
        }
    }

    override suspend fun getDraft(draftId: Long): Result<DraftEntity?> {
        return try {
            val activeAccount = accountDao.getActiveAccount()
                ?: return Result.Error("No active account found")

            val draft = draftDao.getDraftById(activeAccount.id, draftId)
            Result.Success(draft)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get draft")
        }
    }

    override suspend fun deleteDraft(draftId: Long): Result<Unit> {
        return try {
            val activeAccount = accountDao.getActiveAccount()
                ?: return Result.Error("No active account found")

            draftDao.deleteDraft(activeAccount.id, draftId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete draft")
        }
    }

    override suspend fun deleteAllDrafts(): Result<Unit> {
        return try {
            val activeAccount = accountDao.getActiveAccount()
                ?: return Result.Error("No active account found")

            draftDao.deleteAllDrafts(activeAccount.id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete all drafts")
        }
    }

    override fun getDraftCount(): Flow<Int> {
        // Reactively count drafts for active account
        return accountDao.getActiveAccountFlow().flatMapLatest { activeAccount ->
            if (activeAccount != null) {
                draftDao.getDraftCount(activeAccount.id)
            } else {
                flowOf(0)
            }
        }
    }

    override suspend fun getMostRecentDraft(): Result<DraftEntity?> {
        return try {
            val activeAccount = accountDao.getActiveAccount()
                ?: return Result.Error("No active account found")

            val draft = draftDao.getMostRecentDraft(activeAccount.id)
            Result.Success(draft)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get recent draft")
        }
    }
}
