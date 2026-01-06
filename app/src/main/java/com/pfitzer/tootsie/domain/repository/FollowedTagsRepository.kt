package com.pfitzer.tootsie.domain.repository

import com.pfitzer.tootsie.domain.model.FollowedTag
import com.pfitzer.tootsie.util.Result

/**
 * Repository interface for followed tags operations.
 */
interface FollowedTagsRepository {
    /**
     * Get all hashtags followed by the authenticated user.
     *
     * @return Result containing list of followed tags
     */
    suspend fun getFollowedTags(): Result<List<FollowedTag>>

    /**
     * Follow a hashtag.
     *
     * @param tagName Tag name (without the # symbol)
     * @return Result containing the updated tag
     */
    suspend fun followTag(tagName: String): Result<FollowedTag>

    /**
     * Unfollow a hashtag.
     *
     * @param tagName Tag name (without the # symbol)
     * @return Result containing the updated tag
     */
    suspend fun unfollowTag(tagName: String): Result<FollowedTag>
}
