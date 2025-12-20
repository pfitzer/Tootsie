package com.pfitzer.tootsie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pfitzer.tootsie.data.local.entities.TimelineRemoteKeys

/**
 * Data Access Object for Timeline Remote Keys.
 * Used by Paging 3 RemoteMediator to track pagination state.
 */
@Dao
interface TimelineRemoteKeysDao {

    /**
     * Insert remote keys for a status.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: TimelineRemoteKeys)

    /**
     * Insert multiple remote keys.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeys(keys: List<TimelineRemoteKeys>)

    /**
     * Get remote keys for a specific status.
     */
    @Query("SELECT * FROM timeline_remote_keys WHERE statusId = :statusId AND timelineType = :timelineType")
    suspend fun getRemoteKeys(statusId: String, timelineType: String): TimelineRemoteKeys?

    /**
     * Delete all remote keys for a specific timeline.
     * Used when clearing/refreshing timeline cache.
     */
    @Query("DELETE FROM timeline_remote_keys WHERE timelineType = :timelineType")
    suspend fun clearRemoteKeys(timelineType: String)

    /**
     * Delete all remote keys.
     */
    @Query("DELETE FROM timeline_remote_keys")
    suspend fun clearAllRemoteKeys()

    /**
     * Get the creation time of the oldest key for a timeline.
     * Used to determine if cache is stale.
     */
    @Query("SELECT lastRefreshed FROM timeline_remote_keys WHERE timelineType = :timelineType ORDER BY lastRefreshed ASC LIMIT 1")
    suspend fun getOldestRefreshTime(timelineType: String): Long?
}
