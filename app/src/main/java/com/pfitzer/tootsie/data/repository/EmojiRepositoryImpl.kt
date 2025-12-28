package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.mapper.toDomain
import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.domain.model.CustomEmoji
import com.pfitzer.tootsie.domain.repository.EmojiRepository
import com.pfitzer.tootsie.util.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of EmojiRepository.
 * Fetches custom emojis from the Mastodon API.
 */
@Singleton
class EmojiRepositoryImpl @Inject constructor(
    private val mastodonApi: MastodonApiService
) : EmojiRepository {

    override suspend fun getCustomEmojis(): Result<List<CustomEmoji>> {
        return try {
            val emojisDto = mastodonApi.getCustomEmojis()
            val emojis = emojisDto.map { it.toDomain() }
            Result.Success(emojis)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch custom emojis")
        }
    }
}
