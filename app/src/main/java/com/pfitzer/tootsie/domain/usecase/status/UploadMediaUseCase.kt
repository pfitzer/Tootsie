package com.pfitzer.tootsie.domain.usecase.status

import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.domain.repository.StatusRepository
import com.pfitzer.tootsie.util.Result
import java.io.File
import javax.inject.Inject

/**
 * Use case for uploading media (images, videos, etc.) to Mastodon.
 *
 * @param repository Status repository for API operations
 */
class UploadMediaUseCase @Inject constructor(
    private val repository: StatusRepository
) {
    /**
     * Upload a media file to Mastodon.
     *
     * @param file The file to upload
     * @param description Alt text description for accessibility
     * @param focus Optional focus point (x,y coordinates from -1 to 1)
     * @return Result containing the uploaded MediaAttachment
     */
    suspend operator fun invoke(
        file: File,
        description: String? = null,
        focus: Pair<Float, Float>? = null
    ): Result<MediaAttachment> {
        return repository.uploadMedia(file, description, focus)
    }
}
