package com.pfitzer.tootsie.domain.usecase.announcement

import com.pfitzer.tootsie.domain.repository.AnnouncementRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for dismissing (marking as read) an announcement.
 */
class DismissAnnouncementUseCase @Inject constructor(
    private val announcementRepository: AnnouncementRepository
) {
    suspend operator fun invoke(announcementId: String): Result<Unit> {
        return announcementRepository.dismissAnnouncement(announcementId)
    }
}
