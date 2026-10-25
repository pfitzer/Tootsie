package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.LinkHeader
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.NewStatus
import com.pfitzer.tootsie.domain.model.ScheduledStatus
import com.pfitzer.tootsie.domain.repository.ScheduledStatusRepository
import com.pfitzer.tootsie.util.Result
import retrofit2.HttpException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ScheduledStatusRepository.
 */
@Singleton
class ScheduledStatusRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : ScheduledStatusRepository {

    override suspend fun scheduleStatus(status: NewStatus, scheduledAt: Instant): Result<ScheduledStatus> {
        return try {
            val dto = mastodonApi.scheduleStatus(
                status = status.content,
                visibility = status.visibility,
                inReplyToId = status.inReplyToId,
                mediaIds = status.mediaIds,
                sensitive = status.sensitive,
                spoilerText = status.spoilerText,
                pollOptions = status.pollConfig?.options,
                pollExpiresIn = status.pollConfig?.expiresIn,
                pollMultiple = status.pollConfig?.multiple,
                pollHideTotals = status.pollConfig?.hideTotals,
                quotedStatusId = status.quotedStatusId,
                language = status.language,
                scheduledAt = scheduledAt.toString()
            )
            Result.Success(dto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to schedule status")
        }
    }

    override suspend fun getScheduledStatuses(): Result<List<ScheduledStatus>> {
        return try {
            val statuses = mutableListOf<ScheduledStatus>()
            var maxId: String? = null
            // Mastodon allows at most 300 scheduled statuses, so this ends after a few pages
            do {
                val response = mastodonApi.getScheduledStatuses(maxId = maxId, limit = PAGE_SIZE)
                if (!response.isSuccessful) throw HttpException(response)
                statuses += response.body().orEmpty().map { it.toDomain() }
                maxId = LinkHeader.nextMaxId(response.headers()["Link"])
            } while (maxId != null)
            Result.Success(statuses.sortedBy { it.scheduledAt })
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load scheduled statuses")
        }
    }

    override suspend fun getScheduledStatus(id: String): Result<ScheduledStatus> {
        return try {
            Result.Success(mastodonApi.getScheduledStatus(id).toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load scheduled status")
        }
    }

    override suspend fun reschedule(id: String, scheduledAt: Instant): Result<ScheduledStatus> {
        return try {
            Result.Success(mastodonApi.updateScheduledStatus(id, scheduledAt.toString()).toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to reschedule status")
        }
    }

    override suspend fun deleteScheduledStatus(id: String): Result<Unit> {
        return try {
            mastodonApi.deleteScheduledStatus(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete scheduled status")
        }
    }

    private companion object {
        /** Maximum page size of the scheduled statuses API. */
        const val PAGE_SIZE = 40
    }
}
