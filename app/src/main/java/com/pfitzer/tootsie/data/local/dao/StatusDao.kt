package com.pfitzer.tootsie.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pfitzer.tootsie.data.local.entities.StatusEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Status operations.
 * Provides methods for querying and managing cached statuses (posts).
 */
@Dao
interface StatusDao {

    /**
     * Insert a single status.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusEntity)

    /**
     * Insert multiple statuses.
     * Used when fetching timeline data.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatuses(statuses: List<StatusEntity>)

    /**
     * Update a status.
     * Used when interaction state changes (favorited, reblogged, etc.)
     */
    @Update
    suspend fun updateStatus(status: StatusEntity)

    /**
     * Get a status by ID.
     */
    @Query("SELECT * FROM statuses WHERE id = :statusId")
    suspend fun getStatusById(statusId: String): StatusEntity?

    /**
     * Get a status by ID as Flow.
     */
    @Query("SELECT * FROM statuses WHERE id = :statusId")
    fun getStatusByIdFlow(statusId: String): Flow<StatusEntity?>

    /**
     * Get statuses for a specific timeline type and owner account.
     * Returns PagingSource for use with Paging 3.
     */
    @Query("SELECT * FROM statuses WHERE timelineType = :timelineType AND ownerAccountId = :ownerAccountId ORDER BY createdAt DESC")
    fun getStatusesByTimelineType(timelineType: String, ownerAccountId: String): PagingSource<Int, StatusEntity>

    /**
     * Get statuses for a specific timeline and owner account as Flow.
     */
    /**
     * Update only the interaction columns of a cached status.
     * Keeps ownerAccountId and timelineType intact, unlike replacing the whole row.
     */
    @Query(
        "UPDATE statuses SET favourited = :favourited, favouritesCount = :favouritesCount, " +
            "reblogged = :reblogged, reblogsCount = :reblogsCount, bookmarked = :bookmarked, " +
            "pinned = :pinned WHERE id = :statusId"
    )
    suspend fun updateInteractionState(
        statusId: String,
        favourited: Boolean,
        favouritesCount: Int,
        reblogged: Boolean,
        reblogsCount: Int,
        bookmarked: Boolean,
        pinned: Boolean
    )

    @Query("SELECT * FROM statuses WHERE timelineType = :timelineType AND ownerAccountId = :ownerAccountId ORDER BY createdAt DESC LIMIT :limit")
    fun getStatusesFlow(timelineType: String, ownerAccountId: String, limit: Int = 20): Flow<List<StatusEntity>>

    /**
     * Get statuses by account ID.
     * Used for profile timeline.
     */
    @Query("SELECT * FROM statuses WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun getStatusesByAccountId(accountId: String): PagingSource<Int, StatusEntity>

    /**
     * Delete a status by ID.
     */
    @Query("DELETE FROM statuses WHERE id = :statusId")
    suspend fun deleteStatusById(statusId: String)

    /**
     * Delete all statuses for a specific timeline type and owner account.
     * Used when refreshing a timeline.
     */
    @Query("DELETE FROM statuses WHERE timelineType = :timelineType AND ownerAccountId = :ownerAccountId")
    suspend fun deleteStatusesByTimelineType(timelineType: String, ownerAccountId: String)

    /**
     * Delete statuses older than a certain timestamp.
     * Used for cache cleanup.
     */
    @Query("DELETE FROM statuses WHERE lastSyncedAt < :timestamp")
    suspend fun deleteOldStatuses(timestamp: Long)

    /**
     * Clear all statuses.
     */
    @Query("DELETE FROM statuses")
    suspend fun clearAllStatuses()

    /**
     * Update status interaction state.
     * Efficiently updates favorite/reblog/bookmark state without full replacement.
     */
    @Query("""
        UPDATE statuses
        SET favourited = :favourited,
            reblogged = :reblogged,
            bookmarked = :bookmarked,
            favouritesCount = :favouritesCount,
            reblogsCount = :reblogsCount
        WHERE id = :statusId
    """)
    suspend fun updateStatusInteraction(
        statusId: String,
        favourited: Boolean,
        reblogged: Boolean,
        bookmarked: Boolean,
        favouritesCount: Int,
        reblogsCount: Int
    )

    /**
     * Get count of statuses in timeline for a specific owner account.
     */
    @Query("SELECT COUNT(*) FROM statuses WHERE timelineType = :timelineType AND ownerAccountId = :ownerAccountId")
    suspend fun getStatusCount(timelineType: String, ownerAccountId: String): Int

    /**
     * Clear all statuses for a specific owner account.
     * Used when removing an account from the app.
     */
    @Query("DELETE FROM statuses WHERE ownerAccountId = :ownerAccountId")
    suspend fun clearStatusesForAccount(ownerAccountId: String)
}
