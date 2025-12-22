package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject

/**
 * Use case for posting (creating) a new status.
 * Handles text-only posts with optional reply context and visibility settings.
 */
class PostStatusUseCase @Inject constructor(
    private val statusRepository: StatusRepository
) {
    /**
     * Posts a new status with the given parameters.
     *
     * @param content The text content of the status
     * @param visibility Visibility level string (public, unlisted, private, direct)
     * @param inReplyToId Optional ID of status being replied to
     * @param sensitive Whether content should be marked as sensitive
     * @param spoilerText Optional content warning text
     * @return Result containing the newly created Status
     */
    suspend operator fun invoke(
        content: String,
        visibility: String = "public",
        inReplyToId: String? = null,
        sensitive: Boolean = false,
        spoilerText: String? = null
    ): Result<Status> {
        return statusRepository.postStatus(
            content = content,
            visibility = visibility,
            inReplyToId = inReplyToId,
            mediaIds = null, // Text-only for Phase 2a
            sensitive = sensitive,
            spoilerText = spoilerText
        )
    }
}
