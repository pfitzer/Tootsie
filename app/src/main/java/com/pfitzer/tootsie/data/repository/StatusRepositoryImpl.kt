package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.domain.model.Translation
import com.pfitzer.tootsie.domain.model.DeletedStatus
import com.pfitzer.tootsie.data.remote.StatusEditForm
import com.pfitzer.tootsie.domain.model.NewStatus
import com.pfitzer.tootsie.domain.model.StatusEditVersion
import com.pfitzer.tootsie.domain.model.StatusSource
import com.pfitzer.tootsie.data.local.dao.StatusDao
import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.mapper.toEntity
import com.pfitzer.tootsie.data.remote.AsyncRefreshHeader
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.StatusDto
import com.pfitzer.tootsie.domain.model.AsyncRefreshProgress
import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.domain.model.MediaMeta
import com.pfitzer.tootsie.domain.model.MediaType
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusInteractionStore
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
    private val statusDao: StatusDao,
    private val statusInteractionStore: StatusInteractionStore
) : StatusRepository {

    override suspend fun getStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.getStatus(statusId)
            // Not cached: the cache is scoped to timeline + owner account, and replacing a
            // cached row with an unscoped copy would drop it from the timeline cache
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
        pollConfig: com.pfitzer.tootsie.domain.model.PollConfig?,
        quotedStatusId: String?,
        language: String?
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
                pollHideTotals = pollConfig?.hideTotals,
                quotedStatusId = quotedStatusId,
                language = language
            )
            // Not cached here; it is cached with its timeline scope when the timeline loads
            Result.Success(statusDto.toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to post status")
        }
    }

    override suspend fun deleteStatus(statusId: String, deleteMedia: Boolean): Result<DeletedStatus> {
        return try {
            val statusDto = mastodonApi.deleteStatus(statusId, deleteMedia)
            // Remove from the timeline cache and from the lists loaded from the network
            statusDao.deleteStatusById(statusId)
            statusInteractionStore.markDeleted(statusId)
            Result.Success(DeletedStatus(status = statusDto.toDomain(), text = statusDto.text))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete status")
        }
    }

    override suspend fun favoriteStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.favoriteStatus(statusId)
            Result.Success(recordInteraction(statusDto.toDomain()))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to favorite status")
        }
    }

    override suspend fun unfavoriteStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.unfavoriteStatus(statusId)
            Result.Success(recordInteraction(statusDto.toDomain()))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unfavorite status")
        }
    }

    override suspend fun reblogStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.reblogStatus(statusId)
            Result.Success(recordInteraction(statusDto.toDomain()))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to reblog status")
        }
    }

    override suspend fun unreblogStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.unreblogStatus(statusId)
            Result.Success(recordInteraction(statusDto.toDomain()))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unreblog status")
        }
    }

    override suspend fun bookmarkStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.bookmarkStatus(statusId)
            Result.Success(recordInteraction(statusDto.toDomain()))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to bookmark status")
        }
    }

    override suspend fun unbookmarkStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.unbookmarkStatus(statusId)
            Result.Success(recordInteraction(statusDto.toDomain()))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unbookmark status")
        }
    }

    override suspend fun pinStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.pinStatus(statusId)
            Result.Success(recordInteraction(statusDto.toDomain()))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to pin status")
        }
    }

    override suspend fun unpinStatus(statusId: String): Result<Status> {
        return try {
            val statusDto = mastodonApi.unpinStatus(statusId)
            Result.Success(recordInteraction(statusDto.toDomain()))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unpin status")
        }
    }

    override suspend fun translateStatus(statusId: String, language: String): Result<Translation> {
        return try {
            val dto = mastodonApi.translateStatus(statusId, language)
            Result.Success(
                Translation(
                    content = dto.content,
                    spoilerText = dto.spoilerText,
                    pollOptions = dto.poll?.options.orEmpty().map { it.title },
                    mediaDescriptions = dto.mediaAttachments
                        .mapNotNull { media -> media.description?.let { media.id to it } }
                        .toMap(),
                    detectedSourceLanguage = dto.detectedSourceLanguage,
                    provider = dto.provider
                )
            )
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to translate status", e)
        }
    }

    override suspend fun getStatusSource(statusId: String): Result<StatusSource> {
        return try {
            val source = mastodonApi.getStatusSource(statusId)
            Result.Success(StatusSource(id = source.id, text = source.text, spoilerText = source.spoilerText))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load status source")
        }
    }

    override suspend fun editStatus(
        statusId: String,
        status: NewStatus,
        descriptions: Map<String, String>
    ): Result<Status> {
        return try {
            val edited = mastodonApi.editStatus(statusId, StatusEditForm.build(status, descriptions)).toDomain()
            // Shown in all lists right away; the timeline cache gets it with the next refresh
            statusInteractionStore.updateEdited(edited)
            Result.Success(edited)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to edit status")
        }
    }

    override suspend fun getStatusHistory(statusId: String): Result<List<StatusEditVersion>> {
        return try {
            val versions = mastodonApi.getStatusHistory(statusId).map { edit ->
                StatusEditVersion(
                    content = edit.content,
                    spoilerText = edit.spoilerText,
                    sensitive = edit.sensitive,
                    createdAt = edit.createdAt,
                    account = edit.account.toDomain(),
                    pollOptions = edit.poll?.options.orEmpty().map { it.title },
                    mediaAttachments = edit.mediaAttachments.map { it.toDomain() },
                    emojis = edit.emojis.map { it.toDomain() }
                )
            }
            Result.Success(versions.reversed())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load edit history")
        }
    }

    override suspend fun getMedia(id: String): Result<MediaAttachment?> {
        return try {
            val response = mastodonApi.getMedia(id)
            when {
                response.isSuccessful -> Result.Success(response.body()?.toDomain())
                // Cleaned up by the server, or attached to a status meanwhile
                response.code() == 404 -> Result.Success(null)
                else -> Result.Error("HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load media")
        }
    }

    override suspend fun updateMediaDescription(id: String, description: String): Result<MediaAttachment> {
        return try {
            Result.Success(mastodonApi.updateMedia(id, description).toDomain())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update media")
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
            val response = mastodonApi.getContext(statusId)
            val contextDto = response.body()
            if (!response.isSuccessful || contextDto == null) throw retrofit2.HttpException(response)
            val context = contextDto.toDomain().copy(
                asyncRefresh = AsyncRefreshHeader.parse(response.headers()[AsyncRefreshHeader.NAME])
            )
            Result.Success(context)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load conversation context")
        }
    }

    override suspend fun getAsyncRefresh(id: String): Result<AsyncRefreshProgress> {
        return try {
            val refresh = mastodonApi.getAsyncRefresh(id).asyncRefresh
            Result.Success(
                AsyncRefreshProgress(
                    finished = refresh.status == "finished",
                    resultCount = refresh.resultCount
                )
            )
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get async refresh state", e)
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

    /**
     * Publish the confirmed interaction state and update the cached row in place.
     */
    private suspend fun recordInteraction(status: Status): Status {
        statusInteractionStore.update(status)
        val target = status.reblog ?: status
        statusDao.updateInteractionState(
            statusId = target.id,
            favourited = target.favourited,
            favouritesCount = target.favouritesCount,
            reblogged = target.reblogged,
            reblogsCount = target.reblogsCount,
            bookmarked = target.bookmarked,
            pinned = target.pinned
        )
        return status
    }
}
