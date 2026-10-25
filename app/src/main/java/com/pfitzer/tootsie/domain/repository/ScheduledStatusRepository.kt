package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.NewStatus
import com.pfitzer.tootsie.domain.model.ScheduledStatus
import com.pfitzer.tootsie.util.Result
import java.time.Instant

/**
 * Repository for scheduled statuses.
 */
interface ScheduledStatusRepository {

    /**
     * Schedule a status to be published at [scheduledAt] (at least 5 minutes ahead).
     */
    suspend fun scheduleStatus(status: NewStatus, scheduledAt: Instant): Result<ScheduledStatus>

    /**
     * All scheduled statuses (all pages), soonest first.
     */
    suspend fun getScheduledStatuses(): Result<List<ScheduledStatus>>

    /**
     * Get a scheduled status by ID.
     */
    suspend fun getScheduledStatus(id: String): Result<ScheduledStatus>

    /**
     * Move a scheduled status to another time.
     */
    suspend fun reschedule(id: String, scheduledAt: Instant): Result<ScheduledStatus>

    /**
     * Delete a scheduled status, so it is not published.
     */
    suspend fun deleteScheduledStatus(id: String): Result<Unit>
}
