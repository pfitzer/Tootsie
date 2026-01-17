package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.MediaAttachment
import com.pfitzer.tootsie.domain.model.Status
import com.pfitzer.tootsie.util.Result
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for status (post) operations.
 * Defines the contract for interacting with statuses.
 */
interface StatusRepository {

    /**
     * Get a status by ID.
     *
     * @param statusId Status ID
     * @return Status details
     */
    suspend fun getStatus(statusId: String): Result<Status>

    /**
     * Get a status by ID as Flow.
     * Updates automatically when status changes.
     */
    fun getStatusFlow(statusId: String): Flow<Status?>

    /**
     * Post a new status.
     *
     * @param content Text content
     * @param visibility Visibility level (public, unlisted, private, direct)
     * @param inReplyToId ID of status being replied to (optional)
     * @param mediaIds List of media attachment IDs (optional)
     * @param sensitive Whether media is sensitive (optional)
     * @param spoilerText Content warning text (optional)
     * @param pollConfig Configuration for a poll (optional)
     * @return Posted status
     */
    suspend fun postStatus(
        content: String,
        visibility: String = "public",
        inReplyToId: String? = null,
        mediaIds: List<String>? = null,
        sensitive: Boolean = false,
        spoilerText: String? = null,
        pollConfig: com.pfitzer.tootsie.domain.model.PollConfig? = null
    ): Result<Status>

    /**
     * Delete a status.
     *
     * @param statusId Status ID to delete
     */
    suspend fun deleteStatus(statusId: String): Result<Status>

    /**
     * Favorite (like) a status.
     *
     * @param statusId Status ID
     * @return Updated status
     */
    suspend fun favoriteStatus(statusId: String): Result<Status>

    /**
     * Unfavorite a status.
     *
     * @param statusId Status ID
     * @return Updated status
     */
    suspend fun unfavoriteStatus(statusId: String): Result<Status>

    /**
     * Reblog (boost) a status.
     *
     * @param statusId Status ID
     * @return Updated status
     */
    suspend fun reblogStatus(statusId: String): Result<Status>

    /**
     * Unreblog a status.
     *
     * @param statusId Status ID
     * @return Updated status
     */
    suspend fun unreblogStatus(statusId: String): Result<Status>

    /**
     * Bookmark a status.
     *
     * @param statusId Status ID
     * @return Updated status
     */
    suspend fun bookmarkStatus(statusId: String): Result<Status>

    /**
     * Unbookmark a status.
     *
     * @param statusId Status ID
     * @return Updated status
     */
    suspend fun unbookmarkStatus(statusId: String): Result<Status>

    /**
     * Upload a media file (image, video, audio).
     *
     * @param file The file to upload
     * @param description Alt text description for accessibility (optional)
     * @param focus Focus point for cropping (x,y coordinates from -1 to 1, optional)
     * @return Uploaded MediaAttachment with ID for attaching to posts
     */
    suspend fun uploadMedia(
        file: File,
        description: String? = null,
        focus: Pair<Float, Float>? = null
    ): Result<MediaAttachment>

    /**
     * Get conversation context for a status.
     * Returns ancestors (posts before) and descendants (replies).
     *
     * @param statusId Status ID
     * @return Context with ancestors and descendants
     */
    suspend fun getStatusContext(statusId: String): Result<com.pfitzer.tootsie.domain.model.Context>

    /**
     * Vote on a poll.
     *
     * @param pollId Poll ID
     * @param choices List of option indices to vote for
     * @return Updated status containing the updated poll
     */
    suspend fun voteOnPoll(pollId: String, choices: List<Int>): Result<com.pfitzer.tootsie.domain.model.Poll>
}
