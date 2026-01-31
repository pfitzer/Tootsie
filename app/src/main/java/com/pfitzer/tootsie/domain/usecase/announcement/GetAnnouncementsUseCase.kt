package com.pfitzer.tootsie.domain.usecase.announcement

import com.pfitzer.tootsie.domain.model.Announcement
import com.pfitzer.tootsie.domain.repository.AnnouncementRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for getting server announcements.
 */
class GetAnnouncementsUseCase @Inject constructor(
    private val announcementRepository: AnnouncementRepository
) {
    suspend operator fun invoke(): Result<List<Announcement>> {
        return announcementRepository.getAnnouncements()
    }
}
