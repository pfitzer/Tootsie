package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Announcement
import com.pfitzer.tootsie.domain.repository.AnnouncementRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AnnouncementRepository.
 * Fetches and manages server announcements via the Mastodon API.
 */
@Singleton
class AnnouncementRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : AnnouncementRepository {

    override suspend fun getAnnouncements(): Result<List<Announcement>> {
        return try {
            val announcementsDto = mastodonApi.getAnnouncements()
            val announcements = announcementsDto.map { it.toDomain() }
            Result.Success(announcements)
        } catch (e: Exception) {
            Result.Error(e.message)
        }
    }

    override suspend fun dismissAnnouncement(id: String): Result<Unit> {
        return try {
            mastodonApi.dismissAnnouncement(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message)
        }
    }
}
