package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.Announcement
import com.pfitzer.tootsie.util.Result

/**
 * Repository interface for announcement operations.
 */
interface AnnouncementRepository {
    /**
     * Get all active announcements from the server.
     *
     * @return Result containing list of announcements
     */
    suspend fun getAnnouncements(): Result<List<Announcement>>

    /**
     * Dismiss an announcement (mark as read).
     *
     * @param id Announcement ID
     * @return Result indicating success or failure
     */
    suspend fun dismissAnnouncement(id: String): Result<Unit>
}
