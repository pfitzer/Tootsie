package com.pfitzer.tootsie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pfitzer.tootsie.data.local.entities.FilterEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for filters.
 */
@Dao
interface FilterDao {
    /**
     * Get all filters for an account.
     */
    @Query("SELECT * FROM filters WHERE ownerAccountId = :accountId")
    fun getFilters(accountId: String): Flow<List<FilterEntity>>

    /**
     * Get filters for a specific context.
     * Uses LIKE to check if the context is in the JSON array.
     */
    @Query("SELECT * FROM filters WHERE ownerAccountId = :accountId AND contextJson LIKE '%' || :context || '%'")
    fun getFiltersForContext(accountId: String, context: String): Flow<List<FilterEntity>>

    /**
     * Get a single filter by ID.
     */
    @Query("SELECT * FROM filters WHERE id = :id")
    suspend fun getFilter(id: String): FilterEntity?

    /**
     * Insert or replace filters.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilters(filters: List<FilterEntity>)

    /**
     * Insert or replace a single filter.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(filter: FilterEntity)

    /**
     * Delete all filters for an account.
     */
    @Query("DELETE FROM filters WHERE ownerAccountId = :accountId")
    suspend fun deleteAllFilters(accountId: String)

    /**
     * Delete a specific filter.
     */
    @Query("DELETE FROM filters WHERE id = :id")
    suspend fun deleteFilter(id: String)
}
