package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.local.dao.StatusDao
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.mapper.toEntity
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StatusRepository.
 * Manages status operations with both local database and remote API.
 */
@Singleton
class StatusRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService,
    private val statusDao: StatusDao
) : StatusRepository {

    override suspend fun getStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.getStatus(statusId)
            // Cache the status
            statusDao.insertStatus(statusDto.toEntity())
            Result.Success(statusDto.toDomain())
        } catch (e: Exception) {
            // Try local cache if network fails
            val cachedStatus = statusDao.getStatusById(statusId)
            if (cachedStatus != null) {
                Result.Success(cachedStatus.toDomain())
            } else {
                Result.Error(e.message ?: "Failed to fetch status")
            }
        }
    }

    override fun getStatusFlow(statusId: String): Flow<Status?> {
        return statusDao.getStatusByIdFlow(statusId)
            .map { it?.toDomain() }
    }

    override suspend fun postStatus(
        content: String,
        visibility: String,
        inReplyToId: String?,
        mediaIds: List<String>?,
        sensitive: Boolean,
        spoilerText: String?
    ): Result<Status> {
        return try {
            val statusDto = mastodonApi.postStatus(
                status = content,
                visibility = visibility,
                inReplyToId = inReplyToId,
                mediaIds = mediaIds,
                sensitive = sensitive,
                spoilerText = spoilerText
            )
            // Cache the posted status
            statusDao.insertStatus(statusDto.toEntity())
            Result.Success(statusDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to post status")
        }
    }

    override suspend fun deleteStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.deleteStatus(statusId)
            // Remove from local cache
            statusDao.deleteStatusById(statusId)
            Result.Success(statusDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete status")
        }
    }

    override suspend fun favoriteStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.favoriteStatus(statusId)
            // Update local cache
            statusDao.updateStatus(statusDto.toEntity())
            Result.Success(statusDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to favorite status")
        }
    }

    override suspend fun unfavoriteStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.unfavoriteStatus(statusId)
            // Update local cache
            statusDao.updateStatus(statusDto.toEntity())
            Result.Success(statusDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unfavorite status")
        }
    }

    override suspend fun reblogStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.reblogStatus(statusId)
            // Update local cache
            statusDao.updateStatus(statusDto.toEntity())
            Result.Success(statusDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to reblog status")
        }
    }

    override suspend fun unreblogStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.unreblogStatus(statusId)
            // Update local cache
            statusDao.updateStatus(statusDto.toEntity())
            Result.Success(statusDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unreblog status")
        }
    }

    override suspend fun bookmarkStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.bookmarkStatus(statusId)
            // Update local cache
            statusDao.updateStatus(statusDto.toEntity())
            Result.Success(statusDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to bookmark status")
        }
    }

    override suspend fun unbookmarkStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.unbookmarkStatus(statusId)
            // Update local cache
            statusDao.updateStatus(statusDto.toEntity())
            Result.Success(statusDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unbookmark status")
        }
    }

    override suspend fun getStatusContext(statusId: String): Result<com.pfitzer.tootsie.domain.model.Context> {
        return try {
            val contextDto = mastodonApi.getContext(statusId)
            Result.Success(contextDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load conversation context")
        }
    }
}
