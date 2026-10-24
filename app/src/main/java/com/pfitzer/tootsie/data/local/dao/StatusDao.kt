package com.pfitzer.tootsie.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pfitzer.tootsie.data.local.entities.StatusEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for cached timeline statuses.
 *
 * Mastodon status IDs are numeric strings that grow over time, and timelines are sorted
 * by them. Ordering by length first sorts them numerically without a cast, and also works
 * for the fixed-length IDs some other servers use.
 */
@Dao
interface StatusDao {

    /**
     * Insert or replace statuses of a timeline page.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatuses(statuses: List<StatusEntity>)

    /**
     * Get a cached status by ID (any timeline or owner).
     */
    @Query("SELECT * FROM statuses WHERE id = :statusId LIMIT 1")
    suspend fun getStatusById(statusId: String): StatusEntity?

    /**
     * Get a cached status by ID as Flow (any timeline or owner).
     */
    @Query("SELECT * FROM statuses WHERE id = :statusId LIMIT 1")
    fun getStatusByIdFlow(statusId: String): Flow<StatusEntity?>

    /**
     * Paged statuses of a timeline, newest first. Source of truth for the timeline UI.
     */
    @Query(
        "SELECT * FROM statuses WHERE timelineType = :timelineType AND ownerAccountId = :ownerAccountId " +
            "ORDER BY LENGTH(id) DESC, id DESC"
    )
    fun pagingSource(timelineType: String, ownerAccountId: String): PagingSource<Int, StatusEntity>

    /**
     * ID of the oldest cached status of a timeline; the `max_id` for loading the next page.
     */
    @Query(
        "SELECT id FROM statuses WHERE timelineType = :timelineType AND ownerAccountId = :ownerAccountId " +
            "ORDER BY LENGTH(id) ASC, id ASC LIMIT 1"
    )
    suspend fun getOldestStatusId(timelineType: String, ownerAccountId: String): String?

    /**
     * When a timeline was last written, or null if nothing is cached.
     */
    @Query("SELECT MAX(lastSyncedAt) FROM statuses WHERE timelineType = :timelineType AND ownerAccountId = :ownerAccountId")
    suspend fun getLastSyncTime(timelineType: String, ownerAccountId: String): Long?

    /**
     * Update the interaction state of a status in every cached row that shows it,
     * including boosts of it.
     */
    @Query(
        "UPDATE statuses SET favourited = :favourited, favouritesCount = :favouritesCount, " +
            "reblogged = :reblogged, reblogsCount = :reblogsCount, bookmarked = :bookmarked, " +
            "pinned = :pinned WHERE contentId = :statusId"
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

    /**
     * Delete a status and all boosts of it from every cached timeline.
     */
    @Query("DELETE FROM statuses WHERE id = :statusId OR contentId = :statusId")
    suspend fun deleteStatusById(statusId: String)

    /**
     * Delete all statuses of a timeline and owner account.
     * Used when the timeline is refreshed.
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
     * Clear all statuses for a specific owner account.
     * Used when removing an account from the app.
     */
    @Query("DELETE FROM statuses WHERE ownerAccountId = :ownerAccountId")
    suspend fun clearStatusesForAccount(ownerAccountId: String)
}
