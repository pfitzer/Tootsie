package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.remote.LinkHeader
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.toDomain
import com.pfitzer.tootsie.domain.model.FollowedTag
import com.pfitzer.tootsie.domain.repository.FollowedTagsRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject
import retrofit2.HttpException
import javax.inject.Singleton

/**
 * Implementation of FollowedTagsRepository.
 * Fetches and manages followed hashtags via the Mastodon API.
 */
@Singleton
class FollowedTagsRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : FollowedTagsRepository {

    /**
     * All followed tags. The API returns at most 100 per page and pages via the Link
     * header, so the pages are loaded until there is no next one.
     */
    override suspend fun getFollowedTags(): Result<List<FollowedTag>> {
        return try {
            val tags = mutableListOf<FollowedTag>()
            var maxId: String? = null
            var pages = 0
            do {
                val response = mastodonApi.getFollowedTags(limit = PAGE_SIZE, maxId = maxId)
                if (!response.isSuccessful) throw HttpException(response)
                tags += response.body().orEmpty().map { it.toDomain() }
                maxId = LinkHeader.nextMaxId(response.headers()["Link"])
                pages++
            } while (maxId != null && pages < MAX_PAGES)
            Result.Success(tags.distinctBy { it.name.lowercase() })
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

    private companion object {
        const val PAGE_SIZE = 100

        // Safety limit against endless paging (10,000 tags)
        const val MAX_PAGES = 100
    }
}
