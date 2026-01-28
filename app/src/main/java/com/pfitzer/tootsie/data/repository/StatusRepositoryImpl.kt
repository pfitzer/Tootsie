package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.local.dao.StatusDao
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.mapper.toEntity
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.StatusDto
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.domain.model.MediaMeta
import com.pfitzer.tootsie.domain.model.MediaType
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
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
        spoilerText: String?,
        pollConfig: com.pfitzer.tootsie.domain.model.PollConfig?
    ): Result<Status> {
        return try {
            val statusDto = mastodonApi.postStatus(
                status = content,
                visibility = visibility,
                inReplyToId = inReplyToId,
                mediaIds = mediaIds,
                sensitive = sensitive,
                spoilerText = spoilerText,
                pollOptions = pollConfig?.options,
                pollExpiresIn = pollConfig?.expiresIn,
                pollMultiple = pollConfig?.multiple,
                pollHideTotals = pollConfig?.hideTotals
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

    override suspend fun pinStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.pinStatus(statusId)
            // Update local cache
            statusDao.updateStatus(statusDto.toEntity())
            Result.Success(statusDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to pin status")
        }
    }

    override suspend fun unpinStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.unpinStatus(statusId)
            // Update local cache
            statusDao.updateStatus(statusDto.toEntity())
            Result.Success(statusDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unpin status")
        }
    }

    override suspend fun uploadMedia(
        file: File,
        description: String?,
        focus: Pair<Float, Float>?
    ): Result<MediaAttachment> {
        return try {
            // Determine MIME type from file extension
            val mimeType = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "mp4" -> "video/mp4"
                "webm" -> "video/webm"
                "m4v" -> "video/mp4"
                "mov" -> "video/quicktime"
                "mp3" -> "audio/mpeg"
                "ogg" -> "audio/ogg"
                "wav" -> "audio/wav"
                else -> "application/octet-stream"
            }

            // Create multipart file part
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            // Create optional parts
            val descriptionPart = description?.toRequestBody("text/plain".toMediaTypeOrNull())
            val focusPart = focus?.let { (x, y) ->
                "$x,$y".toRequestBody("text/plain".toMediaTypeOrNull())
            }

            // Upload media
            val mediaDto = mastodonApi.uploadMedia(
                file = filePart,
                description = descriptionPart,
                focus = focusPart
            )

            // Map to domain model manually to avoid overload resolution issues
            val mediaAttachment = MediaAttachment(
                id = mediaDto.id,
                type = when (mediaDto.type.lowercase()) {
                    "image" -> MediaType.IMAGE
                    "video" -> MediaType.VIDEO
                    "gifv" -> MediaType.GIFV
                    "audio" -> MediaType.AUDIO
                    else -> MediaType.UNKNOWN
                },
                url = mediaDto.url,
                previewUrl = mediaDto.previewUrl,
                description = mediaDto.description,
                blurhash = mediaDto.blurhash,
                meta = mediaDto.meta?.original?.let {
                    MediaMeta(
                        width = it.width,
                        height = it.height,
                        duration = it.duration,
                        aspect = it.aspect
                    )
                }
            )

            Result.Success(mediaAttachment)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to upload media")
        }
    }

    override suspend fun getStatusContext(statusId: String): Result<com.pfitzer.tootsie.domain.model.Context> {
        return try {
            val contextDto = mastodonApi.getContext(statusId)
            val context: com.pfitzer.tootsie.domain.model.Context = contextDto.toDomain()
            Result.Success(context)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load conversation context")
        }
    }

    override suspend fun voteOnPoll(pollId: String, choices: List<Int>): Result<com.pfitzer.tootsie.domain.model.Poll> {
        return try {
            val pollDto = mastodonApi.voteOnPoll(pollId, choices)
            Result.Success(pollDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to vote on poll")
        }
    }
}
