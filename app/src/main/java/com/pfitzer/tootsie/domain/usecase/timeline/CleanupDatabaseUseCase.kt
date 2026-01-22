package com.pfitzer.tootsie.domain.usecase.timeline

import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import com.pfitzer.tootsie.domain.repository.TimelineRepository
import javax.inject.Inject

/**
 * Use case to cleanup old statuses from the local database.
 * Reads retention period from PreferencesManager and triggers cleanup.
 */
class CleanupDatabaseUseCase @Inject constructor(
    private val timelineRepository: TimelineRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke() {
        val retentionHours = preferencesManager.getDatabaseRetentionHours().toIntOrNull() ?: 24
        timelineRepository.cleanupOldStatuses(retentionHours)
    }
}
