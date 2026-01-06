package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.toDomain
import com.pfitzer.tootsie.domain.model.FollowedTag
import com.pfitzer.tootsie.domain.repository.FollowedTagsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FollowedTagsRepository.
 * Fetches and manages followed hashtags via the Mastodon API.
 */
@Singleton
class FollowedTagsRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : FollowedTagsRepository {

    override suspend fun getFollowedTags(): Result<List<FollowedTag>> {
        return try {
            val tagsDto = mastodonApi.getFollowedTags()
            val tags = tagsDto.map { it.toDomain() }
            Result.Success(tags)
        } catch (e: Exception) {
            Result.Error(e.message)
        }
    }

    override suspend fun followTag(tagName: String): Result<FollowedTag> {
        return try {
            val tagDto = mastodonApi.followTag(tagName)
            val tag = tagDto.toDomain()
            Result.Success(tag)
        } catch (e: Exception) {
            Result.Error(e.message)
        }
    }

    override suspend fun unfollowTag(tagName: String): Result<FollowedTag> {
        return try {
            val tagDto = mastodonApi.unfollowTag(tagName)
            val tag = tagDto.toDomain()
            Result.Success(tag)
        } catch (e: Exception) {
            Result.Error(e.message)
        }
    }
}
