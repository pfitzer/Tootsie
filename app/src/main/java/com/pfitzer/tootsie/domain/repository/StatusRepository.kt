package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.Translation
import com.pfitzer.tootsie.domain.model.DeletedStatus
import com.pfitzer.tootsie.domain.model.NewStatus
import com.pfitzer.tootsie.domain.model.StatusEditVersion
import com.pfitzer.tootsie.domain.model.StatusSource
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
        pollConfig: com.pfitzer.tootsie.domain.model.PollConfig? = null,
        quotedStatusId: String? = null,
        language: String? = null
    ): Result<Status>

    /**
     * Delete an own status. It disappears from all lists right away.
     *
     * @param statusId Status ID to delete
     * @param deleteMedia Delete the media right away; false keeps it for a redraft
     */
    suspend fun deleteStatus(statusId: String, deleteMedia: Boolean): Result<DeletedStatus>

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
     * Pin a status to profile.
     * Only works for own statuses.
     *
     * @param statusId Status ID
     * @return Updated status
     */
    suspend fun pinStatus(statusId: String): Result<Status>

    /**
     * Unpin a status from profile.
     *
     * @param statusId Status ID
     * @return Updated status
     */
    suspend fun unpinStatus(statusId: String): Result<Status>

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
     * Translate a status with the server's translation service.
     *
     * @param language ISO 639 language to translate into
     */
    suspend fun translateStatus(statusId: String, language: String): Result<Translation>

    /**
     * Get the plain text source of an own status, for editing.
     */
    suspend fun getStatusSource(statusId: String): Result<StatusSource>

    /**
     * Edit an own status.
     *
     * @param descriptions Alt text per media ID; attachments without an entry keep theirs
     */
    suspend fun editStatus(
        statusId: String,
        status: NewStatus,
        descriptions: Map<String, String>
    ): Result<Status>

    /**
     * Get the versions of an edited status, newest first.
     */
    suspend fun getStatusHistory(statusId: String): Result<List<StatusEditVersion>>

    /**
     * Get an uploaded media attachment that is not attached to a status yet.
     *
     * @return The attachment, or null if it no longer exists on the server
     */
    suspend fun getMedia(id: String): Result<MediaAttachment?>

    /**
     * Set the alt text of an uploaded media attachment before it is posted.
     */
    suspend fun updateMediaDescription(id: String, description: String): Result<MediaAttachment>

    /**
     * Get conversation context for a status.
     * Returns ancestors (posts before) and descendants (replies).
     *
     * @param statusId Status ID
     * @return Context with ancestors and descendants
     */
    suspend fun getStatusContext(statusId: String): Result<com.pfitzer.tootsie.domain.model.Context>

    /**
     * Get the state of a server background job (see Context.asyncRefresh).
     */
    suspend fun getAsyncRefresh(id: String): Result<com.pfitzer.tootsie.domain.model.AsyncRefreshProgress>

    /**
     * Vote on a poll.
     *
     * @param pollId Poll ID
     * @param choices List of option indices to vote for
     * @return Updated status containing the updated poll
     */
    suspend fun voteOnPoll(pollId: String, choices: List<Int>): Result<com.pfitzer.tootsie.domain.model.Poll>
}
